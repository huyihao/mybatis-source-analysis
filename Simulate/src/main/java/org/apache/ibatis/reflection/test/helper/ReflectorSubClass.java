package org.apache.ibatis.reflection.test.helper;

import java.sql.Date;
import java.sql.Time;
import java.util.HashMap;

public class ReflectorSubClass<V2> extends ReflectorSupClass<String, Integer, Double, Float, V2> 
								   implements ReflectorSupInterface {
	
	private String str;
	private Date date;
	private Time time;
	
	@Override
	public String getStr() {
		return str;
	}
	
	public void setStr(String str) {
		this.str = str;
	}	
	
	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Time getTime() {
		return time;
	}

	public void setTime(Time time) {
		this.time = time;
	}
	
	@Override
	public void haha() {}
	
	@Override	
	public HashMap<String, Integer> getMap() {
		return (HashMap<String, Integer>) super.map;
	}
}
