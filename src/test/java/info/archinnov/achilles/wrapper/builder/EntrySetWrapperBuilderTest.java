package info.archinnov.achilles.wrapper.builder;

import static org.fest.assertions.api.Assertions.assertThat;
import info.archinnov.achilles.entity.EntityHelper;
import info.archinnov.achilles.entity.metadata.PropertyMeta;
import info.archinnov.achilles.wrapper.EntrySetWrapper;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mapping.entity.CompleteBean;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;

/**
 * EntrySetWrapperBuilderTest
 * 
 * @author DuyHai DOAN
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class EntrySetWrapperBuilderTest
{
	@Mock
	private Map<Method, PropertyMeta<?, ?>> dirtyMap;

	private Method setter;

	@Mock
	private PropertyMeta<Integer, String> propertyMeta;

	@Mock
	private EntityHelper helper;

	@Before
	public void setUp() throws Exception
	{
		setter = CompleteBean.class.getDeclaredMethod("setFriends", List.class);

	}

	@Test
	public void should_build() throws Exception
	{
		Map<Integer, String> target = new HashMap<Integer, String>();
		target.put(1, "FR");
		target.put(2, "Paris");
		target.put(3, "75014");

		EntrySetWrapper<Integer, String> wrapper = EntrySetWrapperBuilder
				.builder(target.entrySet()) //
				.dirtyMap(dirtyMap) //
				.setter(setter) //
				.propertyMeta(propertyMeta) //
				.helper(helper) //
				.build();

		assertThat(wrapper.getDirtyMap()).isSameAs(dirtyMap);
		assertThat(Whitebox.getInternalState(wrapper, "target")).isSameAs(target.entrySet());
		assertThat(Whitebox.getInternalState(wrapper, "setter")).isSameAs(setter);
		assertThat(Whitebox.getInternalState(wrapper, "propertyMeta")).isSameAs(propertyMeta);
		assertThat(Whitebox.getInternalState(wrapper, "helper")).isSameAs(helper);

	}
}
