package org.apache.ibatis.reflection.property.test;

import org.apache.ibatis.reflection.property.PropertyTokenizer;

public class PropertyTokenizerTest {
	public static void main(String[] args) {
		// 多种形式的属性表达式
		// String fullName = "order[0].item[1].name";
		// String fullName = "order.item[1].name";
		String fullName = "order.item.name";
		PropertyTokenizer propertyTokenizer = new PropertyTokenizer(fullName);
		do {
			printPropertyTokenizerInfo(propertyTokenizer);
			propertyTokenizer = propertyTokenizer.next();
		} while (propertyTokenizer.hasNext());
		printPropertyTokenizerInfo(propertyTokenizer);
	}
	
	public static void printPropertyTokenizerInfo(PropertyTokenizer propertyTokenizer) {
		System.out.println("fullName: " + propertyTokenizer.getFullName());
		System.out.println("name: " + propertyTokenizer.getName());
		System.out.println("index: " + propertyTokenizer.getIndex());
		System.out.println("indexedName: " + propertyTokenizer.getIndexedName());
		System.out.println("children: " + propertyTokenizer.getChildren());
		System.out.println();
	} 
}
