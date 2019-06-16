package org.apache.ibatis.reflection.test.helper;

// 本接口用来说明桥接方法的生成和作用
public interface AInterface<T> {
    void func(T t);
}

/**
 * 进入虚拟机运行是泛型会被擦除，T会被替换成Object，所以代码变成了
 * public interface AInterface<Object> {
 *     void func(Object t);
 * }
 */
