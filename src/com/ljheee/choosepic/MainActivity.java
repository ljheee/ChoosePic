package com.ljheee.choosepic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	public static final int TAKE_PHOTO = 1;
	public static final int CROP_PHOTO = 2;
	public static final int CHOOSE_PHOTO = 3;
	
	private Button takePhoto;
	private Button chooseFromAlbum;
	private ImageView picture;
	
	Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        takePhoto = (Button) findViewById(R.id.take_photo);
        chooseFromAlbum = (Button) findViewById(R.id.choose_from_album);
        picture = (ImageView) findViewById(R.id.picture);
        
        takePhoto.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//创建文件
				File outImage = new File(Environment.getExternalStorageDirectory(), "out_image.jpg");
				try{
					if(outImage.exists()){
						outImage.delete();
					}
					outImage.createNewFile();
				}catch(IOException e){
					e.printStackTrace();
				}
				
				imageUri = Uri.fromFile(outImage);
				Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
				intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
				startActivityForResult(intent, TAKE_PHOTO);//通过隐式意图，启动相机
			}
		});
        
        
        chooseFromAlbum.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent ii = new Intent("android.intent.action.GET_CONTENT");
				ii.setType("image/*");
				startActivityForResult(ii, CHOOSE_PHOTO);//隐式意图：启动系统相册
			}
		});
    }
    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    	switch (requestCode) {
		case TAKE_PHOTO:
			
			if(resultCode == RESULT_OK){//拍照完成
				Intent intent = new Intent("com.android.camera.action.CROP");
				intent.setDataAndType(imageUri, "image/*");
				intent.putExtra("scale", true);
				intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
				startActivityForResult(intent, CROP_PHOTO);//启动裁剪程序
			}
			break;
		case CROP_PHOTO:
			
			if(resultCode == RESULT_OK){//拍照完成
				try {
					Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
					picture.setImageBitmap(bitmap);//裁剪后，显示
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
			break;
			
		case CHOOSE_PHOTO:
			
			if(resultCode == RESULT_OK){
				//判断手机-Android系统
				if(Build.VERSION.SDK_INT >=19){//Android4.4以上系统
					handleImageOnKitKat(data);
				} else{
					handleImageBeforeKitKat(data);
				}
			}
			break;

		default:
			break;
		}
    	
    	
    }
    
    /**
     * 从相册选择照片后，处理
     * Android4.4(API19)以上系统使用该方法（不返回图片真实的Uri，做了封装，需要处理）
     * @param data
     */
    @TargetApi(19)
    private void handleImageOnKitKat(Intent data) {
		String imagePath = null;
		Uri uri= data.getData();
		
		//如果是document类型的URI
		if(DocumentsContract.isDocumentUri(this, uri)){
			
			String docId = DocumentsContract.getDocumentId(uri);
			if("com.android.providers.media.documents".equals(uri.getAuthority())){
				String id = docId.split(":")[1];//
				String selection = MediaStore.Images.Media._ID+"="+id;
				imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,selection);
			}else if("com.android.providers.downloads.documents".equals(uri.getAuthority())){
				Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
				imagePath = getImagePath(contentUri, null);
			}
		} else if("content".equalsIgnoreCase(uri.getScheme())){//如果不是document类型的URI,使用普通方式处理
			imagePath = getImagePath(uri, null);
		}
		
		displayImage(imagePath);
	}

    /**
     * 通过uri和selection获取图片真实路径
     * @param externalContentUri
     * @param selection
     * @return
     */
    private String getImagePath(Uri uri, String selection) {
    	String path = null;
    	Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
    	
    	if(null != cursor){
    		if(cursor.moveToFirst()){
    			path = cursor.getString(cursor.getColumnIndex(Media.DATA));
    		}
    		cursor.close();
    	}
		return path;
	}


	/**
     *从相册选择照片后，处理
     * Android4.4以下系统使用该方法（不返回图片真实的Uri，做了封装，需要处理 
     * @param data
     */
	private void handleImageBeforeKitKat(Intent data) {

		Uri uri = data.getData();
		String imagePath = getImagePath(uri, null);
		displayImage(imagePath);
	}


	/**
	 * 显示图片
	 * @param imagePath
	 */
	private void displayImage(String imagePath) {

		if(imagePath != null){
			Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
			picture.setImageBitmap(bitmap);
		}else{
			Toast.makeText(this, "failed to load", Toast.LENGTH_SHORT).show();
		}
	}


	
    
    
    
    
}
