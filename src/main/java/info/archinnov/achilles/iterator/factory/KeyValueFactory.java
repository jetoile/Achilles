package info.archinnov.achilles.iterator.factory;

import info.archinnov.achilles.entity.EntityHelper;
import info.archinnov.achilles.entity.JoinEntityHelper;
import info.archinnov.achilles.entity.metadata.EntityMeta;
import info.archinnov.achilles.entity.metadata.PropertyMeta;
import info.archinnov.achilles.entity.type.KeyValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import me.prettyprint.hector.api.beans.Composite;
import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.HCounterColumn;

import com.google.common.collect.Lists;

/**
 * KeyValueFactory
 * 
 * @author DuyHai DOAN
 * 
 */
public class KeyValueFactory
{
	private JoinEntityHelper joinHelper = new JoinEntityHelper();
	private EntityHelper helper = new EntityHelper();
	private CompositeTransformer compositeTransformer = new CompositeTransformer();
	private DynamicCompositeTransformer dynamicCompositeTransformer = new DynamicCompositeTransformer();

	// Dynamic Composite
	public <K, V> KeyValue<K, V> createKeyValueForDynamicComposite(PropertyMeta<K, V> propertyMeta,
			HColumn<DynamicComposite, String> hColumn)
	{
		return dynamicCompositeTransformer.buildKeyValueFromDynamicComposite(propertyMeta, hColumn);
	}

	public <K, V> K createKeyForDynamicComposite(PropertyMeta<K, V> propertyMeta,
			HColumn<DynamicComposite, String> hColumn)
	{
		return dynamicCompositeTransformer.buildKeyFromDynamicComposite(propertyMeta, hColumn);
	}

	public <K, V> V createValueForDynamicComposite(PropertyMeta<K, V> propertyMeta,
			HColumn<DynamicComposite, String> hColumn)
	{
		return dynamicCompositeTransformer.buildValueFromDynamicComposite(propertyMeta, hColumn);
	}

	public Integer createTtlForDynamicComposite(HColumn<DynamicComposite, ?> hColumn)
	{
		return hColumn.getTtl();
	}

	public <K, V> List<V> createValueListForDynamicComposite(PropertyMeta<K, V> propertyMeta,
			List<HColumn<DynamicComposite, String>> hColumns)
	{
		return Lists.transform(hColumns,
				dynamicCompositeTransformer.buildValueTransformer(propertyMeta));
	}

	@SuppressWarnings(
	{
			"unchecked",
			"rawtypes"
	})
	public <K, V> List<V> createJoinValueListForDynamicComposite(PropertyMeta<K, V> propertyMeta,
			List<HColumn<DynamicComposite, String>> hColumns)
	{
		List<?> joinIds = Lists.transform(hColumns,
				dynamicCompositeTransformer.buildRawValueTransformer(propertyMeta));
		Map<?, V> joinEntities = joinHelper.loadJoinEntities(propertyMeta.getValueClass(), joinIds,
				(EntityMeta) propertyMeta.getJoinProperties().getEntityMeta());
		List<V> result = new ArrayList<V>();
		for (Object joinId : joinIds)
		{

			V value = joinEntities.get(joinId);
			V proxy = helper.buildProxy(value, propertyMeta.joinMeta());
			result.add(proxy);
		}

		return result;
	}

	public <K, V> List<K> createKeyListForDynamicComposite(PropertyMeta<K, V> propertyMeta,
			List<HColumn<DynamicComposite, String>> hColumns)
	{
		return Lists.transform(hColumns,
				dynamicCompositeTransformer.buildKeyTransformer(propertyMeta));
	}

	public <K, V> List<KeyValue<K, V>> createKeyValueListForDynamicComposite(
			PropertyMeta<K, V> propertyMeta, List<HColumn<DynamicComposite, String>> hColumns)
	{
		return Lists.transform(hColumns,
				dynamicCompositeTransformer.buildKeyValueTransformer(propertyMeta));
	}

	@SuppressWarnings(
	{
			"unchecked",
			"rawtypes"
	})
	public <K, V> List<KeyValue<K, V>> createJoinKeyValueListForDynamicComposite(
			PropertyMeta<K, V> propertyMeta, List<HColumn<DynamicComposite, String>> hColumns)
	{
		List<K> keys = Lists.transform(hColumns,
				dynamicCompositeTransformer.buildKeyTransformer(propertyMeta));
		List<Object> joinIds = Lists.transform(hColumns,
				dynamicCompositeTransformer.buildRawValueTransformer(propertyMeta));

		Map<Object, V> joinEntities = joinHelper.loadJoinEntities(propertyMeta.getValueClass(),
				joinIds, (EntityMeta) propertyMeta.getJoinProperties().getEntityMeta());
		List<Integer> ttls = Lists.transform(hColumns,
				dynamicCompositeTransformer.buildTtlTransformer());

		List<KeyValue<K, V>> result = new ArrayList<KeyValue<K, V>>();

		for (int i = 0; i < keys.size(); i++)
		{
			V value = joinEntities.get(joinIds.get(i));
			V proxy = helper.buildProxy(value, propertyMeta.joinMeta());
			result.add(new KeyValue<K, V>(keys.get(i), proxy, ttls.get(i)));
		}
		return result;
	}

	// Composite

	public <K, V> KeyValue<K, V> createKeyValueForComposite(PropertyMeta<K, V> propertyMeta,
			HColumn<Composite, ?> hColumn)
	{
		return compositeTransformer.buildKeyValueFromComposite(propertyMeta, hColumn);
	}

