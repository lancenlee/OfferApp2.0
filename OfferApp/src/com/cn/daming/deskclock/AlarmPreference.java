/*
 * 作品：CIL创新实验室 NoteBook
 * author:lancen、AndroidBirdBoom、JimmyLegend
 * 时间：2016/7/31
 */

package com.cn.daming.deskclock;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.RingtonePreference;
import android.util.AttributeSet;

/**
 * RingtonePreference没有办法获取/设置当前铃声
*我们覆盖onSaveRingtone onRestoreRingtone得到相同的行为。
 */
public class AlarmPreference extends RingtonePreference {
    private Uri mAlert;

    public AlarmPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSaveRingtone(Uri ringtoneUri) {
        setAlert(ringtoneUri);
    }

    @Override
    protected Uri onRestoreRingtone() {
        if (RingtoneManager.isDefault(mAlert)) {
            return RingtoneManager.getActualDefaultRingtoneUri(getContext(),
                    RingtoneManager.TYPE_ALARM);
        }
        return mAlert;
    }

    public void setAlert(Uri alert) {
        mAlert = alert;
        if (alert != null) {
            final Ringtone r = RingtoneManager.getRingtone(getContext(), alert);
            if (r != null) {
                setSummary(r.getTitle(getContext()));
            }
        } else {
            setSummary(R.string.silent_alarm_summary);
        }
    }

    public Uri getAlert() {
        return mAlert;
    }
}
