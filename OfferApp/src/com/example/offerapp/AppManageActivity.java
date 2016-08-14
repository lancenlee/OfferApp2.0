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
		//��ȡϵͳӦ��
		arrApp=getPackageManager().getInstalledPackages(0);
		
		//�������ݰ�
		appAdapter=new AppAdapter();
		lvAppList.setAdapter(appAdapter);
		//����������¼�
		lvAppList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				//ж��Ӧ��
				//1����ȡ�û������Ӧ��
				PackageInfo app=arrApp.get(arg2);
				String appName=app.applicationInfo.loadLabel(getPackageManager()).toString();
				Uri uri=Uri.parse("package:"+app.packageName);
				//����ϵͳӦ��
				if((app.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)!=0){
					
					Toast toast=Toast.makeText(AppManageActivity.this,"ϵͳӦ�ò���ж�أ�",Toast.LENGTH_LONG);
					LinearLayout toast_layout=(LinearLayout) toast.getView();
					ImageView iv=new ImageView(AppManageActivity.this);
					iv.setImageResource(R.drawable.alert);
					toast_layout.addView(iv);
					toast.show();
					return;
				}
				//��Ӧ�ý���ж�ز���
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
		//��������//��ȡϵͳӦ��
		arrApp=getPackageManager().getInstalledPackages(0);
		//����ListView
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
			//ȡ��Ҫ��ʾ��Ӧ������
			PackageInfo app=(PackageInfo) getItem(arg0);
			ViewHolder viewHolder=null;
			//����View
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
			//��ʾӦ�ó���
			//��ʾӦ��ͼ��
			viewHolder.ivAppIcon.setImageDrawable(app.applicationInfo.loadIcon(getPackageManager())
					);
			//��ʾӦ������
			viewHolder.tvAppName.setText(app.applicationInfo.loadLabel(getPackageManager()));
			
			//��ʾ�Ƿ���һ��ϵͳӦ��
			if((app.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)==0){
				viewHolder.tvSys.setText("�û�Ӧ��");
			}
			else{
				viewHolder.tvSys.setText("ϵͳӦ��");
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
