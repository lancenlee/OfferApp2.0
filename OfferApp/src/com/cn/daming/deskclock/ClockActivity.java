/*
 * ��Ʒ��CIL����ʵ���� NoteBook
 * author:lancen��AndroidBirdBoom��JimmyLegend
 * ʱ�䣺2016/8/5
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
		// ���Ͳ˵�;�⽫��Ŀ��ӵ��������Ƿ���ڡ�
		getMenuInflater().inflate(R.menu.clock, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/* ������������������������
		�Զ�������Home /��ť,��ô��ʱ��
		����ָ��һ��AndroidManifest.xml�ҳ����
		*/
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
