package org.apache.ibatis.builder;

import java.util.List;

import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;

public class ResultMapResolver {
	private final MapperBuilderAssistant assistant;
	private String id;
	private Class<?> type;
	private String extend;
	private Discriminator discriminator;
	private List<ResultMapping> resultMappings;
	private Boolean autoMapping;

	public ResultMapResolver(MapperBuilderAssistant assistant, String id, Class<?> type, String extend,
			Discriminator discriminator, List<ResultMapping> resultMappings, Boolean autoMapping) {
		this.assistant = assistant;
		this.id = id;
		this.type = type;
		this.extend = extend;
		this.discriminator = discriminator;
		this.resultMappings = resultMappings;
		this.autoMapping = autoMapping;
	}
	
	public ResultMap resolve() {
		return assistant.addResultMap(id, type, extend, discriminator, resultMappings, autoMapping);
	}
}
