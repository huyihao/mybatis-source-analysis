package org.apache.ibatis.reflection.test.helper;

import java.util.ArrayList;
import java.util.List;

public class Order {
	private String id;
	private List<Item> items;
	
	public Order() {}
	
	public Order(String id, List<Item> items) {
		this.id = id;
		this.items = items;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<Item> getItems() {
		return items;
	}

	public void setItems(List<Item> items) {
		this.items = items;
	}
	
	public void addItem(Item item) {
		if (items == null) {
			items = new ArrayList<Item>();
		}
		items.add(item);
	}
	
	@Override
	public String toString() {
		return "{ id:" + id + ", items:" + items + " }";
	}
}
