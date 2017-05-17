package com.bandaoti.mybatis.generator;

import java.util.List;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;

public class MysqlSupport implements DBSupport {
	/**
	 * 向&lt;mapper&gt;的子节点中添加内容支持批量和分页查询的sql代码块
	 * 
	 * @author 吴帅
	 * @parameter @param document
	 * @parameter @param introspectedTable
	 * @createDate 2015年9月29日 上午10:20:11
	 */
	@Override
	public void sqlDialect(Document document, IntrospectedTable introspectedTable) {
		// 2.增加批量插入的xml配置
		addBatchInsertSelectiveXml(document, introspectedTable);
		addBatchUpdateXml(document, introspectedTable);
	}

	/**
	 * 增加批量插入的xml配置
	 * 
	 * @author 吴帅
	 * @parameter @param document
	 * @parameter @param introspectedTable
	 * @createDate 2015年8月9日 下午6:57:43
	 */
	@Override
	public void addBatchInsertSelectiveXml(Document document, IntrospectedTable introspectedTable) {
		List<IntrospectedColumn> columns = introspectedTable.getAllColumns();
		//获得要自增的列名
		String incrementField = introspectedTable.getTableConfiguration().getProperties().getProperty("incrementField");
		if(incrementField!=null){
			incrementField = incrementField.toUpperCase();
		}
		XmlElement javaPropertyAndDbType = new XmlElement("trim");
		javaPropertyAndDbType.addAttribute(new Attribute("prefix", " ("));
		javaPropertyAndDbType.addAttribute(new Attribute("suffix", ")"));
		javaPropertyAndDbType.addAttribute(new Attribute("suffixOverrides", ","));
		
		XmlElement insertBatchElement = new XmlElement("insert");
		insertBatchElement.addAttribute(new Attribute("id", "insertBatchSelective"));
		insertBatchElement.addAttribute(new Attribute("parameterType", "java.util.List"));

		XmlElement trim1Element = new XmlElement("trim");
		trim1Element.addAttribute(new Attribute("prefix", "("));
		trim1Element.addAttribute(new Attribute("suffix", ")"));
		trim1Element.addAttribute(new Attribute("suffixOverrides", ","));
		for (IntrospectedColumn introspectedColumn : columns) {
			String columnName = introspectedColumn.getActualColumnName();
			if(!columnName.toUpperCase().equals(incrementField)){//不是自增字段的才会出现在批量插入中
				XmlElement iftest=new XmlElement("if");
				iftest.addAttribute(new Attribute("test","list[0]."+introspectedColumn.getJavaProperty()+"!=null"));
				iftest.addElement(new TextElement(columnName+","));
				trim1Element.addElement(iftest);
				XmlElement trimiftest=new XmlElement("if");
				trimiftest.addAttribute(new Attribute("test","item."+introspectedColumn.getJavaProperty()+"!=null"));
				trimiftest.addElement(new TextElement("#{item." + introspectedColumn.getJavaProperty() + ",jdbcType=" + introspectedColumn.getJdbcTypeName() + "},"));
				javaPropertyAndDbType.addElement(trimiftest);
			}
		}

		XmlElement foreachElement = new XmlElement("foreach");
		foreachElement.addAttribute(new Attribute("collection", "list"));
		foreachElement.addAttribute(new Attribute("index", "index"));
		foreachElement.addAttribute(new Attribute("item", "item"));
		foreachElement.addAttribute(new Attribute("separator", ","));
		insertBatchElement.addElement(new TextElement("insert into " + introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime()));
		insertBatchElement.addElement(trim1Element);
		insertBatchElement.addElement(new TextElement(" values "));
		foreachElement.addElement(javaPropertyAndDbType);
		insertBatchElement.addElement(foreachElement);

		document.getRootElement().addElement(insertBatchElement);
	}
	@Override
	public void addBatchUpdateXml(Document document, IntrospectedTable introspectedTable) {
		List<IntrospectedColumn> columns = introspectedTable.getAllColumns();
		String keyColumn=introspectedTable.getPrimaryKeyColumns().get(0).getActualColumnName();
		
		XmlElement insertBatchElement = new XmlElement("update");
		insertBatchElement.addAttribute(new Attribute("id", "updateBatchByPrimaryKeySelective"));
		insertBatchElement.addAttribute(new Attribute("parameterType", "java.util.List"));
		
		XmlElement foreach=new XmlElement("foreach");
		foreach.addAttribute(new Attribute("collection","list"));
		foreach.addAttribute(new Attribute("item","item"));
		foreach.addAttribute(new Attribute("index","index"));
		foreach.addAttribute(new Attribute("separator",";"));
		
		foreach.addElement(new TextElement("update " + introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime()));

		XmlElement trim1Element = new XmlElement("set");
		for (IntrospectedColumn introspectedColumn : columns) {
			String columnName = introspectedColumn.getActualColumnName();
			if(!columnName.toUpperCase().equalsIgnoreCase(keyColumn)){//不是自增字段的才会出现在批量插入中
				XmlElement ifxml=new XmlElement("if");
				ifxml.addAttribute(new Attribute("test", "item."+introspectedColumn.getJavaProperty()+"!=null"));
				ifxml.addElement(new TextElement(columnName+"=#{item."+introspectedColumn.getJavaProperty()+",jdbcType="+introspectedColumn.getJdbcTypeName() + "},"));
				trim1Element.addElement(ifxml);
			}
		}
		foreach.addElement(trim1Element);

		foreach.addElement(new TextElement("where "));
		int index=0;
		for(IntrospectedColumn i:introspectedTable.getPrimaryKeyColumns()){
			foreach.addElement(new TextElement((index>0?" AND ":"")+i.getActualColumnName()+" = #{item."+i.getJavaProperty()+",jdbcType="+i.getJdbcTypeName()+"}"));
		}
		
		insertBatchElement.addElement(foreach);

		document.getRootElement().addElement(insertBatchElement);
	}

	/**
	 * 在xml文件的查询配置中加入分页支持
	 * 
	 * @author 吴帅
	 * @parameter @param element
	 * @parameter @param preFixId
	 * @parameter @param sufFixId
	 * @createDate 2015年9月29日 上午11:59:06
	 */
	@Override
	public XmlElement adaptSelectByExample(XmlElement element, IntrospectedTable introspectedTable) {
//		XmlElement paginationElement = new XmlElement("include"); //$NON-NLS-1$   
//		paginationElement.addAttribute(new Attribute("refid", "MysqlDialectSuffix"));
//		element.getElements().add(paginationElement);
		System.out.println(element.getName());
		return element;
	}

	/**
	 * 在xml的插入配置增加查询序列配置
	 * 
	 * @author 吴帅
	 * @parameter @param element
	 * @parameter @param introspectedTable
	 * @createDate 2015年9月29日 下午12:00:37
	 */
	@Override
	public void adaptInsertSelective(XmlElement element, IntrospectedTable introspectedTable) {
		System.out.println(element.getName());
	}
}
