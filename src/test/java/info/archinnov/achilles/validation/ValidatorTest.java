package info.archinnov.achilles.validation;

import info.archinnov.achilles.entity.metadata.PropertyType;
import info.archinnov.achilles.exception.AchillesException;
import info.archinnov.achilles.exception.BeanMappingException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

/**
 * ValidatorTest
 * 
 * @author DuyHai DOAN
 * 
 */
public class ValidatorTest
{

	@Test(expected = AchillesException.class)
	public void should_exception_when_blank() throws Exception
	{
		Validator.validateNotBlank("", "arg");
	}

	@Test(expected = AchillesException.class)
	public void should_exception_when_string_null() throws Exception
	{
		Validator.validateNotBlank(null, "arg");
	}

	@Test(expected = AchillesException.class)
	public void should_exception_when_null() throws Exception
	{
		Validator.validateNotNull(null, "arg");
	}

	@Test(expected = AchillesException.class)
	public void should_exception_when_empty_collection() throws Exception
	{
		Validator.validateNotEmpty(new ArrayList<String>(), "arg");
	}

	@Test(expected = AchillesException.class)
	public void should_exception_when_null_collection() throws Exception
	{
		Validator.validateNotEmpty((Collection<String>) null, "arg");
	}

	@Test(expected = AchillesException.class)
	public void should_exception_when_empty_map() throws Exception
	{
		Validator.validateNotEmpty(new HashMap<String, String>(), "arg");
	}

	@Test(expected = AchillesException.class)
	public void should_exception_when_null_map() throws Exception
	{
		Validator.validateNotEmpty((Map<String, String>) null, "arg");
	}

	@Test(expected = AchillesException.class)
	public void should_exception_when_incorrect_size_amp() throws Exception
	{
		Validator.validateSize(new HashMap<String, String>(), 2, "arg");
	}

	@Test(expected = BeanMappingException.class)
	public void should_exception_when_no_default_constructor() throws Exception
	{
		Validator.validateNoargsConstructor(TestNoArgConstructor.class);
	}

	@Test
	public void should_match_pattern() throws Exception
	{
		Validator.validateRegExp("1_abcd01_sdf", "[a-zA-Z0-9_]+", "arg");
	}

	@Test(expected = AchillesException.class)
	public void should_exception_when_not_matching_pattern() throws Exception
	{
		Validator.validateRegExp("1_a-bcd01_sdf", "[a-zA-Z0-9_]+", "arg");
	}

	@Test
	public void should_instanciate_a_bean() throws Exception
	{
		Validator.validateInstantiable(NormalClass.class);
	}

	@Test(expected = BeanMappingException.class)
	public void should_exception_when_private_class() throws Exception
	{
		Validator.validateInstantiable(PrivateEntity.class);
	}

	@Test(expected = BeanMappingException.class)
	public void should_exception_when_interface() throws Exception
	{
		Validator.validateInstantiable(TestInterface.class);
	}

	@Test(expected = BeanMappingException.class)
	public void should_exception_when_abstract_class() throws Exception
	{
		Validator.validateInstantiable(AbstractClass.class);
	}

	@Test(expected = BeanMappingException.class)
	public void should_exception_when_primitive() throws Exception
	{
		Validator.validateInstantiable(Long.class);
	}

	@Test(expected = BeanMappingException.class)
	public void should_exception_array_type() throws Exception
	{
		String[] array = new String[2];
		Validator.validateInstantiable(array.getClass());
	}

	@Test(expected = BeanMappingException.class)
	public void should_exception_when_type_not_allowed() throws Exception
	{
		Set<Class<?>> allowedTypes = new HashSet<Class<?>>();
		allowedTypes.add(String.class);
		allowedTypes.add(Long.class);
		Validator.validateAllowedTypes(Integer.class, allowedTypes, "");
	}

	@Test
	public void should_validate_primitive_as_serializable() throws Exception
	{
		Validator.validateSerializable(long.class, "");
	}

	@Test
	public void should_validate_enum_as_serializable() throws Exception
	{

		Validator.validateSerializable(PropertyType.class, "");
	}

	@Test
	public void should_validate_allowed_types_as_serializable() throws Exception
	{

		Validator.validateSerializable(UUID.class, "");
	}

	@Test(expected = BeanMappingException.class)
	public void should_exception_when_not_serializable() throws Exception
	{
		Validator.validateSerializable(Validator.class, "");
	}

	@Test(expected = AchillesException.class)
	public void should_exception_when_map_size_does_not_match() throws Exception
	{
		HashMap<Integer, String> map = new HashMap<Integer, String>();
		map.put(1, "1");
		Validator.validateSize(map, 2, "");
	}

	class TestNoArgConstructor
	{
		private TestNoArgConstructor() {}
	}
}
