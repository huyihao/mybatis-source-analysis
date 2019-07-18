package org.apache.ibatis.type;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 如果使用注解而不是mapper.xml文件来配置，则表示POJO字段类型跟数据库类型映射关系时会用到本注解，本注解用于标识Jdbc数据类型
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MappedJdbcTypes {
	JdbcType[] value();
	boolean includeNullJdbcType() default false;
}
