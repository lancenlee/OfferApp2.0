/*
 * 作品：CIL创新实验室 NoteBook
 * author:lancen、AndroidBirdBoom、JimmyLegend
 * 时间：2016/7/31
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
 * 警报提供者所提供信息的闹钟设置
 *
 */
public class Alarms {

    /*这个动作触发AlarmReceiver以及AlarmKlaxon。它
	是一个公共行动清单中的用于接收报警广播吗
	从报警管理器。
	*/
    public static final String ALARM_ALERT_ACTION = "com.cn.daming.deskclock.ALARM_ALERT";

    /*
     * 公共行动由AlarmKlaxon当闹钟已经停止发送
     * 由于任何原因(如因为它已经从AlarmAlertFullScreen驳回,
     * 由于传入的电话或死亡,等等)。
     */
    public static final String ALARM_DONE_ACTION = "com.cn.daming.deskclock.ALARM_DONE";

    /*
     * AlarmAlertFullScreen监听广播意图,以便其他应用程序
     * 可以打盹报警(ALARM_ALERT_ACTION后ALARM_DONE_ACTION之前)。
     */
    public static final String ALARM_SNOOZE_ACTION = "com.cn.daming.deskclock.ALARM_SNOOZE";

    /*
     * AlarmAlertFullScreen监听广播意图,以便其他应用程序
     * 可以解除警报(ALARM_ALERT_ACTION后和ALARM_DONE_ACTION之前)。
     */
    public static final String ALARM_DISMISS_ACTION = "com.cn.daming.deskclock.ALARM_DISMISS";

    /*
     * 这是一个私人行动AlarmKlaxon用来更新UI
     * 显示报警被杀。
     */
    public static final String ALARM_KILLED = "alarm_killed";

    /*
     * 额外的ALARM_KILLED意图指示用户多长时间
     * 被杀之前报警了。
     */
    public static final String ALARM_KILLED_TIMEOUT = "alarm_killed_timeout";

    // 这个字符串是用于指示在db无声警报。
    public static final String ALARM_ALERT_SILENT = "silent";

    /*
     * 这目的是当用户取消的通知
     * 小睡警觉。
     */
    public static final String CANCEL_SNOOZE = "cancel_snooze";

    // 这个字符串时使用一个报警对象通过一个意图。
    public static final String ALARM_INTENT_EXTRA = "intent.extra.alarm";

    /*
     * 这个额外的原始报警对象数据。这是使用的
     * 当填写AlarmManagerService避免ClassNotFoundException
     * 目的临时演员
     */
    public static final String ALARM_RAW_DATA = "intent.extra.alarm_raw";

    /*
     * 这个字符串用于识别报警id传递给SetAlarm从
     * 警报的列表。
     */
    public static final String ALARM_ID = "alarm_id";

    final static String PREF_SNOOZE_ID = "snooze_id";
    final static String PREF_SNOOZE_TIME = "snooze_time";

    private final static String DM12 = "E h:mm aa";
    private final static String DM24 = "E k:mm";

    private final static String M12 = "h:mm aa";
    // 与DigitalClock共享
    final static String M24 = "kk:mm";

    /**
     * 创建一个新的报警并填充给定报警的id。
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
     * 删除现有的报警。如果这个报警是打盹,禁用
     * 小睡。设置下一个警报
     */
    public static void deleteAlarm(Context context, int alarmId) {
        if (alarmId == -1) return;

        ContentResolver contentResolver = context.getContentResolver();
        /* 如果打盹报警,失去它 */
        disableSnoozeAlert(context, alarmId);

        Uri uri = ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, alarmId);
        contentResolver.delete(uri, "", null);

