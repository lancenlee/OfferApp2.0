/*
 * 作品：CIL创新实验室 NoteBook
 * author:AndroidBirdBoom
 * 时间：2016/7/21
 * 功能：作为综合笔记的适配器
 */
package com.example.notebooks;
import com.cn.daming.deskclock.R;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
public class mAdapter extends BaseAdapter{
	 private Context context;//承接上下文的context
	    private Cursor cursor;//数据库查询出来的是一个cursor对象

	    public mAdapter(Context context, Cursor cursor) {
	        this.cursor = cursor;
	        this.context = context;
	    }

	    @Override
	    //返回的是一个cursor的长度
	    public int getCount() {
	        return cursor.getCount();
	    }

	    @Override
	    public Object getItem(int i) {
	        return cursor.getPosition();
	    }

	    @Override
	    public long getItemId(int i) {
	        return i;
	    }

	    @Override
	    public View getView(int i, View view, ViewGroup viewGroup) {
	        ViewHolder viewHolder;
	        LayoutInflater layoutInflater;//加载试图的权限
	        View layout;
	        if (view == null) {
	            viewHolder = new ViewHolder();
	            layoutInflater = LayoutInflater.from(context);
	            layout = layoutInflater.inflate(R.layout.list_item, null);
	            //获取布局文件中的所有内容
	            viewHolder.contentView = (TextView) layout.findViewById(R.id.listItem_content);
	            viewHolder.timeView = (TextView) layout.findViewById(R.id.listItem_time);
	            viewHolder.imgView = (ImageView) layout.findViewById(R.id.listItem_iv_img);
	            viewHolder.videoView = (ImageView) layout.findViewById(R.id.listItem_Video);
	            viewHolder.bijiImgView = (ImageView) layout.findViewById(R.id.listItem_img_biji);
	            layout.setTag(viewHolder);
	        } else {
	            layout = view;
	            viewHolder = (ViewHolder) layout.getTag();
	        }
	        cursor.moveToPosition(i);
	        String content = cursor.getString(cursor.getColumnIndex("content"));
	        String time = cursor.getString(cursor.getColumnIndex("time"));
	        //加载信息
	        String url = cursor.getString(cursor.getColumnIndex("path"));
	        String urlVideo = cursor.getString(cursor.getColumnIndex("video"));
	        viewHolder.contentView.setText(content);
	        viewHolder.timeView.setText(time);
	        if (!url.equals("null")) {
	            viewHolder.imgView.setVisibility(View.VISIBLE);
	            viewHolder.bijiImgView.setVisibility(View.GONE);
	            viewHolder.videoView.setVisibility(View.GONE);
	            viewHolder.imgView.setImageBitmap(getImageBitmap(url, 200, 200));
	        }
	        if (!urlVideo.equals("null")){
	            viewHolder.videoView.setVisibility(View.VISIBLE);
	            viewHolder.bijiImgView.setVisibility(View.GONE);
	            viewHolder.imgView.setVisibility(View.GONE);
	            viewHolder.videoView.setImageBitmap(getVideoBitmap(urlVideo, 200, 200, MediaStore.Images.Thumbnails.MICRO_KIND));
	        }
	        if (url.equals("null")&&urlVideo.equals("null")){
	            viewHolder.bijiImgView.setVisibility(View.VISIBLE);
	            viewHolder.imgView.setVisibility(View.GONE);
	            viewHolder.videoView.setVisibility(View.GONE);
	            viewHolder.bijiImgView.setImageResource(R.drawable.notepad);
	        }
	        return layout;
	    }

	    class ViewHolder {
	        ImageView bijiImgView;
	        ImageView imgView;
	        ImageView videoView;
	        TextView contentView;
	        TextView timeView;
	    }
	    //显示所有信息的列表的缩略图

	    public Bitmap getImageBitmap(String uri, int width, int height) {
	        Bitmap bitmap = null;//初始化
	        BitmapFactory.Options options = new BitmapFactory.Options();//获取缩略图
	        options.inJustDecodeBounds = true;
	        bitmap = BitmapFactory.decodeFile(uri, options);
	        options.inJustDecodeBounds = false;
	        //图片缩略图的范围
	        int beWidth = options.outWidth / width;
	        int beHeight = options.outHeight / height;
	        int be = 1;
	        if (beWidth < beHeight) {
	            be = beWidth;
	        } else {
	            be = beHeight;
	        }
	        if (be <= 0) {
	            be = 1;
	        }
	        options.inSampleSize = be;
	        //获取缩略图之后的图片
	        bitmap = BitmapFactory.decodeFile(uri, options);
	        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
	        return bitmap;
	    }
	    //创建一个视频的缩略图

	    private Bitmap getVideoBitmap(String uri, int width, int height, int kind) {
	        Bitmap bitmap = null;
	        bitmap = ThumbnailUtils.createVideoThumbnail(uri, kind);
	        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
	        return bitmap;
	    }
}
