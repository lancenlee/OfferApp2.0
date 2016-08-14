/*
 * ��Ʒ��CIL����ʵ���� NoteBook
 * author:lancen��AndroidBirdBoom��JimmyLegend
 * ʱ�䣺2016/7/31
 */

package com.cn.daming.deskclock;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;

import java.util.Calendar;
import java.text.DateFormatSymbols;

/**
 * �����ṩ�����ṩ��Ϣ����������
 *
 */
public class Alarms {

    /*�����������AlarmReceiver�Լ�AlarmKlaxon����
	��һ�������ж��嵥�е����ڽ��ձ����㲥��
	�ӱ�����������
	*/
    public static final String ALARM_ALERT_ACTION = "com.cn.daming.deskclock.ALARM_ALERT";

    /*
     * �����ж���AlarmKlaxon�������Ѿ�ֹͣ����
     * �����κ�ԭ��(����Ϊ���Ѿ���AlarmAlertFullScreen����,
     * ���ڴ���ĵ绰������,�ȵ�)��
     */
    public static final String ALARM_DONE_ACTION = "com.cn.daming.deskclock.ALARM_DONE";

    /*
     * AlarmAlertFullScreen�����㲥��ͼ,�Ա�����Ӧ�ó���
     * ���Դ��ﱨ��(ALARM_ALERT_ACTION��ALARM_DONE_ACTION֮ǰ)��
     */
    public static final String ALARM_SNOOZE_ACTION = "com.cn.daming.deskclock.ALARM_SNOOZE";

    /*
     * AlarmAlertFullScreen�����㲥��ͼ,�Ա�����Ӧ�ó���
     * ���Խ������(ALARM_ALERT_ACTION���ALARM_DONE_ACTION֮ǰ)��
     */
    public static final String ALARM_DISMISS_ACTION = "com.cn.daming.deskclock.ALARM_DISMISS";

    /*
     * ����һ��˽���ж�AlarmKlaxon��������UI
     * ��ʾ������ɱ��
     */
    public static final String ALARM_KILLED = "alarm_killed";

    /*
     * �����ALARM_KILLED��ͼָʾ�û��೤ʱ��
     * ��ɱ֮ǰ�����ˡ�
     */
    public static final String ALARM_KILLED_TIMEOUT = "alarm_killed_timeout";

    // ����ַ���������ָʾ��db����������
    public static final String ALARM_ALERT_SILENT = "silent";

    /*
     * ��Ŀ���ǵ��û�ȡ����֪ͨ
     * С˯������
     */
    public static final String CANCEL_SNOOZE = "cancel_snooze";

    // ����ַ���ʱʹ��һ����������ͨ��һ����ͼ��
    public static final String ALARM_INTENT_EXTRA = "intent.extra.alarm";

    /*
     * ��������ԭʼ�����������ݡ�����ʹ�õ�
     * ����дAlarmManagerService����ClassNotFoundException
     * Ŀ����ʱ��Ա
     */
    public static final String ALARM_RAW_DATA = "intent.extra.alarm_raw";

    /*
     * ����ַ�������ʶ�𱨾�id���ݸ�SetAlarm��
     * �������б�
     */
    public static final String ALARM_ID = "alarm_id";

    final static String PREF_SNOOZE_ID = "snooze_id";
    final static String PREF_SNOOZE_TIME = "snooze_time";

    private final static String DM12 = "E h:mm aa";
    private final static String DM24 = "E k:mm";

    private final static String M12 = "h:mm aa";
    // ��DigitalClock����
    final static String M24 = "kk:mm";

    /**
     * ����һ���µı�����������������id��
     */
    public static long addAlarm(Context context, Alarm alarm) {
        ContentValues values = createContentValues(alarm);
        Uri uri = context.getContentResolver().insert(
                Alarm.Columns.CONTENT_URI, values);
        alarm.id = (int) ContentUris.parseId(uri);

        long timeInMillis = calculateAlarm(alarm);
        if (alarm.enabled) {
            clearSnoozeIfNeeded(context, timeInMillis);
        }
        setNextAlert(context);
        return timeInMillis;
    }

    /**
     * ɾ�����еı����������������Ǵ���,����
     * С˯��������һ������
     */
    public static void deleteAlarm(Context context, int alarmId) {
        if (alarmId == -1) return;

        ContentResolver contentResolver = context.getContentResolver();
        /* ������ﱨ��,ʧȥ�� */
        disableSnoozeAlert(context, alarmId);

        Uri uri = ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, alarmId);
        contentResolver.delete(uri, "", null);

