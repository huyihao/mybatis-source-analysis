package org.apache.ibatis.logging.jdbc;

import java.util.ArrayList;
import java.util.List;

public class Test {
	public static void main(String[] args) {
		List<Object> typeList = new ArrayList<Object>();
		typeList.add("null");
		Object value = new Integer(10);
		typeList.add(value + "(" + value.getClass().getSimpleName() + ")");
		final String parameters = typeList.toString();
		System.out.println(parameters);
	}
}
