package org.apache.ibatis.reflection.test.helper;

public class Tele {
	private String country;
	private String type;
	private String num;
	
	public Tele() {}
	
	public Tele(String country, String type, String num) {
		this.country = country;
		this.type = type;
		this.num = num;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getNum() {
		return num;
	}

	public void setNum(String num) {
		this.num = num;
	}
	
	@Override
	public String toString() {
		return "{ country:" + country + ", type:" + type + ", num:" + num + " }";
	}
}
