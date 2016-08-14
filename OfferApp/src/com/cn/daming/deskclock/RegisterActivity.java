/*
 * ��Ʒ��CIL����ʵ���� NoteBook
 * author:lancen��AndroidBirdBoom��JimmyLegend
 * ʱ�䣺2016/8/5
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
				// TODO �Զ����ɵķ������
				String number=userEditText.getText().toString();
				String password=passworsEditText.getText().toString();
				if(number.equals("")||password.equals("")){
					Toast.makeText(RegisterActivity.this,"��Ϣ��������",Toast.LENGTH_SHORT).show();
				}else if(exist(number)){
					Toast.makeText(RegisterActivity.this,"�û����Ѵ��ڣ�",Toast.LENGTH_SHORT).show();
				}
				else{
					add(number, password);
					Toast.makeText(RegisterActivity.this,"ע��ɹ���",Toast.LENGTH_SHORT).show();
					Intent intent=new Intent(RegisterActivity.this,LoginActivity.class);
					startActivity(intent);
				}
			}
		});
	}
	// �������
	public void add(String number,String password){
		
		SQLiteDatabase db = helper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("number", number);
		values.put("password",password);
		db.insert("User", null, values);
		db.close();
	}
	// ��ѯ����
			public boolean exist(String number){
				boolean result = false;
				
				// ��ȡ���ݿ�
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
