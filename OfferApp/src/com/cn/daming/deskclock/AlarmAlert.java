
/*
 * 作品：CIL创新实验室 NoteBook
 * author:lancen、AndroidBirdBoom、JimmyLegend
 * 时间：2016/8/3
 */

package com.cn.daming.deskclock;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;

public class AlarmAlert extends AlarmAlertFullScreen {

   
    private int mKeyguardRetryCount;
    private final int MAX_KEYGUARD_CHECKS = 5;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            handleScreenOff((KeyguardManager) msg.obj);
        }
    };

    private final BroadcastReceiver mScreenOffReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    KeyguardManager km =
                            (KeyguardManager) context.getSystemService(
                            Context.KEYGUARD_SERVICE);
                    handleScreenOff(km);
                }
            };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        
        registerReceiver(mScreenOffReceiver,
                new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mScreenOffReceiver);
       
        mHandler.removeMessages(0);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private boolean checkRetryCount() {
        if (mKeyguardRetryCount++ >= MAX_KEYGUARD_CHECKS) {
            Log.v("wangxianming", "Tried to read keyguard status too many times, bailing...");
            return false;
        }
        return true;
    }

    private void handleScreenOff(final KeyguardManager km) {
        if (!km.inKeyguardRestrictedInputMode() && checkRetryCount()) {
            if (checkRetryCount()) {
                mHandler.sendMessageDelayed(mHandler.obtainMessage(0, km), 500);
            }
        } else {
            
            Intent i = new Intent(this, AlarmAlertFullScreen.class);
            i.putExtra(Alarms.ALARM_INTENT_EXTRA, mAlarm);
            i.putExtra(SCREEN_OFF, true);
            startActivity(i);
            finish();
        }
    }
}
