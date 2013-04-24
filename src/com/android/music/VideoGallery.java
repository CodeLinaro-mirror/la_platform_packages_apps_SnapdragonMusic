package com.android.music;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class VideoGallery extends Activity{
	private final String VIDEO_UNSPECIFIED =  "video/*";
	private final String INTENT_ACTION_VIDEO_GALLERY = "video_gallery";
	 public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        
	        Intent intent = new Intent();
	        intent.setClassName("com.android.gallery3d", "com.android.gallery3d.app.Gallery");
	        intent.setAction(INTENT_ACTION_VIDEO_GALLERY);
	        intent.setType(VIDEO_UNSPECIFIED);
	        //intent.putExtra("VideoEntry", true);
	        startActivity(intent);	        
	        finish();
	    }
}