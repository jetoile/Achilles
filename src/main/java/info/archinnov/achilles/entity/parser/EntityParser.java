package info.archinnov.achilles.entity.parser;

import static info.archinnov.achilles.entity.manager.ThriftEntityManagerFactoryImpl.configurableCLPolicyTL;
import static info.archinnov.achilles.entity.metadata.PropertyType.EXTERNAL_JOIN_WIDE_MAP;
import static info.archinnov.achilles.entity.metadata.PropertyType.WIDE_MAP;
import static info.archinnov.achilles.entity.metadata.builder.EntityMetaBuilder.entityMetaBuilder;
import info.archinnov.achilles.annotations.ColumnFamily;
import info.archinnov.achilles.dao.CounterDao;
import info.archinnov.achilles.dao.GenericCompositeDao;
import info.archinnov.achilles.dao.Pair;
import info.archinnov.achilles.entity.EntityHelper;
import info.archinnov.achilles.entity.metadata.EntityMeta;
import info.archinnov.achilles.entity.metadata.ExternalWideMapProperties;
import info.archinnov.achilles.entity.metadata.PropertyMeta;
import info.archinnov.achilles.entity.metadata.PropertyType;
import info.archinnov.achilles.entity.type.ConsistencyLevel;
import info.archinnov.achilles.exception.BeanMappingException;
import info.archinnov.achilles.json.ObjectMapperFactory;
import info.archinnov.achilles.validation.Validator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.JoinColumn;

import me.prettyprint.hector.api.HConsistencyLevel;
import me.prettyprint.hector.api.Keyspace;

import org.codehaus.jackson.map.ObjectMapper;

/**
 * EntityParser
 * 
 * @author DuyHai DOAN
 * 
 */
public class EntityParser
{
	private PropertyParser parser = new PropertyParser();
	private JoinPropertyParser joinParser = new JoinPropertyParser();
	private PropertyFilter filter = new PropertyFilter();
	private EntityHelper helper = new EntityHelper();
	private ObjectMapperFactory objectMapperFactory;

	static ThreadLocal<Map<PropertyMeta<?, ?>, String>> externalWideMapTL = new ThreadLocal<Map<PropertyMeta<?, ?>, String>>();
	static ThreadLocal<Map<PropertyMeta<?, ?>, String>> joinExternalWideMapTL = new ThreadLocal<Map<PropertyMeta<?, ?>, String>>();
	static ThreadLocal<Map<String, PropertyMeta<?, ?>>> propertyMetasTL = new ThreadLocal<Map<String, PropertyMeta<?, ?>>>();
	static ThreadLocal<List<PropertyMeta<?, ?>>> counterMetasTL = new ThreadLocal<List<PropertyMeta<?, ?>>>();
	static ThreadLocal<ObjectMapper> objectMapperTL = new ThreadLocal<ObjectMapper>();
	static ThreadLocal<Class<?>> entityClassTL = new ThreadLocal<Class<?>>();
	static ThreadLocal<Pair<ConsistencyLevel, ConsistencyLevel>> consistencyLevelsTL = new ThreadLocal<Pair<ConsistencyLevel, ConsistencyLevel>>();

	public EntityParser(ObjectMapperFactory objectMapperFactory) {
		Validator.validateNotNull(objectMapperFactory,
				"A non null ObjectMapperFactory is required for creating an EntityParser");
		this.objectMapperFactory = objectMapperFactory;
	}