        setNextAlert(context);
    }

    /**
     * ��ѯ���о���
     * ��������о���
     */
    public static Cursor getAlarmsCursor(ContentResolver contentResolver) {
        return contentResolver.query(
                Alarm.Columns.CONTENT_URI, Alarm.Columns.ALARM_QUERY_COLUMNS,
                null, null, Alarm.Columns.DEFAULT_SORT_ORDER);
    }

    // ˽�з����õ�һ������޵ľ��������ݿ⡣
    private static Cursor getFilteredAlarmsCursor(
            ContentResolver contentResolver) {
        return contentResolver.query(Alarm.Columns.CONTENT_URI,
                Alarm.Columns.ALARM_QUERY_COLUMNS, Alarm.Columns.WHERE_ENABLED,
                null, null);
    }

    private static ContentValues createContentValues(Alarm alarm) {
        ContentValues values = new ContentValues(8);
        // ��������������alarm_time��ֵ���ظ����⽫��ʹ�ú�رվ������ڡ�
        long time = 0;
        if (!alarm.daysOfWeek.isRepeatSet()) {
            time = calculateAlarm(alarm);
        }

        values.put(Alarm.Columns.ENABLED, alarm.enabled ? 1 : 0);
        values.put(Alarm.Columns.HOUR, alarm.hour);
        values.put(Alarm.Columns.MINUTES, alarm.minutes);
        values.put(Alarm.Columns.ALARM_TIME, alarm.time);
        values.put(Alarm.Columns.DAYS_OF_WEEK, alarm.daysOfWeek.getCoded());
        values.put(Alarm.Columns.VIBRATE, alarm.vibrate);
        values.put(Alarm.Columns.MESSAGE, alarm.label);

        // �վ���Uri��ʾ����������
        values.put(Alarm.Columns.ALERT, alarm.alert == null ? ALARM_ALERT_SILENT
                : alarm.alert.toString());

        return values;
    }

    private static void clearSnoozeIfNeeded(Context context, long alarmTime) {
        /*
         * �����������Ļ�����С˯֮ǰ,���С˯
         * �������������
         */
        SharedPreferences prefs =
                context.getSharedPreferences(DeskClockMainActivity.PREFERENCES, 0);
        long snoozeTime = prefs.getLong(PREF_SNOOZE_TIME, 0);
        if (alarmTime < snoozeTime) {
            clearSnoozePreference(context, prefs);
        }
    }

    /**
     * ����һ�������ݿ��б�ʾ�����ı�������id��
     * ��������ڱ�������null��
     */
    public static Alarm getAlarm(ContentResolver contentResolver, int alarmId) {
        Cursor cursor = contentResolver.query(
                ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, alarmId),
                Alarm.Columns.ALARM_QUERY_COLUMNS,
                null, null, null);
        Alarm alarm = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                alarm = new Alarm(cursor);
            }
            cursor.close();
        }
        return alarm;
    }


    /**
     * һ������ķ���������һ����������
     * �����ṩ�ߡ�
     * ���������
     */
    public static long setAlarm(Context context, Alarm alarm) {
        ContentValues values = createContentValues(alarm);
        ContentResolver resolver = context.getContentResolver();
        resolver.update(
                ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, alarm.id),
                values, null, null);

        long timeInMillis = calculateAlarm(alarm);

        if (alarm.enabled) {
            /*
             * ����С˯�������ֻ�Ǹı��˱������˯֮�ʡ���
             * ��������������˯֮�ʱ�����һ����
             * ������
             * ��������:disableSnoozeAlertӦ����һ�����õ����֡�
             */
            disableSnoozeAlert(context, alarm.id);

            /*
             * ����С˯֮ǰ�����������Ļ��ֱ������˯֮�ʡ�
             * �����������б������û����п��ܵĴ���
             * �Ѿ��޸ĺ�Ļ��ֱ�����
             */
            clearSnoozeIfNeeded(context, timeInMillis);
        }

        setNextAlert(context);

        return timeInMillis;
    }

    /**
     * һ������ķ��������û����һ��������
     * @param id��Ӧ��_id��
     * @param�������ö�Ӧ����
     */

    public static void enableAlarm(
            final Context context, final int id, boolean enabled) {
        enableAlarmInternal(context, id, enabled);
        setNextAlert(context);
    }

    private static void enableAlarmInternal(final Context context,
            final int id, boolean enabled) {
        enableAlarmInternal(context, getAlarm(context.getContentResolver(), id),
                enabled);
    }

    private static void enableAlarmInternal(final Context context,
            final Alarm alarm, boolean enabled) {
        if (alarm == null) {
            return;
        }
        ContentResolver resolver = context.getContentResolver();

        ContentValues values = new ContentValues(2);
        values.put(Alarm.Columns.ENABLED, enabled ? 1 : 0);

        /*
         * �������ʹ����,����ʱ�����
         * ��ֵ�ھ������������ˡ�
         */
        if (enabled) {
            long time = 0;
            if (!alarm.daysOfWeek.isRepeatSet()) {
                time = calculateAlarm(alarm);
            }
            values.put(Alarm.Columns.ALARM_TIME, time);
        } else {
            // С˯���idƥ�䡣
            disableSnoozeAlert(context, alarm.id);
        }

        resolver.update(ContentUris.withAppendedId(
                Alarm.Columns.CONTENT_URI, alarm.id), values, null, null);
    }

    public static Alarm calculateNextAlert(final Context context) {
        Alarm alarm = null;
        long minTime = Long.MAX_VALUE;
        long now = System.currentTimeMillis();
        Cursor cursor = getFilteredAlarmsCursor(context.getContentResolver());
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Alarm a = new Alarm(cursor);
                    /*
                     * 0��ʾ����һ���ظ��ľ���,����
                     * ������һ��������ʱ�䡣
                     */
                    if (a.time == 0) {
                        a.time = calculateAlarm(a);
                    } else if (a.time < now) {
                        Log.v("wangxianming", "Disabling expired alarm set for ");
                        //��������,������,���š�
                        enableAlarmInternal(context, a, false);
                        continue;
                    }
                    if (a.time < minTime) {
                        minTime = a.time;
                        alarm = a;
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return alarm;
    }

    /**
     * ͨ���������ظ�����������
     * ������
     */
    public static void disableExpiredAlarms(final Context context) {
        Cursor cur = getFilteredAlarmsCursor(context.getContentResolver());
        long now = System.currentTimeMillis();

        if (cur.moveToFirst()) {
            do {
                Alarm alarm = new Alarm(cur);
                /*
                 * 0��ζ������������ظ������ʱ����
                 * ����,���ʱ���������ڡ�
                 */
                if (alarm.time != 0 && alarm.time < now) {
                    enableAlarmInternal(context, alarm, false);
                }
            } while (cur.moveToNext());
        }
        cur.close();
    }

    /**
     * ��ϵͳ����ʱ,ʱ��/ʱ���仯,���ۺ�ʱ
     * �û����ı������á�����С˯�������,
     * ����������о���,������һ��������
     */
    public static void setNextAlert(final Context context) {
        if (!enableSnoozeAlert(context)) {
            Alarm alarm = calculateNextAlert(context);
            if (alarm != null) {
                enableAlert(context, alarm, alarm.time);
            } else {
                disableAlert(context);
            }
        }
    }

    /**
     * ���þ���AlarmManger��״̬��������ʲô
     * ʵ������������������������
     * @param������������
     * @param atTimeInMillisʱ�������ĺ���
     */
    private static void enableAlert(Context context, final Alarm alarm,
            final long atTimeInMillis) {
        AlarmManager am = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);

        if (true) {
            Log.v("wangxianming", "** setAlert id " + alarm.id + " atTime " + atTimeInMillis);
        }

        Intent intent = new Intent(ALARM_ALERT_ACTION);

        /*
         * ����һ����΢�Ĺ���,�Ա����쳣ңԶ
         * AlarmManagerService���̡�AlarmManager��Ӷ��������
         * �����ͼʹ�����͡���ΪԶ�̹���
         * ��֪�������׼�,�����׳�һ����
         * ClassNotFoundException��
         * Ϊ�˱����������,������Ъ��������Ȼ�����ƽԭ
         * byte[]���顣AlarmReceiver��֪����������
         * �����byte[]���顣

         */
        Parcel out = Parcel.obtain();
        alarm.writeToParcel(out, 0);
        out.setDataPosition(0);
        intent.putExtra(ALARM_RAW_DATA, out.marshall());

        PendingIntent sender = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        am.set(AlarmManager.RTC_WAKEUP, atTimeInMillis, sender);

        setStatusBarIcon(context, true);

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(atTimeInMillis);
        String timeString = formatDayAndTime(context, c);
        saveNextAlarm(context, timeString);
    }

    /**
     * isables����AlarmManger��״̬����
     * @param id����id��
     */
    static void disableAlert(Context context) {
        AlarmManager am = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent sender = PendingIntent.getBroadcast(
                context, 0, new Intent(ALARM_ALERT_ACTION),
                PendingIntent.FLAG_CANCEL_CURRENT);
        am.cancel(sender);
        setStatusBarIcon(context, false);
        saveNextAlarm(context, "");
    }

    static void saveSnoozeAlert(final Context context, final int id,
            final long time) {
        SharedPreferences prefs = context.getSharedPreferences(
        		DeskClockMainActivity.PREFERENCES, 0);
        if (id == -1) {
            clearSnoozePreference(context, prefs);
        } else {
            SharedPreferences.Editor ed = prefs.edit();
            ed.putInt(PREF_SNOOZE_ID, id);
            ed.putLong(PREF_SNOOZE_TIME, time);
            ed.apply();
        }
        // ������һ���������º�С˯��
        setNextAlert(context);
    }

    /**
     * ����С˯�����������idƥ���С˯id��
     */
    static void disableSnoozeAlert(final Context context, final int id) {
        SharedPreferences prefs = context.getSharedPreferences(
        		DeskClockMainActivity.PREFERENCES, 0);
        int snoozeId = prefs.getInt(PREF_SNOOZE_ID, -1);
        if (snoozeId == -1) {
            // û��˯,ʲôҲ����.
            return;
        } else if (snoozeId == id) {
            //������ͬ��id��ȷ������ѡ�
            clearSnoozePreference(context, prefs);
        }
    }

    /*
     * ���ְ�С˯ƫ�á���Ҫʹ�������Ϊ��
     * ������ʱ�ӵ�ƫ�á�Ҳ�����С˯֪ͨ
     * ��������
     */
    private static void clearSnoozePreference(final Context context,
            final SharedPreferences prefs) {
        final int alarmId = prefs.getInt(PREF_SNOOZE_ID, -1);
        if (alarmId != -1) {
            NotificationManager nm = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(alarmId);
        }

        final SharedPreferences.Editor ed = prefs.edit();
        ed.remove(PREF_SNOOZE_ID);
        ed.remove(PREF_SNOOZE_TIME);
        ed.apply();
    };

    /**
     * �����һ��С˯,AlarmManager������
     * �������С˯@return���
     */
    private static boolean enableSnoozeAlert(final Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
        		DeskClockMainActivity.PREFERENCES, 0);

        int id = prefs.getInt(PREF_SNOOZE_ID, -1);
        if (id == -1) {
            return false;
        }
        long time = prefs.getLong(PREF_SNOOZE_TIME, -1);

        // ��db�õ�������
        final Alarm alarm = getAlarm(context.getContentResolver(), id);
        if (alarm == null) {
            return false;
        }
        /*���ݿ��е�ʱ����0(�ظ�)��һ���ض���ʱ��
         * ���ظ�����������AlarmReceiver�������ֵ
         * �ȽϺ��ʵ�ʱ�䡣
        */
        alarm.time = time;

        enableAlert(context, alarm, time);
        return true;
    }

    /**
     * ������״̬���Ƿ����û���ñ���
     */
    private static void setStatusBarIcon(Context context, boolean enabled) {
        Intent alarmChanged = new Intent("android.intent.action.ALARM_CHANGED");
        alarmChanged.putExtra("alarmSet", enabled);
        context.sendBroadcast(alarmChanged);
    }

    private static long calculateAlarm(Alarm alarm) {
        return calculateAlarm(alarm.hour, alarm.minutes, alarm.daysOfWeek)
                .getTimeInMillis();
    }

    /**
     * ����Сʱ�ͷ��Ӿ���,����һ���ʺϵ�ʱ��
     * ��AlarmManager���á�
     */
    static Calendar calculateAlarm(int hour, int minute,
            Alarm.DaysOfWeek daysOfWeek) {

        // �����ڿ�ʼ
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());

        int nowHour = c.get(Calendar.HOUR_OF_DAY);
        int nowMinute = c.get(Calendar.MINUTE);

        // ����������ڵ�ǰʱ��,��ǰһ��
        if (hour < nowHour  ||
            hour == nowHour && minute <= nowMinute) {
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        int addDays = daysOfWeek.getNextAlarm(c);
        if (addDays > 0) c.add(Calendar.DAY_OF_WEEK, addDays);
        return c;
    }

    static String formatTime(final Context context, int hour, int minute,
                             Alarm.DaysOfWeek daysOfWeek) {
        Calendar c = calculateAlarm(hour, minute, daysOfWeek);
        return formatTime(context, c);
    }

    /* ʹ��AlarmAlert */
    static String formatTime(final Context context, Calendar c) {
        String format = get24HourMode(context) ? M24 : M12;
        return (c == null) ? "" : (String)DateFormat.format(format, c);
    }

    /**
     * ��ʾ���ں�ʱ�䡪������������Ļ
     */
    private static String formatDayAndTime(final Context context, Calendar c) {
        String format = get24HourMode(context) ? DM24 : DM12;
        return (c == null) ? "" : (String)DateFormat.format(format, c);
    }

    /**
     *��ʡʱ�����һ������,һ����ʽ�����ַ���,��ϵͳ��
     *���û����������������.
     */
    static void saveNextAlarm(final Context context, String timeString) {
        Settings.System.putString(context.getContentResolver(),
                                  Settings.System.NEXT_ALARM_FORMATTED,
                                  timeString);
    }

    /**
     * @return��ʵ���ʱ������Ϊ24Сʱģʽ
     */
    static boolean get24HourMode(final Context context) {
        return android.text.format.DateFormat.is24HourFormat(context);
    }
}
