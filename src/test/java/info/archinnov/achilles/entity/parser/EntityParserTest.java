package info.archinnov.achilles.entity.parser;

import static info.archinnov.achilles.entity.manager.ThriftEntityManagerFactoryImpl.configurableCLPolicyTL;
import static info.archinnov.achilles.entity.metadata.PropertyType.EXTERNAL_JOIN_WIDE_MAP;
import static info.archinnov.achilles.entity.metadata.PropertyType.EXTERNAL_WIDE_MAP;
import static info.archinnov.achilles.entity.metadata.PropertyType.JOIN_SIMPLE;
import static info.archinnov.achilles.entity.metadata.PropertyType.JOIN_WIDE_MAP;
import static info.archinnov.achilles.entity.metadata.PropertyType.SIMPLE;
import static info.archinnov.achilles.entity.metadata.PropertyType.WIDE_MAP;
import static info.archinnov.achilles.entity.parser.EntityParser.consistencyLevelsTL;
import static info.archinnov.achilles.serializer.SerializerUtils.LONG_SRZ;
import static info.archinnov.achilles.serializer.SerializerUtils.STRING_SRZ;
import static javax.persistence.CascadeType.ALL;
import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import info.archinnov.achilles.columnFamily.ColumnFamilyCreator;
import info.archinnov.achilles.columnFamily.ColumnFamilyHelper;
import info.archinnov.achilles.dao.AchillesConfigurableConsistencyLevelPolicy;
import info.archinnov.achilles.dao.CounterDao;
import info.archinnov.achilles.dao.GenericCompositeDao;
import info.archinnov.achilles.entity.manager.ThriftEntityManagerFactoryImpl;
import info.archinnov.achilles.entity.metadata.CounterProperties;
import info.archinnov.achilles.entity.metadata.EntityMeta;
import info.archinnov.achilles.entity.metadata.ExternalWideMapProperties;
import info.archinnov.achilles.entity.metadata.JoinProperties;
import info.archinnov.achilles.entity.metadata.PropertyMeta;
import info.archinnov.achilles.entity.metadata.PropertyType;
import info.archinnov.achilles.entity.type.ConsistencyLevel;
import info.archinnov.achilles.exception.BeanMappingException;
import info.archinnov.achilles.json.ObjectMapperFactory;
import info.archinnov.achilles.serializer.SerializerUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.persistence.CascadeType;

import me.prettyprint.hector.api.HConsistencyLevel;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;

import parser.entity.Bean;
import parser.entity.BeanWithColumnFamilyName;
import parser.entity.BeanWithDuplicatedColumnName;
import parser.entity.BeanWithDuplicatedJoinColumnName;
import parser.entity.BeanWithExternalJoinWideMap;
import parser.entity.BeanWithExternalWideMap;
import parser.entity.BeanWithJoinColumnAsWideMap;
import parser.entity.BeanWithNoColumn;
import parser.entity.BeanWithNoId;
import parser.entity.BeanWithNotSerializableId;
import parser.entity.BeanWithSimpleCounter;
import parser.entity.BeanWithWideMapCounter;
import parser.entity.ChildBean;
import parser.entity.ColumnFamilyBean;
import parser.entity.ColumnFamilyBeanWithJoinEntity;
import parser.entity.ColumnFamilyBeanWithTwoColumns;
import parser.entity.ColumnFamilyBeanWithWrongColumnType;
import parser.entity.UserBean;

