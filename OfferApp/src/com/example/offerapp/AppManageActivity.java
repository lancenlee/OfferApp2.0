package com.example.offerapp;
import com.cn.daming.deskclock.R;

import java.util.List;

import org.w3c.dom.Text;

import android.app.Activity;
import android.content.ClipData.Item;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class AppManageActivity extends Activity {

	private ListView lvAppList;
	private List<PackageInfo> arrApp;
	private AppAdapter appAdapter;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_app_manage);
		lvAppList =(ListView) findViewById(R.id.lv_app);
		//获取系统应用
		arrApp=getPackageManager().getInstalledPackages(0);
		
		//进行数据绑定
		appAdapter=new AppAdapter();
		lvAppList.setAdapter(appAdapter);
		//添加子项点击事件
		lvAppList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				//卸载应用
				//1、获取用户点击的应用
				PackageInfo app=arrApp.get(arg2);
				String appName=app.applicationInfo.loadLabel(getPackageManager()).toString();
				Uri uri=Uri.parse("package:"+app.packageName);
				//过滤系统应用
				if((app.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)!=0){
					
					Toast toast=Toast.makeText(AppManageActivity.this,"系统应用不可卸载！",Toast.LENGTH_LONG);
					LinearLayout toast_layout=(LinearLayout) toast.getView();
					ImageView iv=new ImageView(AppManageActivity.this);
					iv.setImageResource(R.drawable.alert);
					toast_layout.addView(iv);
					toast.show();
					return;
				}
				//对应用进行卸载操作
				Intent intent=new Intent();
				intent.setAction(Intent.ACTION_DELETE);
				intent.setData(uri);
				startActivity(intent);
				//DialogInterface
			}
		});
	}
	
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		//更新数据//获取系统应用
		arrApp=getPackageManager().getInstalledPackages(0);
		//更新ListView
		appAdapter.notifyDataSetChanged();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.app_manage, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	class AppAdapter extends BaseAdapter{

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return arrApp.size();
		}

		@Override
		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return arrApp.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return arg0;
		}

		@Override
		public View getView(int arg0, View convertView, ViewGroup arg2) {
			// TODO Auto-generated method stub
			//取出要显示的应用数据
			PackageInfo app=(PackageInfo) getItem(arg0);
			ViewHolder viewHolder=null;
			//创建View
			if(convertView==null){
				convertView=View.inflate(AppManageActivity.this,R.layout.item_applist, null);
				viewHolder=new ViewHolder();
				viewHolder.ivAppIcon=(ImageView) convertView.findViewById(R.id.iv_appicon);
				viewHolder.tvAppName=(TextView) convertView.findViewById(R.id.tv_appname);
				viewHolder.tvSys=(TextView) convertView.findViewById(R.id.tv_sys);
				convertView.setTag(viewHolder);
			}
			else{
				viewHolder=(ViewHolder) convertView.getTag();
			}
			//显示应用程序
			//显示应用图标
			viewHolder.ivAppIcon.setImageDrawable(app.applicationInfo.loadIcon(getPackageManager())
					);
			//显示应用名称
			viewHolder.tvAppName.setText(app.applicationInfo.loadLabel(getPackageManager()));
			
			//显示是否是一个系统应用
			if((app.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)==0){
				viewHolder.tvSys.setText("用户应用");
			}
			else{
				viewHolder.tvSys.setText("系统应用");
			}
			return convertView;
		}
		
	}
	class ViewHolder{
		ImageView ivAppIcon;
		TextView tvAppName;
		TextView tvSys;
	}
}
