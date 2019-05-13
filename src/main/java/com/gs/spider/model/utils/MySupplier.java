package com.gs.spider.model.utils;

/**
 * MySupplier
 *
 * @author Gao Shen
 * @version 16/2/27
 */
@FunctionalInterface
public interface MySupplier<T> {

    /**
     * Gets a result.
     *
     * @return a result
     */
    T get() throws Exception;
}

// 2019-04-29 23:10 为了配合实现 lambda 表达式而专门设立的类