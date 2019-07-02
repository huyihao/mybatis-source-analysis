package org.apache.ibatis.reflection.test.helper;

import java.util.ArrayList;
import java.util.List;

public class User {
	private String id;
	private Tele tele;
	private List<Order> orders;
	private Tele phone;
	
	public User() {}
	
	public User(String id, Tele tele, List<Order> orders) {
		this.id = id;
		this.tele = tele;
		this.orders = orders;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Tele getTele() {
		return tele;
	}

	public void setTele(Tele tele) {
		this.tele = tele;
	}

	public List<Order> getOrders() {
		return orders;
	}

	public void setOrders(List<Order> orders) {
		this.orders = orders;
	}
	
	public void addOrder(Order order) {
		if (orders == null) {
			orders = new ArrayList<Order>();
		}
		orders.add(order);
	}

	public Tele getPhone() {
		return phone;
	}

	public void setPhone(Tele phone) {
		this.phone = phone;
	}
}
