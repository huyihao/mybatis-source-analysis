package org.apache.ibatis.parsing.upd;

import java.util.Properties;

import org.apache.ibatis.parsing.TokenHandler;

public class PropertyParserUpd {
	private PropertyParserUpd() {}
	
	public static String parse(String string, Properties variables) {
		VariableTokenHandler handler = new VariableTokenHandler(variables);
		GenericTokenParserUpd parser = new GenericTokenParserUpd("${", "}", handler);		
		return parser.parse(string);
	}
	
	private static class VariableTokenHandler implements TokenHandler {
		private Properties variables;
		
		public VariableTokenHandler(Properties variables) {
			this.variables = variables;
		}
		
		@Override
		public String handleToken(String context) {
			if (variables != null && variables.containsKey(context)) {
				return variables.getProperty(context);
			}
			
			return "${" + context + "}";
		}
		
	}
}
