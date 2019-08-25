package org.apache.ibatis.builder;

import org.apache.ibatis.cache.Cache;

/**
 * cache引用解析器，主要是根据引用的namespace从configration的caches中取到引用namespace的缓存
 */
public class CacheRefResolver {
	private final MapperBuilderAssistant assistant;
	private final String cacheRefNamespace;
	
	public CacheRefResolver(MapperBuilderAssistant assistant, String cacheRefNamespace) {
		this.assistant = assistant;
		this.cacheRefNamespace = cacheRefNamespace;
	}
	
	public Cache resolveCacheRef() {
		return assistant.useCacheRef(cacheRefNamespace);
	}
}
