# mybatis-source-analysis
本仓库用来存放解读mybatis源码中用到的一些技术的使用示例，以及通过分析重写一步步演化mybatis系统的构建过程！

## 提交记录

* (1) 使用DOM解析XML的示例
* (2) 使用XPath查询XML的示例
* (3) 构建通用占位符解析器GenericTokenParser及其优化
* (4) 构建包装了Node节点类XNode
* (5) 构建反射模块中的核心类Reflector
* (6) 构建反射模块中解析字段、方法返回值、方法参数类型的类TypeParameterResolver
* (7) 构建属性表达式解析工具类PropertyTokenizer
* (8) 构建可解析属性表达式并获取指定属性描述信息的类MetaClass
* (9) 构建可根据对象类型获取对象的类元信息、解析属性表达式、获取设置属性值的类MetaObject
* (10) 构建对象包装类BeanWrapper、MapWrapper、CollectionWrapper，分别包装普通JavaBean、Map、Collection类型对象
* (11) 构建类型转换器基础接口TypeHandler、解析类型引用的抽象类TypeReference、类型转换器基类BaseTypeHandler，所有类型的类型转换器       