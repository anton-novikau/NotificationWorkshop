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

import android.app.Activity;
import android.app.ListFragment;
import android.content.ComponentName;
import android.content.Context;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class BrowseFragment extends ListFragment {

    private static final String LOG_TAG = "BrowseFragment";

    public static final String ARG_MEDIA_ID = "media_id";

    private String mMediaId;
    private MediaBrowser mMediaBrowser;
    private BrowseAdapter mBrowserAdapter;

    private OnMediaSelectedListener mOnMediaSelectedListener;

    private final MediaBrowser.ConnectionCallback mConnectionCallback = new MediaBrowser.ConnectionCallback() {
        @Override
        public void onConnected() {
            Log.d(LOG_TAG, "Media browser connected.");

            if (mMediaId == null) {
                mMediaId = mMediaBrowser.getRoot();
            }
            mMediaBrowser.subscribe(mMediaId, mSubscriptionCallback);
            if (mMediaBrowser.getSessionToken() == null) {
                throw new IllegalArgumentException("No Session token");
            }
            MediaController mediaController = new MediaController(getActivity(),
                    mMediaBrowser.getSessionToken());
            getActivity().setMediaController(mediaController);
        }

        @Override
        public void onConnectionSuspended() {
            Log.d(LOG_TAG, "Media browser suspended connection");
            getActivity().setMediaController(null);
        }
    };

    private final MediaBrowser.SubscriptionCallback mSubscriptionCallback = new MediaBrowser.SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(String parentId, List<MediaBrowser.MediaItem> children) {
            mBrowserAdapter.clear();
            mBrowserAdapter.notifyDataSetInvalidated();
            for (MediaBrowser.MediaItem item : children) {
                mBrowserAdapter.add(item);
            }
            mBrowserAdapter.notifyDataSetChanged();
            setListShown(true);
        }

        @Override
        public void onError(String id) {
            Log.e(LOG_TAG, "Error loading media");
        }
    };

    public static BrowseFragment newInstance() {
        return newInstance(null);
    }

    public static BrowseFragment newInstance(String mediaId) {
        BrowseFragment fragment = new BrowseFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MEDIA_ID, mediaId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) mMediaId = args.getString(ARG_MEDIA_ID);

        mBrowserAdapter = new BrowseAdapter(getActivity());
        mMediaBrowser = new MediaBrowser(getActivity(), new ComponentName(getActivity(), MusicService.class), mConnectionCallback, null);
        setListAdapter(mBrowserAdapter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        MediaBrowser.MediaItem item = mBrowserAdapter.getItem(position);
        if (mOnMediaSelectedListener != null) mOnMediaSelectedListener.onMediaItemSelected(item);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnMediaSelectedListener) {
            mOnMediaSelectedListener = (OnMediaSelectedListener) activity;
        } else {
            Log.e(LOG_TAG, "Host activity is not implementing OnMediaSelectedListener.");
        }
    }

    @Override
    public void onDetach() {
        mOnMediaSelectedListener = null;
        super.onDetach();
    }

    @Override
    public void onStart() {
        super.onStart();
        mMediaBrowser.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        mMediaBrowser.disconnect();
    }

    public interface OnMediaSelectedListener {
        void onMediaItemSelected(MediaBrowser.MediaItem item);
    }

    private static class BrowseAdapter extends ArrayAdapter<MediaBrowser.MediaItem> {
        private final LayoutInflater mInflater;

        public BrowseAdapter(Context context) {
            super(context, R.layout.list_item_browse, new ArrayList<MediaBrowser.MediaItem>());
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_item_browse, parent, false);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            MediaBrowser.MediaItem item = getItem(position);
            MediaDescription description = item.getDescription();
            holder.title.setText(description.getTitle());
            holder.description.setText(description.getDescription());

            return convertView;
        }

        static class ViewHolder {
            TextView title;
            TextView description;
            ViewHolder(View root) {
                title = (TextView) root.findViewById(R.id.title);
                description = (TextView) root.findViewById(R.id.description);
            }
        }
    }
}
