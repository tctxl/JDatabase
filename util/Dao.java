package util;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.database.Cursor;

/**
 * @author : Jeffrey Shey
 * Mail : shijunfan@gmail.com
 */
public interface Dao {
public void save(Object object);
public List<?> findAll(String sql, String[] selectionArgs,Class<?> cls);
public void dropTable(String tableName);
public long getCount(String sql, String[] selectionArgs);
public void execTransaction(ArrayList<HashMap<String, String[]>> args);
public Cursor queryObject(String sql, String[] selectionArgs);
}
