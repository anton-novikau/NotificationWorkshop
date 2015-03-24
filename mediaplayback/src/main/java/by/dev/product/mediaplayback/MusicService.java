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

package by.dev.product.mediaplayback;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.service.media.MediaBrowserService;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends MediaBrowserService implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener, AudioManager.OnAudioFocusChangeListener {

    private static final String LOG_TAG = "MusicService";

    // Delay stopSelf by using a handler.
    private static final int STOP_DELAY = 30000;

    // The volume we set the media player to when we lose audio focus, but are
    // allowed to reduce the volume instead of stopping playback.
    public static final float VOLUME_DUCK = 0.2f;

    // The volume we set the media player when we have audio focus.
    public static final float VOLUME_NORMAL = 1.0f;

    private MediaPlayer mMediaPlayer;
    private MediaSession mSession;
    private List<MediaSession.QueueItem> mPlayingQueue;
    private int mCurrentIndexOnQueue;

    private AudioManager mAudioManager;

    // Current local media player state
    private int mState = PlaybackState.STATE_NONE;

    // Indicates whether the service was started.
    private boolean mServiceStarted;

    enum AudioFocus {
        NoFocusNoDuck, // we don't have audio focus, and can't duck
        NoFocusCanDuck, // we don't have focus, but can play at a low volume ("ducking")
        Focused // we have full audio focus
    }

    // Type of audio focus we have:
    private AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;

    // Indicates if we should start playing immediately after we gain focus.
    private boolean mPlayOnFocusGain;

    private Runnable mStopPlayerAction = new Runnable() {
        @Override
        public void run() {
            if ((mMediaPlayer != null && mMediaPlayer.isPlaying()) ||
                    mPlayOnFocusGain) {
                Log.d(LOG_TAG, "Ignoring delayed stop since the media player is in use.");
                return;
            }
            Log.d(LOG_TAG, "Stopping service with delay handler.");
            stopSelf();
            mServiceStarted = false;
        }
    };

    private final Handler mUiHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        mPlayingQueue = new ArrayList<>();

        // TODO: INIT Music provider
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        // Start a new MediaSession
        mSession = new MediaSession(this, "MusicService");
        setSessionToken(mSession.getSessionToken());
        mSession.setCallback(new MediaSessionCallback());
        mSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

        updatePlaybackState(null);

        // TODO: create notifiction manager
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");

        // Service is being killed, so make sure we release our resources
        handleStopRequest(null);

        mUiHandler.removeCallbacks(mStopPlayerAction);
        // In particular, always release the MediaSession to clean up resources
        // and notify associated MediaController(s).
        mSession.release();

        super.onDestroy();
    }

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        Log.d(LOG_TAG, "OnGetRoot: clientPackageName=" + clientPackageName + "; clientUid=" + clientUid + " ; rootHints=" + rootHints);
        // TODO: implement me
        return null;// new BrowserRoot(MEDIA_ID_ROOT, null);
    }

    @Override
    public void onLoadChildren(String parentId, Result<List<MediaBrowser.MediaItem>> result) {
        // TODO: implement me
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.d(LOG_TAG, "onAudioFocusChange. focusChange=" + focusChange);
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // We have gained focus:
            mAudioFocus = AudioFocus.Focused;

        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // We have lost focus. If we can duck (low playback volume), we can keep playing.
            // Otherwise, we need to pause the playback.
            boolean canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
            mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;

            // If we are playing, we need to reset media player by calling configMediaPlayerState
            // with mAudioFocus properly set.
            if (mState == PlaybackState.STATE_PLAYING && !canDuck) {
                // If we don't have audio focus and can't duck, we save the information that
                // we were playing, so that we can resume playback once we get the focus back.
                mPlayOnFocusGain = true;
            }
        } else {
            Log.e(LOG_TAG, "onAudioFocusChange: Ignoring unsupported focusChange: " + focusChange);
        }

        configMediaPlayerState();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(LOG_TAG, "onCompletion from MediaPlayer");
        // The media player finished playing the current song, so we go ahead
        // and start the next.
        if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
            // In this sample, we restart the playing queue when it gets to the end:
            mCurrentIndexOnQueue++;
            if (mCurrentIndexOnQueue >= mPlayingQueue.size()) {
                mCurrentIndexOnQueue = 0;
            }
            handlePlayRequest();
        } else {
            // If there is nothing to play, we stop and release the resources:
            handleStopRequest(null);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(LOG_TAG, "Media player error: what=" + what + ", extra=" + extra);
        handleStopRequest("MediaPlayer error " + what + " (" + extra + ")");
        return true; // true indicates we handled the error
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(LOG_TAG, "onPrepared from MediaPlayer");
        // The media player is done preparing. That means we can start playing if we
        // have audio focus.
        configMediaPlayerState();
    }

    private void updatePlaybackState(String error) {
        Log.d(LOG_TAG, "updatePlaybackState, setting session playback state to " + mState);
        long position = PlaybackState.PLAYBACK_POSITION_UNKNOWN;
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            position = mMediaPlayer.getCurrentPosition();
        }
        PlaybackState.Builder stateBuilder = new PlaybackState.Builder()
                .setActions(getAvailableActions());

        // If there is an error message, send it to the playback state:
        if (error != null) {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(error);
            mState = PlaybackState.STATE_ERROR;
        }
        stateBuilder.setState(mState, position, 1.0f, SystemClock.elapsedRealtime());

        // Set the activeQueueItemId if the current index is valid.
        if (canPlayCurrent()) {
            MediaSession.QueueItem item = mPlayingQueue.get(mCurrentIndexOnQueue);
            stateBuilder.setActiveQueueItemId(item.getQueueId());
        }

        mSession.setPlaybackState(stateBuilder.build());

        if (mState == PlaybackState.STATE_PLAYING || mState == PlaybackState.STATE_PAUSED) {
            // TODO: show playback notification
            //mMediaNotificationManager.startNotification();
        }
    }

    private long getAvailableActions() {
        long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PLAY_FROM_MEDIA_ID |
                PlaybackState.ACTION_PLAY_FROM_SEARCH;
        if (mPlayingQueue == null || mPlayingQueue.isEmpty()) {
            return actions;
        }
        if (mState == PlaybackState.STATE_PLAYING) {
            actions |= PlaybackState.ACTION_PAUSE;
        }
        if (mCurrentIndexOnQueue > 0) {
            actions |= PlaybackState.ACTION_SKIP_TO_PREVIOUS;
        }
        if (mCurrentIndexOnQueue < mPlayingQueue.size() - 1) {
            actions |= PlaybackState.ACTION_SKIP_TO_NEXT;
        }
        return actions;
    }

    private MediaMetadata getCurrentPlayingMusic() {
        if (canPlayCurrent()) {
            MediaSession.QueueItem item = mPlayingQueue.get(mCurrentIndexOnQueue);
            if (item != null) {
                Log.d(LOG_TAG, "getCurrentPlayingMusic for musicId=" + item.getDescription().getMediaId());
                // TODO: get media metadata from google music
                return null; // mMusicProvider.getMusic(item.getDescription().getMediaId());
            }
        }
        return null;
    }

    private void releaseResources(boolean releaseMediaPlayer) {
        Log.d(LOG_TAG, "relaxResources. releaseMediaPlayer=" + releaseMediaPlayer);
        // stop being a foreground service
        stopForeground(true);

        // reset the delayed stop handler.
        mUiHandler.removeCallbacks(mStopPlayerAction);
        mUiHandler.postDelayed(mStopPlayerAction, STOP_DELAY);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private void createMediaPlayerIfNeeded() {
        Log.d(LOG_TAG, "createMediaPlayerIfNeeded. needed? " + (mMediaPlayer == null));
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();

            // Make sure the media player will acquire a wake-lock while
            // playing. If we don't do that, the CPU might go to sleep while the
            // song is playing, causing playback to stop.
            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            // we want the media player to notify us when it's ready preparing,
            // and when it's done playing:
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
        } else {
            mMediaPlayer.reset();
        }
    }

    private void configMediaPlayerState() {
        Log.d(LOG_TAG, "configAndStartMediaPlayer. mAudioFocus=" + mAudioFocus);
        if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
            // If we don't have audio focus and can't duck, we have to pause,
            if (mState == PlaybackState.STATE_PLAYING) {
                handlePauseRequest();
            }
        } else {  // we have audio focus:
            if (mAudioFocus == AudioFocus.NoFocusCanDuck) {
                mMediaPlayer.setVolume(VOLUME_DUCK, VOLUME_DUCK); // we'll be relatively quiet
            } else {
                mMediaPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL); // we can be loud again
            }
            // If we were playing when we lost focus, we need to resume playing.
            if (mPlayOnFocusGain) {
                if (!mMediaPlayer.isPlaying()) {
                    Log.d(LOG_TAG, "configAndStartMediaPlayer startMediaPlayer.");
                    mMediaPlayer.start();
                }
                mPlayOnFocusGain = false;
                mState = PlaybackState.STATE_PLAYING;
            }
        }
        updatePlaybackState(null);
    }

    void playCurrentSong() {
        MediaMetadata track = getCurrentPlayingMusic();
        if (track == null) {
            Log.e(LOG_TAG, "playSong:  ignoring request to play next song, because cannot" +
                    " find it." +
                    " currentIndex=" + mCurrentIndexOnQueue +
                    " playQueue.size=" + (mPlayingQueue==null?"null": mPlayingQueue.size()));
            return;
        }
        // TODO: obtain track path from media metadata
        String source = "";// track.getString(MusicProvider.CUSTOM_METADATA_TRACK_SOURCE);
        Log.d(LOG_TAG, "playSong:  current (" + mCurrentIndexOnQueue + ") in playingQueue. " +
                " musicId=" + track.getString(MediaMetadata.METADATA_KEY_MEDIA_ID) +
                " source=" + source);

        mState = PlaybackState.STATE_STOPPED;
        releaseResources(false); // release everything except MediaPlayer

        try {
            createMediaPlayerIfNeeded();

            mState = PlaybackState.STATE_BUFFERING;

            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setDataSource(source);

            // Starts preparing the media player in the background. When
            // it's done, it will call our OnPreparedListener (that is,
            // the onPrepared() method on this class, since we set the
            // listener to 'this'). Until the media player is prepared,
            // we *cannot* call start() on it!
            mMediaPlayer.prepareAsync();

            updatePlaybackState(null);
            updateMetadata();

        } catch (IOException ex) {
            Log.e(LOG_TAG, "IOException playing song", ex);
            updatePlaybackState(ex.getMessage());
        }
    }

    private void updateMetadata() {
        if (!canPlayCurrent()) {
            Log.e(LOG_TAG, "Can't retrieve current metadata.");
            mState = PlaybackState.STATE_ERROR;
            updatePlaybackState("No Metadata");
            return;
        }
        MediaSession.QueueItem queueItem = mPlayingQueue.get(mCurrentIndexOnQueue);
        String mediaId = queueItem.getDescription().getMediaId();
        // TODO: obtain track metadata from google music
        MediaMetadata track = null;// mMusicProvider.getMusic(mediaId);
        String trackId = track.getString(MediaMetadata.METADATA_KEY_MEDIA_ID);
        if (!mediaId.equals(trackId)) {
            throw new IllegalStateException("track ID (" + trackId + ") " +
                    "should match mediaId (" + mediaId + ")");
        }
        Log.d(LOG_TAG, "Updating metadata for MusicID= " + mediaId);
        mSession.setMetadata(track);
    }

    void tryToGetAudioFocus() {
        Log.d(LOG_TAG, "tryToGetAudioFocus");
        if (mAudioFocus != AudioFocus.Focused) {
            int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AudioFocus.Focused;
            }
        }
    }

    void giveUpAudioFocus() {
        Log.d(LOG_TAG, "giveUpAudioFocus");
        if (mAudioFocus == AudioFocus.Focused) {
            if (mAudioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AudioFocus.NoFocusNoDuck;
            }
        }
    }

    private void handlePlayRequest() {
        mUiHandler.removeCallbacks(mStopPlayerAction);
        if (!mServiceStarted) {
            Log.v(LOG_TAG, "Starting service");
            // The MusicService needs to keep running even after the calling MediaBrowser
            // is disconnected. Call startService(Intent) and then stopSelf(..) when we no longer
            // need to play media.
            startService(new Intent(getApplicationContext(), MusicService.class));
            mServiceStarted = true;
        }

        mPlayOnFocusGain = true;
        tryToGetAudioFocus();

        if (!mSession.isActive()) {
            mSession.setActive(true);
        }

        // actually play the song
        if (mState == PlaybackState.STATE_PAUSED) {
            // If we're paused, just continue playback and restore the
            // 'foreground service' state.
            configMediaPlayerState();
        } else {
            // If we're stopped or playing a song,
            // just go ahead to the new song and (re)start playing
            playCurrentSong();
        }
    }

    private void handlePauseRequest() {
        if (mState == PlaybackState.STATE_PLAYING) {
            // Pause media player and cancel the 'foreground service' state.
            mState = PlaybackState.STATE_PAUSED;
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
            }
            // while paused, retain the MediaPlayer but give up audio focus
            releaseResources(false);
            giveUpAudioFocus();
        }
        updatePlaybackState(null);
    }

    private void handleStopRequest(String withError) {
        mState = PlaybackState.STATE_STOPPED;

        // let go of all resources...
        releaseResources(true);
        giveUpAudioFocus();
        updatePlaybackState(withError);

        // TODO: hide media playback notification
        // mMediaNotificationManager.stopNotification();

        // service is no longer necessary. Will be started again if needed.
        stopSelf();
        mServiceStarted = false;
    }

    private boolean canPlayCurrent() {
        return canPlay(mCurrentIndexOnQueue);
    }

    private boolean canPlay(int index) {
        return mPlayingQueue != null && index >= 0 && index < mPlayingQueue.size();
    }

    private class MediaSessionCallback extends MediaSession.Callback {
        @Override
        public void onPlay() {
            if (mPlayingQueue == null || mPlayingQueue.isEmpty()) {
                // TODO: random queue from google music
                mPlayingQueue = null;// QueueHelper.getRandomQueue(mMusicProvider);
                mSession.setQueue(mPlayingQueue);
                // TODO: get random queue title
                mSession.setQueueTitle(null/*getString(R.string.random_queue_title)*/);
                // start playing from the beginning of the queue
                mCurrentIndexOnQueue = 0;
            }

            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                handlePlayRequest();
            }
        }

        @Override
        public void onSkipToQueueItem(long queueId) {
            if (mState == PlaybackState.STATE_PAUSED) {
                mState = PlaybackState.STATE_STOPPED;
            }

            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {

                // set the current index on queue from the music Id:
                // TODO: obtain current track index for queue
                mCurrentIndexOnQueue = 0; // QueueHelper.getMusicIndexOnQueue(mPlayingQueue, queueId);

                // play the music
                handlePlayRequest();
            }
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            if (mState == PlaybackState.STATE_PAUSED) {
                mState = PlaybackState.STATE_STOPPED;
            }

            // The mediaId used here is not the unique musicId. This one comes from the
            // MediaBrowser, and is actually a "hierarchy-aware mediaID": a concatenation of
            // the hierarchy in MediaBrowser and the actual unique musicID. This is necessary
            // so we can build the correct playing queue, based on where the track was
            // selected from.
            // TODO: obtain playing queue
            mPlayingQueue = null; // QueueHelper.getPlayingQueue(mediaId, mMusicProvider);
            mSession.setQueue(mPlayingQueue);
            // TODO: obtain queue title for id
            String queueTitle = "";// getString(R.string.browse_musics_by_genre_subtitle, MediaIDHelper.extractBrowseCategoryValueFromMediaID(mediaId));
            mSession.setQueueTitle(queueTitle);

            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                // convert media id to unique id
                String uniqueMusicID = "";// MediaIDHelper.extractMusicIDFromMediaID(mediaId);

                // set the current index on queue from the music Id:
                // TODO: get index by unique id
                mCurrentIndexOnQueue = 0;// QueueHelper.getMusicIndexOnQueue(mPlayingQueue, uniqueMusicID);

                // play the music
                handlePlayRequest();
            }
        }

        @Override
        public void onPause() {
            handlePauseRequest();
        }

        @Override
        public void onStop() {
            handleStopRequest(null);
        }

        @Override
        public void onSkipToNext() {
            mCurrentIndexOnQueue++;
            if (mPlayingQueue != null && mCurrentIndexOnQueue >= mPlayingQueue.size()) {
                mCurrentIndexOnQueue = 0;
            }
            if (canPlayCurrent()) {
                mState = PlaybackState.STATE_STOPPED;
                handlePlayRequest();
            } else {
                Log.e(LOG_TAG, "skipToNext: cannot skip to next. next Index=" +
                        mCurrentIndexOnQueue + " queue length=" +
                        (mPlayingQueue == null ? "null" : mPlayingQueue.size()));
                handleStopRequest("Cannot skip");
            }
        }

        @Override
        public void onSkipToPrevious() {
            mCurrentIndexOnQueue--;
            if (mPlayingQueue != null && mCurrentIndexOnQueue < 0) {
                // This sample's behavior: skipping to previous when in first song restarts the
                // first song.
                mCurrentIndexOnQueue = 0;
            }
            if (canPlayCurrent()) {
                mState = PlaybackState.STATE_STOPPED;
                handlePlayRequest();
            } else {
                Log.e(LOG_TAG, "skipToPrevious: cannot skip to previous. previous Index=" +
                        mCurrentIndexOnQueue + " queue length=" +
                        (mPlayingQueue == null ? "null" : mPlayingQueue.size()));
                handleStopRequest("Cannot skip");
            }
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            Log.d(LOG_TAG, "Custom action '" + action + "' triggered");
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            Log.d(LOG_TAG, "Play from search requested");
        }
    }
}
