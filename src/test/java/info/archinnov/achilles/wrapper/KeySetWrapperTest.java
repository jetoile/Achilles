package info.archinnov.achilles.wrapper;

import info.archinnov.achilles.wrapper.KeySetWrapper;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;


/**
 * KeySetWrapperTest
 * 
 * @author DuyHai DOAN
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class KeySetWrapperTest
{

	@Test(expected = UnsupportedOperationException.class)
	public void should_exception_on_add() throws Exception
	{
		KeySetWrapper<Integer> wrapper = new KeySetWrapper<Integer>(new HashSet<Integer>());

		wrapper.add(5);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void should_exception_on_add_all() throws Exception
	{
		KeySetWrapper<Integer> wrapper = new KeySetWrapper<Integer>(new HashSet<Integer>());

		wrapper.addAll(Arrays.asList(5, 7));
	}

}
