package info.archinnov.achilles.wrapper.builder;

import info.archinnov.achilles.wrapper.ValueCollectionWrapper;

import java.util.Collection;


/**
 * ValueCollectionWrapperBuilder
 * 
 * @author DuyHai DOAN
 * 
 */
public class ValueCollectionWrapperBuilder<V> extends
		AbstractWrapperBuilder<ValueCollectionWrapperBuilder<V>, Void, V>
{
	private Collection<V> target;

	public ValueCollectionWrapperBuilder(Collection<V> target) {
		this.target = target;
	}

	public static <V> ValueCollectionWrapperBuilder<V> builder(Collection<V> target)
	{
		return new ValueCollectionWrapperBuilder<V>(target);
	}

	public ValueCollectionWrapper<V> build()
	{
		ValueCollectionWrapper<V> valueCollectionWrapper = new ValueCollectionWrapper<V>(
				this.target);
		super.build(valueCollectionWrapper);
		return valueCollectionWrapper;
	}

}
