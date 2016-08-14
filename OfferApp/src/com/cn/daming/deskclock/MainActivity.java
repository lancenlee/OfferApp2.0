/*
 * ��Ʒ��CIL����ʵ���� NoteBook
 * author:lancen��AndroidBirdBoom��JimmyLegend
 * ʱ�䣺2016/7/31
 */

package com.cn.daming.deskclock;

import com.cn.daming.deskclock.R;
import com.example.notebooks.NotebookActivity;
import com.example.offerapp.AppManageActivity;

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
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;


public class MainActivity extends Activity {
	private GridView gridView;
	private int[] icon={
			R.drawable.note1,
			R.drawable.clock,
			R.drawable.ic_launcher
	};
	private String[] iconName = { "���±�","����","Ӧ�ù���"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
       //add("123", "123");
gridView=(GridView) findViewById(R.id.gridView);
        
        GridAdapter adapter=new GridAdapter();
        gridView.setAdapter(adapter);
        //ΪGridView����������¼�
        gridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				//��Ӧָ�����¼�
				//ͨ��arg2����ȷ���û����������һ������
				switch (arg2) {
				case 0://���±�
				{
					Intent intent=new Intent(MainActivity.this,NotebookActivity.class);
				startActivity(intent);
					break;
				}
				case 1://����
				{
					Intent intent=new Intent(MainActivity.this,DeskClockMainActivity.class);
					startActivity(intent);
					break;
				}
				case 2://Ӧ�ù���
				{
					Intent intent=new Intent(MainActivity.this,AppManageActivity.class);
					startActivity(intent);
					break;
				}
				default:
					break;
				}
			}
        	
		});
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // ���Ͳ˵�;�⽫��Ŀ��ӵ��������Ƿ���ڡ�
        getMenuInflater().inflate(R.menu.main, menu);
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
    class GridAdapter extends BaseAdapter{

    	@Override
    	public int getCount() {
    		// TODO Auto-generated method stub
    		//��������ķ���ֵ��������GridView
    		return iconName.length;
    	}

    	@Override
    	public Object getItem(int arg0) {
    		// TODO Auto-generated method stub
    		return null;
    	}

    	@Override
    	public long getItemId(int position) {
    		// TODO Auto-generated method stub
    		return position;
    	}

    	@Override
    	public View getView(int arg0, View arg1, ViewGroup arg2) {
    		// TODO Auto-generated method stub
    		View view=View.inflate(MainActivity.this,R.layout.item_gridview,null);
    		//��ȡ�����ļ��еģ�ͼƬ���ı�����
    		ImageView iv_Item=(ImageView) view.findViewById(R.id.imageView);
    		TextView tv_Item=(TextView) view.findViewById(R.id.textView);
    		
    		iv_Item.setImageResource(icon[arg0]);
    		tv_Item.setText(iconName[arg0]);
    		return view;
    	}}
}
