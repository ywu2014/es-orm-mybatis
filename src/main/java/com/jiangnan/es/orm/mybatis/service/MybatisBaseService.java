/**
 * Copyright (c) 2015-2016 yejunwu126@126.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.jiangnan.es.orm.mybatis.service;

import com.jiangnan.es.common.entity.query.Page;
import com.jiangnan.es.common.service.BaseService;

/**
 * @description 基于mybatis的业务层接口,提供基于mybatis page helper插件的分页方法
 * @author ywu@wuxicloud.com
 * 2016年1月19日 下午1:53:55
 */
public interface MybatisBaseService<T> extends BaseService<T> {
	/**
	 * 分页
	 * @param params
	 * @return
	 */
	<E> Page<E> list(T params);
}
