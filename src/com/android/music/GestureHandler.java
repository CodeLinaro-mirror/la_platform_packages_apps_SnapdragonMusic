/*Copyright (c) 2014, The Linux Foundation. All rights reserved.

 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.
    * Neither the name of The Linux Foundation nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.android.music;

import com.android.music.MusicUtils.ServiceToken;

import android.app.Activity;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class GestureHandler extends Activity
        implements MusicUtils.Defs {
    private static final String GESTURE_CONTROL_PLAY =
           "com.android.music.MusticGesturePlayActivity";
    private static final String GESTURE_CONTROL_PREV =
           "com.android.music.MusticGesturePrevActivity";
    private static final String GESTURE_CONTROL_NEXT =
           "com.android.music.MusticGestureNextActivity";
    private ServiceToken mToken;
    private static final String LOGTAG = "GestureHandler";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mToken = MusicUtils.bindToService(this, autoshuffle);
        Intent intent = getIntent();
        String componentName = intent.getComponent().getClassName();
        Log.d(LOGTAG,"GestureHandler get componentName : "+ componentName);
        if (GESTURE_CONTROL_PLAY.equals(componentName)) {
            Intent i = new Intent("com.android.music.musicservicecommand");
            i.putExtra("command", "togglepause");
            this.sendBroadcast(i);
        } else if (GESTURE_CONTROL_PREV.equals(componentName)) {
            Intent i = new Intent("com.android.music.musicservicecommand");
            i.putExtra("command", "previous");
            this.sendBroadcast(i);
        } else if (GESTURE_CONTROL_NEXT.equals(componentName)) {
            Intent i = new Intent("com.android.music.musicservicecommand");
            i.putExtra("command", "next");
            this.sendBroadcast(i);
        }
        finish();
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        MusicUtils.unbindFromService(mToken);
    }

    private ServiceConnection autoshuffle = new ServiceConnection() {
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            IMediaPlaybackService serv = IMediaPlaybackService.Stub.asInterface(obj);
            if (serv != null) {
                try {
                    serv.setShuffleMode(MediaPlaybackService.SHUFFLE_AUTO);
                } catch (RemoteException ex) {
                }
            }
        }
        public void onServiceDisconnected(ComponentName classname) {
        }
    };
}