	@SuppressWarnings("unchecked")
	public EntityMeta<?> parseEntity(Keyspace keyspace, Class<?> entityClass)
	{

		ObjectMapper objectMapper = validateEntityAndGetObjectMapper(entityClass);

		initThreadLocals(entityClass, objectMapper);

		String columnFamilyName = helper.inferColumnFamilyName(entityClass, entityClass.getName());
		Long serialVersionUID = helper.findSerialVersionUID(entityClass);

		Pair<Pair<ConsistencyLevel, ConsistencyLevel>, Pair<HConsistencyLevel, HConsistencyLevel>> consistencyLevels = initConsistencyLevelsThreadLocals(
				entityClass, columnFamilyName);

		boolean columnFamilyDirectMapping = entityClass.getAnnotation(ColumnFamily.class) != null ? true
				: false;

		PropertyMeta<Void, ?> idMeta = null;

		List<Field> inheritedFields = helper.getInheritedPrivateFields(entityClass);

		for (Field field : inheritedFields)
		{
			if (filter.hasAnnotation(field, Id.class))
			{
				idMeta = parser.parseSimpleProperty(field, field.getName(),
						entityClass.getCanonicalName());
			}
			else if (filter.hasAnnotation(field, Column.class))
			{
				parser.parse(field, false);
			}
			else if (filter.hasAnnotation(field, JoinColumn.class))
			{
				joinParser.parseJoin(field);
			}

		}

		// First validate id meta
		validateIdMeta(entityClass, idMeta);

		// Deferred external wide map fields parsing
		parseExternalWideMap(keyspace, idMeta);

		// Deferred external join wide map fields parsing
		parseExternalJoinWideMap(keyspace, columnFamilyName, columnFamilyDirectMapping, idMeta);

		// Deferred counter property meta completion
		completeCounterPropertyMeta(idMeta);

		// Finish validation of property metas and column family direct mappings
		validatePropertyMetas(entityClass, columnFamilyDirectMapping);
		validateColumnFamilyDirectMappings(entityClass, columnFamilyDirectMapping);

		boolean hasCounter = !counterMetasTL.get().isEmpty();
		CounterDao counterDao = retrieveCounterDaoIfNecessary(hasCounter);

		EntityMeta<?> entityMeta = entityMetaBuilder((PropertyMeta<Void, Object>) idMeta)
				.keyspace(keyspace).className(entityClass.getCanonicalName()) //
				.columnFamilyName(columnFamilyName) //
				.serialVersionUID(serialVersionUID) //
				.propertyMetas(propertyMetasTL.get()) //
				.columnFamilyDirectMapping(columnFamilyDirectMapping) //
				.hasCounter(hasCounter) //
				.counterDao(counterDao) //
				.consistencyLevels(consistencyLevels.left) //
				.build();

		cleanThreadLocals();

		return entityMeta;
	}

	@SuppressWarnings("unchecked")
	public <ID, JOIN_ID> void fillJoinEntityMeta(
			Keyspace keyspace, //
			Map<PropertyMeta<?, ?>, Class<?>> joinPropertyMetaToBeFilled,
			Map<Class<?>, EntityMeta<?>> entityMetaMap)
	{
		// Retrieve EntityMeta objects for join columns after entities parsing
		for (Entry<PropertyMeta<?, ?>, Class<?>> entry : joinPropertyMetaToBeFilled.entrySet())
		{
			Class<?> clazz = entry.getValue();
			if (entityMetaMap.containsKey(clazz))
			{
				PropertyMeta<?, ?> propertyMeta = entry.getKey();
				EntityMeta<JOIN_ID> joinEntityMeta = (EntityMeta<JOIN_ID>) entityMetaMap.get(clazz);
	
				if (joinEntityMeta.isColumnFamilyDirectMapping())
				{
					throw new BeanMappingException("The entity '" + clazz.getCanonicalName()
							+ "' is a direct Column Family mapping and cannot be a join entity");
				}
	
				propertyMeta.getJoinProperties().setEntityMeta(joinEntityMeta);
				if (propertyMeta.type().isExternal())
				{
					ExternalWideMapProperties<ID> externalWideMapProperties = (ExternalWideMapProperties<ID>) propertyMeta
							.getExternalWideMapProperties();
	
					externalWideMapProperties.setExternalWideMapDao( //
							new GenericCompositeDao<ID, JOIN_ID>(keyspace, //
									externalWideMapProperties.getIdSerializer(), //
									joinEntityMeta.getIdSerializer(), //
									externalWideMapProperties.getExternalColumnFamilyName(),//
									configurableCLPolicyTL.get()));
				}
			}
			else
			{
				throw new BeanMappingException("Cannot find mapping for join entity '"
						+ clazz.getCanonicalName() + "'");
			}
		}
	}

	private ObjectMapper validateEntityAndGetObjectMapper(Class<?> entityClass)
	{
		Validator.validateSerializable(entityClass, "The entity '" + entityClass.getCanonicalName()
				+ "' should implements java.io.Serializable");
		Validator.validateInstantiable(entityClass);
		ObjectMapper objectMapper = objectMapperFactory.getMapper(entityClass);
		Validator.validateNotNull(objectMapper, "No Jackson ObjectMapper found for entity '"
				+ entityClass.getCanonicalName() + "'");
		return objectMapper;
	}

	private void validateIdMeta(Class<?> entityClass, PropertyMeta<Void, ?> idMeta)
	{
		if (idMeta == null)
		{
			throw new BeanMappingException("The entity '" + entityClass.getCanonicalName()
					+ "' should have at least one field with javax.persistence.Id annotation");
		}
	}

