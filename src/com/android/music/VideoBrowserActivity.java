/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.music;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.drm.DrmManagerClientWrapper;
import android.drm.DrmRights;
import android.drm.DrmStore.Action;
import android.drm.DrmStore.DrmDeliveryType;
import android.drm.DrmStore.RightsStatus;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.lang.Integer;

public class VideoBrowserActivity extends ListActivity implements MusicUtils.Defs
{
    private static final String LOGTAG = "VideoBrowser";
    public static final String BUY_LICENSE = "android.drmservice.intent.action.BUY_LICENSE";
    public VideoBrowserActivity()
    {
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        init();
    }

    public void init() {

        // Set the layout for this activity.  You can find it
        // in assets/res/any/layout/media_picker_activity.xml
        setContentView(R.layout.media_picker_activity);

        MakeCursor();

        if (mCursor == null) {
            MusicUtils.displayDatabaseError(this);
            return;
        }

        if (mCursor.getCount() > 0) {
            setTitle(R.string.videos_title);
        } else {
            setTitle(R.string.no_videos_title);
        }

        // Map Cursor columns to views defined in media_list_item.xml
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_list_item_1,
                mCursor,
                new String[] { MediaStore.Video.Media.TITLE},
                new int[] { android.R.id.text1 });

        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        mCursor.moveToPosition(position);
        String type = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE));
        String path = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
        Log.i(LOGTAG, "onListItemClick, path of the file is"+path);
        if (path.endsWith(".dcf") || path.endsWith(".dm")) {
            DrmManagerClientWrapper drmClient = new DrmManagerClientWrapper(VideoBrowserActivity.this);
            path = path.replace("/storage/emulated/0", "/storage/emulated/legacy");
            int status = drmClient.checkRightsStatus(path, Action.PLAY);
            Log.i(LOGTAG, "onListItemClick:status fron drmClient.checkRightsStatus is " + Integer.toString(status));
            if (RightsStatus.RIGHTS_VALID != status) {
                ContentValues values = drmClient.getMetadata(path);
                String address = values.getAsString("Rights-Issuer");
                Intent drm_intent = new Intent(BUY_LICENSE);
                drm_intent.putExtra("DRM_FILE_PATH", address);
                this.sendBroadcast(drm_intent);
                return;
            }

            if (drmClient != null) drmClient.release();
        }
        intent.setDataAndType(ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id), type);
        
        startActivity(intent);
    }

    private void MakeCursor() {
        String[] cols = new String[] {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.ARTIST
        };
        ContentResolver resolver = getContentResolver();
        if (resolver == null) {
            System.out.println("resolver = null");
        } else {
            mSortOrder = MediaStore.Video.Media.TITLE + " COLLATE UNICODE";
            mWhereClause = MediaStore.Video.Media.TITLE + " != ''";
            mCursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                cols, mWhereClause , null, mSortOrder);
        }
    }

    @Override
    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
        }
        super.onDestroy();
    }

    private Cursor mCursor;
    private String mWhereClause;
    private String mSortOrder;
}

