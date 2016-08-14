/*
 * 作品：CIL创新实验室 NoteBook
 * author:lancen、AndroidBirdBoom、JimmyLegend
 * 时间：2016/8/9
 */


package com.cn.daming.deskclock;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TimePicker;
import android.widget.Toast;


public class SetAlarm extends PreferenceActivity
        implements TimePickerDialog.OnTimeSetListener,
        Preference.OnPreferenceChangeListener {

    private EditTextPreference mLabel;
    private CheckBoxPreference mEnabledPref;
    private Preference mTimePref;
    private AlarmPreference mAlarmPref;
    private CheckBoxPreference mVibratePref;
    private RepeatPreference mRepeatPref;
    private MenuItem mTestAlarmItem;

    private int     mId;
    private int     mHour;
    private int     mMinutes;
    private boolean mTimePickerCancelled;
    private Alarm   mOriginalAlarm;

    /**
     * 设置一个闹钟。需要一个警报。ALARM_ID作为一个传递
     * 额外的。FIXME:通过报警对象像其他活动
     */
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        //覆盖默认的视图的内容。
        setContentView(R.layout.set_alarm);

        addPreferencesFromResource(R.xml.alarm_prefs);

        // 得到每个偏好我们可以检索值。
        mLabel = (EditTextPreference) findPreference("label");
        mLabel.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference p,
                            Object newValue) {
                        String val = (String) newValue;
                        // 摘要基于新标签。
                        p.setSummary(val);
                        if (val != null && !val.equals(mLabel.getText())) {
                            // 通过通用调用侦听器.
                            return SetAlarm.this.onPreferenceChange(p,
                                newValue);
                        }
                        return true;
                    }
                });
        mEnabledPref = (CheckBoxPreference) findPreference("enabled");
        mEnabledPref.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference p,
                            Object newValue) {
                        // 当启用警报。
                        if (!mEnabledPref.isChecked()) {
                            popAlarmSetToast(SetAlarm.this, mHour, mMinutes,
                                mRepeatPref.getDaysOfWeek());
                        }
                        return SetAlarm.this.onPreferenceChange(p, newValue);
                    }
                });
        mTimePref = findPreference("time");
        mAlarmPref = (AlarmPreference) findPreference("alarm");
        mAlarmPref.setOnPreferenceChangeListener(this);
        mVibratePref = (CheckBoxPreference) findPreference("vibrate");
        mVibratePref.setOnPreferenceChangeListener(this);
        mRepeatPref = (RepeatPreference) findPreference("setRepeat");
        mRepeatPref.setOnPreferenceChangeListener(this);

        Intent i = getIntent();
        mId = i.getIntExtra(Alarms.ALARM_ID, -1);
        if (true) {
            Log.v("wangxianming", "In SetAlarm, alarm id = " + mId);
        }

        Alarm alarm = null;
        if (mId == -1) {
            //没有报警id意味着创建一个新闹钟。
            alarm = new Alarm();
        } else {
            /* 从数据库加载警报详细信息 */
            alarm = Alarms.getAlarm(getContentResolver(), mId);
            // 坏的闹钟,。
            if (alarm == null) {
                finish();
                return;
            }
        }
        mOriginalAlarm = alarm;

        updatePrefs(mOriginalAlarm);

        // 我们必须这样做才能保存/取消按钮突出
       
        getListView().setItemsCanFocus(true);

        // 行为附加到每个按钮.
        Button b = (Button) findViewById(R.id.alarm_save);
        b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    saveAlarm();
                    finish();
                }
        });
        final Button revert = (Button) findViewById(R.id.alarm_revert);
        revert.setEnabled(false);
        revert.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    int newId = mId;
                    updatePrefs(mOriginalAlarm);
                    // 在新创建的“恢复”报警应该删除它。
                    if (mOriginalAlarm.id == -1) {
                        Alarms.deleteAlarm(SetAlarm.this, newId);
                    } else {
                        saveAlarm();
                    }
                    revert.setEnabled(false);
                }
        });
        b = (Button) findViewById(R.id.alarm_delete);
        if (mId == -1) {
            b.setEnabled(false);
        } else {
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    deleteAlarm();
                }
            });
        }

        // 我们做的最后一件事就是流行时间选择器,如果这是一个新的警报。
        if (mId == -1) {
            // 假设用户点击取消
            mTimePickerCancelled = true;
            showTimePicker();
        }
    }

    // 用于异步运行的.
    private static final Handler sHandler = new Handler();

    public boolean onPreferenceChange(final Preference p, Object newValue) {
        /*
         * 异步保存报警自_before_调用此方法
         * 的价值偏好发生了变化。
         */
        sHandler.post(new Runnable() {
            public void run() {
                // 编辑任何偏好(启用除外)使报警。
                if (p != mEnabledPref) {
                    mEnabledPref.setChecked(true);
                }
                saveAlarmAndEnableRevert();
            }
        });
        return true;
    }

    private void updatePrefs(Alarm alarm) {
        mId = alarm.id;
        mEnabledPref.setChecked(alarm.enabled);
        mLabel.setText(alarm.label);
        mLabel.setSummary(alarm.label);
        mHour = alarm.hour;
        mMinutes = alarm.minutes;
        mRepeatPref.setDaysOfWeek(alarm.daysOfWeek);
        mVibratePref.setChecked(alarm.vibrate);
        // 提醒uri的偏好。
        mAlarmPref.setAlert(alarm.alert);
        updateTime();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mTimePref) {
            showTimePicker();
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onBackPressed() {
        /*
         * 在通常的情况下查看警报,mTimePickerCancelled
         * 初始化错误。当创建一个新的报警,此值
         * 以为真,直到用户改变了时间。
         */
        if (!mTimePickerCancelled) {
            saveAlarm();
        }
        finish();
    }

    private void showTimePicker() {
        new TimePickerDialog(this, this, mHour, mMinutes,
                DateFormat.is24HourFormat(this)).show();
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        // onTimeSet叫做当用户单击“设置
        mTimePickerCancelled = false;
        mHour = hourOfDay;
        mMinutes = minute;
        updateTime();
        // 如果时间改变了,使报警。
        mEnabledPref.setChecked(true);
        // 保存报警
        popAlarmSetToast(this, saveAlarmAndEnableRevert());
    }

    private void updateTime() {
        Log.v("wangxianming", "updateTime " + mId);
        mTimePref.setSummary(Alarms.formatTime(this, mHour, mMinutes,
                mRepeatPref.getDaysOfWeek()));
    }

    private long saveAlarmAndEnableRevert() {
        //启用“恢复”回到最初的警报。
        final Button revert = (Button) findViewById(R.id.alarm_revert);
        revert.setEnabled(true);
        return saveAlarm();
    }

    private long saveAlarm() {
        Alarm alarm = new Alarm();
        alarm.id = mId;
        alarm.enabled = mEnabledPref.isChecked();
        alarm.hour = mHour;
        alarm.minutes = mMinutes;
        alarm.daysOfWeek = mRepeatPref.getDaysOfWeek();
        alarm.vibrate = mVibratePref.isChecked();
        alarm.label = mLabel.getText();
        alarm.alert = mAlarmPref.getAlert();

        long time;
        if (alarm.id == -1) {
            time = Alarms.addAlarm(this, alarm);
            /*
             * addAlarm填充报警与新的id。中期,以便更新
             * 更改其他首选项更新新的报警……
             */
            mId = alarm.id;
        } else {
            time = Alarms.setAlarm(this, alarm);
        }
        return time;
    }

    private void deleteAlarm() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_alarm))
                .setMessage(getString(R.string.delete_alarm_confirm))
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                Alarms.deleteAlarm(SetAlarm.this, mId);
                                finish();
                            }
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * 显示一个面包,告诉用户多长时间,直到报警
     * 离开。这有助于防止“am / pm“错误…
     */
    static void popAlarmSetToast(Context context, int hour, int minute,
                                 Alarm.DaysOfWeek daysOfWeek) {
        popAlarmSetToast(context,
                Alarms.calculateAlarm(hour, minute, daysOfWeek)
                .getTimeInMillis());
    }

    static void popAlarmSetToast(Context context, long timeInMillis) {
        String toastText = formatToast(context, timeInMillis);
        Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_LONG);
        ToastMaster.setToast(toast);
        toast.show();
    }

    /**
     *格式”报警设置2天7小时53分钟
     *现在
     */
    static String formatToast(Context context, long timeInMillis) {
        long delta = timeInMillis - System.currentTimeMillis();
        long hours = delta / (1000 * 60 * 60);
        long minutes = delta / (1000 * 60) % 60;
        long days = hours / 24;
        hours = hours % 24;

        String daySeq = (days == 0) ? "" :
                (days == 1) ? context.getString(R.string.day) :
                context.getString(R.string.days, Long.toString(days));

        String minSeq = (minutes == 0) ? "" :
                (minutes == 1) ? context.getString(R.string.minute) :
                context.getString(R.string.minutes, Long.toString(minutes));

        String hourSeq = (hours == 0) ? "" :
                (hours == 1) ? context.getString(R.string.hour) :
                context.getString(R.string.hours, Long.toString(hours));

        boolean dispDays = days > 0;
        boolean dispHour = hours > 0;
        boolean dispMinute = minutes > 0;

        int index = (dispDays ? 1 : 0) |
                    (dispHour ? 2 : 0) |
                    (dispMinute ? 4 : 0);

        String[] formats = context.getResources().getStringArray(R.array.alarm_set);
        return String.format(formats[index], daySeq, hourSeq, minSeq);
    }
}