/**
 * EntityParserTest
 * 
 * @author DuyHai DOAN
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class EntityParserTest
{
	private EntityParser parser;

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@Mock
	private Keyspace keyspace;

	@Mock
	private CounterDao counterDao;

	@Mock
	private Map<Class<?>, EntityMeta<?>> entityMetaMap;

	private Map<PropertyMeta<?, ?>, Class<?>> joinPropertyMetaToBeFilled = new HashMap<PropertyMeta<?, ?>, Class<?>>();

	@Mock
	private ColumnFamilyCreator columnFamilyCreator;

	private ObjectMapper objectMapper = new ObjectMapper();

	@Before
	public void setUp()
	{
		ObjectMapperFactory factory = new ObjectMapperFactory()
		{
			@Override
			public <T> ObjectMapper getMapper(Class<T> type)
			{
				return objectMapper;
			}
		};
		parser = new EntityParser(factory);
		joinPropertyMetaToBeFilled.clear();
		ThriftEntityManagerFactoryImpl.counterDaoTL.set(counterDao);
		ThriftEntityManagerFactoryImpl.joinPropertyMetaToBeFilledTL.set(joinPropertyMetaToBeFilled);
		ThriftEntityManagerFactoryImpl.configurableCLPolicyTL
				.set(new AchillesConfigurableConsistencyLevelPolicy());
	}

	@SuppressWarnings(
	{
			"unchecked",
			"rawtypes"
	})
	@Test
	public void should_parse_entity() throws Exception
	{

		EntityMeta<?> meta = parser.parseEntity(keyspace, Bean.class);

		assertThat(meta.getClassName()).isEqualTo("parser.entity.Bean");
		assertThat(meta.getColumnFamilyName()).isEqualTo("Bean");
		assertThat(meta.getSerialVersionUID()).isEqualTo(1L);
		assertThat((Class) meta.getIdMeta().getValueClass()).isEqualTo(Long.class);
		assertThat(meta.getIdMeta().getPropertyName()).isEqualTo("id");
		assertThat(meta.getIdMeta().getValueSerializer().getComparatorType()).isEqualTo(
				LONG_SRZ.getComparatorType());
		assertThat((Serializer<Long>) meta.getIdSerializer()).isEqualTo(LONG_SRZ);
		assertThat(meta.getPropertyMetas()).hasSize(7);

		PropertyMeta<?, ?> name = meta.getPropertyMetas().get("name");
		PropertyMeta<?, ?> age = meta.getPropertyMetas().get("age_in_year");
		PropertyMeta<Void, String> friends = (PropertyMeta<Void, String>) meta.getPropertyMetas()
				.get("friends");
		PropertyMeta<Void, String> followers = (PropertyMeta<Void, String>) meta.getPropertyMetas()
				.get("followers");
		PropertyMeta<Integer, String> preferences = (PropertyMeta<Integer, String>) meta
				.getPropertyMetas().get("preferences");

		PropertyMeta<Void, UserBean> creator = (PropertyMeta<Void, UserBean>) meta
				.getPropertyMetas().get("creator");
		PropertyMeta<String, UserBean> linkedUsers = (PropertyMeta<String, UserBean>) meta
				.getPropertyMetas().get("linked_users");

		assertThat(name).isNotNull();
		assertThat(age).isNotNull();
		assertThat(friends).isNotNull();
		assertThat(followers).isNotNull();
		assertThat(preferences).isNotNull();
		assertThat(creator).isNotNull();
		assertThat(linkedUsers).isNotNull();

		assertThat(name.getPropertyName()).isEqualTo("name");
		assertThat((Class<String>) name.getValueClass()).isEqualTo(String.class);
		assertThat((Serializer<String>) name.getValueSerializer()).isEqualTo(STRING_SRZ);
		assertThat(name.type()).isEqualTo(SIMPLE);
		assertThat(name.getReadConsistencyLevel()).isEqualTo(ConsistencyLevel.ONE);
		assertThat(name.getWriteConsistencyLevel()).isEqualTo(ConsistencyLevel.ALL);

		assertThat(age.getPropertyName()).isEqualTo("age_in_year");
		assertThat((Class<Long>) age.getValueClass()).isEqualTo(Long.class);
		assertThat((Serializer<Long>) age.getValueSerializer()).isEqualTo(LONG_SRZ);
		assertThat(age.type()).isEqualTo(SIMPLE);
		assertThat(age.getReadConsistencyLevel()).isEqualTo(ConsistencyLevel.ONE);
		assertThat(age.getWriteConsistencyLevel()).isEqualTo(ConsistencyLevel.ALL);

		assertThat(friends.getPropertyName()).isEqualTo("friends");
		assertThat(friends.getValueClass()).isEqualTo(String.class);
		assertThat(friends.getValueSerializer()).isEqualTo(STRING_SRZ);
		assertThat(friends.type()).isEqualTo(PropertyType.LAZY_LIST);
		assertThat(friends.newListInstance()).isNotNull();
		assertThat(friends.newListInstance()).isEmpty();
		assertThat(friends.type().isLazy()).isTrue();
		assertThat((Class<ArrayList>) friends.newListInstance().getClass()).isEqualTo(
				ArrayList.class);
		assertThat(friends.getReadConsistencyLevel()).isEqualTo(ConsistencyLevel.ONE);
		assertThat(friends.getWriteConsistencyLevel()).isEqualTo(ConsistencyLevel.ALL);

		assertThat(followers.getPropertyName()).isEqualTo("followers");
		assertThat(followers.getValueClass()).isEqualTo(String.class);
		assertThat(followers.getValueSerializer()).isEqualTo(STRING_SRZ);
		assertThat(followers.type()).isEqualTo(PropertyType.SET);
		assertThat(followers.newSetInstance()).isNotNull();
		assertThat(followers.newSetInstance()).isEmpty();
		assertThat((Class<HashSet>) followers.newSetInstance().getClass()).isEqualTo(HashSet.class);
		assertThat(followers.getReadConsistencyLevel()).isEqualTo(ConsistencyLevel.ONE);
		assertThat(followers.getWriteConsistencyLevel()).isEqualTo(ConsistencyLevel.ALL);

		assertThat(preferences.getPropertyName()).isEqualTo("preferences");
		assertThat(preferences.getValueClass()).isEqualTo(String.class);
		assertThat(preferences.getValueSerializer()).isEqualTo(STRING_SRZ);
		assertThat(preferences.type()).isEqualTo(PropertyType.MAP);
		assertThat(preferences.getKeyClass()).isEqualTo(Integer.class);
		assertThat(preferences.getKeySerializer()).isEqualTo(SerializerUtils.INT_SRZ);
		assertThat(preferences.newMapInstance()).isNotNull();
		assertThat(preferences.newMapInstance()).isEmpty();
		assertThat((Class<HashMap>) preferences.newMapInstance().getClass()).isEqualTo(
				HashMap.class);
		assertThat(preferences.getReadConsistencyLevel()).isEqualTo(ConsistencyLevel.ONE);
		assertThat(preferences.getWriteConsistencyLevel()).isEqualTo(ConsistencyLevel.ALL);

		assertThat(creator.getPropertyName()).isEqualTo("creator");
		assertThat(creator.getValueClass()).isEqualTo(UserBean.class);
		assertThat((Serializer) creator.getValueSerializer()).isEqualTo(SerializerUtils.OBJECT_SRZ);
		assertThat(creator.type()).isEqualTo(JOIN_SIMPLE);
		assertThat(creator.getJoinProperties().getCascadeTypes()).containsExactly(ALL);

		assertThat(linkedUsers.getPropertyName()).isEqualTo("linked_users");
		assertThat(linkedUsers.getValueClass()).isEqualTo(UserBean.class);
		assertThat((Serializer) linkedUsers.getValueSerializer()).isEqualTo(
				SerializerUtils.OBJECT_SRZ);
		assertThat(linkedUsers.type()).isEqualTo(JOIN_WIDE_MAP);
		assertThat(linkedUsers.getJoinProperties().getCascadeTypes()).containsExactly(PERSIST,
				MERGE);

		assertThat((Class) joinPropertyMetaToBeFilled.get(creator)).isEqualTo(UserBean.class);
		assertThat((Class) joinPropertyMetaToBeFilled.get(linkedUsers)).isEqualTo(UserBean.class);

		assertThat(meta.getConsistencyLevels().left).isEqualTo(ConsistencyLevel.ONE);
		assertThat(meta.getConsistencyLevels().right).isEqualTo(ConsistencyLevel.ALL);

		assertThat(
				configurableCLPolicyTL.get().getConsistencyLevelForRead(meta.getColumnFamilyName()))
				.isEqualTo(HConsistencyLevel.ONE);
		assertThat(
				configurableCLPolicyTL.get()
						.getConsistencyLevelForWrite(meta.getColumnFamilyName())).isEqualTo(
				HConsistencyLevel.ALL);

		assertThat(consistencyLevelsTL.get()).isNull();
	}

	@Test
	public void should_parse_entity_with_table_name() throws Exception
	{

		EntityMeta<?> meta = parser.parseEntity(keyspace, BeanWithColumnFamilyName.class);;

		assertThat(meta).isNotNull();
		assertThat(meta.getColumnFamilyName()).isEqualTo("myOwnCF");
	}

	@Test
	public void should_parse_inherited_bean() throws Exception
	{

		EntityMeta<?> meta = parser.parseEntity(keyspace, ChildBean.class);

		assertThat(meta).isNotNull();
		assertThat(meta.getIdMeta().getPropertyName()).isEqualTo("id");
		assertThat(meta.getPropertyMetas().get("name").getPropertyName()).isEqualTo("name");
		assertThat(meta.getPropertyMetas().get("address").getPropertyName()).isEqualTo("address");
		assertThat(meta.getPropertyMetas().get("nickname").getPropertyName()).isEqualTo("nickname");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void should_parse_bean_with_simple_counter_field() throws Exception
	{
		EntityMeta<?> meta = parser.parseEntity(keyspace, BeanWithSimpleCounter.class);

		assertThat(meta).isNotNull();
		assertThat(meta.hasCounter()).isTrue();
		PropertyMeta<Void, Long> idMeta = (PropertyMeta<Void, Long>) meta.getIdMeta();
		assertThat(idMeta).isNotNull();
		PropertyMeta<?, ?> counterMeta = meta.getPropertyMetas().get("counter");
		assertThat(counterMeta).isNotNull();

		CounterProperties counterProperties = counterMeta.getCounterProperties();

		assertThat(counterProperties).isNotNull();
		assertThat(counterProperties.getFqcn()).isEqualTo(
				BeanWithSimpleCounter.class.getCanonicalName());
		assertThat(counterProperties.getDao()).isSameAs(counterDao);
		assertThat((PropertyMeta<Void, Long>) counterProperties.getIdMeta()).isSameAs(idMeta);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void should_parse_bean_with_widemap_counter_field() throws Exception
	{
		EntityMeta<?> meta = parser.parseEntity(keyspace, BeanWithWideMapCounter.class);

		assertThat(meta).isNotNull();
		assertThat(meta.hasCounter()).isTrue();
		PropertyMeta<Void, Long> idMeta = (PropertyMeta<Void, Long>) meta.getIdMeta();
		assertThat(idMeta).isNotNull();
		PropertyMeta<?, ?> counterMeta = meta.getPropertyMetas().get("counters");
		assertThat(counterMeta).isNotNull();

		CounterProperties counterProperties = counterMeta.getCounterProperties();

		assertThat(counterProperties).isNotNull();
		assertThat(counterProperties.getFqcn()).isEqualTo(
				BeanWithWideMapCounter.class.getCanonicalName());
		assertThat(counterProperties.getDao()).isSameAs(counterDao);
		assertThat((PropertyMeta<Void, Long>) counterProperties.getIdMeta()).isSameAs(idMeta);
	}

	@Test
	public void should_parse_bean_with_external_wide_map() throws Exception
	{

		EntityMeta<?> meta = parser.parseEntity(keyspace, BeanWithExternalWideMap.class);

		assertThat(meta).isNotNull();
		PropertyMeta<?, ?> usersPropertyMeta = meta.getPropertyMetas().get("users");
		assertThat(usersPropertyMeta.type()).isEqualTo(EXTERNAL_WIDE_MAP);
		ExternalWideMapProperties<?> externalWideMapProperties = usersPropertyMeta
				.getExternalWideMapProperties();

		assertThat(externalWideMapProperties.getExternalColumnFamilyName()).isEqualTo(
				"external_users");
		assertThat(externalWideMapProperties.getExternalWideMapDao()).isNotNull();
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void should_parse_bean_with_external_join_wide_map() throws Exception
	{

		EntityMeta<?> meta = parser.parseEntity(keyspace, BeanWithExternalJoinWideMap.class);

		assertThat(meta).isNotNull();
		PropertyMeta<?, ?> usersPropertyMeta = meta.getPropertyMetas().get("users");
		assertThat(usersPropertyMeta.type()).isEqualTo(EXTERNAL_JOIN_WIDE_MAP);
		ExternalWideMapProperties<?> externalWideMapProperties = usersPropertyMeta
				.getExternalWideMapProperties();

		assertThat(externalWideMapProperties.getExternalColumnFamilyName()).isEqualTo(
				"external_users");
		assertThat(externalWideMapProperties.getExternalWideMapDao()).isNull();
		assertThat((Class) joinPropertyMetaToBeFilled.get(usersPropertyMeta)).isEqualTo(
				UserBean.class);
	}

	@Test
	public void should_exception_when_entity_has_no_id() throws Exception
	{
		expectedEx.expect(BeanMappingException.class);
		expectedEx.expectMessage("The entity '" + BeanWithNoId.class.getCanonicalName()
				+ "' should have at least one field with javax.persistence.Id annotation");
		parser.parseEntity(keyspace, BeanWithNoId.class);
	}

	@Test
	public void should_exception_when_id_type_not_serializable() throws Exception
	{
		expectedEx.expect(BeanMappingException.class);
		expectedEx.expectMessage("Value of 'id' should be Serializable");
		parser.parseEntity(keyspace, BeanWithNotSerializableId.class);
	}

	@Test
	public void should_exception_when_entity_has_no_column() throws Exception
	{
		expectedEx.expect(BeanMappingException.class);
		expectedEx
				.expectMessage("The entity '"
						+ BeanWithNoColumn.class.getCanonicalName()
						+ "' should have at least one field with javax.persistence.Column or javax.persistence.JoinColumn annotations");
		parser.parseEntity(keyspace, BeanWithNoColumn.class);
	}

	@Test
	public void should_exception_when_entity_has_duplicated_column_name() throws Exception
	{
		expectedEx.expect(BeanMappingException.class);
		expectedEx.expectMessage("The property 'name' is already used for the entity '"
				+ BeanWithDuplicatedColumnName.class.getCanonicalName() + "'");

		parser.parseEntity(keyspace, BeanWithDuplicatedColumnName.class);
	}

	@Test
	public void should_exception_when_entity_has_duplicated_join_column_name() throws Exception
	{
		expectedEx.expect(BeanMappingException.class);
		expectedEx.expectMessage("The property 'name' is already used for the entity '"
				+ BeanWithDuplicatedJoinColumnName.class.getCanonicalName() + "'");

		parser.parseEntity(keyspace, BeanWithDuplicatedJoinColumnName.class);
	}

	@SuppressWarnings(
	{
			"rawtypes",
			"unchecked"
	})
	@Test
	public void should_parse_column_family() throws Exception
	{

		EntityMeta<?> meta = parser.parseEntity(keyspace, ColumnFamilyBean.class);

		assertThat(meta.isColumnFamilyDirectMapping()).isTrue();

		assertThat(meta.getIdMeta().getPropertyName()).isEqualTo("id");
		assertThat(meta.getIdMeta().getValueClass()).isEqualTo((Class) Long.class);

		assertThat(meta.getPropertyMetas()).hasSize(1);
		assertThat(meta.getPropertyMetas().get("values").type()).isEqualTo(WIDE_MAP);
	}

	@SuppressWarnings(
	{
			"rawtypes",
			"unchecked"
	})
	@Test
	public void should_parse_column_family_with_join() throws Exception
	{
		EntityMeta<?> meta = parser.parseEntity(keyspace, ColumnFamilyBeanWithJoinEntity.class);

		assertThat(meta.isColumnFamilyDirectMapping()).isTrue();
		assertThat(meta.getColumnFamilyDao()).isNotNull();
		assertThat(meta.getEntityDao()).isNull();

		assertThat(meta.getIdMeta().getPropertyName()).isEqualTo("id");
		assertThat(meta.getIdMeta().getValueClass()).isEqualTo((Class) Long.class);

		Map<String, PropertyMeta<?, ?>> propertyMetas = meta.getPropertyMetas();
		assertThat(propertyMetas).hasSize(1);
		PropertyMeta<?, ?> friendMeta = propertyMetas.get("friends");

		assertThat(friendMeta.type()).isEqualTo(EXTERNAL_JOIN_WIDE_MAP);

		JoinProperties joinProperties = friendMeta.getJoinProperties();
		assertThat(joinProperties).isNotNull();
		assertThat(joinProperties.getCascadeTypes()).containsExactly(CascadeType.ALL);

		EntityMeta joinEntityMeta = joinProperties.getEntityMeta();
		assertThat(joinEntityMeta).isNull();

		ExternalWideMapProperties<?> externalWideMapProperties = friendMeta
				.getExternalWideMapProperties();

		assertThat(externalWideMapProperties).isNotNull();
		assertThat(externalWideMapProperties.getExternalColumnFamilyName()).isEqualTo(
				ColumnFamilyHelper
						.normalizerAndValidateColumnFamilyName(ColumnFamilyBeanWithJoinEntity.class
								.getName()));
		assertThat(externalWideMapProperties.getExternalWideMapDao()).isNull();

	}

	@Test
	public void should_exception_when_wide_row_more_than_one_mapped_column() throws Exception
	{
		expectedEx.expect(BeanMappingException.class);
		expectedEx.expectMessage("The ColumnFamily entity '"
				+ ColumnFamilyBeanWithTwoColumns.class.getCanonicalName()
				+ "' should not have more than one property annotated with @Column");

		parser.parseEntity(keyspace, ColumnFamilyBeanWithTwoColumns.class);

	}

	@Test
	public void should_exception_when_wide_row_has_wrong_column_type() throws Exception
	{
		expectedEx.expect(BeanMappingException.class);
		expectedEx.expectMessage("The ColumnFamily entity '"
				+ ColumnFamilyBeanWithWrongColumnType.class.getCanonicalName()
				+ "' should have one and only one @Column/@JoinColumn of type WideMap");

		parser.parseEntity(keyspace, ColumnFamilyBeanWithWrongColumnType.class);

	}

	@SuppressWarnings("unchecked")
	@Test
	public void should_fill_join_entity_meta_map_with_entity_meta() throws Exception
	{
		joinPropertyMetaToBeFilled = new HashMap<PropertyMeta<?, ?>, Class<?>>();
		entityMetaMap = new HashMap<Class<?>, EntityMeta<?>>();

		EntityMeta<Long> joinEntityMeta = new EntityMeta<Long>();
		joinEntityMeta.setColumnFamilyDirectMapping(false);

		PropertyMeta<Integer, String> joinPropertyMeta = new PropertyMeta<Integer, String>();
		joinPropertyMeta.setJoinProperties(new JoinProperties());
		joinPropertyMeta.setType(JOIN_WIDE_MAP);

		joinPropertyMetaToBeFilled.put(joinPropertyMeta, BeanWithJoinColumnAsWideMap.class);
		entityMetaMap.put(BeanWithJoinColumnAsWideMap.class, joinEntityMeta);

		parser.fillJoinEntityMeta(keyspace, joinPropertyMetaToBeFilled, entityMetaMap);

		assertThat((EntityMeta<Long>) joinPropertyMeta.getJoinProperties().getEntityMeta())
				.isSameAs(joinEntityMeta);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void should_fill_join_entity_meta_map_with_entity_meta_for_external() throws Exception
	{
		joinPropertyMetaToBeFilled = new HashMap<PropertyMeta<?, ?>, Class<?>>();
		entityMetaMap = new HashMap<Class<?>, EntityMeta<?>>();

		EntityMeta<Long> joinEntityMeta = new EntityMeta<Long>();
		joinEntityMeta.setColumnFamilyDirectMapping(false);
		joinEntityMeta.setIdSerializer(LONG_SRZ);

		PropertyMeta<Integer, String> joinPropertyMeta = new PropertyMeta<Integer, String>();
		joinPropertyMeta.setJoinProperties(new JoinProperties());
		joinPropertyMeta.setType(PropertyType.EXTERNAL_JOIN_WIDE_MAP);

		GenericCompositeDao<Long, String> dao = mock(GenericCompositeDao.class);
		ExternalWideMapProperties<Long> externalWideMapProperties = new ExternalWideMapProperties<Long>(
				"cfExternal", dao, LONG_SRZ);

		joinPropertyMeta.setExternalWideMapProperties(externalWideMapProperties);

		joinPropertyMetaToBeFilled.put(joinPropertyMeta, BeanWithJoinColumnAsWideMap.class);
		entityMetaMap.put(BeanWithJoinColumnAsWideMap.class, joinEntityMeta);

		parser.fillJoinEntityMeta(keyspace, joinPropertyMetaToBeFilled, entityMetaMap);

		assertThat((EntityMeta<Long>) joinPropertyMeta.getJoinProperties().getEntityMeta())
				.isSameAs(joinEntityMeta);

		GenericCompositeDao<?, ?> externalWideMapDao = joinPropertyMeta
				.getExternalWideMapProperties().getExternalWideMapDao();
		assertThat(externalWideMapDao).isNotNull();
		assertThat(Whitebox.getInternalState(externalWideMapDao, "keyspace")).isSameAs(keyspace);
		assertThat(Whitebox.getInternalState(externalWideMapDao, "keySerializer")).isSameAs(
				LONG_SRZ);
		assertThat(Whitebox.getInternalState(externalWideMapDao, "columnFamily")).isSameAs(
				"cfExternal");
		assertThat(Whitebox.getInternalState(externalWideMapDao, "valueSerializer")).isSameAs(
				LONG_SRZ);
	}

	@Test
	public void should_exception_when_join_entity_is_a_direct_cf_mapping() throws Exception
	{
		joinPropertyMetaToBeFilled = new HashMap<PropertyMeta<?, ?>, Class<?>>();
		entityMetaMap = new HashMap<Class<?>, EntityMeta<?>>();

		EntityMeta<Long> joinEntityMeta = new EntityMeta<Long>();
		joinEntityMeta.setColumnFamilyDirectMapping(true);
		PropertyMeta<Integer, String> joinPropertyMeta = new PropertyMeta<Integer, String>();

		joinPropertyMetaToBeFilled.put(joinPropertyMeta, BeanWithJoinColumnAsWideMap.class);
		entityMetaMap.put(BeanWithJoinColumnAsWideMap.class, joinEntityMeta);

		expectedEx.expect(BeanMappingException.class);
		expectedEx.expectMessage("The entity '"
				+ BeanWithJoinColumnAsWideMap.class.getCanonicalName()
				+ "' is a direct Column Family mapping and cannot be a join entity");

		parser.fillJoinEntityMeta(keyspace, joinPropertyMetaToBeFilled, entityMetaMap);

	}

	@Test
	public void should_exception_when_no_entity_meta_found_for_join_property() throws Exception
	{
		joinPropertyMetaToBeFilled = new HashMap<PropertyMeta<?, ?>, Class<?>>();
		entityMetaMap = new HashMap<Class<?>, EntityMeta<?>>();

		PropertyMeta<Integer, String> joinPropertyMeta = new PropertyMeta<Integer, String>();

		joinPropertyMetaToBeFilled.put(joinPropertyMeta, BeanWithJoinColumnAsWideMap.class);

		expectedEx.expect(BeanMappingException.class);
		expectedEx.expectMessage("Cannot find mapping for join entity '"
				+ BeanWithJoinColumnAsWideMap.class.getCanonicalName() + "'");

		parser.fillJoinEntityMeta(keyspace, joinPropertyMetaToBeFilled, entityMetaMap);

	}

}
