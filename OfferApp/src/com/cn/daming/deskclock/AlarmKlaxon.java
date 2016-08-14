
/*
 * 作品：CIL创新实验室 NoteBook
 * author:lancen、AndroidBirdBoom、JimmyLegend
 * 时间：2016/8/7
 */

package com.cn.daming.deskclock;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.media.AudioManager.OnAudioFocusChangeListener;
/**
 * 管理警报和氛围。作为一个服务运行,这样就可以继续发挥如果另一个活动覆盖AlarmAlert对话框。
 */
public class AlarmKlaxon extends Service {

    /** 打报警前10分钟沉默 */
    private static final int ALARM_TIMEOUT_SECONDS = 10 * 60;

    private static final long[] sVibratePattern = new long[] { 500, 500 };

    private boolean mPlaying = false;
    private Vibrator mVibrator;
    private MediaPlayer mMediaPlayer;
    private Alarm mCurrentAlarm;
    private long mStartTime;
    private TelephonyManager mTelephonyManager;
    private int mInitialCallState;
    private AudioManager mAudioManager = null;
    private boolean mCurrentStates = true;
    
    // 内部消息
    private static final int KILLER = 1;
    private static final int FOCUSCHANGE = 2;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case KILLER:
                    Log.v("wangxianming", "*********** Alarm killer triggered ***********");
                    sendKillBroadcast((Alarm) msg.obj);
                    stopSelf();
                    break;
                case FOCUSCHANGE:
                    switch (msg.arg1) {
                        case AudioManager.AUDIOFOCUS_LOSS:
                            
                            if(!mPlaying && mMediaPlayer != null) {
                                stop();
                            }
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            
                            if(!mPlaying && mMediaPlayer != null) {
                                mMediaPlayer.pause();
                                mCurrentStates = false;
                            }
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                            
                            if(mPlaying && !mCurrentStates) {
                                play(mCurrentAlarm); 
                            } 
                            break;
                        default:
                            
                            break;
                    }                                    
                  default:
                          break;

            }
        }
    };

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String ignored) {
            /*
             * 用户可能已经在打电话报警触发。
             * 当我们注册onCallStateChanged,最初在呼叫状态杀死了报警。
             * 检查初始呼叫状态我们不杀在打电话报警。
             */
            if (state != TelephonyManager.CALL_STATE_IDLE
                    && state != mInitialCallState) {
                sendKillBroadcast(mCurrentAlarm);
                stopSelf();
            }
        }
    };

    @Override
    public void onCreate() {
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        // 监听来电杀死警报.
        mTelephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(
                mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        AlarmAlertWakeLock.acquireCpuWakeLock(this);
    }

    @Override
    public void onDestroy() {
        stop();
        // 停止监听来电.
        mTelephonyManager.listen(mPhoneStateListener, 0);
        AlarmAlertWakeLock.releaseCpuLock();
        mAudioManager.abandonAudioFocus(mAudioFocusListener);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 没有意图,告诉系统重启不了我们.
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        final Alarm alarm = intent.getParcelableExtra(
                Alarms.ALARM_INTENT_EXTRA);

        if (alarm == null) {
            Log.v("wangxianming", "AlarmKlaxon failed to parse the alarm from the intent");
            stopSelf();
            return START_NOT_STICKY;
        }

        if (mCurrentAlarm != null) {
            sendKillBroadcast(mCurrentAlarm);
        }

        play(alarm);
        mCurrentAlarm = alarm;
        // 记录这里的初始呼叫状态,以便新报警 最新的状态。
        mInitialCallState = mTelephonyManager.getCallState();

        return START_STICKY;
    }

    private void sendKillBroadcast(Alarm alarm) {
        long millis = System.currentTimeMillis() - mStartTime;
        int minutes = (int) Math.round(millis / 60000.0);
        Intent alarmKilled = new Intent(Alarms.ALARM_KILLED);
        alarmKilled.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm);
        alarmKilled.putExtra(Alarms.ALARM_KILLED_TIMEOUT, minutes);
        sendBroadcast(alarmKilled);
    }

    // 在呼叫报警系统的体系上提出了媒体键
    private static final float IN_CALL_VOLUME = 0.125f;

    private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            mHandler.obtainMessage(FOCUSCHANGE, focusChange, 0).sendToTarget();
        }
    };
    private void play(Alarm alarm) {
        // stop()检查是否我们已经完。
        mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        stop();

        Log.v("wangxianming", "AlarmKlaxon.play() " + alarm.id + " alert " + alarm.alert);

        if (!alarm.silent) {
            Uri alert = alarm.alert;
            // 依靠默认报警如果没有数据库 报警存储。
            if (alert == null) {
                alert = RingtoneManager.getDefaultUri(
                        RingtoneManager.TYPE_ALARM);
                Log.v("wangxianming", "Using default alarm: " + alert.toString());
            }

            /*
             * 待办事项:重用mMediaPlayer而不是创建一个新的和/或使用
             * RingtoneManager。
             */
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnErrorListener(new OnErrorListener() {
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.v("wangxianming", "Error occurred while playing audio.");
                    mp.stop();
                    mp.release();
                    mMediaPlayer = null;
                    return true;
                }
            });

            try {
                /*
                 * 检查是否我们在打电话。如果我们使用在呼叫报警
                 * 资源低体积不中断调用。
                 */
                if (mTelephonyManager.getCallState()
                        != TelephonyManager.CALL_STATE_IDLE) {
                    Log.v("wangxianming", "Using the in-call alarm");
                    mMediaPlayer.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);
                    setDataSourceFromResource(getResources(), mMediaPlayer,
                            R.raw.in_call_alarm);
                } else {
                    mMediaPlayer.setDataSource(this, alert);
                }
                startAlarm(mMediaPlayer);
            } catch (Exception ex) {
                Log.v("wangxianming", "Using the fallback ringtone");
                /*
                 * sd卡上的警告可能会忙
                 * 现在。使用回退铃声。
                 */
                try {
                    // 必须重置媒体播放器清除错误状态。
                    mMediaPlayer.reset();
                    setDataSourceFromResource(getResources(), mMediaPlayer,
                            R.raw.fallbackring);
                    startAlarm(mMediaPlayer);
                } catch (Exception ex2) {
                    //在这一点上我们不玩任何东西。
                    Log.v("wangxianming", "Failed to play fallback ringtone"+ex2);
                }
            }
        }

        /* 启动振动器与媒体播放器后一切正常 */
        if (alarm.vibrate) {
            mVibrator.vibrate(sVibratePattern, 0);
        } else {
            mVibrator.cancel();
        }

        enableKiller(alarm);
        mPlaying = true;
        mStartTime = System.currentTimeMillis();
    }

    // 启动报警时常见的东西.
    private void startAlarm(MediaPlayer player)
            throws java.io.IOException, IllegalArgumentException,
                   IllegalStateException {
        final AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        /*
         * 不玩警报如果流体积是0吗
         * (通常因为铃声模式是沉默)。
         */
        if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
            player.setAudioStreamType(AudioManager.STREAM_ALARM);
            player.setLooping(true);
            player.prepare();
            player.start();
        }
    }

    private void setDataSourceFromResource(Resources resources,
            MediaPlayer player, int res) throws java.io.IOException {
        AssetFileDescriptor afd = resources.openRawResourceFd(res);
        if (afd != null) {
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
                    afd.getLength());
            afd.close();
        }
    }

    /**
     * Stops alarm audio and disables alarm if it not snoozed and not
     * repeating
     */
    public void stop() {
        Log.v("wangxianming", "AlarmKlaxon.stop()");
        if (mPlaying) {
            mPlaying = false;

            Intent alarmDone = new Intent(Alarms.ALARM_DONE_ACTION);
            sendBroadcast(alarmDone);

            // 停止音频播放
            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }

            // 停止振动器
            mVibrator.cancel();
        }
        disableKiller();
    }

    /**
     * 杀死ALARM_TIMEOUT_SECONDS报警音频后,报警
     * 无法运行一整天。
     * 这只是取消音频,但离开了通知
     * 所以用户就知道闹钟停止了.
     */
    private void enableKiller(Alarm alarm) {
        mHandler.sendMessageDelayed(mHandler.obtainMessage(KILLER, alarm),
                1000 * ALARM_TIMEOUT_SECONDS);
    }

    private void disableKiller() {
        mHandler.removeMessages(KILLER);
    }


}
