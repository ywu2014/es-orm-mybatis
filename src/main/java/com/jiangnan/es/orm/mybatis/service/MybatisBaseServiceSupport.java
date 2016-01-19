/**
 * Copyright (c) 2015-2016 yejunwu126@126.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.jiangnan.es.orm.mybatis.service;

import java.util.List;

import com.jiangnan.es.common.entity.query.Page;
import com.jiangnan.es.common.service.impl.BaseServiceSupport;
import com.jiangnan.es.orm.mybatis.plugin.PageHelper;

/**
 * @description 基于mybatis的业务实现类
 * @author ywu@wuxicloud.com
 * 2016年1月19日 下午2:04:05
 */
public abstract class MybatisBaseServiceSupport<T> extends BaseServiceSupport<T>
	implements MybatisBaseService<T> {
	
	@Override
	public <E> Page<E> list(T params) {
		PageHelper.startPage();
		List<E> users = getRepository().list(params);
		return PageHelper.getPageResult(users);
	}
}
