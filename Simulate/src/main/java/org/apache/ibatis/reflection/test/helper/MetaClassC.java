package org.apache.ibatis.reflection.test.helper;

import java.sql.Time;
import java.util.List;

public class MetaClassC {
	public String username;
	public String password;
	public List<Time> loginTimes;
	
	public MetaClassC(String username, String password, List<Time> loginTimes) {
		this.username = username;
		this.password = password;
		this.loginTimes = loginTimes;
	}
	
	@Override
	public String toString() {
		return "{ username:" + username + ", password:" + password + ", loginTimes:" + loginTimes + " }";
	}
}
