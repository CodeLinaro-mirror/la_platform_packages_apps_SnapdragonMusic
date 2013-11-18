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
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.net.Uri;
import android.view.KeyEvent;
import android.drm.DrmManagerClient;
import android.drm.DrmRights;
import android.drm.DrmStore.Action;
import android.drm.DrmStore.DrmDeliveryType;
import android.drm.DrmStore.RightsStatus;
import android.widget.Toast;
import android.content.ContentValues;
import android.util.Log;
import java.lang.Integer;

public class VideoBrowserActivity extends ListActivity implements MusicUtils.Defs
{
    private String mCurrentVideoName;
    private String mCurrentVideoPath;
    // Drm start
    private static final String LOGTAG = "VideoBrowser";

    public static final String BUY_LICENSE="android.drmservice.intent.action.BUY_LICENSE";
    // Drm end

    private String mFilterString = "";
    private int mSelectedPosition; // Position of selected view
    private static final int SHARE = 0; // Menu to share video

    public VideoBrowserActivity()
    {
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            mFilterString = intent.getStringExtra(SearchManager.QUERY);
        }

        init();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        // Get position of selected view
        AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfo;
        mSelectedPosition =  mi.position;

        // Set menu title
        if (null == mCursor) {
            return;
        }
        mCursor.moveToPosition(mSelectedPosition);
        mCurrentVideoName = mCursor.getString(mCursor.getColumnIndexOrThrow(
                MediaStore.Video.Media.TITLE));
        menu.setHeaderTitle(mCurrentVideoName);

        mCurrentVideoPath = mCursor.getString(mCursor.getColumnIndexOrThrow(
                MediaStore.Video.Media.DATA));

        menu.add(0, PLAY_SELECTION, 0, R.string.play_selection);
        menu.add(0, DELETE_ITEM, 0, R.string.delete_item);

        // Menu item to share video
        menu.add(0, SHARE, 0, R.string.share);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
           case PLAY_SELECTION: {
                // play the video
                int position = mSelectedPosition;
                mCursor.moveToPosition(position);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                long id = mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID));
                String type = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE));
                intent.setDataAndType(ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id), type);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException ex) {
                    Toast.makeText(this, R.string.enable_gallery_app, Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            case DELETE_ITEM: {
                // delete the video
                int position = mSelectedPosition;
                mCursor.moveToPosition(position);
                long id = mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID));
                long [] list = new long[1];
                list[0] = id;
                Bundle b = new Bundle();
                String f;
                if (mCurrentVideoPath.contains("sdcard0")) {
                    f = getString(R.string.delete_song_desc);
                } else {
                    f = getString(R.string.delete_song_desc_nosdcard);
                }
                String desc = String.format(f, mCurrentVideoName);
                b.putString("description", desc);
                b.putLongArray("items", list);
                Intent intent = new Intent();
                intent.setClass(this, DeleteVideoItems.class);
                intent.putExtras(b);
                startActivityForResult(intent, -1);
                return true;
            }

            case SHARE:
                // Send intent to share video
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("video/*");

                if (null == mCursor) {
                    return super.onContextItemSelected(item);
                }
                mCursor.moveToPosition(mSelectedPosition);
                long id = mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID));
                Uri uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                startActivity(shareIntent);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    public void init() {

        // Set the layout for this activity.  You can find it
        // in assets/res/any/layout/media_picker_activity.xml
        setContentView(R.layout.media_picker_activity);
        View listView = findViewById(R.id.buttonbar);
        listView.setVisibility(View.GONE);
        getListView().setOnCreateContextMenuListener(this); // Set OnCreateContextMenuListener interface

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

        //Drm Start
        String path = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
        Log.i(LOGTAG, "onListItemClick, path of the file is"+path);
        if (path.endsWith(".dcf") || path.endsWith(".dm")) {
            DrmManagerClient drmClient = new DrmManagerClient(VideoBrowserActivity.this);
            int status = drmClient.checkRightsStatus(path, Action.PLAY);
            Log.i(LOGTAG, "onListItemClick:status fron drmClient.checkRightsStatus is " + Integer.toString(status));

            ContentValues values = drmClient.getMetadata(path);

            if (RightsStatus.RIGHTS_VALID != status) {
                String address = values.getAsString("Rights-Issuer");
                Intent drm_intent = new Intent(BUY_LICENSE);
                drm_intent.putExtra("DRM_FILE_PATH", address);
                this.sendBroadcast(drm_intent);
                return;
            }

            int drmType = values.getAsInteger("DRM-TYPE");
            Log.i(LOGTAG, "onListItemClick:DRM-TYPE = " + Integer.toString(drmType));
            if (drmType > DrmDeliveryType.FORWARD_LOCK) { //Not FL
                Toast.makeText(VideoBrowserActivity.this, R.string.action_consumes_rights,
                        Toast.LENGTH_LONG).show();
            }
            if (drmClient != null) drmClient.release();
        }
        //Drm End

        String type = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE));
        intent.setDataAndType(ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id), type);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, R.string.enable_gallery_app, Toast.LENGTH_SHORT).show();
        }
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
            if (TextUtils.isEmpty(mFilterString)){
                mWhereClause = MediaStore.Video.Media.TITLE + " != ''";
            }else{
                mWhereClause = MediaStore.Video.Media.TITLE + " like '%"+mFilterString+"%'";
            }
            mCursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                cols, mWhereClause , null, mSortOrder);
        }
    }

    @Override
    public void onResume() {
        if (mCursor != null && mCursor.getCount() > 0) {
            setTitle(R.string.videos_title);
        }
        super.onResume();
    }

    @Override
    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
        }
        super.onDestroy();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP &&
                event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private Cursor mCursor;
    private String mWhereClause;
    private String mSortOrder;
}

