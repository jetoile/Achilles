package info.archinnov.achilles.entity.metadata.factory;

import static info.archinnov.achilles.entity.metadata.PropertyType.LAZY_LIST;
import static info.archinnov.achilles.entity.metadata.PropertyType.LAZY_MAP;
import static info.archinnov.achilles.entity.metadata.PropertyType.LAZY_SET;
import static info.archinnov.achilles.entity.metadata.PropertyType.LAZY_SIMPLE;
import static info.archinnov.achilles.entity.metadata.PropertyType.LIST;
import static info.archinnov.achilles.entity.metadata.PropertyType.MAP;
import static info.archinnov.achilles.entity.metadata.PropertyType.SET;
import static info.archinnov.achilles.entity.metadata.PropertyType.SIMPLE;
import static info.archinnov.achilles.entity.metadata.PropertyType.WIDE_MAP;
import static info.archinnov.achilles.entity.type.ConsistencyLevel.ALL;
import static info.archinnov.achilles.entity.type.ConsistencyLevel.ONE;
import static info.archinnov.achilles.serializer.SerializerUtils.INT_SRZ;
import static info.archinnov.achilles.serializer.SerializerUtils.OBJECT_SRZ;
import static info.archinnov.achilles.serializer.SerializerUtils.STRING_SRZ;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import info.archinnov.achilles.dao.Pair;
import info.archinnov.achilles.entity.metadata.MultiKeyProperties;
import info.archinnov.achilles.entity.metadata.PropertyMeta;
import info.archinnov.achilles.entity.type.ConsistencyLevel;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import me.prettyprint.hector.api.Serializer;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import parser.entity.Bean;
import parser.entity.MyMultiKey;

/**
 * PropertyMetaFactoryTest
 * 
 * @author DuyHai DOAN
 * 
 */
public class PropertyMetaFactoryTest
{
	Method[] accessors = new Method[2];

	private ObjectMapper mapper = new ObjectMapper();

	private ObjectMapper objectMapper = new ObjectMapper();

	@Before
	public void setUp() throws Exception
	{
		accessors[0] = Bean.class.getDeclaredMethod("getId");
		accessors[1] = Bean.class.getDeclaredMethod("setId", Long.class);
	}

	@Test
	public void should_build_simple() throws Exception
	{

		PropertyMeta<Void, String> built = PropertyMetaFactory.factory(String.class).type(SIMPLE)
				.propertyName("prop") //
				.accessors(accessors) //
				.objectMapper(objectMapper) //
				.consistencyLevels(new Pair<ConsistencyLevel, ConsistencyLevel>(ONE, ALL)) //
				.build();

		assertThat(built.type()).isEqualTo(SIMPLE);
		assertThat(built.getPropertyName()).isEqualTo("prop");

		assertThat(built.getValueFromString("\"val\"")).isInstanceOf(String.class);
		assertThat(built.getValueClass()).isEqualTo(String.class);
		assertThat(built.getValueSerializer().getComparatorType()).isEqualTo(
				STRING_SRZ.getComparatorType());

		assertThat(built.type().isLazy()).isFalse();
		assertThat(built.isSingleKey()).isTrue();
		assertThat(built.type().isJoinColumn()).isFalse();
		assertThat(built.getReadConsistencyLevel()).isEqualTo(ONE);
		assertThat(built.getWriteConsistencyLevel()).isEqualTo(ALL);
	}

	@Test
	public void should_build_simple_lazy() throws Exception
	{

		PropertyMeta<Void, String> built = PropertyMetaFactory.factory(String.class)
				.type(LAZY_SIMPLE).propertyName("prop").accessors(accessors)
				.objectMapper(objectMapper).build();

		assertThat(built.type()).isEqualTo(LAZY_SIMPLE);
		assertThat(built.getPropertyName()).isEqualTo("prop");

		assertThat(built.getValueFromString("\"val\"")).isInstanceOf(String.class);
		assertThat(built.getValueClass()).isEqualTo(String.class);
		assertThat(built.getValueSerializer().getComparatorType()).isEqualTo(
				STRING_SRZ.getComparatorType());

		assertThat(built.type().isLazy()).isTrue();
		assertThat(built.isSingleKey()).isTrue();
		assertThat(built.type().isJoinColumn()).isFalse();
	}

