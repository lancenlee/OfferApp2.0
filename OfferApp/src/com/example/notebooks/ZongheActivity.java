/*
 * 作品：CIL创新实验室 NoteBook
 * author:AndroidBirdBoom
 * 时间：2016/7/23
 * 功能：内容界面的显示
 */
package com.example.notebooks;

import java.io.File;
import java.io.IOException;
//import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.cn.daming.deskclock.R;
import com.cn.daming.deskclock.R.id;
import com.cn.daming.deskclock.R.layout;
import com.cn.daming.deskclock.R.menu;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

public class ZongheActivity extends Activity implements View.OnClickListener{

	private String flag;
    private Button quit_Btn, finish_Btn;
    private EditText content_et, name_et;
    private ImageView img;
    private VideoView video;
    private NotesDB notesDB;
    private SQLiteDatabase dbWriter;
    private File phoneFile;
    private File videoFile;
    private File musicFile;
    private String ss = "AB";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_zonghe);
		flag = getIntent().getStringExtra("flag");
        quit_Btn = (Button) findViewById(R.id.biji_quit_Btn);
        finish_Btn = (Button) findViewById(R.id.biji_finish_Btn);
        img = (ImageView) findViewById(R.id.biji_img);
        video = (VideoView) findViewById(R.id.biji_video);
        content_et = (EditText) findViewById(R.id.biji_content_ed);
        name_et = (EditText) findViewById(R.id.biji_et_zonghe_name);
        quit_Btn.setOnClickListener(this);
        finish_Btn.setOnClickListener(this);
        notesDB = new NotesDB(this);
        dbWriter = notesDB.getWritableDatabase();
        initView();
	}

	//初始化操作
    public void initView() {
        if (flag.equals("1")) {                   //文字
            img.setVisibility(View.GONE);
            video.setVisibility(View.GONE);
        }
        if (flag.equals("2")) {                   //图片
            img.setVisibility(View.VISIBLE);
            video.setVisibility(View.GONE);
            Intent iimg = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            phoneFile = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/" + getTime() + ".jpg");
            iimg.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(phoneFile));
            startActivityForResult(iimg, 1);
        }
        if (flag.equals("3")) {                   //录音
            img.setVisibility(View.GONE);
            video.setVisibility(View.GONE);
            Intent music = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
            startActivityForResult(music, 3);
        }
        if (flag.equals("4")) {                   //视频
            img.setVisibility(View.GONE);
            video.setVisibility(View.VISIBLE);
            Intent video = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            videoFile = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/" + getTime() + ".mp4");
            video.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(videoFile));
            startActivityForResult(video, 2);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.biji_quit_Btn:
                finish();
                break;
            case R.id.biji_finish_Btn:
                addDB();
                finish();
                break;
            default:
                break;
        }
    }

    public void addDB() {
        ContentValues values = new ContentValues();
        values.put(NotesDB.CONTENT, content_et.getText().toString());
        values.put(NotesDB.NAME, name_et.getText().toString());
        values.put(NotesDB.TIME, getTime());
        values.put(NotesDB.PATH, phoneFile + "");
        values.put(NotesDB.VIDEO, videoFile + "");
        values.put(NotesDB.MUSIC,ss);
        dbWriter.insert(NotesDB.TABLE_NAME, null, values);
    }

    public String getTime() {
        Date nowTime = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
        String times = simpleDateFormat.format(nowTime);
        return times;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            /*如果输出文件被写入外部存储，
            本应用需要具有写外部存储的权限,
            还要具有录音的权限．这些权限必须
            在AndroidManifest.xml 文件中声明，像这样：
            //读取外部文件的权限
            <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
            //获取录音的权限
            <uses-permission android:name="android.permission.RECORD_AUDIO" />*/
            //这里记得要加权限，不然bitmap会返回null
            //<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
            Bitmap bitmap = BitmapFactory.decodeFile(phoneFile.getAbsolutePath());
            img.setImageBitmap(bitmap);
        }
        if (requestCode == 2) {
            video.setVideoURI(Uri.fromFile(videoFile));
            video.start();
        }
        if (requestCode == 3) {
        	try {
        		Uri audiopath = data.getData();
                	Cursor cursor = getContentResolver().query(audiopath, null, null, null, null);
                    cursor.moveToFirst();
                    ss = cursor.getString(1);
                    MediaPlayer mediaPlayer = new MediaPlayer();
                    try {
        				mediaPlayer.setDataSource(ss);
        				mediaPlayer.prepare();
        				mediaPlayer.start();
        			} catch (Exception e) {
        				// TODO Auto-generated catch block
        				e.printStackTrace();
        			}
                    cursor.close();
			} catch (Exception e) {
				// TODO: handle exception
				ss = "很抱歉，并没有使用录音机";
				e.printStackTrace();
			}
        }
    }
}
