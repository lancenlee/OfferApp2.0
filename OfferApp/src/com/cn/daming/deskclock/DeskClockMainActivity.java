/*
 * ��Ʒ��CIL����ʵ���� NoteBook
 * author:lancen��AndroidBirdBoom��JimmyLegend
 * ʱ�䣺2016/8/11
 */

package com.cn.daming.deskclock;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class DeskClockMainActivity extends Activity implements OnItemClickListener{
	
    static final String PREFERENCES = "AlarmClock";

    /**  ��һ���Ǽٵ�����������������,����־,
     * ���Դ���ȡ� */
    static final boolean DEBUG = false;

    private SharedPreferences mPrefs;
    private LayoutInflater mFactory;
    private ListView mAlarmsList;
    private Cursor mCursor;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mFactory = LayoutInflater.from(this);
       
        mPrefs = getSharedPreferences(PREFERENCES, 0);
        mCursor = Alarms.getAlarmsCursor(getContentResolver());
        
        
        updateLayout();

    }
    
   
    private void updateLayout() {
    	setContentView(R.layout.alarm_clock);
        mAlarmsList = (ListView) findViewById(R.id.alarms_list);
        AlarmTimeAdapter adapter = new AlarmTimeAdapter(this, mCursor);
        mAlarmsList.setAdapter(adapter);
        mAlarmsList.setVerticalScrollBarEnabled(true);
        mAlarmsList.setOnItemClickListener(this);
        mAlarmsList.setOnCreateContextMenuListener(this);

        View addAlarm = findViewById(R.id.add_alarm);
        addAlarm.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    addNewAlarm();
                }
            });
        // ʹ������ͼѡ���е�ʱ��
        addAlarm.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                public void onFocusChange(View v, boolean hasFocus) {
                    v.setSelected(hasFocus);
                }
        });

        ImageButton deskClock =
                (ImageButton) findViewById(R.id.desk_clock_button);
        deskClock.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    
                }
        });
    }
    
    private void addNewAlarm() {
        startActivity(new Intent(this, SetAlarm.class));
    }
   
    private class AlarmTimeAdapter extends CursorAdapter {
        public AlarmTimeAdapter(Context context, Cursor cursor) {
            super(context, cursor);
        }

        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View ret = mFactory.inflate(R.layout.alarm_time, parent, false);

            DigitalClock digitalClock =
                    (DigitalClock) ret.findViewById(R.id.digitalClock);
            digitalClock.setLive(false);
            return ret;
        }

        
        public void bindView(View view, Context context, Cursor cursor) {
            final Alarm alarm = new Alarm(cursor);

            View indicator = view.findViewById(R.id.indicator);

            // ���ó�ʼ��Դ
            final ImageView barOnOff =
                    (ImageView) indicator.findViewById(R.id.bar_onoff);
            barOnOff.setImageResource(alarm.enabled ?
                    R.drawable.ic_indicator_on : R.drawable.ic_indicator_off);

            // ���ó�ʼ״̬��ʱ�� "checkbox"
            final CheckBox clockOnOff =
                    (CheckBox) indicator.findViewById(R.id.clock_onoff);
            clockOnOff.setChecked(alarm.enabled);

            // �������ѡ���⻹Ӧ�øı�״̬��
            
            indicator.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        clockOnOff.toggle();
                        updateIndicatorAndAlarm(clockOnOff.isChecked(),
                                barOnOff, alarm);
                    }
            });

            DigitalClock digitalClock =
                    (DigitalClock) view.findViewById(R.id.digitalClock);

            // ���ñ����ı�
            final Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, alarm.hour);
            c.set(Calendar.MINUTE, alarm.minutes);
            digitalClock.updateTime(c);
            digitalClock.setTypeface(Typeface.DEFAULT);

            // �����ظ��ı��������հ���������ظ�
            TextView daysOfWeekView =
                    (TextView) digitalClock.findViewById(R.id.daysOfWeek);
            final String daysOfWeekStr =
                    alarm.daysOfWeek.toString(DeskClockMainActivity.this, false);
            if (daysOfWeekStr != null && daysOfWeekStr.length() != 0) {
                daysOfWeekView.setText(daysOfWeekStr);
                daysOfWeekView.setVisibility(View.VISIBLE);
            } else {
                daysOfWeekView.setVisibility(View.GONE);
            }

            // ��ʾ��ǩ
            TextView labelView =
                    (TextView) view.findViewById(R.id.label);
            if (alarm.label != null && alarm.label.length() != 0) {
                labelView.setText(alarm.label);
                labelView.setVisibility(View.VISIBLE);
            } else {
                labelView.setVisibility(View.GONE);
            }
        }
    };
    
    
    private void updateIndicatorAndAlarm(boolean enabled, ImageView bar,
            Alarm alarm) {
        bar.setImageResource(enabled ? R.drawable.ic_indicator_on
                : R.drawable.ic_indicator_off);
        Alarms.enableAlarm(this, alarm.id, enabled);
        if (enabled) {
            SetAlarm.popAlarmSetToast(this, alarm.hour, alarm.minutes,
                    alarm.daysOfWeek);
        }
    }
    
  
    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        final AdapterContextMenuInfo info =
                (AdapterContextMenuInfo) item.getMenuInfo();
        final int id = (int) info.id;
        //�������Է���
        if (id == -1) {
            return super.onContextItemSelected(item);
        }
        switch (item.getItemId()) {
            case R.id.delete_alarm:
                // ȷ�Ͼ�������ɾ��.
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.delete_alarm))
                        .setMessage(getString(R.string.delete_alarm_confirm))
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface d,
                                            int w) {
                                        Alarms.deleteAlarm(DeskClockMainActivity.this, id);
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                return true;

            case R.id.enable_alarm:
                final Cursor c = (Cursor) mAlarmsList.getAdapter()
                        .getItem(info.position);
                final Alarm alarm = new Alarm(c);
                Alarms.enableAlarm(this, alarm.id, !alarm.enabled);
                if (!alarm.enabled) {
                    SetAlarm.popAlarmSetToast(this, alarm.hour, alarm.minutes,
                            alarm.daysOfWeek);
                }
                return true;

            case R.id.edit_alarm:
                Intent intent = new Intent(this, SetAlarm.class);
                intent.putExtra(Alarms.ALARM_ID, id);
                startActivity(intent);
                return true;

            default:
                break;
        }
        return super.onContextItemSelected(item);
    }
    
  
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
            ContextMenuInfo menuInfo) {
        //���Ӳ˵���xml.
        getMenuInflater().inflate(R.menu.context_menu, menu);

        // ʹ�õ�ǰ���һ���Զ�����ͼ�ı��⡣
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        final Cursor c =
                (Cursor) mAlarmsList.getAdapter().getItem((int) info.position);
        final Alarm alarm = new Alarm(c);

        // ������������ʱ�䡣
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, alarm.hour);
        cal.set(Calendar.MINUTE, alarm.minutes);
        final String time = Alarms.formatTime(this, cal);

        // �����Զ�����ͼ,ÿ��TextView���ı�.
        final View v = mFactory.inflate(R.layout.context_menu_header, null);
        TextView textView = (TextView) v.findViewById(R.id.header_time);
        textView.setText(time);
        textView = (TextView) v.findViewById(R.id.header_label);
        textView.setText(alarm.label);

        // �����Զ�����ͼ�˵��ϡ�
        menu.setHeaderView(v);
        //�ı��ı����ڱ�����״̬
        if (alarm.enabled) {
            menu.findItem(R.id.enable_alarm).setTitle(R.string.disable_alarm);
        }
    }
    

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.menu_item_desk_clock:
     
                return true;
            case R.id.menu_item_add_alarm:
                addNewAlarm();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
   
 
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.alarm_list_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    

	public void onItemClick(AdapterView<?> adapterView, View v, int pos, long id) {
		Intent intent = new Intent(this, SetAlarm.class);
        intent.putExtra(Alarms.ALARM_ID, (int) id);
        startActivity(intent);
		
	}
	
   @Override
    protected void onDestroy() {
        super.onDestroy();
        ToastMaster.cancelToast();
        mCursor.close();
    }
}