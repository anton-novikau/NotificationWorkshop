/*
 * Copyright 2014 The Android Open Source Project
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

import android.app.Fragment;
import android.app.ListFragment;
import android.content.ComponentName;
import android.content.Context;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class QueueFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener {
    private static final String LOG_TAG = "QueueFragment";

    private ImageButton mNextTrack;
    private ImageButton mPreviousTrack;
    private ImageButton mPlayPause;

    private MediaBrowser mMediaBrowser;
    private MediaController mMediaController;
    private MediaController.TransportControls mTransportControls;
    private PlaybackState mPlaybackState;
    private QueueAdapter mQueueAdapter;

    private final MediaController.Callback mSessionCallback = new MediaController.Callback() {
        @Override
        public void onSessionDestroyed() {
            Log.d(LOG_TAG, "Session destroyed. Need to fetch a new Media Session");
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            if (state == null) return; // early exit

            Log.d(LOG_TAG, "Received playback state change to state " + state.getState());
            mPlaybackState = state;
            QueueFragment.this.onPlaybackStateChanged(state);
        }

        @Override
        public void onQueueChanged(List<MediaSession.QueueItem> queue) {
            Log.d(LOG_TAG, "onQueueChanged " + queue);
            if (queue != null) {
                mQueueAdapter.clear();
                mQueueAdapter.notifyDataSetInvalidated();
                mQueueAdapter.addAll(queue);
                mQueueAdapter.notifyDataSetChanged();
            }
        }
    };

    private final MediaBrowser.ConnectionCallback mConnectionCallback = new MediaBrowser.ConnectionCallback() {
        @Override
        public void onConnected() {
            Log.d(LOG_TAG, "onConnected()");

            if (mMediaBrowser.getSessionToken() == null) {
                throw new IllegalArgumentException("No Session token");
            }

            mMediaController = new MediaController(getActivity(),
                    mMediaBrowser.getSessionToken());
            mTransportControls = mMediaController.getTransportControls();
            mMediaController.registerCallback(mSessionCallback);

            getActivity().setMediaController(mMediaController);
            mPlaybackState = mMediaController.getPlaybackState();

            List<MediaSession.QueueItem> queue = mMediaController.getQueue();
            if (queue != null) {
                mQueueAdapter.clear();
                mQueueAdapter.notifyDataSetInvalidated();
                mQueueAdapter.addAll(queue);
                mQueueAdapter.notifyDataSetChanged();
            }
            onPlaybackStateChanged(mPlaybackState);
        }

        @Override
        public void onConnectionFailed() {
            Log.d(LOG_TAG, "onConnectionFailed()");
        }

        @Override
        public void onConnectionSuspended() {
            Log.d(LOG_TAG, "onConnectionSuspended");
            mMediaController.unregisterCallback(mSessionCallback);
            mTransportControls = null;
            mMediaController = null;
            getActivity().setMediaController(null);
        }
    };

    public static QueueFragment newInstance() {
        return new QueueFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_queue, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View fragmentView = getView();

        if (fragmentView == null) return;

        ListView list = (ListView) fragmentView.findViewById(android.R.id.list);
        mPreviousTrack = (ImageButton) fragmentView.findViewById(R.id.skip_previous);
        mPlayPause = (ImageButton) fragmentView.findViewById(R.id.play_pause);
        mNextTrack = (ImageButton) fragmentView.findViewById(R.id.skip_next);

        mPreviousTrack.setEnabled(false);
        mPlayPause.setEnabled(true);
        mNextTrack.setEnabled(false);
        mPreviousTrack.setOnClickListener(this);
        mPlayPause.setOnClickListener(this);
        mNextTrack.setOnClickListener(this);
        list.setOnItemClickListener(this);

        mQueueAdapter = new QueueAdapter(getActivity());

        list.setAdapter(mQueueAdapter);

        mMediaBrowser = new MediaBrowser(getActivity(),
                new ComponentName(getActivity(), MusicService.class),
                mConnectionCallback, null);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMediaBrowser != null) mMediaBrowser.connect();
    }

    @Override
    public void onPause() {
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mSessionCallback);
        }
        if (mMediaBrowser != null) {
            mMediaBrowser.disconnect();
        }
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        final int state = mPlaybackState == null ?
                PlaybackState.STATE_NONE : mPlaybackState.getState();
        switch (v.getId()) {
            case R.id.play_pause:
                if (state == PlaybackState.STATE_PAUSED ||
                        state == PlaybackState.STATE_STOPPED ||
                        state == PlaybackState.STATE_NONE) {
                    playMedia();
                } else if (state == PlaybackState.STATE_PLAYING) {
                    pauseMedia();
                }
                break;
            case R.id.skip_next:
                skipToNext();
                break;
            case R.id.skip_previous:
                skipToPrevious();
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MediaSession.QueueItem item = mQueueAdapter.getItem(position);
        mTransportControls.skipToQueueItem(item.getQueueId());
    }

    private void onPlaybackStateChanged(PlaybackState state) {
        if (state == null) return; // early exit

        mQueueAdapter.setActiveQueueItemId(state.getActiveQueueItemId());
        mQueueAdapter.notifyDataSetChanged();

        boolean enablePlay = state.getState() == PlaybackState.STATE_PAUSED || state.getState() == PlaybackState.STATE_STOPPED;

        mPlayPause.setImageDrawable(
                getActivity().getDrawable(enablePlay ? R.drawable.ic_play_arrow_white : R.drawable.ic_pause_white));

        mPreviousTrack.setEnabled((state.getActions() & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0);
        mNextTrack.setEnabled((state.getActions() & PlaybackState.ACTION_SKIP_TO_NEXT) != 0);
    }

    private void playMedia() {
        if (mTransportControls != null) {
            mTransportControls.play();
        }
    }

    private void pauseMedia() {
        if (mTransportControls != null) {
            mTransportControls.pause();
        }
    }

    private void skipToPrevious() {
        if (mTransportControls != null) {
            mTransportControls.skipToPrevious();
        }
    }

    private void skipToNext() {
        if (mTransportControls != null) {
            mTransportControls.skipToNext();
        }
    }

    private static class QueueAdapter extends ArrayAdapter<MediaSession.QueueItem> {

        private final LayoutInflater mInflater;
        private long mActiveQueueItem;

        QueueAdapter(Context context) {
            super(context, R.layout.list_item_queue, new ArrayList<MediaSession.QueueItem>());
            mInflater = LayoutInflater.from(context);
        }

        void setActiveQueueItemId(long id) {
            mActiveQueueItem = id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_item_queue, parent, false);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            MediaSession.QueueItem item = getItem(position);
            MediaDescription description = item.getDescription();
            boolean isActive = mActiveQueueItem == item.getQueueId();
            convertView.setBackgroundResource( isActive ? R.drawable.selected_queue_item : android.R.drawable.list_selector_background);
            holder.title.setText(description.getTitle());
            holder.artist.setText(description.getDescription());
            holder.album.setText(description.getSubtitle());
            return convertView;
        }

        static class ViewHolder {
            TextView title;
            TextView album;
            TextView artist;

            ViewHolder(View root) {
                title = (TextView) root.findViewById(R.id.title);
                album = (TextView) root.findViewById(R.id.album);
                artist = (TextView) root.findViewById(R.id.artist);
            }
        }
    }
}