	public <K, V> K createKeyForComposite(PropertyMeta<K, V> propertyMeta,
			HColumn<Composite, ?> hColumn)
	{
		return compositeTransformer.buildKeyFromComposite(propertyMeta, hColumn);
	}

	public <K, V> V createValueForComposite(PropertyMeta<K, V> propertyMeta,
			HColumn<Composite, ?> hColumn)
	{
		return compositeTransformer.buildValueFromComposite(propertyMeta, hColumn);
	}

	public Integer createTtlForComposite(HColumn<Composite, ?> hColumn)
	{
		return hColumn.getTtl();
	}

	public <K, V> List<V> createValueListForComposite(PropertyMeta<K, V> propertyMeta,
			List<HColumn<Composite, ?>> hColumns)
	{
		return Lists.transform(hColumns, compositeTransformer.buildValueTransformer(propertyMeta));
	}

	public <K, V> List<K> createKeyListForComposite(PropertyMeta<K, V> propertyMeta,
			List<HColumn<Composite, ?>> hColumns)
	{
		return Lists.transform(hColumns, compositeTransformer.buildKeyTransformer(propertyMeta));
	}

	@SuppressWarnings(
	{
			"unchecked",
			"rawtypes"
	})
	public <K, V> List<V> createJoinValueListForComposite(PropertyMeta<K, V> propertyMeta,
			List<HColumn<Composite, ?>> hColumns)
	{
		List<?> joinIds = Lists
				.transform(hColumns, compositeTransformer.buildRawValueTransformer());
		Map<?, V> joinEntities = joinHelper.loadJoinEntities(propertyMeta.getValueClass(), joinIds,
				(EntityMeta) propertyMeta.getJoinProperties().getEntityMeta());
		List<V> result = new ArrayList<V>();
		for (Object joinId : joinIds)
		{
			V joinEntity = joinEntities.get(joinId);
			V proxy = helper.buildProxy(joinEntity, propertyMeta.getJoinProperties()
					.getEntityMeta());
			result.add(proxy);
		}

		return result;
	}

	public <K, V> List<KeyValue<K, V>> createKeyValueListForComposite(
			PropertyMeta<K, V> propertyMeta, List<HColumn<Composite, ?>> hColumns)
	{
		return Lists.transform(hColumns,
				compositeTransformer.buildKeyValueTransformer(propertyMeta));
	}

	@SuppressWarnings(
	{
			"unchecked",
			"rawtypes"
	})
	public <K, V> List<KeyValue<K, V>> createJoinKeyValueListForComposite(
			PropertyMeta<K, V> propertyMeta, List<HColumn<Composite, ?>> hColumns)
	{
		List<K> keys = Lists.transform(hColumns,
				compositeTransformer.buildKeyTransformer(propertyMeta));
		List<Object> joinIds = Lists.transform(hColumns,
				compositeTransformer.buildRawValueTransformer());
		Map<Object, V> joinEntities = joinHelper.loadJoinEntities(propertyMeta.getValueClass(),
				joinIds, (EntityMeta) propertyMeta.getJoinProperties().getEntityMeta());
		List<Integer> ttls = Lists.transform(hColumns, compositeTransformer.buildTtlTransformer());

		List<KeyValue<K, V>> result = new ArrayList<KeyValue<K, V>>();

		for (int i = 0; i < keys.size(); i++)
		{
			V joinEntity = joinEntities.get(joinIds.get(i));
			V proxy = helper.buildProxy(joinEntity, propertyMeta.getJoinProperties()
					.getEntityMeta());
			result.add(new KeyValue<K, V>(keys.get(i), proxy, ttls.get(i)));
		}
		return result;
	}

	// Counter
	public <K> KeyValue<K, Long> createCounterKeyValueForDynamicComposite(
			PropertyMeta<K, Long> propertyMeta, HCounterColumn<DynamicComposite> hColumn)
	{
		return dynamicCompositeTransformer.buildCounterKeyValueFromDynamicComposite(propertyMeta,
				hColumn);
	}

	public <K> K createCounterKeyForDynamicComposite(PropertyMeta<K, Long> propertyMeta,
			HCounterColumn<DynamicComposite> hColumn)
	{
		return dynamicCompositeTransformer.buildCounterKeyFromDynamicComposite(propertyMeta,
				hColumn);
	}

	public <K> Long createCounterValueForDynamicComposite(PropertyMeta<K, Long> propertyMeta,
			HCounterColumn<DynamicComposite> hColumn)
	{
		return dynamicCompositeTransformer.buildCounterValueFromDynamicComposite(propertyMeta,
				hColumn);
	}

	public <K> List<KeyValue<K, Long>> createCounterKeyValueListForDynamicComposite(
			PropertyMeta<K, Long> propertyMeta, List<HCounterColumn<DynamicComposite>> hColumns)
	{
		return Lists.transform(hColumns,
				dynamicCompositeTransformer.buildCounterKeyValueTransformer(propertyMeta));
	}

	public <K> List<Long> createCounterValueListForDynamicComposite(
			PropertyMeta<K, Long> propertyMeta, List<HCounterColumn<DynamicComposite>> hColumns)
	{
		return Lists.transform(hColumns,
				dynamicCompositeTransformer.buildCounterValueTransformer(propertyMeta));
	}

	public <K> List<K> createCounterKeyListForDynamicComposite(PropertyMeta<K, Long> propertyMeta,
			List<HCounterColumn<DynamicComposite>> hColumns)
	{
		return Lists.transform(hColumns,
				dynamicCompositeTransformer.buildCounterKeyTransformer(propertyMeta));
	}
}
