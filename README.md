# Halo

> 名字的由来
>
> “Halo”这个名字来源于 《SG-1》系列美剧第十季，剧中的牧师口中经常会念叨一句不明就里的话`“Halo de are the Ori”`，然后底下的一帮人就开始信仰Ori为真神，在遥远的星系上Ori家里壁炉里面的火就会烧得更旺。其实也不知道这句话是什么意思，而且也不清楚是不是字幕组小哥打错字。总之，发音是对的。

用这个名字来命名的原因，也是希望更多的开发者来信任本项目。



## 二、主要功能

本项目主要提供一个数据访问工具类 `SuperDAO`，利用这个工具类可以实现快速的面向对象的数据库操作，对于一般的增删改查操作，无需编写任何SQL。

> 也许要问，为什么不用Hibernate或者MyBatis？
>
> 呃，这个么...，Halo当前还远不足以跟Hibernate或者MyBatis去相提并论，并且Halo的应用也会限定于一些简单的业务场景，如果不想要在这些简单的场景里引入太多的配置，那么Halo将会是一个很好的选择。



## 三、实体注解

### @Table

标注在实体类上，用来映射至对应的数据表

### @Column

标注在实体类属性上，用来映射至对应的数据表字段

### @OneToMany

标注在实体类属性上，用来配置“1：N”的关联

### @ManyToOne

标注在实体类属性上，用来配置“N：1”的关联

### @Transient

标注在实体类属性上，用来配置该字段不需要映射至数据库



## 四、SuperDAO

一般情况下，可以直接实例化SuperDAO这个类，然后调用它提供的方法完成各种数据操作。实例化SuperDAO的步骤如下：

```java
// 1、创建IConnectionFactory
IConnectionFactory factory = new IConnectionFactory() {
    ... something code ...
};

// 2、实例化SuperDAO
SuperDAO dao = new SuperDAO(factory);
// 或
SuperDAO dao = new SuperDAO();
dao.setFactory(factory);

// 3、接下来就可以自由使用 dao 了
// dao.save();
// dao.update();
// dao.find();
// dao.list();
// ... ...
```



## 五、一些工具类

### 1、BeanUtil

```java
public class BeanUtil {

	/**
	 * 将对象转换成字节数组
	 * 
	 * @param obj
	 * @return
	 */
	public static <T> byte[] convertToBytes(T obj);

	/**
	 * 将字节数组转换成对象
	 * 
	 * @param bytesOfObj
	 * @param objClass
	 * @return
	 */
	public static <T> T convertToObject(byte[] bytesOfObj, Class<T> objClass);

	public static <T> T newInstance(Class<T> t);

	/**
	 * 在给定的类中查找所有具有某种标注类型的属性
	 * 
	 * @param beanClass
	 * @param annotationClass
	 * @return
	 */
	public static Field[] getFieldByAnnotation(Class<?> beanClass, Class<? extends Annotation> annotationClass);

	/**
	 * 在给定的类中查找指定名称的属性，向上查找所有的父类
	 * 
	 * @param fieldName
	 * @param beanClass
	 * @return
	 */
	public static Field getDeclaredField(String fieldName, Class<?> beanClass);

	/**
	 * 在给定的类中查找所有属性，可以根据需要向上查找所有的父类
	 * 
	 * @param beanClass
	 * @param includeSuperClass
	 * @return
	 */
	public static Field[] getDeclaredFields(Class<?> beanClass, boolean includeSuperClass);

	/**
	 * 从给定的Bean获取属性值；
	 * 
	 * @param bean    给定的Bean，可以是Map
	 * @param fldName 属性名称，支持级联表达式“.”
	 * @return
	 */
	public static <T> Object getFieldValue(T bean, String fldName);

	/**
	 * 设置单个属性
	 * 
	 * @param bean
	 * @param fieldName
	 * @param fieldType
	 * @param fieldValue
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static <T> void setFieldValue(T bean, String fieldName, Class<?> fieldType, Object fieldValue);

	/**
	 * 判断给定的字段是否是常量（包含静态）
	 * 
	 * @param field
	 * @return
	 */
	public static boolean isConstField(Field field);

	/**
	 * 判断给定的字段是否是临时的（无须持久化的）
	 * 
	 * @param field
	 * @return
	 */
	public static boolean isTransient(Field field);

	/**
	 * 判断给定的字段是否是主键
	 * 
	 * @param field
	 * @return
	 */
	public static boolean isPrimaryField(Table metaTable, String fieldName);

	/**
	 * 获取字段对应的数据列名，除非有特别标注(Column)，否则直接使用字段名
	 * 
	 * @param fieldName 字段名称
	 * @param beanClass
	 * @return
	 */
	public static String getColumnName(String fieldName, Class<?> beanClass);

	/**
	 * 获取字段对应的数据列名，除非有特别标注(Column)，否则直接使用字段名
	 * 
	 * @param field
	 * @return
	 */
	public static String getColumnName(Field field);

	/**
	 * 根据结果集信息，构造结果Map。对于此结果集的遍历操作，应该在此方法的宿主内进行。
	 * 
	 * @param rs   结果集
	 * @param rsmd 结果集元数据
	 * @return
	 */
	public static Map<String, Object> getEntityMap(ResultSet rs, ResultSetMetaData rsmd);

	/**
	 * 拷贝全部属性
	 * 
	 * @param <T>
	 * @param fromBean
	 * @param toBean
	 */
	public static <T> T copyProperties(T fromBean, T toBean);

	/**
	 * 拷贝部分属性
	 * 
	 * @param <T>
	 * @param fromBean
	 * @param toBean
	 * @param ignoreFields
	 */
	public static <T> T copyProperties(T fromBean, T toBean, String[] ignoreFields);

	/**
	 * 拷贝属性
	 * 
	 * @param <T>
	 * @param fromObj
	 * @param toObj
	 * @param ignoreFields
	 * @param limitFields
	 */
	@SuppressWarnings("unchecked")
	public static <T> T copyProperties(T fromObj, T toObj, String[] ignoreFields, String[] limitFields,
			ITypeConverter[] typeConverters);

	/**
	 * 将对象转换为期望的类型
	 * 
	 * @param value
	 * @param targetCls
	 * @param typeConverters
	 * @return
	 */
	public static Object convert(final Object value, Class<?> targetCls, ITypeConverter[] typeConverters);
}

```

