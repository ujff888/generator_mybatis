package com.bandaoti.mybatis.generator;

import static org.mybatis.generator.internal.util.messages.Messages.getString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.Parameter;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.api.dom.xml.Element;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;

/**
 * .支持oracle/mysql数据库分页查询<br/>
 * .支持oracle/mysql数据库插入时自增主键<br/>
 * .支持oracle/mysql数据库批量插入<br/>
 * 
 * @author 吴帅
 * @CreationDate 2015年8月2日
 * @version 1.0
 */
public class CustomPlugin extends PluginAdapter {
	private DBSupport supportPlugin;
	private XmlElement ele=new XmlElement("collection");
	private XmlElement selectEle=new XmlElement("select");
	private boolean isUp=false;
	/**
	 * 修改Model类
	 */
	@Override
	public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
		Map<String,IntrospectedColumn> colMap=new HashMap<>();
		for(IntrospectedColumn col :introspectedTable.getAllColumns()){
			colMap.put(col.getActualColumnName(), col);
		}
		for(String key:colMap.keySet()){
			if(key.toUpperCase().startsWith("PARENT")){
				String childId=key.substring(7);
				IntrospectedColumn ic=colMap.get(childId);
				if(ic!=null){
					isUp=true;
					//属性
					org.mybatis.generator.api.dom.java.Field field=new org.mybatis.generator.api.dom.java.Field();
					String fieldName=introspectedTable.getTableConfiguration().getDomainObjectName().toLowerCase()+"s";
					field.setName(fieldName);
					String type="java.util.List<"+introspectedTable.getTableConfiguration().getDomainObjectName()+">";
					FullyQualifiedJavaType fqjt=new FullyQualifiedJavaType(type);
					field.setType(fqjt);
					field.setVisibility(JavaVisibility.PRIVATE);
					topLevelClass.addImportedType(new FullyQualifiedJavaType("java.util.List"));
					topLevelClass.addField(field);
					
					//get
					Method getMethod = new Method();
					getMethod.setVisibility(JavaVisibility.PUBLIC);
					getMethod.setReturnType(fqjt);
					getMethod.setName("get"+introspectedTable.getTableConfiguration().getDomainObjectName()+"s");
					getMethod.addBodyLine("return "+introspectedTable.getTableConfiguration().getDomainObjectName().toLowerCase()+"s;");
					topLevelClass.addMethod(getMethod);
					//set
					Method setMethod = new Method();
					setMethod.setVisibility(JavaVisibility.PUBLIC);
					setMethod.setName("set"+introspectedTable.getTableConfiguration().getDomainObjectName()+"s");
					setMethod.addParameter(new Parameter(new FullyQualifiedJavaType(type), fieldName));
					setMethod.addBodyLine("this."+fieldName+"="+fieldName+";");
					topLevelClass.addMethod(setMethod);
					ele.addAttribute(new Attribute("property", fieldName));
					ele.addAttribute(new Attribute("column", childId));
					ele.addAttribute(new Attribute("select", "getParentElement"));
					selectEle.addAttribute(new Attribute("id", "getParentElement"));
					selectEle.addAttribute(new Attribute("parameterType", introspectedTable.getPrimaryKeyColumns().get(0).getJdbcTypeName()));
					selectEle.addAttribute(new Attribute("resultMap", "BaseResultMap"));
					selectEle.addElement(new TextElement("select * from "+introspectedTable.getTableConfiguration().getTableName()+" where "+key+"=#{"+childId+"}"));
				}
			}
		}
		return super.modelBaseRecordClassGenerated(topLevelClass, introspectedTable);
	}

	/**
	 * 修改Example类
	 */
	@Override
	public boolean modelExampleClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
		// addPage(topLevelClass, introspectedTable, "pageHelper");
		return super.modelExampleClassGenerated(topLevelClass, introspectedTable);
	}

	/**
	 * 修改Mapper类
	 */
	@Override
	public boolean clientGenerated(Interface interfaze, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
		addBatchInsertMethod(interfaze, introspectedTable);
		addBatchUpdateMethod(interfaze, introspectedTable);
		
		return super.clientGenerated(interfaze, topLevelClass, introspectedTable);
	}

	/**
	 * 修改mapper.xml,支持分页和批量
	 */
	@Override
	public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
		supportPlugin.sqlDialect(document, introspectedTable);
		if(isUp){
			for(Element e:document.getRootElement().getElements()){
				XmlElement xe=(XmlElement)e;
				if(xe.getName().equals("resultMap")){
					xe.addElement(ele);
				}
			}
			document.getRootElement().addElement(selectEle);
		}
		return super.sqlMapDocumentGenerated(document, introspectedTable);
	}

	@Override
	public boolean sqlMapSelectByExampleWithoutBLOBsElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
		XmlElement newElement = supportPlugin.adaptSelectByExample(element, introspectedTable);
		return super.sqlMapUpdateByExampleWithoutBLOBsElementGenerated(newElement, introspectedTable);
	}

	@Override
	public boolean sqlMapInsertSelectiveElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
		supportPlugin.adaptInsertSelective(element, introspectedTable);
		return super.sqlMapInsertSelectiveElementGenerated(element, introspectedTable);
	}

	/**
	 * This plugin is always valid - no properties are required
	 */
	@Override
	public boolean validate(List<String> warnings) {
		supportPlugin = new MysqlSupport();
		return true;
	}

	/**
	 * 在Mapper类中增加批量插入方法声明
	 * 
	 * @author 吴帅
	 * @parameter @param interfaze
	 * @parameter @param introspectedTable
	 * @createDate 2015年9月30日 下午4:43:32
	 */
	private void addBatchInsertMethod(Interface interfaze, IntrospectedTable introspectedTable) {
		// 设置需要导入的类
		Set<FullyQualifiedJavaType> importedTypes = new TreeSet<FullyQualifiedJavaType>();
		importedTypes.add(FullyQualifiedJavaType.getNewListInstance());
		importedTypes.add(new FullyQualifiedJavaType(introspectedTable.getBaseRecordType()));

		Method ibsmethod = new Method();
		// 1.设置方法可见性
		ibsmethod.setVisibility(JavaVisibility.PUBLIC);
		// 2.设置返回值类型
		FullyQualifiedJavaType ibsreturnType = FullyQualifiedJavaType.getIntInstance();// int型
		ibsmethod.setReturnType(ibsreturnType);
		// 3.设置方法名
		ibsmethod.setName("insertBatchSelective");
		// 4.设置参数列表
		FullyQualifiedJavaType paramType = FullyQualifiedJavaType.getNewListInstance();
		FullyQualifiedJavaType paramListType;
		if (introspectedTable.getRules().generateBaseRecordClass()) {
			paramListType = new FullyQualifiedJavaType(introspectedTable.getBaseRecordType());
		} else if (introspectedTable.getRules().generatePrimaryKeyClass()) {
			paramListType = new FullyQualifiedJavaType(introspectedTable.getPrimaryKeyType());
		} else {
			throw new RuntimeException(getString("RuntimeError.12")); //$NON-NLS-1$
		}
		paramType.addTypeArgument(paramListType);

		ibsmethod.addParameter(new Parameter(paramType, "records"));

		interfaze.addImportedTypes(importedTypes);
		interfaze.addMethod(ibsmethod);
	}
	
	private void addBatchUpdateMethod(Interface interfaze, IntrospectedTable introspectedTable) {
		// 设置需要导入的类
		Set<FullyQualifiedJavaType> importedTypes = new TreeSet<FullyQualifiedJavaType>();
		importedTypes.add(FullyQualifiedJavaType.getNewListInstance());
		importedTypes.add(new FullyQualifiedJavaType(introspectedTable.getBaseRecordType()));

		Method ibsmethod = new Method();
		// 1.设置方法可见性
		ibsmethod.setVisibility(JavaVisibility.PUBLIC);
		// 2.设置返回值类型
		FullyQualifiedJavaType ibsreturnType = FullyQualifiedJavaType.getIntInstance();// int型
		ibsmethod.setReturnType(ibsreturnType);
		// 3.设置方法名
		ibsmethod.setName("updateBatchByPrimaryKeySelective");
		// 4.设置参数列表
		FullyQualifiedJavaType paramType = FullyQualifiedJavaType.getNewListInstance();
		FullyQualifiedJavaType paramListType;
		if (introspectedTable.getRules().generateBaseRecordClass()) {
			paramListType = new FullyQualifiedJavaType(introspectedTable.getBaseRecordType());
		} else if (introspectedTable.getRules().generatePrimaryKeyClass()) {
			paramListType = new FullyQualifiedJavaType(introspectedTable.getPrimaryKeyType());
		} else {
			throw new RuntimeException(getString("RuntimeError.12")); //$NON-NLS-1$
		}
		paramType.addTypeArgument(paramListType);

		ibsmethod.addParameter(new Parameter(paramType, "records"));

		interfaze.addImportedTypes(importedTypes);
		interfaze.addMethod(ibsmethod);
	}
}
