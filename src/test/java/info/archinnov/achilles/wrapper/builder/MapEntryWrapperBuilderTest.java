package info.archinnov.achilles.wrapper.builder;

import static org.fest.assertions.api.Assertions.assertThat;
import info.archinnov.achilles.entity.EntityHelper;
import info.archinnov.achilles.entity.metadata.PropertyMeta;
import info.archinnov.achilles.wrapper.MapEntryWrapper;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import mapping.entity.CompleteBean;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * MapEntryWrapperBuilderTest
 * 
 * @author DuyHai DOAN
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class MapEntryWrapperBuilderTest
{

	@Mock
	private Map<Method, PropertyMeta<?, ?>> dirtyMap;

	private Method setter;

	@Mock
	private EntityHelper helper;

	@Mock
	private PropertyMeta<Integer, String> propertyMeta;

	@Before
	public void setUp() throws Exception
	{
		setter = CompleteBean.class.getDeclaredMethod("setFriends", List.class);
	}

	@Test
	public void should_build() throws Exception
	{
		Map<Integer, String> map = new HashMap<Integer, String>();
		map.put(1, "FR");
		map.put(2, "Paris");
		map.put(3, "75014");
		Entry<Integer, String> mapEntry = map.entrySet().iterator().next();

		MapEntryWrapper<Integer, String> wrapper = MapEntryWrapperBuilder //
				.builder(mapEntry)//
				.dirtyMap(dirtyMap) //
				.setter(setter) //
				.propertyMeta(propertyMeta) //
				.helper(helper) //
				.build();

		assertThat(wrapper.getTarget()).isSameAs(mapEntry);
		assertThat(wrapper.getDirtyMap()).isSameAs(dirtyMap);
		assertThat(Whitebox.getInternalState(wrapper, "setter")).isSameAs(setter);
		assertThat(Whitebox.getInternalState(wrapper, "propertyMeta")).isSameAs(propertyMeta);
		assertThat(Whitebox.getInternalState(wrapper, "helper")).isSameAs(helper);

	}
}
