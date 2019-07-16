# mybatis-source-analysis
本仓库用来存放解读`mybatis`源码中用到的一些技术的使用示例，对源码进行详细的注释，以及通过分析重写一步步演化`mybatis`系统的构建过程！

## 提交记录

* (1) 使用DOM解析XML的示例
* (2) 使用XPath查询XML的示例
* (3) 构建通用占位符解析器`GenericTokenParser`及其优化
* (4) 构建包装了`Node`节点类`XNode`
* (5) 构建反射模块中的核心类`Reflector`
* (6) 构建反射模块中解析字段、方法返回值、方法参数类型的类`TypeParameterResolver`
* (7) 构建属性表达式解析工具类`PropertyTokenizer`
* (8) 构建可解析属性表达式并获取指定属性描述信息的类`MetaClass`
* (9) 构建可根据对象类型获取对象的类元信息、解析属性表达式、获取设置属性值的类`MetaObject`
* (10) 构建对象包装类`BeanWrapper`、`MapWrapper`、`CollectionWrapper`，分别包装普通JavaBean、Map、Collection类型对象
* (11) 构建类型转换器基础接口`TypeHandler`、解析类型引用的抽象类`TypeReference`、类型转换器基类`BaseTypeHandler`，所有类型的类型转换器
* (12) 构建类型转换器注册器`TypeHandlerRegistry`，注册器提供了常用类型转换器的注册、存在判断和获取的功能
* (13) 构建资源加载类`Resources`，可通过资源路径(一般是文件路径)、网络URL路径去加载得到资源，`Resources`依赖`ClassLoaderWrapper`将资源转化为各种格式
* (14) 构建类别名注册器`TypeAliasRegistry`，支持对单个类指定或默认别名注册，也支持多单个或多个包中的所有特定类进行别名注册(注册的别名全部都是默认的简单小写类名)