	@Test
	public void should_build_simple_with_object_as_value() throws Exception
	{
		PropertyMeta<Void, Bean> built = PropertyMetaFactory.factory(Bean.class).type(SIMPLE)
				.propertyName("prop").accessors(accessors).objectMapper(objectMapper).build();

		assertThat(built.type()).isEqualTo(SIMPLE);
		assertThat(built.getPropertyName()).isEqualTo("prop");

		Bean bean = new Bean();
		assertThat(built.getValueFromString(writeString(bean))).isInstanceOf(Bean.class);
		assertThat(built.getValueClass()).isEqualTo(Bean.class);
		assertThat(built.getValueSerializer().getComparatorType()).isEqualTo(
				OBJECT_SRZ.getComparatorType());

		assertThat(built.type().isLazy()).isFalse();
		assertThat(built.isSingleKey()).isTrue();
		assertThat(built.type().isJoinColumn()).isFalse();
	}

	@Test
	public void should_build_list() throws Exception
	{

		PropertyMeta<Void, String> built = PropertyMetaFactory.factory(String.class).type(LIST)
				.propertyName("prop").accessors(accessors).objectMapper(objectMapper).build();

		assertThat(built.type()).isEqualTo(LIST);
		assertThat(built.getPropertyName()).isEqualTo("prop");

		assertThat(built.getValueFromString("\"val\"")).isInstanceOf(String.class);
		assertThat(built.getValueClass()).isEqualTo(String.class);
		assertThat(built.getValueSerializer().getComparatorType()).isEqualTo(
				STRING_SRZ.getComparatorType());

		assertThat(built.type().isLazy()).isFalse();
		assertThat(built.isSingleKey()).isTrue();
		assertThat(built.type().isJoinColumn()).isFalse();
	}

	@Test
	public void should_build_list_lazy() throws Exception
	{

		PropertyMeta<Void, String> built = PropertyMetaFactory.factory(String.class)
				.type(LAZY_LIST).propertyName("prop").accessors(accessors)
				.objectMapper(objectMapper).build();

		assertThat(built.type()).isEqualTo(LAZY_LIST);
		assertThat(built.getPropertyName()).isEqualTo("prop");

		assertThat(built.getValueFromString("\"val\"")).isInstanceOf(String.class);
		assertThat(built.getValueClass()).isEqualTo(String.class);
		assertThat(built.getValueSerializer().getComparatorType()).isEqualTo(
				STRING_SRZ.getComparatorType());

		assertThat(built.type().isLazy()).isTrue();
		assertThat(built.isSingleKey()).isTrue();
		assertThat(built.type().isJoinColumn()).isFalse();
	}

	@Test
	public void should_build_set() throws Exception
	{

		PropertyMeta<Void, String> built = PropertyMetaFactory.factory(String.class).type(SET)
				.propertyName("prop").accessors(accessors).objectMapper(objectMapper).build();

		assertThat(built.type()).isEqualTo(SET);
		assertThat(built.getPropertyName()).isEqualTo("prop");

		assertThat(built.getValueFromString("\"val\"")).isInstanceOf(String.class);
		assertThat(built.getValueClass()).isEqualTo(String.class);
		assertThat(built.getValueSerializer().getComparatorType()).isEqualTo(
				STRING_SRZ.getComparatorType());

		assertThat(built.type().isLazy()).isFalse();
		assertThat(built.isSingleKey()).isTrue();
		assertThat(built.type().isJoinColumn()).isFalse();
	}

