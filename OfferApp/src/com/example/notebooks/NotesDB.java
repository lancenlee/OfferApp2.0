/*
 * 作品：CIL创新实验室 NoteBook
 * author:AndroidBirdBoom
 * 时间：2016/7/21
 * 功能：数据库管理类的创建
 */
package com.example.notebooks;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class NotesDB extends SQLiteOpenHelper{
	 public static final String TABLE_NAME = "notes";//指定表名
	    public static final String CONTENT = "content";
	    public static final String ID = "_id";
	    public static final String TIME = "time";//当前创建的文本的时间
	    public static final String VIDEO = "video";
	    public static final String PATH = "path";
	    public static final String NAME = "name";
	    public static final String MUSIC = "music";

	    public NotesDB(Context context) {
	        super(context, "Note.db", null, 1);
	    }

	    @Override
	    //创建表
	    public void onCreate(SQLiteDatabase sqLiteDatabase) {
	        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_NAME + "("
	                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
	                + CONTENT + " TEXT NOT NULL,"
	                + TIME + " TEXT NOT NULL,"
	                + VIDEO + " TEXT,"
	                + NAME + " TEXT,"
	                + MUSIC + " TEXT,"
	                + PATH + " TEXT)");
	    }

	    @Override
	    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

	    }

}
