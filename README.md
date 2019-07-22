# mybatis-source-analysis
本仓库用来存放解读`mybatis`源码中用到的一些技术的使用示例，对源码进行详细的注释，以及通过分析重写一步步演化`mybatis`系统的构建过程！

## 版本说明
mybatis-3.4.1

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
* (15) 构建日志模块，定义框架使用的统一`Log`接口，使用适配器模式来适配包装不同的日志框架，在`LogFactory`中完成功能组装加载适配器，mybatis的使用日志时直接使用`LogFactory`创建`Log`对象
* (16) 构建JDBC调试代理类，`BaseJdbcLogger`是代理类的抽象基类，定义了打印SQL日志时的一些公共操作，实现的代理子类有`ConnectionLogger`、`PreparedStatementLogger`、`StatementLogger`、`ResultSetLogger`
* (17) 构建资源加载模块`ResolverUtil`根据指定条件查找指定包下的类，依赖于`VFS`找到类的路径，`VFS`有两个实现类`DefaultVFS`、`JBoss6VFS`
* (18) 构建数据源模块，使用工厂方法模式，实现非连接池数据源工厂`UnpooledDataSourceFactory`创建非连接池数据源`UnpooledDataSource`，带连接池数据源工厂`PooledDataSourceFactory`创建数据源连接池`PooledDataSource`
* (19) 构建`Transaction`模块，使用工厂方法模式，实现简单JDBC管理的事务`JdbcTransaction`、容器管理的`ManagedTransaction`
* (20) 构建`binging`模块，使用工厂模式 + 动态代理，实现mapper.xml的SQL标签(id)和Mapper接口的方法(方法名)的动态绑定，如果不匹配则会在初始化阶段报错