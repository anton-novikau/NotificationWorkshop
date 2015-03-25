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

package by.dev.product.mediaplayback;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadata;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

import by.dev.product.mediaplayback.md.Album;
import by.dev.product.mediaplayback.md.Artist;
import by.dev.product.mediaplayback.util.MediaIdUtil;

public class MusicProvider {

    public static final String METADATA_TRACK_PATH = "__TRACK_PATH__";

    public static final Uri URI_ALBUMS = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
    public static final Uri URI_ARTISTS = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
    public static final Uri URI_MEDIA = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

    private static final String ALBUMS_SELECTION = "artist_key = ?";
    private static final String MEDIA_SELECTION = "album_key = ? AND (is_music = 1 OR is_podcast = 1)";
    private static final String TRACK_BY_ID_SELECTION = "_id = ?";

    private static final String[] ALBUMS_PROJECTION = {
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM_KEY,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
    };

    private static final String[] ARTISTS_PROJECTION = {
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST_KEY,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS
    };

    private static final String[] MEDIA_PROJECTION = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA
    };

    private final Context mContext;
    private final ContentResolver mBackend;

    MusicProvider(Context context) {
        mContext = context;
        mBackend = context.getContentResolver();
    }

    public Iterable<Artist> getArtists() {
        try (Cursor c = mBackend.query(URI_ARTISTS, ARTISTS_PROJECTION, null, null, null)) {
            List<Artist> artists = new ArrayList<>(c.getCount());
            while (c.moveToNext()) {
                Artist artist = new Artist(c.getString(1));
                artist.setName(c.getString(2));
                artist.setNumberOfAlbums(c.getInt(3));
                artists.add(artist);
            }
            return artists;
        }
    }

    public Iterable<Album> getAlbums(String artistKey) {
        try (Cursor c = mBackend.query(URI_ALBUMS, ALBUMS_PROJECTION, ALBUMS_SELECTION, new String[]{artistKey}, null)) {
            List<Album> albums = new ArrayList<>();
            while (c.moveToNext()) {
                Album album = new Album(c.getString(1));
                album.setTitle(c.getString(2));
                album.setArtistName(c.getString(3));
                albums.add(album);
            }
            return albums;
        }
    }

    public Iterable<MediaMetadata> getTracks(String albumKey) {
        try (Cursor c = mBackend.query(URI_MEDIA, MEDIA_PROJECTION, MEDIA_SELECTION, new String[]{albumKey}, null)) {
            List<MediaMetadata> albums = new ArrayList<>();
            while (c.moveToNext()) {
                MediaMetadata mm = cursorToTrack(albumKey, c);
                albums.add(mm);
            }
            return albums;
        }
    }

    public MediaMetadata findTrackById(String trackId) {
        Pair<String, String> albumToTrack = MediaIdUtil.extractFromTrackId(trackId);
        if (albumToTrack == null) return null;

        MediaMetadata track = null;
        try (Cursor c = mBackend.query(URI_MEDIA, MEDIA_PROJECTION, TRACK_BY_ID_SELECTION, new String[]{albumToTrack.second}, null)) {
            if (c.moveToNext()) {
                track = cursorToTrack(albumToTrack.first, c);
            }
            return track;
        }
    }

    private static MediaMetadata cursorToTrack(String albumKey, Cursor c) {
        return new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, MediaIdUtil.createTrackId(albumKey, c.getString(0)))
                .putString(MediaMetadata.METADATA_KEY_TITLE, c.getString(1))
                .putString(MediaMetadata.METADATA_KEY_ALBUM, c.getString(2))
                .putString(MediaMetadata.METADATA_KEY_ARTIST, c.getString(3))
                .putString(METADATA_TRACK_PATH, c.getString(4))
                .build();
    }
}
