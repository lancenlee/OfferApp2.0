
/*
 * ��Ʒ��CIL����ʵ���� NoteBook
 * author:lancen��AndroidBirdBoom��JimmyLegend
 * ʱ�䣺2016/8/7
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
 * �������ͷ�Χ����Ϊһ����������,�����Ϳ��Լ������������һ�������AlarmAlert�Ի���
 */
public class AlarmKlaxon extends Service {

    /** �򱨾�ǰ10���ӳ�Ĭ */
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
    
    // �ڲ���Ϣ
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
             * �û������Ѿ��ڴ�绰����������
             * ������ע��onCallStateChanged,����ں���״̬ɱ���˱�����
             * ����ʼ����״̬���ǲ�ɱ�ڴ�绰������
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
        // ��������ɱ������.
        mTelephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(
                mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        AlarmAlertWakeLock.acquireCpuWakeLock(this);
    }

    @Override
    public void onDestroy() {
        stop();
        // ֹͣ��������.
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
        // û����ͼ,����ϵͳ������������.
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
        // ��¼����ĳ�ʼ����״̬,�Ա��±��� ���µ�״̬��
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

    // �ں��б���ϵͳ����ϵ�������ý���
    private static final float IN_CALL_VOLUME = 0.125f;

    private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            mHandler.obtainMessage(FOCUSCHANGE, focusChange, 0).sendToTarget();
        }
    };
    private void play(Alarm alarm) {
        // stop()����Ƿ������Ѿ��ꡣ
        mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        stop();

        Log.v("wangxianming", "AlarmKlaxon.play() " + alarm.id + " alert " + alarm.alert);

        if (!alarm.silent) {
            Uri alert = alarm.alert;
            // ����Ĭ�ϱ������û�����ݿ� �����洢��
            if (alert == null) {
                alert = RingtoneManager.getDefaultUri(
                        RingtoneManager.TYPE_ALARM);
                Log.v("wangxianming", "Using default alarm: " + alert.toString());
            }

            /*
             * ��������:����mMediaPlayer�����Ǵ���һ���µĺ�/��ʹ��
             * RingtoneManager��
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
                 * ����Ƿ������ڴ�绰���������ʹ���ں��б���
                 * ��Դ��������жϵ��á�
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
                 * sd���ϵľ�����ܻ�æ
                 * ���ڡ�ʹ�û���������
                 */
                try {
                    // ��������ý�岥�����������״̬��
                    mMediaPlayer.reset();
                    setDataSourceFromResource(getResources(), mMediaPlayer,
                            R.raw.fallbackring);
                    startAlarm(mMediaPlayer);
                } catch (Exception ex2) {
                    //����һ�������ǲ����κζ�����
                    Log.v("wangxianming", "Failed to play fallback ringtone"+ex2);
                }
            }
        }

        /* ����������ý�岥������һ������ */
        if (alarm.vibrate) {
            mVibrator.vibrate(sVibratePattern, 0);
        } else {
            mVibrator.cancel();
        }

        enableKiller(alarm);
        mPlaying = true;
        mStartTime = System.currentTimeMillis();
    }

    // ��������ʱ�����Ķ���.
    private void startAlarm(MediaPlayer player)
            throws java.io.IOException, IllegalArgumentException,
                   IllegalStateException {
        final AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        /*
         * ���澯������������0��
         * (ͨ����Ϊ����ģʽ�ǳ�Ĭ)��
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

            // ֹͣ��Ƶ����
            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }

            // ֹͣ����
            mVibrator.cancel();
        }
        disableKiller();
    }

    /**
     * ɱ��ALARM_TIMEOUT_SECONDS������Ƶ��,����
     * �޷�����һ���졣
     * ��ֻ��ȡ����Ƶ,���뿪��֪ͨ
     * �����û���֪������ֹͣ��.
     */
    private void enableKiller(Alarm alarm) {
        mHandler.sendMessageDelayed(mHandler.obtainMessage(KILLER, alarm),
                1000 * ALARM_TIMEOUT_SECONDS);
    }

    private void disableKiller() {
        mHandler.removeMessages(KILLER);
    }


}
