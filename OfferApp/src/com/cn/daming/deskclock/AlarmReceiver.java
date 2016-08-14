/*
 * 作品：CIL创新实验室 NoteBook
 * author:lancen、AndroidBirdBoom、JimmyLegend
 * 时间：2016/8/1
 */

package com.cn.daming.deskclock;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.database.Cursor;
import android.os.Parcel;
import android.util.Log;

/**
 * 连接AlarmAlert IntentReceiver AlarmAlertactivity。通过报警ID。
 */
public class AlarmReceiver extends BroadcastReceiver {

    /** If the alarm is older than STALE_WINDOW, ignore.  It
        is probably the result of a time or timezone change */
    private final static int STALE_WINDOW = 30 * 60 * 1000;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Alarms.ALARM_KILLED.equals(intent.getAction())) {
            // 警报已经被杀死,更新通知
            updateNotification(context, (Alarm)
                    intent.getParcelableExtra(Alarms.ALARM_INTENT_EXTRA),
                    intent.getIntExtra(Alarms.ALARM_KILLED_TIMEOUT, -1));
            return;
        } else if (Alarms.CANCEL_SNOOZE.equals(intent.getAction())) {
            Alarms.saveSnoozeAlert(context, -1, -1);
            return;
        } else if (!Alarms.ALARM_ALERT_ACTION.equals(intent.getAction())) {
            // 未知的意图,保释
            return;
        }

        Alarm alarm = null;
        /*
         * 抓住报警的目的。因为远程AlarmManagerService
         * 填写目的添加一些额外的数据,它必须unparcel
         * 报警对象。unparcelling时抛出ClassNotFoundException。
         * 为了避免这种情况,整理自己。
         */
        final byte[] data = intent.getByteArrayExtra(Alarms.ALARM_RAW_DATA);
        if (data != null) {
            Parcel in = Parcel.obtain();
            in.unmarshall(data, 0, data.length);
            in.setDataPosition(0);
            alarm = Alarm.CREATOR.createFromParcel(in);
        }

        if (alarm == null) {
            Log.v("wangxianming", "Failed to parse the alarm from the intent");
            //如果需要确保我们设置下一个警报.
            Alarms.setNextAlert(context);
            return;
        }

        // 禁用小睡警告如果这个警报是小睡。
        Alarms.disableSnoozeAlert(context, alarm.id);
        // 禁用这个警报如果它不重复。
        if (!alarm.daysOfWeek.isRepeatSet()) {
            Alarms.enableAlarm(context, alarm.id, false);
        } else {
           /*
            * 如果有一个启用下一个警报。上面的调用
            * enableAlarm将调用setNextAlert所以避免调用两次。
            */
            Alarms.setNextAlert(context);
        }

     /*
      * 故意详细:总是记录报警时间提供有用的
      * 在bug报告的信息。
      */
        long now = System.currentTimeMillis();

        // 总是详细追踪时间变化问题.
        if (now > alarm.time + STALE_WINDOW) {
            Log.v("wangxianming", "Ignoring stale alarm");
            return;
        }

       /*
        * 维持一个cpu锁定,直到AlarmAlert AlarmKlaxon可以醒来
        * 把它捡起来。
        */
        AlarmAlertWakeLock.acquireCpuWakeLock(context);

        /* 关闭对话框和窗口 */
        Intent closeDialogs = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(closeDialogs);

        // 决定哪些活动开始基于键盘守卫的状态。
        Class c = AlarmAlert.class;
        KeyguardManager km = (KeyguardManager) context.getSystemService(
                Context.KEYGUARD_SERVICE);
        if (km.inKeyguardRestrictedInputMode()) {
            // 为安全使用全屏活动。
            c = AlarmAlertFullScreen.class;
        }

        // 	报警警报和振动设备。
        Intent playAlarm = new Intent(Alarms.ALARM_ALERT_ACTION);
        playAlarm.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm);
        context.startService(playAlarm);
        /*
         * 触发一个通知,当点击时,将显示报警警报
         * 对话框。不需要检查全屏因为这将永远是
         * 从一个用户操作。
         */

        Intent notify = new Intent(context, AlarmAlert.class);
        notify.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm);
        PendingIntent pendingNotify = PendingIntent.getActivity(context,
                alarm.id, notify, 0);

        // 使用警报的标签或股票文本和默认的标签 主要内容的通知.
        String label = alarm.getLabelOrDefault(context);
        Notification n = new Notification(R.drawable.stat_notify_alarm,
                label, alarm.time);
        n.setLatestEventInfo(context, label,
                context.getString(R.string.alarm_notify_text),
                pendingNotify);
        n.flags |= Notification.FLAG_SHOW_LIGHTS
                | Notification.FLAG_ONGOING_EVENT;
        n.defaults |= Notification.DEFAULT_LIGHTS;

        // NEW: Embed the full-screen UI here. The notification manager will
        // take care of displaying it if it's OK to do so.
        Intent alarmAlert = new Intent(context, c);
        alarmAlert.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm);
        alarmAlert.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        n.fullScreenIntent = PendingIntent.getActivity(context, alarm.id, alarmAlert, 0);

        //新:嵌入全屏界面。通知经理会照顾显示这样做的话就太好了。
        NotificationManager nm = getNotificationManager(context);
        nm.notify(alarm.id, n);
    }

    private NotificationManager getNotificationManager(Context context) {
        return (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private void updateNotification(Context context, Alarm alarm, int timeout) {
        NotificationManager nm = getNotificationManager(context);

        //如果警报是null,就取消通知.
        if (alarm == null) {
            if (true) {
                Log.v("wangxianming", "Cannot update notification for killer callback");
            }
            return;
        }

        // 当点击发射SetAlarm。
        Intent viewAlarm = new Intent(context, SetAlarm.class);
        viewAlarm.putExtra(Alarms.ALARM_ID, alarm.id);
        PendingIntent intent =
                PendingIntent.getActivity(context, alarm.id, viewAlarm, 0);

        // 更新通知表明警报 沉默。
        String label = alarm.getLabelOrDefault(context);
        Notification n = new Notification(R.drawable.stat_notify_alarm,
                label, alarm.time);
        n.setLatestEventInfo(context, label,
                context.getString(R.string.alarm_alert_alert_silenced, timeout),
                intent);
        n.flags |= Notification.FLAG_AUTO_CANCEL;
        /*
         * 我们不得不取消原来的通知,因为它是
         * 正在进行的部分,我们希望通知。
         */
        nm.cancel(alarm.id);
        nm.notify(alarm.id, n);
    }
}
