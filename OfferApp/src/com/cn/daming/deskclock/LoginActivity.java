/*
 * 作品：CIL创新实验室 NoteBook
 * author:lancen、AndroidBirdBoom、JimmyLegend
 * 时间：2016/8/12
 */

package com.cn.daming.deskclock;

import com.cn.daming.deskclock.R;

import edu.sdut.offerapp.db.DBOpenHelper;
import android.app.Activity;
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

public class LoginActivity extends Activity {
private EditText userEditText;
private EditText passwordEditText;
private Button loginButton;
private Button registerButton;
private DBOpenHelper helper;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		helper=new DBOpenHelper(LoginActivity.this);
		userEditText=(EditText) findViewById(R.id.user_edit2);
		passwordEditText=(EditText) findViewById(R.id.password_edit2);
		loginButton=(Button) findViewById(R.id.login_button);
		registerButton=(Button) findViewById(R.id.register_button);
		loginButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO 自动生成方法存根
				String number=userEditText.getText().toString();
				String password=passwordEditText.getText().toString();
				if(number.equals("")||password.equals("")){
					Toast.makeText(LoginActivity.this,"用户名或密码不能为空！",Toast.LENGTH_SHORT).show();
				}
				else if(exist(number, password)){
					Intent intent=new Intent(LoginActivity.this,WelcomeActivity.class);
					startActivity(intent);
					Toast.makeText(LoginActivity.this,"登陆成功！",Toast.LENGTH_SHORT).show();
				}else{
					
					if(!exist1(number)){
						Toast.makeText(LoginActivity.this,"用户名不正确！",Toast.LENGTH_SHORT).show();
					}
					else{
						Toast.makeText(LoginActivity.this,"密码不正确！",Toast.LENGTH_LONG).show();
					}
					
				}
			}
		});
		registerButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO 自动生成的方法存根
				Intent intent=new Intent(LoginActivity.this,RegisterActivity.class);
				startActivity(intent);
			}
		});
	}
	// 查询号码
		public boolean exist(String number,String password){
			boolean result = false;
			
			// 获取数据库
			SQLiteDatabase db = helper.getReadableDatabase();
			
			Cursor cursor = db.rawQuery("select * from User where number=?", new String[]{number});
			//Cursor cursor1 = db.rawQuery("select * from User where password=?", new String[]{password});
			
			if(cursor.moveToNext()&&cursor.getString(2).equals(password)){
				//Toast.makeText(LoginActivity.this,cursor.getString(0)+" "+cursor.getString(1)+""+cursor.getString(2),Toast.LENGTH_LONG).show();
				result = true;
			}
			
			cursor.close();
			db.close();
			
			return result;
		}
		// 查询号码
				public boolean exist1(String number){
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
				// 查询号码
				public boolean exist2(String number,String password){
					boolean result = false;
					
					// 获取数据库
					SQLiteDatabase db = helper.getReadableDatabase();
					
					Cursor cursor = db.rawQuery("select * from User where number=?", new String[]{password});
					if(cursor.moveToNext()&&!cursor.getString(2).equals(password)){
						Toast.makeText(LoginActivity.this,"密码不正确！",Toast.LENGTH_SHORT).show();
						result = true;
					}
					
					cursor.close();
					db.close();
					
					return result;
				}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// 膨胀菜单;这将条目添加到操作栏是否存在。
		getMenuInflater().inflate(R.menu.login, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/*
		 * 处理操作栏项点击这里。操作栏将
		 * 自动处理单击Home /按钮,这么长时间
		 * 当你指定一个AndroidManifest.xml家长活动.
		 */
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
