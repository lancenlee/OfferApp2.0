
/*
 * ��Ʒ��CIL����ʵ���� NoteBook
 * author:lancen��AndroidBirdBoom��JimmyLegend
 * ʱ�䣺2016/8/2
 */

package com.cn.daming.deskclock;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.util.Log;

public class AlarmInitReceiver extends BroadcastReceiver {

    /**
     * ��ACTION_BOOT_COMPLETED�趨���塣����ʱ������TIMEZONE���ľ���
      */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.v("wangxianming", "AlarmInitReceiver" + action);

        // ������ɾ����Ъ���塣
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Alarms.saveSnoozeAlert(context, -1, -1);
        }

        Alarms.disableExpiredAlarms(context);
        Alarms.setNextAlert(context);
    }
}
