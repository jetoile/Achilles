package info.archinnov.achilles.wrapper.builder;

import info.archinnov.achilles.composite.factory.CompositeKeyFactory;
import info.archinnov.achilles.dao.GenericCompositeDao;
import info.archinnov.achilles.entity.metadata.PropertyMeta;
import info.archinnov.achilles.helper.CompositeHelper;
import info.archinnov.achilles.iterator.factory.IteratorFactory;
import info.archinnov.achilles.iterator.factory.KeyValueFactory;
import info.archinnov.achilles.proxy.interceptor.AchillesInterceptor;
import info.archinnov.achilles.wrapper.ExternalWideMapWrapper;

/**
 * ExternalWideMapWrapperBuilder
 * 
 * @author DuyHai DOAN
 * 
 */
public class ExternalWideMapWrapperBuilder<ID, K, V>
{
	private ID id;
	private GenericCompositeDao<ID, V> dao;
	private PropertyMeta<K, V> wideMapMeta;
	private AchillesInterceptor interceptor;
	private CompositeHelper compositeHelper;
	private KeyValueFactory keyValueFactory;
	private IteratorFactory iteratorFactory;
	private CompositeKeyFactory compositeKeyFactory;

	public ExternalWideMapWrapperBuilder(ID id, GenericCompositeDao<ID, V> dao,
			PropertyMeta<K, V> wideMapMeta)
	{
		this.id = id;
		this.dao = dao;
		this.wideMapMeta = wideMapMeta;
	}

	public static <ID, K, V> ExternalWideMapWrapperBuilder<ID, K, V> builder(ID id,
			GenericCompositeDao<ID, V> dao, PropertyMeta<K, V> wideMapMeta)
	{
		return new ExternalWideMapWrapperBuilder<ID, K, V>(id, dao, wideMapMeta);
	}

	public ExternalWideMapWrapperBuilder<ID, K, V> interceptor(AchillesInterceptor interceptor)
	{
		this.interceptor = interceptor;
		return this;
	}

	public ExternalWideMapWrapperBuilder<ID, K, V> compositeHelper(CompositeHelper compositeHelper)
	{
		this.compositeHelper = compositeHelper;
		return this;
	}

	public ExternalWideMapWrapperBuilder<ID, K, V> keyValueFactory(KeyValueFactory keyValueFactory)
	{
		this.keyValueFactory = keyValueFactory;
		return this;
	}

	public ExternalWideMapWrapperBuilder<ID, K, V> iteratorFactory(IteratorFactory iteratorFactory)
	{
		this.iteratorFactory = iteratorFactory;
		return this;
	}

	public ExternalWideMapWrapperBuilder<ID, K, V> compositeKeyFactory(
			CompositeKeyFactory compositeKeyFactory)
	{
		this.compositeKeyFactory = compositeKeyFactory;
		return this;
	}

	public ExternalWideMapWrapper<ID, K, V> build()
	{
		ExternalWideMapWrapper<ID, K, V> wrapper = new ExternalWideMapWrapper<ID, K, V>();
		wrapper.setId(id);
		wrapper.setDao(dao);
		wrapper.setWideMapMeta(wideMapMeta);
		wrapper.setInterceptor(interceptor);
		wrapper.setCompositeHelper(compositeHelper);
		wrapper.setCompositeKeyFactory(compositeKeyFactory);
		wrapper.setIteratorFactory(iteratorFactory);
		wrapper.setKeyValueFactory(keyValueFactory);
		return wrapper;
	}

}
