/*
 * ��Ʒ��CIL����ʵ���� NoteBook
 * author:lancen��AndroidBirdBoom��JimmyLegend
 * ʱ�䣺2016/8/1
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
 * ����AlarmAlert IntentReceiver AlarmAlertactivity��ͨ������ID��
 */
public class AlarmReceiver extends BroadcastReceiver {

    /** If the alarm is older than STALE_WINDOW, ignore.  It
        is probably the result of a time or timezone change */
    private final static int STALE_WINDOW = 30 * 60 * 1000;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Alarms.ALARM_KILLED.equals(intent.getAction())) {
            // �����Ѿ���ɱ��,����֪ͨ
            updateNotification(context, (Alarm)
                    intent.getParcelableExtra(Alarms.ALARM_INTENT_EXTRA),
                    intent.getIntExtra(Alarms.ALARM_KILLED_TIMEOUT, -1));
            return;
        } else if (Alarms.CANCEL_SNOOZE.equals(intent.getAction())) {
            Alarms.saveSnoozeAlert(context, -1, -1);
            return;
        } else if (!Alarms.ALARM_ALERT_ACTION.equals(intent.getAction())) {
            // δ֪����ͼ,����
            return;
        }

        Alarm alarm = null;
        /*
         * ץס������Ŀ�ġ���ΪԶ��AlarmManagerService
         * ��дĿ�����һЩ���������,������unparcel
         * ��������unparcellingʱ�׳�ClassNotFoundException��
         * Ϊ�˱����������,�����Լ���
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
            //�����Ҫȷ������������һ������.
            Alarms.setNextAlert(context);
            return;
        }

        // ����С˯����������������С˯��
        Alarms.disableSnoozeAlert(context, alarm.id);
        // �������������������ظ���
        if (!alarm.daysOfWeek.isRepeatSet()) {
            Alarms.enableAlarm(context, alarm.id, false);
        } else {
           /*
            * �����һ��������һ������������ĵ���
            * enableAlarm������setNextAlert���Ա���������Ρ�
            */
            Alarms.setNextAlert(context);
        }

     /*
      * ������ϸ:���Ǽ�¼����ʱ���ṩ���õ�
      * ��bug�������Ϣ��
      */
        long now = System.currentTimeMillis();

        // ������ϸ׷��ʱ��仯����.
        if (now > alarm.time + STALE_WINDOW) {
            Log.v("wangxianming", "Ignoring stale alarm");
            return;
        }

       /*
        * ά��һ��cpu����,ֱ��AlarmAlert AlarmKlaxon��������
        * ������������
        */
        AlarmAlertWakeLock.acquireCpuWakeLock(context);

        /* �رնԻ���ʹ��� */
        Intent closeDialogs = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(closeDialogs);

        // ������Щ���ʼ���ڼ���������״̬��
        Class c = AlarmAlert.class;
        KeyguardManager km = (KeyguardManager) context.getSystemService(
                Context.KEYGUARD_SERVICE);
        if (km.inKeyguardRestrictedInputMode()) {
            // Ϊ��ȫʹ��ȫ�����
            c = AlarmAlertFullScreen.class;
        }

        // 	�������������豸��
        Intent playAlarm = new Intent(Alarms.ALARM_ALERT_ACTION);
        playAlarm.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm);
        context.startService(playAlarm);
        /*
         * ����һ��֪ͨ,�����ʱ,����ʾ��������
         * �Ի��򡣲���Ҫ���ȫ����Ϊ�⽫��Զ��
         * ��һ���û�������
         */

        Intent notify = new Intent(context, AlarmAlert.class);
        notify.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm);
        PendingIntent pendingNotify = PendingIntent.getActivity(context,
                alarm.id, notify, 0);

        // ʹ�þ����ı�ǩ���Ʊ�ı���Ĭ�ϵı�ǩ ��Ҫ���ݵ�֪ͨ.
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

        //��:Ƕ��ȫ�����档֪ͨ������չ���ʾ�������Ļ���̫���ˡ�
        NotificationManager nm = getNotificationManager(context);
        nm.notify(alarm.id, n);
    }

    private NotificationManager getNotificationManager(Context context) {
        return (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private void updateNotification(Context context, Alarm alarm, int timeout) {
        NotificationManager nm = getNotificationManager(context);

        //���������null,��ȡ��֪ͨ.
        if (alarm == null) {
            if (true) {
                Log.v("wangxianming", "Cannot update notification for killer callback");
            }
            return;
        }

        // ���������SetAlarm��
        Intent viewAlarm = new Intent(context, SetAlarm.class);
        viewAlarm.putExtra(Alarms.ALARM_ID, alarm.id);
        PendingIntent intent =
                PendingIntent.getActivity(context, alarm.id, viewAlarm, 0);

        // ����֪ͨ�������� ��Ĭ��
        String label = alarm.getLabelOrDefault(context);
        Notification n = new Notification(R.drawable.stat_notify_alarm,
                label, alarm.time);
        n.setLatestEventInfo(context, label,
                context.getString(R.string.alarm_alert_alert_silenced, timeout),
                intent);
        n.flags |= Notification.FLAG_AUTO_CANCEL;
        /*
         * ���ǲ��ò�ȡ��ԭ����֪ͨ,��Ϊ����
         * ���ڽ��еĲ���,����ϣ��֪ͨ��
         */
        nm.cancel(alarm.id);
        nm.notify(alarm.id, n);
    }
}