	@Test
	public void should_build_set_lazy() throws Exception
	{

		PropertyMeta<Void, String> built = PropertyMetaFactory.factory(String.class).type(LAZY_SET)
				.propertyName("prop").accessors(accessors).objectMapper(objectMapper).build();

		assertThat(built.type()).isEqualTo(LAZY_SET);
		assertThat(built.getPropertyName()).isEqualTo("prop");

		assertThat(built.getValueFromString("\"val\"")).isInstanceOf(String.class);
		assertThat(built.getValueClass()).isEqualTo(String.class);
		assertThat(built.getValueSerializer().getComparatorType()).isEqualTo(
				STRING_SRZ.getComparatorType());

		assertThat(built.type().isLazy()).isTrue();
		assertThat(built.isSingleKey()).isTrue();
		assertThat(built.type().isJoinColumn()).isFalse();
	}

	@Test
	public void should_build_map() throws Exception
	{

		PropertyMeta<Integer, String> built = PropertyMetaFactory
				.factory(Integer.class, String.class).type(MAP).propertyName("prop")
				.accessors(accessors).objectMapper(objectMapper).build();

		assertThat(built.type()).isEqualTo(MAP);
		assertThat(built.getPropertyName()).isEqualTo("prop");

		assertThat(built.getKey((Object) 12)).isInstanceOf(Integer.class);
		assertThat(built.getKeyClass()).isEqualTo(Integer.class);
		assertThat(built.getKeySerializer().getComparatorType()).isEqualTo(
				INT_SRZ.getComparatorType());

		assertThat(built.getValueFromString("\"val\"")).isInstanceOf(String.class);
		assertThat(built.getValueClass()).isEqualTo(String.class);
		assertThat(built.getValueSerializer().getComparatorType()).isEqualTo(
				STRING_SRZ.getComparatorType());

		assertThat(built.type().isLazy()).isFalse();
		assertThat(built.isSingleKey()).isTrue();
		assertThat(built.type().isJoinColumn()).isFalse();
	}

	@Test
	public void should_build_map_with_object_as_key() throws Exception
	{
		PropertyMeta<Bean, String> built = PropertyMetaFactory.factory(Bean.class, String.class)
				.type(MAP).propertyName("prop").accessors(accessors).objectMapper(objectMapper)
				.build();

		assertThat(built.type()).isEqualTo(MAP);
		assertThat(built.getPropertyName()).isEqualTo("prop");

		Bean bean = new Bean();
		assertThat(built.getKey((Object) bean)).isInstanceOf(Bean.class);
		assertThat(built.getKeyClass()).isEqualTo(Bean.class);
		assertThat(built.getKeySerializer().getComparatorType()).isEqualTo(
				OBJECT_SRZ.getComparatorType());

		assertThat(built.getValueFromString("\"val\"")).isInstanceOf(String.class);
		assertThat(built.getValueClass()).isEqualTo(String.class);
		assertThat(built.getValueSerializer().getComparatorType()).isEqualTo(
				STRING_SRZ.getComparatorType());

		assertThat(built.type().isLazy()).isFalse();
		assertThat(built.isSingleKey()).isTrue();
		assertThat(built.type().isJoinColumn()).isFalse();
	}

	@Test
	public void should_build_map_lazy() throws Exception
	{

		PropertyMeta<Integer, String> built = PropertyMetaFactory
				.factory(Integer.class, String.class).type(LAZY_MAP).propertyName("prop")
				.accessors(accessors).objectMapper(objectMapper).build();

		assertThat(built.type()).isEqualTo(LAZY_MAP);
		assertThat(built.getPropertyName()).isEqualTo("prop");

		assertThat(built.getKey((Object) 12)).isInstanceOf(Integer.class);
		assertThat(built.getKeyClass()).isEqualTo(Integer.class);
		assertThat(built.getKeySerializer().getComparatorType()).isEqualTo(
				INT_SRZ.getComparatorType());

		assertThat(built.getValueFromString("\"val\"")).isInstanceOf(String.class);
		assertThat(built.getValueClass()).isEqualTo(String.class);
		assertThat(built.getValueSerializer().getComparatorType()).isEqualTo(
				STRING_SRZ.getComparatorType());

		assertThat(built.type().isLazy()).isTrue();
		assertThat(built.isSingleKey()).isTrue();
		assertThat(built.type().isJoinColumn()).isFalse();
	}

