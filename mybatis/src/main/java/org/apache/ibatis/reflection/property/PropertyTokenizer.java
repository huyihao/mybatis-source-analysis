package org.apache.ibatis.reflection.property;

import java.util.Iterator;

/**
 * mybatis中经常有一些属性表达式 ，当从数据库里查询数据映射到resultMap对象的JavaBean对象时，如果JavaBean里
 * 又嵌套了JavaBean，需要将级联数据关联到子JavaBean对象的属性中，eg:
 * 
 * User类中有类型为List<Order>的属性order，Order类中又有类型为List<Item>的属性item，Item类中有String属性name
 * 现在需要把从数据库里查到的数据映射的字段赋值给name，这里面有多层嵌套关系
 * 
 * 因此需要支持通过如下形式进行映射:
 * <resultMap id="xx" type="User">
 * 	 <id column="id" property="id">
 *   <result property="order[0].item[0].name" column="item1"/>
 *   <result property="order[1].item[1].name" column="item2"/>
 * </resultMap>
 * 
 */
public class PropertyTokenizer implements Iterable<PropertyTokenizer>, Iterator<PropertyTokenizer> {
	
	private String fullName;
	private String name;
	private String index;
	private String indexedName;
	private String children;
	
	public PropertyTokenizer(String fullName) {
		this.fullName = fullName;
		int pointIndex = fullName.indexOf('.');
		if (pointIndex > -1) {
			indexedName = fullName.substring(0, pointIndex);
			children = fullName.substring(pointIndex + 1);
		} else {
			indexedName = fullName;
			children = null;
		}
		name = indexedName;
		int bracketIndex = indexedName.indexOf('[');
		if (bracketIndex > -1) {
			index = indexedName.substring(bracketIndex + 1, indexedName.length() - 1);
			name = indexedName.substring(0, bracketIndex);
		}
	}

	@Override
	public Iterator<PropertyTokenizer> iterator() {
		return this;
	}

	@Override
	public boolean hasNext() {
		return children != null;
	}

	@Override
	public PropertyTokenizer next() {
		return new PropertyTokenizer(children);
	}

	@Override
	public void remove() {
	    throw new UnsupportedOperationException("Remove is not supported, as it has no meaning in the context of properties.");
	}	
	
	public String getFullName() {
		return fullName;
	}

	public String getName() {
		return name;
	}

	public String getIndex() {
		return index;
	}

	public String getIndexedName() {
		return indexedName;
	}

	public String getChildren() {
		return children;
	}
}
