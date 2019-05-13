package com.gs.spider.model.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * ResultBundleBuilder
 *
 * @author Gao Shen
 * @version 16/2/27
 */
@Component
@Scope("singleton")
public class ResultBundleBuilder {
    private Logger LOG = LogManager.getLogger(ResultBundleBuilder.class);

    // 当 supplier 里面封装的是一个对象的时候
    public <T> ResultBundle<T> bundle(String keyword, MySupplier<T> supplier) {
        ResultBundle<T> resultBundle;
        long start = System.currentTimeMillis();
        try {
            T t = supplier.get();
            resultBundle = new ResultBundle<>(t, keyword, System.currentTimeMillis() - start);
        } catch (Exception e) {
            resultBundle = new ResultBundle<>(keyword, System.currentTimeMillis() - start, false, e.getClass().getName() + ":" + e.getLocalizedMessage());
            e.printStackTrace();
        }
        return resultBundle;
    }

    // 当 supplier 里面封装的是一个列表的时候
    public <T> ResultListBundle<T> listBundle(String keyword, MySupplier<? extends Collection<T>> supplier) {
        ResultListBundle<T> resultBundle;
        long start = System.currentTimeMillis();
        try {
            Collection<T> t = supplier.get();
            resultBundle = new ResultListBundle<>(t, keyword, System.currentTimeMillis() - start);
        } catch (Exception e) {
            resultBundle = new ResultListBundle<>(keyword, System.currentTimeMillis() - start, false, e.getClass().getName() + ":" + e.getLocalizedMessage());
            e.printStackTrace();
        }
        return resultBundle;
    }
}

// 2019-04-29 23:23 不是很明白，这个 Bundle 的意义在哪里
// 2019-05-05 22:47 Bundle 采用单例模式，更像是一个随时准备封装结果集的工厂；而 supplier 则像是为了使“类型”这个概念与 Bundle 松耦合而做的“载体”