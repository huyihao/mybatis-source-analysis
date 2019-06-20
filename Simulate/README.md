# Simulate工程
本工程用来对重构过程中的组件模块进行测试，对于一些我觉得是bug的代码进行优化和测试

## 代码规则说明

* (1) 对于包名为package中的代码xx.java，对应的测试案例放在package.test包中，案例名一般为xxTest.java
* (2) 对于笔者觉得有问题的代码，重构后的代码放在mybatis2工程中，包名为package.upd，文件名为xxUpd.java
* (3) 对于优化代码的测试案例，规则同(1)，即包名为package.upd.test，案例文件名为xxUpdTest.java

## 提交记录
* (1) 新增PropertyParser测试案例
* (2) 新增XNode测试案例
* (3) 新增Reflector测试案例
* (4) 新增TypeParameterResolver测试案例
* (5) 新增PropertyTokenizer测试案例