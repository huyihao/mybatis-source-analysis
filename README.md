# mybatis-source-analysis
���ֿ�������Ž��`mybatis`Դ�����õ���һЩ������ʹ��ʾ������Դ�������ϸ��ע�ͣ��Լ�ͨ��������дһ�����ݻ�`mybatis`ϵͳ�Ĺ������̣�

## �汾˵��
mybatis-3.4.1

## �ύ��¼

* (1) ʹ��DOM����XML��ʾ��
* (2) ʹ��XPath��ѯXML��ʾ��
* (3) ����ͨ��ռλ��������`GenericTokenParser`�����Ż�
* (4) ������װ��`Node`�ڵ���`XNode`
* (5) ��������ģ���еĺ�����`Reflector`
* (6) ��������ģ���н����ֶΡ���������ֵ�������������͵���`TypeParameterResolver`
* (7) �������Ա��ʽ����������`PropertyTokenizer`
* (8) �����ɽ������Ա��ʽ����ȡָ������������Ϣ����`MetaClass`
* (9) �����ɸ��ݶ������ͻ�ȡ�������Ԫ��Ϣ���������Ա��ʽ����ȡ��������ֵ����`MetaObject`
* (10) ���������װ��`BeanWrapper`��`MapWrapper`��`CollectionWrapper`���ֱ��װ��ͨJavaBean��Map��Collection���Ͷ���
* (11) ��������ת���������ӿ�`TypeHandler`�������������õĳ�����`TypeReference`������ת��������`BaseTypeHandler`���������͵�����ת����
* (12) ��������ת����ע����`TypeHandlerRegistry`��ע�����ṩ�˳�������ת������ע�ᡢ�����жϺͻ�ȡ�Ĺ���
* (13) ������Դ������`Resources`����ͨ����Դ·��(һ�����ļ�·��)������URL·��ȥ���صõ���Դ��`Resources`����`ClassLoaderWrapper`����Դת��Ϊ���ָ�ʽ
* (14) ���������ע����`TypeAliasRegistry`��֧�ֶԵ�����ָ����Ĭ�ϱ���ע�ᣬҲ֧�ֶ൥���������е������ض�����б���ע��(ע��ı���ȫ������Ĭ�ϵļ�Сд����)
* (15) ������־ģ�飬������ʹ�õ�ͳһ`Log`�ӿڣ�ʹ��������ģʽ�������װ��ͬ����־��ܣ���`LogFactory`����ɹ�����װ������������mybatis��ʹ����־ʱֱ��ʹ��`LogFactory`����`Log`����
* (16) ����JDBC���Դ����࣬`BaseJdbcLogger`�Ǵ�����ĳ�����࣬�����˴�ӡSQL��־ʱ��һЩ����������ʵ�ֵĴ���������`ConnectionLogger`��`PreparedStatementLogger`��`StatementLogger`��`ResultSetLogger`
* (17) ������Դ����ģ��`ResolverUtil`����ָ����������ָ�����µ��࣬������`VFS`�ҵ����·����`VFS`������ʵ����`DefaultVFS`��`JBoss6VFS`
* (18) ��������Դģ�飬ʹ�ù�������ģʽ��ʵ�ַ����ӳ�����Դ����`UnpooledDataSourceFactory`���������ӳ�����Դ`UnpooledDataSource`�������ӳ�����Դ����`PooledDataSourceFactory`��������Դ���ӳ�`PooledDataSource`
* (19) ����`Transaction`ģ�飬ʹ�ù�������ģʽ��ʵ�ּ�JDBC���������`JdbcTransaction`�����������`ManagedTransaction`
* (20) ����`binging`ģ�飬ʹ�ù���ģʽ + ��̬����ʵ��mapper.xml��SQL��ǩ(id)��Mapper�ӿڵķ���(������)�Ķ�̬�󶨣������ƥ������ڳ�ʼ���׶α���