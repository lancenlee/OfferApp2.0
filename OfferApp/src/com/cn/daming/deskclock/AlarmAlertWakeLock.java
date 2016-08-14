/*
 * ��Ʒ��CIL����ʵ���� NoteBook
 * author:lancen��AndroidBirdBoom��JimmyLegend
 * ʱ�䣺2016/8/15
 */


package com.cn.daming.deskclock;

import android.content.Context;
import android.os.PowerManager;

/*
 * ��Ϊ������AlarmReceiver���չ������ڱ�������������˼�������
 */
class AlarmAlertWakeLock {

    private static PowerManager.WakeLock sCpuWakeLock;

    static void acquireCpuWakeLock(Context context) {
        if (sCpuWakeLock != null) {
            return;
        }

        PowerManager pm =
                (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        sCpuWakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE, "AlarmClock");
        sCpuWakeLock.acquire();
    }

    static void releaseCpuLock() {
        if (sCpuWakeLock != null) {
            sCpuWakeLock.release();
            sCpuWakeLock = null;
        }
    }
}
