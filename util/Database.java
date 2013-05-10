package util;


import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import util.anotation.Column;
import util.anotation.Ignore;
import util.anotation.MergeTable;
import util.anotation.PrimaryKey;
import util.anotation.Table;
import util.anotation.Unique;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


/**
 * @author : Jeffrey Shey
 * Mail : shijunfan@gmail.com
 */
public abstract class Database extends SQLiteOpenHelper{

	public boolean isTransaction() {
		return isTransaction;
	}
	private ArrayList<HashMap<String, String[]>> args=new ArrayList<HashMap<String,String[]>>();

	private boolean isTransaction=false;
	public Database(Context context,String DATABASE_NAME,int DATABASE_VERSION) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	private SQLiteDatabase sqldb;

	@Override
	public void onCreate(SQLiteDatabase arg0) {this.sqldb=arg0;onCreate(this);}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {this.sqldb=arg0;onUpgrade(this, arg1, arg2);}
	

	public abstract void onCreate(Database database);

	public abstract void onUpgrade(Database database, int oldVersion, int newVersion);
	
	public void createTable(Class<?> table){
		Table ltable = table.getAnnotation(Table.class);
		String tableName = null ;
		if(ltable!=null){
			tableName = ltable.tableName();
		}else{
			tableName = table.getSimpleName();
		}
		StringBuilder column = new StringBuilder();
		Field[] fields = table.getDeclaredFields();
		for(int i=0;i<fields.length;i++){
			Field field=fields[i];
			field.setAccessible(true);
			Ignore ignore = field.getAnnotation(Ignore.class);
			MergeTable mergeTable = field.getAnnotation(MergeTable.class);
			if(ignore!=null&&ignore.value()){
				continue;
			}
			if (field.getName().equals("serialVersionUID")) {
				continue;
			}
			String columnName = field.getName();
			Column lColumn = field.getAnnotation(Column.class);
			if(lColumn!=null)
				columnName = lColumn.columnName();
			Class<?> clz = field.getType();
			column.append(columnName);
			Unique unique = field.getAnnotation(Unique.class);
			PrimaryKey primarykey = field.getAnnotation(PrimaryKey.class);
			
			if(clz.isPrimitive()||String.class.isAssignableFrom(clz)||Enum.class.isAssignableFrom(clz)||byte[].class.isAssignableFrom(clz)){
				if(byte[].class.isAssignableFrom(clz)){
					column.append(" ".concat("BLOB"));
				}else{
					column.append(" ".concat(int.class.isAssignableFrom(clz)?"INTEGER":"TEXT"));
				}

				if(primarykey!=null){
					column.append(" PRIMARY");
					if(primarykey.autoincrement())
					column.append(" KEY AUTOINCREMENT");
				}
				if(unique!=null&&unique.value())
					column.append(" unique");
			}else if(mergeTable!=null&&mergeTable.value()){
				String str = buildColumn(clz);
				column.append(str);
			}
			
			if(i!=fields.length-1){
				column.append(","); 
			}
		}
		createTable(tableName, column.toString());
	}
	
	private void buildValues(ContentValues cv,Class<?> table, Object obj) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException{
		Field[] fields = table.getFields();
		for(int i=0;i<fields.length;i++){
			Field field = fields[i];
			field.setAccessible(true);
			String fieldName = field.getName();
			if (fieldName.equals("serialVersionUID")) {
				continue;
			}
			Class<?> clz = field.getType();
			MergeTable mergeTable = field.getAnnotation(MergeTable.class);
			String stringLetter = fieldName.substring(0, 1).toUpperCase();
			String getName = String.format("get%s%s", stringLetter,
						fieldName.substring(1));
			Method getMethod = table.getMethod(getName, new Class[] {});
			Object value = getMethod.invoke(obj, new Object[] {});
			if(mergeTable!=null&&mergeTable.value()){
				buildValues(cv, clz, value);
			}else{
				cv.put(fieldName, value != null ? value.toString() : "");
			}
		}
	}
	
