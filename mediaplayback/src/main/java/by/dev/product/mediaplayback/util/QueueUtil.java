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

package by.dev.product.mediaplayback.util;

import android.media.MediaMetadata;
import android.media.session.MediaSession;

import java.util.ArrayList;
import java.util.List;

public final class QueueUtil {


    public static boolean canPlayItem(List<MediaSession.QueueItem> queue, int index) {
        return queue != null && index >= 0 && index < queue.size();
    }

    public static int getMusicIndexOnQueue(Iterable<MediaSession.QueueItem> queue, String mediaId) {
        int index = 0;
        for (MediaSession.QueueItem item: queue) {
            if (mediaId.equals(item.getDescription().getMediaId())) {
                return index;
            }
            index++;
        }
        return -1;
    }

    public static int getMusicIndexOnQueue(Iterable<MediaSession.QueueItem> queue, long queueId) {
        int index = 0;
        for (MediaSession.QueueItem item: queue) {
            if (queueId == item.getQueueId()) {
                return index;
            }
            index++;
        }
        return -1;
    }

    public static List<MediaSession.QueueItem> convertToQueue(Iterable<MediaMetadata> tracks) {
        List<MediaSession.QueueItem> queue = new ArrayList<>();
        int count = 0;
        for (MediaMetadata track : tracks) {
            // We don't expect queues to change after created, so we use the item index as the
            // queueId. Any other number unique in the queue would work.
            MediaSession.QueueItem item = new MediaSession.QueueItem(
                    track.getDescription(), count++);
            queue.add(item);
        }
        return queue;

    }
}
