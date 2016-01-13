/**
 * Copyright (c) 2015-2016 yejunwu126@126.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.jiangnan.es.orm.mybatis.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

/**
 * @description mybatis mapper xml 生成器
 * @author ywu@wuxicloud.com
 * 2016年1月12日 下午3:46:59
 */
public class MybatisMapperXmlGenerator {
	
	private static final String ROOT_ELEMENT_NAME = "mapper";
	private static final String SAVE_ID = "save";
	private static final String GET_ID = "get";
	private static final String UPDATE_ID = "update";
	private static final String LIST_ID = "list";
	private static final String DELETE_ID = "remove";
	
	/**
	 * 文件生成路径
	 */
	private String exportPath;
	/**
	 * domain 对象
	 */
	private Class<?> domainClazz;
	
	private String domainSimpleName;
	/**
	 * dao 对象
	 */
	private Class<?> daoClazz;
	
	private String exportFileName;
	
	private List<Field> fields;
	private List<String> fieldNames;
	
	/**
	 * 是否包含父类的属性
	 */
	private boolean includeParentProperty = false;
	
	public MybatisMapperXmlGenerator(Class<?> domainClazz, Class<?> daoClazz) {
		this(domainClazz, daoClazz, null);
	}
	
	public MybatisMapperXmlGenerator(Class<?> domainClazz, Class<?> daoClazz, String exportPath) {
		this.exportPath = exportPath;
		this.domainClazz = domainClazz;
		this.domainSimpleName = this.domainClazz.getSimpleName();
		this.daoClazz = daoClazz;
		
		this.exportFileName = this.exportPath + File.separator + this.domainSimpleName + "-mybatis.xml";
	}
	
	/**
	 * 生成文档
	 * @throws IOException
	 */
	public void generate() throws IOException {
		
		resolveFields(domainClazz);
		
		Document document = DocumentHelper.createDocument();
		
		generateDocType(document);
		Element root = generateRoot(document);
		generateInsert(root);
		generateGet(root);
		generateUpdate(root);
		generateList(root);
		generateDelete(root);
		generateResultMap(root);
		
		write(document);
	}
	
	/**
	 * doc type
	 * @param document
	 */
	private void generateDocType(Document document) {
		document.addDocType(ROOT_ELEMENT_NAME, "-//ibatis.apache.org//DTD Mapper 3.0//EN", "http://ibatis.apache.org/dtd/ibatis-3-mapper.dtd");
	}
	
	/**
	 * 文档根节点
	 * @param document
	 */
	private Element generateRoot(Document document) {
		Element root = document.addElement(ROOT_ELEMENT_NAME);
		root.addAttribute("namespace", daoClazz.getName());
		return root;
	}
	
	/**
	 * 生成插入语句
	 * @param document
	 */
	private void generateInsert(Element root) {
		root.addComment("新增");
		Element insert = root.addElement("insert");
		insert.addAttribute("id", SAVE_ID);
		insert.addAttribute("parameterType", this.domainSimpleName);
		StringBuilder sb = new StringBuilder();
		//sb.append("\n");
		//sb.append("\t\t");
		sb.append("INSERT INTO {tableName} (");
		int i = 0;
		int fileNameSize = fieldNames.size();
		//插入字段
		for (String fieldName : fieldNames) {
			i++;
			sb.append(fieldName.toUpperCase());
			if (i != fileNameSize) {
				sb.append(", ");
			}
		}
		sb.append(") VALUES (");
		i = 0;
		//值
		for (String fieldName : fieldNames) {
			i++;
			sb.append("#{");
			sb.append(fieldName);
			sb.append("}");
			if (i != fileNameSize) {
				sb.append(", ");
			}
		}
		sb.append(")");
		//sb.append("\n");
		insert.setText(sb.toString());
	}
	
	/**
	 * 生成获取方法
	 * @param root
	 */
	private void generateGet(Element root) {
		root.addComment("根据ID获取");
		Element get = root.addElement("select");
		get.addAttribute("id", GET_ID);
		get.addAttribute("parameterType", "int");
		get.addAttribute("resultMap", this.domainSimpleName.toLowerCase() + "Map");
		StringBuilder sb = new StringBuilder();
		//sb.append("\n");
		//sb.append("\t\t");
		sb.append("SELECT ");
		int i = 0;
		int fileNameSize = fieldNames.size();
		//查询字段
		for (String fieldName : fieldNames) {
			i++;
			sb.append(fieldName.toUpperCase());
			if (i != fileNameSize) {
				sb.append(", ");
			}
		}
		sb.append(" FROM {tableName} WHERE ID = #{param2}");
		//sb.append("\n");
		get.setText(sb.toString());
	}
	