	private String buildColumn(Class<?> table){
		StringBuilder column = new StringBuilder();
		Field[] fields = table.getFields();
		for(int i=0;i<fields.length;i++){
			Field field = fields[i];
			field.setAccessible(true);
			if (field.getName().equals("serialVersionUID")) {
				continue;
			}
			if(i!=0){
				column.append(",");
			}
			String columnName = field.getName();
			Column lColumn = field.getAnnotation(Column.class);
			if(lColumn!=null)
				columnName = lColumn.columnName();
			Class<?> clz = field.getType();
			column.append(columnName);
			MergeTable mergeTable = field.getAnnotation(MergeTable.class);
			if(clz.isPrimitive()||String.class.isAssignableFrom(clz)||Enum.class.isAssignableFrom(clz)||byte[].class.isAssignableFrom(clz)){
				if(byte[].class.isAssignableFrom(clz)){
					column.append(" ".concat("BLOB"));
				}else{
					column.append(" ".concat(int.class.isAssignableFrom(clz)?"INTEGER":"TEXT"));
				}
				Unique unique = field.getAnnotation(Unique.class);
				if(unique!=null&&unique.value())
					column.append(" unique");
			}else if(mergeTable!=null&&mergeTable.value()){
				String str = buildColumn(clz);
				column.append(str);
			}
		}
		return column.toString();
	}
	
	private void createTable(String tableName,String column){
		Log.e("CREATE TABLE",String.format("CREATE TABLE IF NOT EXISTS %s (%s)", tableName,column));
		sqldb.execSQL(String.format("CREATE TABLE IF NOT EXISTS %s (%s)", tableName,column));
	}
	
	public Cursor queryObject(String sql, String[] selectionArgs) {
		SQLiteDatabase db = this.getWritableDatabase();
		return db.rawQuery(sql, selectionArgs);
	}
	
