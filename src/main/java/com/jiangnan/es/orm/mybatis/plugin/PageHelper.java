/**
 * Copyright (c) 2015-2016 yejunwu126@126.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.jiangnan.es.orm.mybatis.plugin;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.sql.DataSource;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import com.github.pagehelper.Dialect;
import com.github.pagehelper.ISelect;
import com.github.pagehelper.Page;
import com.github.pagehelper.SqlUtil;
import com.github.pagehelper.SqlUtilConfig;
import com.github.pagehelper.StringUtil;
import com.jiangnan.es.common.entity.query.PageContext;

/**
 * @description 分页插件扩展,提供了自动从线程上下文中获取分页参数的方法
 * @author ywu@wuxicloud.com
 * 2016年1月9日 上午10:59:40
 */
@Intercepts(@Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}))
public class PageHelper implements Interceptor {

	//sql工具类
    private SqlUtil sqlUtil;
    //属性参数信息
    private Properties properties;
    //配置对象方式
    private SqlUtilConfig sqlUtilConfig;
    //自动获取dialect,如果没有setProperties或setSqlUtilConfig，也可以正常进行
    private boolean autoDialect = true;
    //运行时自动获取dialect
    private boolean autoRuntimeDialect;
    //缓存
    private Map<String, SqlUtil> urlSqlUtilMap = new ConcurrentHashMap<String, SqlUtil>();

    /**
     * 获取任意查询方法的count总数
     *
     * @param select
     * @return
     */
    public static long count(ISelect select) {
        Page<?> page = startPage(1, -1, true);
        select.doSelect();
        return page.getTotal();
    }
    
    /**
     * 开始分页
     * @return
     */
    public static <E> Page<E> startPage() {
    	Integer currentPage = PageContext.getCurrentPage();
		Integer pageSize = PageContext.getPageSize();
		if (null != currentPage && null != pageSize) {
			 return startPage(currentPage, pageSize, true);
		}
		return null;
    }

    /**
     * 开始分页
     *
     * @param pageNum  页码
     * @param pageSize 每页显示数量
     */
    public static <E> Page<E> startPage(int pageNum, int pageSize) {
        return startPage(pageNum, pageSize, true);
    }

    /**
     * 开始分页
     *
     * @param pageNum  页码
     * @param pageSize 每页显示数量
     * @param count    是否进行count查询
     */
    public static <E> Page<E> startPage(int pageNum, int pageSize, boolean count) {
        return startPage(pageNum, pageSize, count, null);
    }

    /**
     * 开始分页
     *
     * @param pageNum  页码
     * @param pageSize 每页显示数量
     * @param orderBy  排序
     */
    public static <E> Page<E> startPage(int pageNum, int pageSize, String orderBy) {
        Page<E> page = startPage(pageNum, pageSize);
        page.setOrderBy(orderBy);
        return page;
    }

    /**
     * 开始分页
     *
     * @param offset 页码
     * @param limit  每页显示数量
     */
    public static <E> Page<E> offsetPage(int offset, int limit) {
        return offsetPage(offset, limit, true);
    }

    /**
     * 开始分页
     *
     * @param offset 页码
     * @param limit  每页显示数量
     * @param count  是否进行count查询
     */
    public static <E> Page<E> offsetPage(int offset, int limit, boolean count) {
        Page<E> page = new Page<E>(new int[]{offset, limit}, count);
        //当已经执行过orderBy的时候
        Page<E> oldPage = SqlUtil.getLocalPage();
        if (oldPage != null && oldPage.isOrderByOnly()) {
            page.setOrderBy(oldPage.getOrderBy());
        }
        SqlUtil.setLocalPage(page);
        return page;
    }

    /**
     * 开始分页
     *
     * @param offset  页码
     * @param limit   每页显示数量
     * @param orderBy 排序
     */
    public static <E> Page<E> offsetPage(int offset, int limit, String orderBy) {
        Page<E> page = offsetPage(offset, limit);
        page.setOrderBy(orderBy);
        return page;
    }

    /**
     * 开始分页
     *
     * @param pageNum    页码
     * @param pageSize   每页显示数量
     * @param count      是否进行count查询
     * @param reasonable 分页合理化,null时用默认配置
     */
    public static <E> Page<E> startPage(int pageNum, int pageSize, boolean count, Boolean reasonable) {
        return startPage(pageNum, pageSize, count, reasonable, null);
    }

    /**
     * 开始分页
     *
     * @param pageNum      页码
     * @param pageSize     每页显示数量
     * @param count        是否进行count查询
     * @param reasonable   分页合理化,null时用默认配置
     * @param pageSizeZero true且pageSize=0时返回全部结果，false时分页,null时用默认配置
     */
    public static <E> Page<E> startPage(int pageNum, int pageSize, boolean count, Boolean reasonable, Boolean pageSizeZero) {
        Page<E> page = new Page<E>(pageNum, pageSize, count);
        page.setReasonable(reasonable);
        page.setPageSizeZero(pageSizeZero);
        //当已经执行过orderBy的时候
        Page<E> oldPage = SqlUtil.getLocalPage();
        if (oldPage != null && oldPage.isOrderByOnly()) {
            page.setOrderBy(oldPage.getOrderBy());
        }
        SqlUtil.setLocalPage(page);
        return page;
    }

    /**
     * 开始分页
     *
     * @param params
     */
    public static <E> Page<E> startPage(Object params) {
        Page<E> page = SqlUtil.getPageFromObject(params);
        //当已经执行过orderBy的时候
        Page<E> oldPage = SqlUtil.getLocalPage();
        if (oldPage != null && oldPage.isOrderByOnly()) {
            page.setOrderBy(oldPage.getOrderBy());
        }
        SqlUtil.setLocalPage(page);
        return page;
    }

