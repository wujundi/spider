package com.gs.spider.model.utils;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Created by gaoshen on 16/1/5.
 */
public class ResultListBundle<T> extends ResultBundle<T> {
    /**
     * 结果
     */
    private Collection<T> resultList;

    public ResultListBundle(Collection<T> resultList, String keyword, long time) {
        this.resultList = resultList;
        this.keyword = keyword;
        this.time = time;
        this.count = resultList.size();
        this.success = true;
    }

    public ResultListBundle(String keyword, long time, boolean success, String errorMsg) {
        resultList = new LinkedList<>();
        this.success = success;
        this.errorMsg = errorMsg;
        this.keyword = keyword;
        this.time = time;
        this.count = 0;
    }

    public Collection<T> getResultList() {
        return resultList;
    }

    public void setResultList(Collection<T> resultList) {
        this.resultList = resultList;
    }

    @Override
    public String toString() {
        return "ResultListBundle{" +
                "resultList=" + resultList +
                '}';
    }
}

// ResultBundle 的加强版，不止可以存一个 泛型的 result, 还可以存一个泛型的集合 resultList 了