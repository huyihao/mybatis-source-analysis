package org.apache.ibatis.reflection.test.helper;

import java.util.ArrayList;
import java.util.List;

public class ReflectorDemoClass extends ReflectorDemoSupclass 
							    implements ReflectorDemoInterface<String> {
	private int num;
	private String str;
	private Double[] doubleArr;
	private ArrayList<Object> list;
	
	public ReflectorDemoClass() {}
	
	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

	public String getStr() {
		return str;
	}

	public void setStr(String str) {
		this.str = str;
	}

	public Double[] getDoubleArr() {
		return doubleArr;
	}

	public void setDoubleArr(Double[] doubleArr) {
		this.doubleArr = doubleArr;
	}
	
	public ArrayList<Object> getList() {
		return list;
	}

	public void setList(ArrayList<Object> list) {
		this.list = list;
	}
	
	public <T> List<T> testSignature(T para1, String para2) {
		return null;
	}

	@Override
	public void test(String t) {
		System.out.println(t);
	}
	
	// 继承带泛型接口是为了校验一下对桥接方法的过滤处理
	// 编译器会为重载方法生成一个桥接方法，如下所示:
	// public void test(Object t) {
	//     test((String) t);
	// }
	
	// 测试相同方法签名取子类方法
	public String haha() {
		return "son haha";
	}
}
