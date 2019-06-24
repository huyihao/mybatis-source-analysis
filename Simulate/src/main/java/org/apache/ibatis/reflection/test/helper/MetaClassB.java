package org.apache.ibatis.reflection.test.helper;

public class MetaClassB {
	public String name;
	public Double price;
	public int num;
	
	public MetaClassB(String name, Double price, int num) {
		this.name = name;
		this.price = price;
		this.num = num;
	}
	
	@Override
	public String toString() {
		return "{ name:" + name + ", price:" + price + ", num:" + num + " }";
	}
}
