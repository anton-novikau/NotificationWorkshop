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

package by.dev.product.mediaplayback.md;

public class Artist {

    private final String mId;
    private String mName;
    private int mNumberOfAlbums;

    public Artist(String id) {
        mId = id;
    }

    public String getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public int getNumberOfAlbums() {
        return mNumberOfAlbums;
    }

    public void setNumberOfAlbums(int numberOfAlbums) {
        this.mNumberOfAlbums = numberOfAlbums;
    }

    @Override
    public String toString() {
        return "Artist{" +
                "id='" + mId + '\'' +
                ", name='" + mName + '\'' +
                ", numberOfAlbums='" + mNumberOfAlbums + '\'' +
                '}';
    }
}
