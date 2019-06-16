package org.apache.ibatis.reflection.test.helper;

import java.util.List;

public class ReflectorDemoSupclass {
	private List<Object> list;
	
	public ReflectorDemoSupclass() {}
	
	public String haha() {
		return "father haha";
	}
	
	public String hehe() {
		return "father hehe";
	}

	public List<Object> getList() {
		return list;
	}

	public void setList(List<Object> list) {
		this.list = list;
	}
}