        setNextAlert(context);
    }

    /**
     * 查询所有警报
     * 光标在所有警报
     */
    public static Cursor getAlarmsCursor(ContentResolver contentResolver) {
        return contentResolver.query(
                Alarm.Columns.CONTENT_URI, Alarm.Columns.ALARM_QUERY_COLUMNS,
                null, null, Alarm.Columns.DEFAULT_SORT_ORDER);
    }

    // 私有方法得到一组更有限的警报从数据库。
    private static Cursor getFilteredAlarmsCursor(
            ContentResolver contentResolver) {
        return contentResolver.query(Alarm.Columns.CONTENT_URI,
                Alarm.Columns.ALARM_QUERY_COLUMNS, Alarm.Columns.WHERE_ENABLED,
                null, null);
    }

    private static ContentValues createContentValues(Alarm alarm) {
        ContentValues values = new ContentValues(8);
        // 如果这个报警设置alarm_time价值不重复。这将是使用后关闭警报到期。
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

        // 空警报Uri表示无声警报。
        values.put(Alarm.Columns.ALERT, alarm.alert == null ? ALARM_ALERT_SILENT
                : alarm.alert.toString());

        return values;
    }

    private static void clearSnoozeIfNeeded(Context context, long alarmTime) {
        /*
         * 如果这个报警的火灾下小睡之前,清除小睡
         * 启用这个警报。
         */
        SharedPreferences prefs =
                context.getSharedPreferences(DeskClockMainActivity.PREFERENCES, 0);
        long snoozeTime = prefs.getLong(PREF_SNOOZE_TIME, 0);
        if (alarmTime < snoozeTime) {
            clearSnoozePreference(context, prefs);
        }
    }

    /**
     * 返回一个在数据库中表示报警的报警对象id。
     * 如果不存在报警返回null。
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
     * 一个方便的方法来设置一个报警警报
     * 内容提供者。
     * 当警报会火。
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
             * 禁用小睡如果我们只是改变了报警打瞌睡之际。这
             * 不仅工作如果打瞌睡之际报警是一样的
             * 警报。
             * 待办事项:disableSnoozeAlert应该有一个更好的名字。
             */
            disableSnoozeAlert(context, alarm.id);

            /*
             * 禁用小睡之前如果这个报警的火灾报警打瞌睡之际。
             * 这适用于所有报警自用户最有可能的打算
             * 已经修改后的火灾报警。
             */
            clearSnoozeIfNeeded(context, timeInMillis);
        }

        setNextAlert(context);

        return timeInMillis;
    }

    /**
     * 一个方便的方法来启用或禁用一个警报。
     * @param id对应于_id列
     * @param启用启用对应的列
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
         * 如果我们使报警,报警时间计算
         * 价值在警报可能是老了。
         */
        if (enabled) {
            long time = 0;
            if (!alarm.daysOfWeek.isRepeatSet()) {
                time = calculateAlarm(alarm);
            }
            values.put(Alarm.Columns.ALARM_TIME, time);
        } else {
            // 小睡如果id匹配。
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
                     * 0表示这是一个重复的警报,所以
                     * 计算下一个警报的时间。
                     */
                    if (a.time == 0) {
                        a.time = calculateAlarm(a);
                    } else if (a.time < now) {
                        Log.v("wangxianming", "Disabling expired alarm set for ");
                        //过期闹钟,禁用它,沿着。
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
     * 通过禁用无重复警报。叫在
     * 引导。
     */
    public static void disableExpiredAlarms(final Context context) {
        Cursor cur = getFilteredAlarmsCursor(context.getContentResolver());
        long now = System.currentTimeMillis();

        if (cur.moveToFirst()) {
            do {
                Alarm alarm = new Alarm(cur);
                /*
                 * 0意味着这个报警的重复。如果时间是
                 * 非零,检查时间是在现在。
                 */
                if (alarm.time != 0 && alarm.time < now) {
                    enableAlarmInternal(context, alarm, false);
                }
            } while (cur.moveToNext());
        }
        cur.close();
    }

    /**
     * 在系统启动时,时间/时区变化,无论何时
     * 用户更改报警设置。激活小睡如果设置,
     * 否则加载所有警报,激活下一个警报。
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
     * 设置警报AlarmManger和状态栏。这是什么
     * 实际上启动警报当报警触发。
     * @param报警器报警。
     * @param atTimeInMillis时代以来的毫秒
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
         * 这是一个轻微的攻击,以避免异常遥远
         * AlarmManagerService过程。AlarmManager添加额外的数据
         * 这个意图使它膨胀。因为远程过程
         * 不知道报警阶级,它会抛出一个吗
         * ClassNotFoundException。
         * 为了避免这种情况,我们马歇尔的数据然后包裹平原
         * byte[]数组。AlarmReceiver类知道构建报警
         * 对象的byte[]数组。

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
     * isables警报AlarmManger和状态栏。
     * @param id报警id。
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
        // 设置下一个警报更新后小睡。
        setNextAlert(context);
    }

    /**
     * 禁用小睡警告如果给定id匹配的小睡id。
     */
    static void disableSnoozeAlert(final Context context, final int id) {
        SharedPreferences prefs = context.getSharedPreferences(
        		DeskClockMainActivity.PREFERENCES, 0);
        int snoozeId = prefs.getInt(PREF_SNOOZE_ID, -1);
        if (snoozeId == -1) {
            // 没有睡,什么也不做.
            return;
        } else if (snoozeId == id) {
            //这是相同的id明确共享首选项。
            clearSnoozePreference(context, prefs);
        }
    }

    /*
     * 助手把小睡偏好。不要使用清楚因为那
     * 将消除时钟的偏好。也清楚的小睡通知
     * 窗户帘。
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
     * 如果有一个小睡,AlarmManager启用它
     * 如果设置小睡@return如此
     */
    private static boolean enableSnoozeAlert(final Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
        		DeskClockMainActivity.PREFERENCES, 0);

        int id = prefs.getInt(PREF_SNOOZE_ID, -1);
        if (id == -1) {
            return false;
        }
        long time = prefs.getLong(PREF_SNOOZE_TIME, -1);

        // 从db得到警报。
        final Alarm alarm = getAlarm(context.getContentResolver(), id);
        if (alarm == null) {
            return false;
        }
        /*数据库中的时间是0(重复)或一个特定的时间
         * 无重复报警。所以AlarmReceiver更新这个值
         * 比较合适的时间。
        */
        alarm.time = time;

        enableAlert(context, alarm, time);
        return true;
    }

    /**
     * 讲述了状态栏是否启用或禁用报警
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
     * 鉴于小时和分钟警报,返回一个适合的时间
     * 在AlarmManager设置。
     */
    static Calendar calculateAlarm(int hour, int minute,
            Alarm.DaysOfWeek daysOfWeek) {

        // 从现在开始
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());

        int nowHour = c.get(Calendar.HOUR_OF_DAY);
        int nowMinute = c.get(Calendar.MINUTE);

        // 如果警报是在当前时间,提前一天
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

    /* 使用AlarmAlert */
    static String formatTime(final Context context, Calendar c) {
        String format = get24HourMode(context) ? M24 : M12;
        return (c == null) ? "" : (String)DateFormat.format(format, c);
    }

    /**
     * 显示日期和时间——用于锁定屏幕
     */
    private static String formatDayAndTime(final Context context, Calendar c) {
        String format = get24HourMode(context) ? DM24 : DM12;
        return (c == null) ? "" : (String)DateFormat.format(format, c);
    }

    /**
     *节省时间的下一个闹钟,一个格式化的字符串,到系统中
     *设置护理可以利用它的人.
     */
    static void saveNextAlarm(final Context context, String timeString) {
        Settings.System.putString(context.getContentResolver(),
                                  Settings.System.NEXT_ALARM_FORMATTED,
                                  timeString);
    }

    /**
     * @return真实如果时钟设置为24小时模式
     */
    static boolean get24HourMode(final Context context) {
        return android.text.format.DateFormat.is24HourFormat(context);
    }
}