	/**
	 * 生成更新
	 * @param root
	 */
	private void generateUpdate(Element root) {
		root.addComment("更新");
		Element update = root.addElement("update");
		update.addAttribute("id", UPDATE_ID);
		update.addAttribute("parameterType", this.domainSimpleName);
		//sb.append("\n");
		//sb.append("\t\t");
		update.addText("\n\t\tUPDATE {tableName} ");
		
		Element set = update.addElement("set");
		
		//更新字段
		int i = 0;
		int fileNameSize = fieldNames.size();
		for (String fieldName : fieldNames) {
			i++;
			Element ifElement =set.addElement("if");
			ifElement.addAttribute("test", fieldName + " != null and " + fieldName + " != ''");
			String text = fieldName.toUpperCase() + " = #{" + fieldName + "}";
			if (i != fileNameSize) {
				text += ",";
			}
			ifElement.setText(text);
		}
		update.addText("\n\t\t WHERE ID = #{id}");
		//sb.append("\n");
	}
	
	/**
	 * 生成分页查询
	 * @param root
	 */
	private void generateList(Element root) {
		root.addComment("列表");
		Element list = root.addElement("select");
		list.addAttribute("id", LIST_ID);
		list.addAttribute("resultMap", this.domainSimpleName.toLowerCase() + "Map");
		StringBuilder sb = new StringBuilder();
		//sb.append("\n");
		//sb.append("\t\t");
		sb.append("SELECT ");
		int i = 0;
		int fileNameSize = fieldNames.size();
		//查询字段
		for (String fieldName : fieldNames) {
			i++;
			sb.append(fieldName.toUpperCase());
			if (i != fileNameSize) {
				sb.append(", ");
			}
		}
		sb.append(" FROM {tableName} ");
		//sb.append("\n");
		list.setText(sb.toString());
		
		Element where = list.addElement("where");
		
		for (String fieldName : fieldNames) {
			Element ifElement = where.addElement("if");
			ifElement.addAttribute("test", fieldName + " != null and " + fieldName + " != ''");
			ifElement.setText(fieldName.toUpperCase() + " = #{" + fieldName + "}");
		}
	}
	
	/**
	 * 生成删除
	 * @param root
	 */
	private void generateDelete(Element root) {
		root.addComment("删除");
		Element delete = root.addElement("delete");
		delete.addAttribute("id", DELETE_ID);
		delete.addAttribute("parameterType", "int");
		delete.setText("DELETE FROM {tableName} WHERE ID = #{param2}");
	}
	
	/**
	 * 生成结果映射器
	 * @param root
	 */
	private void generateResultMap(Element root) {
		root.addComment("结果映射器");
		Element resultMap = root.addElement("resultMap");
		resultMap.addAttribute("id", this.domainSimpleName.toLowerCase() + "Map");
		resultMap.addAttribute("type", this.domainSimpleName);
		
		for (String fieldName : fieldNames) {
			Element result = null;
			if (fieldName.equalsIgnoreCase("id")) {
				result = resultMap.addElement("id");
			} else {
				result = resultMap.addElement("result");
			}
			result.addAttribute("column", fieldName.toUpperCase());
			result.addAttribute("property", fieldName);
		}
	}
	
	private void resolveFields(Class<?> clazz) {
		fields = new ArrayList<Field>();
		fieldNames = new ArrayList<String>();
		if (this.includeParentProperty) {
			for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
				addFields(clazz, fields, fieldNames);
			}
		} else {
			addFields(clazz, fields, fieldNames);
		}
	}
	
	private void addFields(Class<?> clazz, List<Field> fields, List<String> fieldNames) {
		Field[] fieldArray = clazz.getDeclaredFields();
		if (null == fieldArray || fieldArray.length <= 0) {
			//throw new RuntimeException("未找到相关field");
			//return;
		} else {
			for (Field field : fieldArray) {
				String fieldName = field.getName();
				if (!"serialVersionUID".equals(fieldName)) {
					fields.add(field);
					fieldNames.add(field.getName());
				}
			}
		}
	}
	
	/**
	 * 输出
	 * @param document
	 * @throws IOException
	 */
	private void write(Document document) throws IOException {
		XMLWriter xmlWriter = null;
		try {
			FileWriter writer = new FileWriter(new File(this.exportFileName));
			OutputFormat format = new OutputFormat();
			format.setEncoding("UTF-8");
			//生成缩进 
			format.setIndent(true);
			format.setIndent("    ");
			//设置换行 
			format.setNewlines(true);
			xmlWriter = new XMLWriter(writer, format);
			xmlWriter.write(document);
		} finally {
			if (null != xmlWriter) {
				xmlWriter.close();
			}
		}
	}

	public boolean isIncludeParentProperty() {
		return includeParentProperty;
	}

	public void setIncludeParentProperty(boolean includeParentProperty) {
		this.includeParentProperty = includeParentProperty;
	}
	
}
