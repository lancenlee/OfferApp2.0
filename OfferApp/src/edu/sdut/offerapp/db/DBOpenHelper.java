package edu.sdut.offerapp.db;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class DBOpenHelper extends SQLiteOpenHelper {
	public DBOpenHelper(Context context) {
		super(context,"OfferApp.db",null,1);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		db.execSQL("CREATE TABLE IF NOT EXISTS " 
		+ "User(" 
		+ "_id integer primary key autoincrement," 
		+ "number Text," 
		+ "password Text" 
		+ ");");
	}
//	// 插入号码
//		public void add(String number,String password){
//			
//			SQLiteDatabase db = helper.getWritableDatabase();
//			ContentValues values = new ContentValues();
//			values.put("number", number);
//			values.put("password",password);
//			db.insert("BlackNum", null, values);
//			db.close();
//		}
//		// 查询号码
//		public boolean exist(String number,String password){
//			boolean result = false;
//			
//			// 获取数据库
//			SQLiteDatabase db = helper.getReadableDatabase();
//			
//			Cursor cursor = db.rawQuery("select * from User where number=?", new String[]{number});
//			Cursor cursor1 = db.rawQuery("select * from User where password=?", new String[]{password});
//			if(cursor.moveToNext()&&cursor1.moveToNext()){
//				result = true;
//			}
//			
//			cursor.close();
//			db.close();
//			
//			return result;
//		}
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		if (oldVersion ==0 ){
			SQLiteDatabase db1 = getWritableDatabase();
			for (int i = 0; i < 5; i++) {
				ContentValues values = new ContentValues();
				values.put("number", "1000" + i);
				values.put("password", "1000" + i);
				db1.insert("User", null, values);
			}
			db1.close();
		}
	}

}
