package info.archinnov.achilles.entity.metadata.builder;

import static info.archinnov.achilles.entity.metadata.PropertyType.SIMPLE;
import static info.archinnov.achilles.entity.metadata.builder.EntityMetaBuilder.entityMetaBuilder;
import static info.archinnov.achilles.serializer.SerializerUtils.STRING_SRZ;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import info.archinnov.achilles.dao.GenericCompositeDao;
import info.archinnov.achilles.dao.GenericDynamicCompositeDao;
import info.archinnov.achilles.entity.metadata.EntityMeta;
import info.archinnov.achilles.entity.metadata.PropertyMeta;
import info.archinnov.achilles.entity.metadata.PropertyType;
import info.archinnov.achilles.serializer.SerializerUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import me.prettyprint.cassandra.model.ExecutingKeyspace;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import parser.entity.Bean;

/**
 * EntityMetaBuilderTest
 * 
 * @author DuyHai DOAN
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class EntityMetaBuilderTest
{

	@Mock
	private ExecutingKeyspace keyspace;

	@Mock
	private GenericDynamicCompositeDao<?> dao;

	@Mock
	private PropertyMeta<Void, Long> idMeta;

	@SuppressWarnings(
	{
			"unchecked",
			"rawtypes"
	})
	@Test
	public void should_build_meta() throws Exception
	{

		Map<String, PropertyMeta<?, ?>> propertyMetas = new HashMap<String, PropertyMeta<?, ?>>();
		PropertyMeta<Void, String> simpleMeta = new PropertyMeta<Void, String>();
		simpleMeta.setType(SIMPLE);

		Method getter = Bean.class.getDeclaredMethod("getName", (Class<?>[]) null);
		simpleMeta.setGetter(getter);

		Method setter = Bean.class.getDeclaredMethod("setName", String.class);
		simpleMeta.setSetter(setter);

		propertyMetas.put("name", simpleMeta);

		when(idMeta.getValueClass()).thenReturn(Long.class);

		EntityMeta<Long> meta = entityMetaBuilder(idMeta).className("Bean") //
				.serialVersionUID(1L) //
				.columnFamilyName("cfName") //
				.propertyMetas(propertyMetas) //
				.keyspace(keyspace) //
				.hasCounter(true) //
				.build();

		assertThat(meta.getClassName()).isEqualTo("Bean");
		assertThat(meta.getColumnFamilyName()).isEqualTo("cfName");
		assertThat(meta.getIdMeta()).isSameAs(idMeta);
		assertThat(meta.getIdSerializer().getComparatorType()).isEqualTo(
				SerializerUtils.LONG_SRZ.getComparatorType());
		assertThat(meta.getPropertyMetas()).containsKey("name");
		assertThat(meta.getPropertyMetas()).containsValue(simpleMeta);

		assertThat(meta.getGetterMetas()).hasSize(1);
		assertThat(meta.getGetterMetas().containsKey(getter));
		assertThat(meta.getGetterMetas().get(getter)).isSameAs((PropertyMeta) simpleMeta);

		assertThat(meta.getSetterMetas()).hasSize(1);
		assertThat(meta.getSetterMetas().containsKey(setter));
		assertThat(meta.getSetterMetas().get(setter)).isSameAs((PropertyMeta) simpleMeta);

		assertThat(meta.getEntityDao()).isNotNull();

		assertThat(meta.hasCounter()).isTrue();

	}

	@SuppressWarnings("unchecked")
	@Test
	public void should_build_meta_with_column_family_name() throws Exception
	{

		Map<String, PropertyMeta<?, ?>> propertyMetas = new HashMap<String, PropertyMeta<?, ?>>();
		PropertyMeta<Void, String> simpleMeta = new PropertyMeta<Void, String>();
		simpleMeta.setType(SIMPLE);
		propertyMetas.put("name", simpleMeta);

		when(idMeta.getValueClass()).thenReturn(Long.class);

		EntityMeta<Long> meta = entityMetaBuilder(idMeta).className("Bean").serialVersionUID(1L)
				.propertyMetas(propertyMetas).columnFamilyName("toto").keyspace(keyspace).build();

		assertThat(meta.getClassName()).isEqualTo("Bean");
		assertThat(meta.getColumnFamilyName()).isEqualTo("toto");
		assertThat(meta.getColumnFamilyDao()).isNull();
		assertThat(meta.getEntityDao()).isExactlyInstanceOf(GenericDynamicCompositeDao.class);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void should_build_meta_for_wide_row() throws Exception
	{

		Map<String, PropertyMeta<?, ?>> propertyMetas = new HashMap<String, PropertyMeta<?, ?>>();
		PropertyMeta<Integer, String> wideMapMeta = new PropertyMeta<Integer, String>();
		wideMapMeta.setValueSerializer(STRING_SRZ);
		wideMapMeta.setType(PropertyType.WIDE_MAP);
		propertyMetas.put("name", wideMapMeta);

		when(idMeta.getValueClass()).thenReturn(Long.class);

		EntityMeta<Long> meta = entityMetaBuilder(idMeta).className("Bean").serialVersionUID(1L)
				.propertyMetas(propertyMetas).columnFamilyName("toto").keyspace(keyspace)
				.columnFamilyDirectMapping(true).build();

		assertThat(meta.isColumnFamilyDirectMapping()).isTrue();
		assertThat(meta.getEntityDao()).isNull();
		assertThat(meta.getColumnFamilyDao()).isExactlyInstanceOf(GenericCompositeDao.class);
	}
}
