/*
 * 作品：CIL创新实验室 NoteBook
 * author:lancen、AndroidBirdBoom、JimmyLegend
 * 时间：2016/8/5
 */

package com.cn.daming.deskclock;

import com.cn.daming.deskclock.R;

import edu.sdut.offerapp.db.DBOpenHelper;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class RegisterActivity extends Activity {
private EditText userEditText;
private EditText passworsEditText;
private Button registerButton;
private DBOpenHelper helper;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);
		helper=new DBOpenHelper(RegisterActivity.this);
		userEditText=(EditText) findViewById(R.id.register_user_edit);
		passworsEditText=(EditText) findViewById(R.id.register_password_edit);
		registerButton=(Button) findViewById(R.id.register_button);
		registerButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO 自动生成的方法存根
				String number=userEditText.getText().toString();
				String password=passworsEditText.getText().toString();
				if(number.equals("")||password.equals("")){
					Toast.makeText(RegisterActivity.this,"信息不完整！",Toast.LENGTH_SHORT).show();
				}else if(exist(number)){
					Toast.makeText(RegisterActivity.this,"用户名已存在！",Toast.LENGTH_SHORT).show();
				}
				else{
					add(number, password);
					Toast.makeText(RegisterActivity.this,"注册成功！",Toast.LENGTH_SHORT).show();
					Intent intent=new Intent(RegisterActivity.this,LoginActivity.class);
					startActivity(intent);
				}
			}
		});
	}
	// 插入号码
	public void add(String number,String password){
		
		SQLiteDatabase db = helper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("number", number);
		values.put("password",password);
		db.insert("User", null, values);
		db.close();
	}
	// 查询号码
			public boolean exist(String number){
				boolean result = false;
				
				// 获取数据库
				SQLiteDatabase db = helper.getReadableDatabase();
				
				Cursor cursor = db.rawQuery("select * from User where number=?", new String[]{number});
				if(cursor.moveToNext()){
					result = true;
				}
				
				cursor.close();
				db.close();
				
				return result;
			}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		getMenuInflater().inflate(R.menu.register, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
