/*
 *  Copyright 2015 GDG Minsk
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

package by.dev.product.mediaplayback.util;

import android.util.Log;
import android.util.Pair;

public final class MediaIdUtil {
    private static final String LOG_TAG = "MediaIdUtil";
    // Media IDs used on browseable items of MediaBrowser
    public static final String MEDIA_ID_ROOT = "__ROOT__";
    public static final String MEDIA_ID_ARTISTS = "__ARTISTS__";
    // track database key has a wired format.
    /// I had to make delimiter even more wired %).
    private static final String TRACK_ID_DELIMITER = "%";

    public static String createArtistId(String artistKey) {
        return MEDIA_ID_ARTISTS + artistKey;
    }

    public static boolean isArtistMediaId(String mediaId) {
        return mediaId != null && mediaId.startsWith(MEDIA_ID_ARTISTS);
    }

    public static String getArtistKeyFromId(String mediaId) {
        if (isArtistMediaId(mediaId)) {
            return mediaId.substring(MEDIA_ID_ARTISTS.length());
        }
        return null;
    }

    public static String createTrackId(String albumKey, String trackKey) {
        return albumKey + TRACK_ID_DELIMITER + trackKey;
    }

    public static Pair<String, String> extractFromTrackId(String trackId) {
        if (trackId == null) return null;

        int delimiterPos = trackId.lastIndexOf(TRACK_ID_DELIMITER);
        if (delimiterPos != -1) {
            String albumKey = trackId.substring(0, delimiterPos);
            String trackKey = trackId.substring(delimiterPos + 1);
            return new Pair<>(albumKey, trackKey);
        } else {
            Log.e(LOG_TAG, "Invalid track id: " + trackId);
            return null;
        }
    }
}
