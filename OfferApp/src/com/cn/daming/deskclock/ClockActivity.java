/*
 * 作品：CIL创新实验室 NoteBook
 * author:lancen、AndroidBirdBoom、JimmyLegend
 * 时间：2016/8/5
 */

package com.cn.daming.deskclock;

import com.cn.daming.deskclock.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class ClockActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_clock);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// 膨胀菜单;这将条目添加到操作栏是否存在。
		getMenuInflater().inflate(R.menu.clock, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/* 处理操作栏项点击这里。操作栏将
		自动处理单击Home /按钮,这么长时间
		当你指定一个AndroidManifest.xml家长活动。
		*/
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
