/*
 * Copyright 2015 GDG Minsk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package by.dev.product.notificationreceiver;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;

import java.io.IOException;

public class SoundManager {
    private static final String LOG_TAG = "SoundManager";
    private final Context mContext;
    private MediaPlayer mMediaPlayer;
    private Vibrator mVibrator;

    SoundManager(Context context) {
        mContext = context;
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void playRingtone(Uri ringtone, boolean loop, boolean useAttrs) {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) mMediaPlayer.stop();
        try {
            AudioAttributes attrs = null;
            if (useAttrs) {
                AudioAttributes.Builder aa = new AudioAttributes.Builder();
                aa.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION);
                aa.setUsage(loop ? AudioAttributes.USAGE_NOTIFICATION_RINGTONE : AudioAttributes.USAGE_NOTIFICATION);
                attrs = aa.build();
            }
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(mContext, ringtone);
            mMediaPlayer.setLooping(loop);
            mMediaPlayer.setAudioStreamType(loop ? AudioManager.STREAM_RING : AudioManager.STREAM_NOTIFICATION);
            if (useAttrs) mMediaPlayer.setAudioAttributes(attrs);
            mMediaPlayer.prepare();
            mMediaPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Unable to play the ringtone.", e);
        }
    }

    public void vibrate(long[] pattern, boolean loop, boolean useAttrs) {
        AudioAttributes attrs = null;
        if (useAttrs) {
            AudioAttributes.Builder aa = new AudioAttributes.Builder();
            aa.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION);
            aa.setUsage(loop ? AudioAttributes.USAGE_NOTIFICATION_RINGTONE : AudioAttributes.USAGE_NOTIFICATION);
            attrs = aa.build();
        }
        mVibrator.vibrate(pattern, loop ? 0 : -1, attrs);
    }

    public void stopRingtone() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) mMediaPlayer.stop();
        mVibrator.cancel();
    }
}