	@Test
	public void should_build_wide_map() throws Exception
	{

		PropertyMeta<Integer, String> built = PropertyMetaFactory
				.factory(Integer.class, String.class).type(WIDE_MAP).propertyName("prop")
				.accessors(accessors).objectMapper(objectMapper).build();

		assertThat(built.type()).isEqualTo(WIDE_MAP);
		assertThat(built.getPropertyName()).isEqualTo("prop");

		assertThat(built.getKey((Object) 12)).isInstanceOf(Integer.class);
		assertThat(built.getKeyClass()).isEqualTo(Integer.class);
		assertThat(built.getKeySerializer().getComparatorType()).isEqualTo(
				INT_SRZ.getComparatorType());

		assertThat(built.getValueFromString("\"val\"")).isInstanceOf(String.class);
		assertThat(built.getValueClass()).isEqualTo(String.class);
		assertThat(built.getValueSerializer().getComparatorType()).isEqualTo(
				STRING_SRZ.getComparatorType());

		assertThat(built.type().isLazy()).isTrue();
		assertThat(built.isSingleKey()).isTrue();
		assertThat(built.type().isJoinColumn()).isFalse();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void should_build_multi_key_wide_map() throws Exception
	{

		Iterator<Class<?>> iterator = mock(Iterator.class);
		List<Class<?>> componentClasses = mock(List.class);
		List<Method> componentGetters = mock(List.class);
		List<Method> componentSetters = mock(List.class);
		List<Serializer<?>> componentSerializers = mock(List.class);
		when(componentClasses.size()).thenReturn(1);
		when(componentClasses.iterator()).thenReturn(iterator);
		when(iterator.hasNext()).thenReturn(false);

		MultiKeyProperties props = new MultiKeyProperties();
		props.setComponentClasses(componentClasses);
		props.setComponentGetters(componentGetters);
		props.setComponentSetters(componentSetters);
		props.setComponentSerializers(componentSerializers);

		PropertyMeta<MyMultiKey, String> built = PropertyMetaFactory
				.factory(MyMultiKey.class, String.class) //
				.type(WIDE_MAP).propertyName("prop") //
				.accessors(accessors) //
				.multiKeyProperties(props) //
				.objectMapper(objectMapper) //
				.build();

		assertThat(built.type()).isEqualTo(WIDE_MAP);
		assertThat(built.getPropertyName()).isEqualTo("prop");

		MyMultiKey multiKey = new MyMultiKey();
		assertThat(built.getKey((Object) multiKey)).isInstanceOf(MyMultiKey.class);
		assertThat(built.getKeyClass()).isEqualTo(MyMultiKey.class);
		assertThat(built.getKeySerializer()).isNull();

		assertThat(built.getMultiKeyProperties().getComponentClasses()).isSameAs(componentClasses);

		assertThat(built.getMultiKeyProperties().getComponentGetters()).isSameAs(componentGetters);

		assertThat(built.getMultiKeyProperties().getComponentSetters()).isSameAs(componentSetters);

		assertThat(built.getValueFromString("\"val\"")).isInstanceOf(String.class);
		assertThat(built.getValueClass()).isEqualTo(String.class);
		assertThat(built.getValueSerializer().getComparatorType()).isEqualTo(
				STRING_SRZ.getComparatorType());

		assertThat(built.type().isLazy()).isTrue();
		assertThat(built.isSingleKey()).isFalse();
		assertThat(built.type().isJoinColumn()).isFalse();
	}

	private String writeString(Object value) throws Exception
	{
		return mapper.writeValueAsString(value);
	}
}
