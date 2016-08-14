/*
 * 作品：CIL创新实验室 NoteBook
 * author:AndroidBirdBoom
 * 时间：2016/7/21
 * 
 */
package com.example.notebooks;

import com.cn.daming.deskclock.R;
import com.cn.daming.deskclock.R.id;
import com.cn.daming.deskclock.R.layout;
import com.cn.daming.deskclock.R.menu;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

public class MyContentActivity extends Activity implements AdapterView.OnItemClickListener{

	private mAdapter adapter;
    private NotesDB notesDB;
    private SQLiteDatabase dbReader;//数据库读取的权限
    private ListView lv;
    private Cursor cursor,cursors;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_content);
        notesDB = new NotesDB(this);
        dbReader = notesDB.getReadableDatabase();
        lv = (ListView)findViewById(R.id.lv_content);
        selectDB();
        lv.setOnItemClickListener(this);
    }
    //获取数据

    public void selectDB(){
        cursor = dbReader.query(NotesDB.TABLE_NAME,null,null,null,null,null,null);
        //将adapter实例化
        adapter = new mAdapter(this,cursor);
        lv.setAdapter(adapter);
    }


    //当删除记录后，刷新此界面
    @Override
    protected void onResume() {
        super.onResume();
        onCreate(null);
    }
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        cursors = cursor;
        cursors.moveToPosition(i);
        Intent intent = new Intent(MyContentActivity.this, ShowContent.class);
        intent.putExtra(NotesDB.ID,cursors.getInt(cursors.getColumnIndex(NotesDB.ID)));
        intent.putExtra(NotesDB.NAME,cursors.getString(cursors.getColumnIndex(NotesDB.NAME)));
        intent.putExtra(NotesDB.TIME,cursors.getString(cursors.getColumnIndex(NotesDB.TIME)));
        intent.putExtra(NotesDB.PATH,cursors.getString(cursors.getColumnIndex(NotesDB.PATH)));
        intent.putExtra(NotesDB.CONTENT,cursors.getString(cursors.getColumnIndex(NotesDB.CONTENT)));
        intent.putExtra(NotesDB.VIDEO,cursors.getString(cursors.getColumnIndex(NotesDB.VIDEO)));
        intent.putExtra(NotesDB.MUSIC, cursors.getString(cursors.getColumnIndex(NotesDB.MUSIC)));
        startActivity(intent);
    }
}
