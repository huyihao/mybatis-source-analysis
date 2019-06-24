package org.apache.ibatis.reflection.test;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.reflection.test.helper.MetaClassA;
import org.apache.ibatis.reflection.test.helper.MetaClassB;
import org.apache.ibatis.reflection.test.helper.MetaClassC;

public class MetaClassTest {
	public static void main(String[] args) throws Exception {
		MetaClassA a = initMetaClassA();
		MetaClass metaClassA = MetaClass.forClass(a.getClass(), new DefaultReflectorFactory());
		
		System.out.println("【1. public String findProperty(String name)】");
		System.out.println(metaClassA.findProperty("customer.username"));
		System.out.println();
		
		System.out.println("【2. public String findProperty(String name, boolean useCamelCaseMapping)】");
		System.out.println(metaClassA.findProperty("customer.user_name", true));
		System.out.println();
		
		System.out.println("【3. public String[] getGetterNames()】");
		String[] getterNames = metaClassA.getGetterNames();
		for (int i = 0; i < getterNames.length; i++) {
			System.out.println("getterNames[" + i + "] = " + getterNames[i]);
		}
		System.out.println();
		
		System.out.println("【4. public String[] getSetterNames()】");
		String[] setterNames = metaClassA.getSetterNames();
		for (int i = 0; i < setterNames.length; i++) {
			System.out.println("setterNames[" + i + "] = " + setterNames[i]);
		}
		System.out.println();
		
		System.out.println("【5. public Class<?> getSetterType(String name)】");
		System.out.println(metaClassA.getSetterType("customer.username"));
		System.out.println(metaClassA.getSetterType("customer.loginTimes[0]"));  // 注意这里跟getGetterType的区别
		System.out.println();
		
		System.out.println("【6. public Class<?> getGetterType(String name)】");
		System.out.println(metaClassA.getGetterType("customer.username"));
		System.out.println(metaClassA.getGetterType("customer.loginTimes[0]"));
		System.out.println();
		
		System.out.println("【7. public boolean hasSetter(String name)】");
		System.out.println(metaClassA.hasSetter("customer.username"));
		System.out.println(metaClassA.hasSetter("customer.loginTimes[0]"));
		System.out.println();
		
		System.out.println("【8. public boolean hasGetter(String name)】");
		System.out.println(metaClassA.hasGetter("customer.username"));
		System.out.println(metaClassA.hasGetter("customer.loginTimes[0]"));
		System.out.println();
		
		System.out.println("【9. public Invoker getGetInvoker(String name)】");
		Invoker getInvoker = metaClassA.getGetInvoker("goods");
		System.out.println(getInvoker.getType());
		System.out.println(getInvoker.invoke(a, new Object[] {}));
		System.out.println();
		
		System.out.println("【10. public Invoker getSetInvoker(String name)】");
		Invoker setInvoker = metaClassA.getSetInvoker("customer");
		setInvoker.invoke(a, new Object[] { new MetaClassC("root", "root", null) });
		Invoker customerGetInvoker = metaClassA.getGetInvoker("customer");
		System.out.println(customerGetInvoker.invoke(a, new Object[] {}));
		System.out.println();
		
		System.out.println("【11. public boolean hasDefaultConstructor()】");
		System.out.println(metaClassA.hasDefaultConstructor());
		System.out.println();
		
		System.out.println("【12. public MetaClass metaClassForProperty(String name)】");
		MetaClass metaClassC = metaClassA.metaClassForProperty("customer");
		// 验证获得的MetaClass对象是否有效
		String[] getterNamesC = metaClassC.getGetterNames();
		for (int i = 0; i < getterNamesC.length; i++) {
			System.out.println("getterNames[" + i + "] = " + getterNamesC[i]);
		}		
		System.out.println();
		
		System.out.println("【13. public MetaClass metaClassForProperty(PropertyTokenizer prop)】");
		MetaClass metaClassB = metaClassA.metaClassForProperty(new PropertyTokenizer("goods[0]"));
		String[] getterNamesB = metaClassB.getGetterNames();
		for (int i = 0; i < getterNamesB.length; i++) {
			System.out.println("getterNames[" + i + "] = " + getterNamesB[i]);
		}		
		System.out.println();		
	}
	
	public static MetaClassA initMetaClassA() {
		List<Time> loginTimes = new ArrayList<Time>();
		loginTimes.add(Time.valueOf("15:28:30"));
		
		MetaClassA a = new MetaClassA();
		a.customer = new MetaClassC("huyihao", "123456", loginTimes);
		
		List<MetaClassB> goods = new ArrayList<MetaClassB>();
		goods.add(new MetaClassB("草莓果干", 15.90, 2));
		goods.add(new MetaClassB("猪肉脯", 19.00, 2));
		a.goods = goods;
		
		return a;
	}
}