	/**
	 * 事务
	 */
	public void execTransaction(ArrayList<HashMap<String, String[]>> args) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.beginTransaction();// 开始事务
		try {
			for (HashMap<String, String[]> arg : args) {
				Iterator<String> it = arg.keySet().iterator();
				for (; it.hasNext();) {
					String sql = it.next();
					db.execSQL(sql, arg.get(sql));
				}
			}
			db.setTransactionSuccessful();
		}catch(Exception e){
		} finally {
			isTransaction=false;
			db.endTransaction();
		}
		db.close();
	}
	public void startTransaction(){
		isTransaction=true;
	}
	/**
	 * 查询总数
	 * 
	 * @param sql
	 * @return
	 */
	public long getCount(String sql, String[] selectionArgs) {
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor c = db.rawQuery(sql, selectionArgs);
		long count = c.getCount();
		c.close();
		db.close();
		return count;
	}
	
	public void dropTable(String tableName) {
		SQLiteDatabase db = this.getWritableDatabase();
		String sql = "DROP TABLE IF EXISTS " + tableName;
		try {
			db.execSQL(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			db.close();
		}
	}
	
	public void execSql(String sql,String...args){
		if(!isTransaction()){
			SQLiteDatabase db = this.getWritableDatabase();
			try {
				db.execSQL(sql,args);
			} catch (SQLException e) {
				e.printStackTrace();
			}finally{
				db.close();	
			}
		}else{
			HashMap<String, String[]> sqls=new HashMap<String, String[]>();
			sqls.put(sql, args);
			this.args.add(sqls);
		}
	}
	
	public void save(List<?> list){
		SQLiteDatabase db = this.getWritableDatabase();
		try {
		db.beginTransaction();
		for(Object o:list){
			save(db,o);
		}
		db.setTransactionSuccessful();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}finally{
			db.endTransaction();
			db.close();
		}
	}
	public void replace(List<?> list){
		SQLiteDatabase db = this.getWritableDatabase();
		try {
		db.beginTransaction();
		for(Object o:list){
			replace(db,o);
		}
		db.setTransactionSuccessful();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} finally{
			db.endTransaction();
			db.close();
		}
	}
	public void save(Object obj){
		SQLiteDatabase db = this.getWritableDatabase();
		try {
			save(db, obj);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		db.close();
	}
	
	private void save(SQLiteDatabase db, Object obj) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Table table = obj.getClass().getAnnotation(Table.class);
		String tableName = obj.getClass().getSimpleName();
		if(table!=null)
			tableName = table.tableName();
		Field[] fields = obj.getClass().getDeclaredFields();
		ContentValues values = new ContentValues();
		for (int i = 0; i < fields.length; i++) {
			Field field = fields[i];
			field.setAccessible(true);
			PrimaryKey primaryKey =field.getAnnotation(PrimaryKey.class);
			if(primaryKey!=null&&primaryKey.autoincrement()&&int.class.isAssignableFrom(field.getType()))
				continue;
			String fieldName = field.getName();
			if (fieldName.equals("serialVersionUID")) {
				continue;
			}
			Class<?> clz = field.getType();
			MergeTable mergeTable = field.getAnnotation(MergeTable.class);
			String stringLetter = fieldName.substring(0, 1).toUpperCase();
			String getName = String.format("get%s%s", stringLetter,
					fieldName.substring(1));
			Method getMethod = obj.getClass()
					.getMethod(getName, new Class[] {});
			Object value = getMethod.invoke(obj, new Object[] {});
				
			if(mergeTable!=null&&mergeTable.value()){
				buildValues(values, clz,value);
			}else{
				values.put(fieldName, value != null ? value.toString() : "");
			}
		}
		db.insert(tableName, null, values);
	}
	

	public void update(Object obj) {
		SQLiteDatabase db = getWritableDatabase();
		Table table = obj.getClass().getAnnotation(Table.class);
		String tableName = obj.getClass().getSimpleName();
		if(table!=null)
			tableName = table.tableName();
		Field[] fields = obj.getClass().getDeclaredFields();
		ContentValues values = new ContentValues();
		for (int i = 0; i < fields.length; i++) {
			Field field = fields[i];
			field.setAccessible(true);
			String fieldName = field.getName();
			if (fieldName.equals("serialVersionUID")) {
				continue;
			}
			Class<?> clz = field.getType();
			MergeTable mergeTable = field.getAnnotation(MergeTable.class);
			String stringLetter = fieldName.substring(0, 1).toUpperCase();
			String getName = String.format("get%s%s", stringLetter,
					fieldName.substring(1));
			Method getMethod;
			try {
				getMethod = obj.getClass()
						.getMethod(getName, new Class[] {});
				Object value = getMethod.invoke(obj, new Object[] {});
				if(mergeTable!=null&&mergeTable.value()){
					buildValues(values, clz,value);
				}else{
					values.put(fieldName, value != null ? value.toString() : "");
				}
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				
		}
		db.update(tableName, values, "", new String[]{});
		db.close();
	}

	public void replace(Object obj){
		SQLiteDatabase db = getWritableDatabase();
		replace(db, obj); 
		db.close();
	}
	
	public void replace(SQLiteDatabase db,Object obj) {
		Table table = obj.getClass().getAnnotation(Table.class);
		String tableName = obj.getClass().getSimpleName();
		if(table!=null)
			tableName = table.tableName();
		Field[] fields = obj.getClass().getDeclaredFields();
		ContentValues values = new ContentValues();
		for (int i = 0; i < fields.length; i++) {
			Field field = fields[i];
			field.setAccessible(true);
			String fieldName = field.getName();
			if (fieldName.equals("serialVersionUID")) {
				continue;
			}
			Class<?> clz = field.getType();
			MergeTable mergeTable = field.getAnnotation(MergeTable.class);
			String stringLetter = fieldName.substring(0, 1).toUpperCase();
			String getName = String.format("get%s%s", stringLetter,
					fieldName.substring(1));
			Method getMethod;
			try {
				getMethod = obj.getClass()
						.getMethod(getName, new Class[] {});
				Object value = getMethod.invoke(obj, new Object[] {});
				
				if(mergeTable!=null&&mergeTable.value()){
					buildValues(values, clz,value);
				}else{
					values.put(fieldName, value != null ? value.toString() : "");
				}
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		db.replace(tableName, null, values);
	}
	
	public void execTransaction(){
		if(isTransaction()){
			execTransaction(args);
			args.clear();
			isTransaction=false;
		}
	}
	
	public void delete(Class<?> class1, String where, String...args){
		Table table = class1.getAnnotation(Table.class);
		String tableName = null ;
		if(table!=null)
			tableName = table.tableName();
		else
			tableName = class1.getSimpleName();

		execSql(String.format("DELETE *FROM %s %s %s",where!=null&&where.length()>0?"where":"" , tableName , where), args);
	}
	

	public void clearTable(Class<?> class1){
		Table table = class1.getAnnotation(Table.class);
		String tableName = null ;
		if(table!=null)
			tableName = table.tableName();
		else
			tableName = class1.getSimpleName();

		execSql(String.format("DELETE FROM %s",tableName));
	}


	/**返回该类的Fields*/
	private HashMap<String,Field> getMethod(Class<?> clz){
		HashMap<String, Field> map=new HashMap<String, Field>();
		Field[] fields=clz.getDeclaredFields();
		for(Field field:fields){
			field.setAccessible(true);
			map.put(field.getName(), field);
		}
		return map;
	}

	public Cursor findAllCursor(Class<?> cls,String where,String[] selectionArgs){
		String sql =String.format("SELECT *FROM %s %s %s", getTableName(cls),where!=null?"where":"",where);
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(sql, selectionArgs);
		return cursor;
	}
	public <T>List<?> findAll(String sql, String[] selectionArgs,Class<?> cls){
		SQLiteDatabase db = this.getWritableDatabase();
		HashMap<String,Field> fields=getMethod(cls);
		List<Object> list=new ArrayList<Object>();
		Cursor cursor = db.rawQuery(sql, selectionArgs);
		while(cursor.moveToNext()){
			Object obj = null;
			try {
				obj = cls.newInstance();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			for(int i=0;i<cursor.getColumnCount();i++){
				String columnName = cursor.getColumnName(i);
				if(fields.containsKey(columnName)){
					Field field = fields.get(columnName);
					try {
						invokeBaseValue(field, obj, cursor.getString(cursor.getColumnIndex(columnName)));
					} catch (SecurityException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (NoSuchMethodException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InstantiationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}else{
					Log.w("DB", columnName.concat(" isn't contains"));
				}
			}
			list.add(obj);
		}
		cursor.close();
		db.close();
		return list;
	}
	

	public List<?> findAll(Class<?> cls){
		return findAll(String.format("SELECT *FROM %s", getTableName(cls)),new String[]{},cls);
	}
	
	public List<?> findAll(Class<?> cls,String where, String...args){
		return findAll(String.format("SELECT *FROM %s %s %s", getTableName(cls),where!=null?"where":"",where),args,cls);
	}
	
	public <T>List<?> findAll(String tableName,Class<?> cls){
		return findAll(String.format("SELECT *FROM %s", tableName),new String[]{},cls);
	}
	public <T> T findLast(String sql, String[] selectionArgs,Class<?> cls) throws Exception{
		SQLiteDatabase db = this.getWritableDatabase();
		HashMap<String,Field> fields=getMethod(cls);
		Cursor cursor = db.rawQuery(sql, selectionArgs);
		Object obj = cls.newInstance();
		if(cursor.moveToLast()){
			for(int i=0;i<cursor.getColumnCount();i++){
				String columnName = cursor.getColumnName(i);
				if(fields.containsKey(columnName)){
					invokeBaseValue(fields.get(columnName), obj, cursor.getString(cursor.getColumnIndex(columnName)));
				}
			}
		}
		cursor.close();
		db.close();
		return (T) obj;
	}
	
	public String getTableName(Class<?> clz){
		Table table = clz.getAnnotation(Table.class);
		String tableName = null ;
		if(table!=null)
			tableName = table.tableName();
		else
			tableName = clz.getSimpleName();
		return tableName;
	}
	
	public <T> T findFrist(String sql, String[] selectionArgs,Class<?> cls) throws Exception{
		SQLiteDatabase db = this.getWritableDatabase();
		HashMap<String,Field> fields=getMethod(cls);
		Cursor cursor = db.rawQuery(sql, selectionArgs);
		Object obj = cls.newInstance();
		if(cursor.moveToFirst()){
			for(int i=0;i<cursor.getColumnCount();i++){
				String columnName = cursor.getColumnName(i);
				if(fields.containsKey(columnName)){
					invokeBaseValue(fields.get(columnName), obj, cursor.getString(cursor.getColumnIndex(columnName)));
				}
			}
		}
		cursor.close();
		db.close();
		return (T) obj;
	}
	
	
	/**反射基本属性方法*/
	private void invokeBaseValue(Field childField,Object object,Object value) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException, ParseException{
		String childName=childField.getName();
		String method="set"+childName.substring(0, 1).toUpperCase()+childName.substring(1,childName.length());
		Method setMethod=object.getClass().getMethod(method, new Class[]{childField.getType()});
		setMethod.invoke(object, castType(value,childField.getType()));
	}

	/**类型转换*/
	private <T> Object castType(Object value,Class<?> class1) throws IllegalAccessException, InstantiationException, ParseException{
		if(class1.equals(int.class)){
			return Integer.parseInt(value.toString());
		}else if(class1.equals(long.class)){
			return Long.parseLong(value.toString());
		}else if(class1.equals(double.class)){
			return Double.parseDouble(value.toString());
		}else if(class1.equals(float.class)){
			return Float.parseFloat(value.toString());
		}else if(class1.equals(boolean.class)){
			return Boolean.parseBoolean(value.toString());
		}
		return value.toString();
	}
}