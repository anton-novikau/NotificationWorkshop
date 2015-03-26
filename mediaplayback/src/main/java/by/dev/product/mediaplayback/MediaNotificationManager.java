/*
 *  Copyright 2014 The Android Open Source Project
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

package by.dev.product.mediaplayback;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.util.Log;

public class MediaNotificationManager {

    private static final String LOG_TAG = "NotificationManager";

    private static final int PLAYBACK_CONTROL_ID = 100;

    private static final String ACTION_PLAY = "by.dev.product.action.PLAY";
    private static final String ACTION_PAUSE = "by.dev.product.action.PAUSE";
    private static final String ACTION_NEXT = "by.dev.product.action.NEXT";
    private static final String ACTION_PREV = "by.dev.product.action.PREV";

    private final MusicService mService;

    private MediaSession.Token mSessionToken;
    private MediaController mController;
    private MediaController.TransportControls mTransportControls;

    private final NotificationManager mNotificationManager;

    private Notification.Builder mNotificationBuilder;
    private Notification.Action mPlayPauseAction;

    private PlaybackState mPlaybackState;
    private MediaMetadata mMetadata;

    private final PendingIntent mPauseIntent;
    private final PendingIntent mPlayIntent;
    private final PendingIntent mPreviousIntent;
    private final PendingIntent mNextIntent;

    private boolean mStarted;

    private final BroadcastReceiver mActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(LOG_TAG, "Received intent with action " + action);

            if (ACTION_PAUSE.equals(action)) {
                mTransportControls.pause();
            } else if (ACTION_PLAY.equals(action)) {
                mTransportControls.play();
            } else if (ACTION_NEXT.equals(action)) {
                mTransportControls.skipToNext();
            } else if (ACTION_PREV.equals(action)) {
                mTransportControls.skipToPrevious();
            }
        }
    };

    private final MediaController.Callback mSessionCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            mPlaybackState = state;
            Log.d(LOG_TAG, "Received new playback state" + state);
            updateNotificationPlaybackState();
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            mMetadata = metadata;
            Log.d(LOG_TAG, "Received new metadata " + metadata);
            updateNotificationMetadata();
        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
            Log.d(LOG_TAG, "Session was destroyed, resetting to the new session token");
            updateSessionToken();
        }
    };

    MediaNotificationManager(MusicService service) {
        mService = service;
        updateSessionToken();
        mNotificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);

        String pkg = mService.getPackageName();

        mPauseIntent = PendingIntent.getBroadcast(mService, 0, new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mPlayIntent = PendingIntent.getBroadcast(mService, 0, new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mPreviousIntent = PendingIntent.getBroadcast(mService, 0, new Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mNextIntent = PendingIntent.getBroadcast(mService, 0, new Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public void showPlaybackManager() {
        if (!mStarted) {
            Log.d(LOG_TAG, "showPlaybackManager");
            mController.registerCallback(mSessionCallback);
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_NEXT);
            filter.addAction(ACTION_PAUSE);
            filter.addAction(ACTION_PLAY);
            filter.addAction(ACTION_PREV);
            mService.registerReceiver(mActionReceiver, filter);

            mMetadata = mController.getMetadata();
            mPlaybackState = mController.getPlaybackState();
            mStarted = true;
            updateNotificationMetadata();
        }
    }

    public void hidePlaybackManager() {
        Log.d(LOG_TAG, "hidePlaybackManager");
        mStarted = false;
        mController.unregisterCallback(mSessionCallback);
        try {
            mNotificationManager.cancel(PLAYBACK_CONTROL_ID);
            mService.unregisterReceiver(mActionReceiver);
        } catch (IllegalArgumentException ex) {
            Log.w(LOG_TAG, "Action receiver is not registered");
        }
        mService.stopForeground(true);
    }

    private void updateSessionToken() {
        MediaSession.Token freshToken = mService.getSessionToken();
        if (mSessionToken == null || !mSessionToken.equals(freshToken)) {
            if (mController != null) {
                mController.unregisterCallback(mSessionCallback);
            }
            mSessionToken = freshToken;
            mController = new MediaController(mService, mSessionToken);
            mTransportControls = mController.getTransportControls();
            if (mStarted) {
                mController.registerCallback(mSessionCallback);
            }
        }
    }

    private void updateNotificationMetadata() {
        if (mMetadata == null || mPlaybackState == null) {
            return;
        }

        updatePlayPauseAction();

        mNotificationBuilder = new Notification.Builder(mService);
        int playPauseActionIndex = 0;

        // If skip to previous action is enabled
        if ((mPlaybackState.getActions() & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0) {
            mNotificationBuilder
                    .addAction(R.drawable.ic_skip_previous_white,
                            mService.getString(R.string.label_previous), mPreviousIntent);
            playPauseActionIndex = 1;
        }

        mNotificationBuilder.addAction(mPlayPauseAction);

        // If skip to next action is enabled
        if ((mPlaybackState.getActions() & PlaybackState.ACTION_SKIP_TO_NEXT) != 0) {
            mNotificationBuilder.addAction(R.drawable.ic_skip_next_white,
                    mService.getString(R.string.label_next), mNextIntent);
        }

        MediaDescription description = mMetadata.getDescription();

        mNotificationBuilder
                .setStyle(new Notification.MediaStyle()
                        .setShowActionsInCompactView(playPauseActionIndex)  // only show play/pause in compact view
                        .setMediaSession(mSessionToken))
                .setSmallIcon(R.drawable.ic_notification)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setUsesChronometer(true)
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle());

        updateNotificationPlaybackState();

        mService.startForeground(PLAYBACK_CONTROL_ID, mNotificationBuilder.build());
    }

    private void updatePlayPauseAction() {
        Log.d(LOG_TAG, "updatePlayPauseAction");
        String label;
        int icon;
        PendingIntent intent;
        if (mPlaybackState.getState() == PlaybackState.STATE_PLAYING) {
            label = mService.getString(R.string.label_pause);
            icon = R.drawable.ic_pause_white;
            intent = mPauseIntent;
        } else {
            label = mService.getString(R.string.label_play);
            icon = R.drawable.ic_play_arrow_white;
            intent = mPlayIntent;
        }
        if (mPlayPauseAction == null) {
            mPlayPauseAction = new Notification.Action(icon, label, intent);
        } else {
            mPlayPauseAction.icon = icon;
            mPlayPauseAction.title = label;
            mPlayPauseAction.actionIntent = intent;
        }
    }

    private void updateNotificationPlaybackState() {
        if (mPlaybackState == null || !mStarted) {
            Log.d(LOG_TAG, "updateNotificationPlaybackState. cancelling notification!");
            mService.stopForeground(true);
            return;
        }
        if (mNotificationBuilder == null) {
            Log.d(LOG_TAG, "updateNotificationPlaybackState. there is no notificationBuilder. Ignoring request to update state!");
            return;
        }
        if (mPlaybackState.getPosition() >= 0) {
            Log.d(LOG_TAG, "updateNotificationPlaybackState. updating playback position to " +
                    ((System.currentTimeMillis() - mPlaybackState.getPosition()) / 1000) + " seconds");
            mNotificationBuilder
                    .setWhen(System.currentTimeMillis() - mPlaybackState.getPosition())
                    .setShowWhen(true)
                    .setUsesChronometer(true);
            mNotificationBuilder.setShowWhen(true);
        } else {
            Log.d(LOG_TAG, "updateNotificationPlaybackState. hiding playback position");
            mNotificationBuilder
                    .setWhen(0)
                    .setShowWhen(false)
                    .setUsesChronometer(false);
        }

        updatePlayPauseAction();

        // Make sure that the notification can be dismissed by the user when we are not playing:
        mNotificationBuilder.setOngoing(mPlaybackState.getState() == PlaybackState.STATE_PLAYING);

        mNotificationManager.notify(PLAYBACK_CONTROL_ID, mNotificationBuilder.build());
    }
}