    /**
     * 排序
     *
     * @param orderBy
     */
    @SuppressWarnings("rawtypes")
	public static void orderBy(String orderBy) {
        Page<?> page = SqlUtil.getLocalPage();
        if (page != null) {
            page.setOrderBy(orderBy);
        } else {
            page = new Page();
            page.setOrderBy(orderBy);
            page.setOrderByOnly(true);
            SqlUtil.setLocalPage(page);
        }
    }

    /**
     * 获取orderBy
     *
     * @return
     */
    public static String getOrderBy() {
        Page<?> page = SqlUtil.getLocalPage();
        if (page != null) {
            String orderBy = page.getOrderBy();
            if (StringUtil.isEmpty(orderBy)) {
                return null;
            } else {
                return orderBy;
            }
        }
        return null;
    }

    /**
     * Mybatis拦截器方法
     *
     * @param invocation 拦截器入参
     * @return 返回执行结果
     * @throws Throwable 抛出异常
     */
    public Object intercept(Invocation invocation) throws Throwable {
        if (autoRuntimeDialect) {
            SqlUtil sqlUtil = getSqlUtil(invocation);
            return sqlUtil.processPage(invocation);
        } else {
            if (autoDialect) {
                initSqlUtil(invocation);
            }
            return sqlUtil.processPage(invocation);
        }
    }

    /**
     * 初始化sqlUtil
     *
     * @param invocation
     */
    public synchronized void initSqlUtil(Invocation invocation) {
        if (this.sqlUtil == null) {
            this.sqlUtil = getSqlUtil(invocation);
            if (!autoRuntimeDialect) {
                properties = null;
                sqlUtilConfig = null;
            }
            autoDialect = false;
        }
    }

    /**
     * 根据daatsource创建对应的sqlUtil
     *
     * @param invocation
     */
    public SqlUtil getSqlUtil(Invocation invocation) {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        //改为对dataSource做缓存
        String url;
        DataSource dataSource = ms.getConfiguration().getEnvironment().getDataSource();
        try {
            url = dataSource.getConnection().getMetaData().getURL();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (urlSqlUtilMap.containsKey(url)) {
            return urlSqlUtilMap.get(url);
        }
        ReentrantLock lock = new ReentrantLock();
        try {
            lock.lock();
            if (urlSqlUtilMap.containsKey(url)) {
                return urlSqlUtilMap.get(url);
            }
            if (StringUtil.isEmpty(url)) {
                throw new RuntimeException("无法自动获取jdbcUrl，请在分页插件中配置dialect参数!");
            }
            String dialect = Dialect.fromJdbcUrl(url);
            if (dialect == null) {
                throw new RuntimeException("无法自动获取数据库类型，请通过dialect参数指定!");
            }
            SqlUtil sqlUtil = new SqlUtil(dialect);
            if (this.properties != null) {
                sqlUtil.setProperties(properties);
            } else if (this.sqlUtilConfig != null) {
                sqlUtil.setSqlUtilConfig(this.sqlUtilConfig);
            }
            urlSqlUtilMap.put(url, sqlUtil);
            return sqlUtil;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 只拦截Executor
     *
     * @param target
     * @return
     */
    public Object plugin(Object target) {
        if (target instanceof Executor) {
            return Plugin.wrap(target, this);
        } else {
            return target;
        }
    }

    private void checkVersion() {
        //MyBatis3.2.0版本校验
        try {
            Class.forName("org.apache.ibatis.scripting.xmltags.SqlNode");//SqlNode是3.2.0之后新增的类
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("您使用的MyBatis版本太低，MyBatis分页插件PageHelper支持MyBatis3.2.0及以上版本!");
        }
    }

    /**
     * 设置属性值
     *
     * @param p 属性值
     */
    public void setProperties(Properties p) {
        checkVersion();
        //数据库方言
        String dialect = p.getProperty("dialect");
        String runtimeDialect = p.getProperty("autoRuntimeDialect");
        if (StringUtil.isNotEmpty(runtimeDialect) && runtimeDialect.equalsIgnoreCase("TRUE")) {
            this.autoRuntimeDialect = true;
            this.autoDialect = false;
            this.properties = p;
        } else if (StringUtil.isEmpty(dialect)) {
            autoDialect = true;
            this.properties = p;
        } else {
            autoDialect = false;
            sqlUtil = new SqlUtil(dialect);
            sqlUtil.setProperties(p);
        }
    }

    /**
     * 设置属性值
     *
     * @param config
     */
    public void setSqlUtilConfig(SqlUtilConfig config) {
        checkVersion();
        if (config.isAutoRuntimeDialect()) {
            this.autoRuntimeDialect = true;
            this.autoDialect = false;
            this.sqlUtilConfig = config;
        } else if (StringUtil.isEmpty(config.getDialect())) {
            autoDialect = true;
            this.sqlUtilConfig = config;
        } else {
            autoDialect = false;
            sqlUtil = new SqlUtil(config.getDialect());
            sqlUtil.setSqlUtilConfig(config);
        }
    }
    
    /**
     * 封装返回分页结果
     * @param data
     * @return
     */
    public static <T> com.jiangnan.es.common.entity.query.Page<T> getPageResult(List<T> data) {
    	if (data instanceof Page) {
    		Page<T> page = (Page<T>)data;
    		com.jiangnan.es.common.entity.query.Page<T> wrapperPage = new com.jiangnan.es.common.entity.query.Page<T>(page.getPageNum(), page.getPageSize());
        	wrapperPage.setData(data);
        	wrapperPage.setTotalRecords(page.getTotal());
        	return wrapperPage;
    	}
    	throw new RuntimeException("非法分页数据...");
    }
}
