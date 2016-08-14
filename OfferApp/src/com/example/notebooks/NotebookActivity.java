package com.example.notebooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cn.daming.deskclock.R;
import com.cn.daming.deskclock.R.id;
import com.cn.daming.deskclock.R.layout;
import com.cn.daming.deskclock.R.menu;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.SimpleAdapter;

public class NotebookActivity extends Activity implements AdapterView.OnItemClickListener{

	private String[] names = {"我的笔记", "拍照笔记", "录音笔记", "视频笔记", "综合笔记"};
    private int[] pictures = {R.drawable.wdbiji64, R.drawable.paizhao64, R.drawable.luyin64, R.drawable.shipin64, R.drawable.zonghebiji64};
    private GridView gridView;
    private SimpleAdapter adapter;
    private List<Map<String,Object>> list;
    private Intent  it;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.notebook);
		gridView = (GridView) findViewById(R.id.gv);
        list = new ArrayList<Map<String,Object>>();
        adapter = new SimpleAdapter(this,getData(),R.layout.grid_item,new String[]{"tubiao","dibiao"},new int[]{R.id.imageview,R.id.tv});
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(this);
	}
	public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        it = new Intent(NotebookActivity.this,ZongheActivity.class);
        switch (i){
            //综合笔记
            case 0:
                //Toast.makeText(MainActivity.this,"0",Toast.LENGTH_SHORT).show();
              it.putExtra("flag","1");
                startActivity(it);
                break;
            //拍照笔记
            case 1:
                //Toast.makeText(MainActivity.this,"1",Toast.LENGTH_SHORT).show();
                it.putExtra("flag","2");
                startActivity(it);
                break;
            //录音笔记
            case 2:
                //Toast.makeText(MainActivity.this,"2",Toast.LENGTH_SHORT).show();
                it.putExtra("flag","3");
                startActivity(it);
                break;
            //视频笔记
            case 3:
                //Toast.makeText(MainActivity.this,"3",Toast.LENGTH_SHORT).show();
                it.putExtra("flag","4");
                startActivity(it);
                break;
            //我的笔记
            case 4:
                //Toast.makeText(MainActivity.this,"4",Toast.LENGTH_SHORT).show();
                Intent mycontent = new Intent(NotebookActivity.this,MyContentActivity.class);
                startActivity(mycontent);
                break;
            default:
                break;
        }
    }
	public List<Map<String,Object>> getData(){
        for (int i = 0; i<names.length;i++){
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("tubiao",pictures[i]);
            map.put("dibiao",names[i]);
            list.add(map);
        }
        return list;
    }
}
