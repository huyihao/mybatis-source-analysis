package org.apache.ibatis.mapping;

import java.util.Collections;
import java.util.Map;

import org.apache.ibatis.session.Configuration;

/**
 * 鉴别器
 * 对应<resultMap> 节点中的<discriminator> 节点
 */
public class Discriminator {

	private ResultMapping resultMapping;
	private Map<String, String> discriminatorMap;

	Discriminator() {
	}

	public static class Builder {
		private Discriminator discriminator = new Discriminator();

		public Builder(Configuration configuration, ResultMapping resultMapping, Map<String, String> discriminatorMap) {
			discriminator.resultMapping = resultMapping;
			discriminator.discriminatorMap = discriminatorMap;
		}

		public Discriminator build() {
			// assert [boolean 表达式]
			// 如果为true，则程序继续执行
			// 如果为false，则程序抛出AssertionError，并终止行为
			assert discriminator.resultMapping != null;
			assert discriminator.discriminatorMap != null;
			assert !discriminator.discriminatorMap.isEmpty();
			discriminator.discriminatorMap = Collections.unmodifiableMap(discriminator.discriminatorMap);
			return discriminator;
		}
	}

	public ResultMapping getResultMapping() {
		return resultMapping;
	}

	public Map<String, String> getDiscriminatorMap() {
		return discriminatorMap;
	}

	public String getMapIdFor(String s) {
		return discriminatorMap.get(s);
	}

}