	private void parseExternalWideMap(Keyspace keyspace, PropertyMeta<Void, ?> idMeta)
	{
		for (Entry<PropertyMeta<?, ?>, String> entry : externalWideMapTL.get().entrySet())
		{
			PropertyMeta<?, ?> externalWideMapMeta = entry.getKey();
			String externalTableName = entry.getValue();
			parser.fillExternalWideMap(keyspace, idMeta, externalWideMapMeta, externalTableName);
		}
	}

	private void parseExternalJoinWideMap(Keyspace keyspace, String columnFamilyName,
			boolean columnFamilyDirectMapping, PropertyMeta<Void, ?> idMeta)
	{
		for (Entry<PropertyMeta<?, ?>, String> entry : joinExternalWideMapTL.get().entrySet())
		{
			PropertyMeta<?, ?> joinExternalWideMapMeta = entry.getKey();
			String externalTableName;
	
			if (columnFamilyDirectMapping)
			{
				externalTableName = columnFamilyName;
			}
			else
			{
				externalTableName = entry.getValue();
			}
			joinParser.fillExternalJoinWideMap(keyspace, idMeta, joinExternalWideMapMeta,
					externalTableName);
		}
	}

	private void completeCounterPropertyMeta(PropertyMeta<Void, ?> idMeta)
	{
		for (PropertyMeta<?, ?> counterMeta : counterMetasTL.get())
		{
			counterMeta.getCounterProperties().setIdMeta(idMeta);
		}
	}

	private CounterDao retrieveCounterDaoIfNecessary(boolean hasCounter)
	{
		CounterDao counterDao = null;
		if (hasCounter)
		{
			counterDao = counterMetasTL.get().get(0).counterDao();
		}
		return counterDao;
	}

	private void validatePropertyMetas(Class<?> entityClass, boolean columnFamilyDirectMapping)
	{
		if (propertyMetasTL.get().isEmpty())
		{
			throw new BeanMappingException(
					"The entity '"
							+ entityClass.getCanonicalName()
							+ "' should have at least one field with javax.persistence.Column or javax.persistence.JoinColumn annotations");
		}
	}

	private void validateColumnFamilyDirectMappings(Class<?> entityClass,
			boolean columnFamilyDirectMapping)
	{
		Map<String, PropertyMeta<?, ?>> propertyMetas = propertyMetasTL.get();
		if (columnFamilyDirectMapping)
		{
			if (propertyMetas != null && propertyMetas.size() > 1)
			{
				throw new BeanMappingException("The ColumnFamily entity '"
						+ entityClass.getCanonicalName()
						+ "' should not have more than one property annotated with @Column");
			}

			PropertyType type = propertyMetas.entrySet().iterator().next().getValue().type();

			if (type != WIDE_MAP && type != EXTERNAL_JOIN_WIDE_MAP)
			{
				throw new BeanMappingException("The ColumnFamily entity '"
						+ entityClass.getCanonicalName()
						+ "' should have one and only one @Column/@JoinColumn of type WideMap");
			}
		}
	}

	private void initThreadLocals(Class<?> entityClass, ObjectMapper objectMapper)
	{
		propertyMetasTL.set(new HashMap<String, PropertyMeta<?, ?>>());
		externalWideMapTL.set(new HashMap<PropertyMeta<?, ?>, String>());
		joinExternalWideMapTL.set(new HashMap<PropertyMeta<?, ?>, String>());
		counterMetasTL.set(new ArrayList<PropertyMeta<?, ?>>());
		entityClassTL.set(entityClass);
		objectMapperTL.set(objectMapper);
	}

	private Pair<Pair<ConsistencyLevel, ConsistencyLevel>, Pair<HConsistencyLevel, HConsistencyLevel>> initConsistencyLevelsThreadLocals(
			Class<?> entityClass, String columnFamilyName)
	{
		Pair<Pair<ConsistencyLevel, ConsistencyLevel>, Pair<HConsistencyLevel, HConsistencyLevel>> consistencyLevels = helper
				.findConsistencyLevels(entityClass);

		configurableCLPolicyTL.get().setConsistencyLevelForRead(consistencyLevels.right.left,
				columnFamilyName);
		configurableCLPolicyTL.get().setConsistencyLevelForWrite(consistencyLevels.right.right,
				columnFamilyName);
		consistencyLevelsTL.set(consistencyLevels.left);
		return consistencyLevels;
	}

	private void cleanThreadLocals()
	{
		externalWideMapTL.remove();
		joinExternalWideMapTL.remove();
		propertyMetasTL.remove();
		counterMetasTL.remove();
		objectMapperTL.remove();
		entityClassTL.remove();
		consistencyLevelsTL.remove();
	}
}
