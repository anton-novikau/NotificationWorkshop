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

package by.dev.product;

public interface NotificationConfig {
    String TITLE = "notification_title";
    String MESSAGE = "notification_message";
    String TICKER = "notification_ticker";
    String STYLE = "notification_style";
    String SOUND = "notification_sound";
    String VIBRATE = "notification_vibration";
    String REPEAT_SOUND = "notification_flag_insistent";
    String ONGOING_NOTIFICATION = "notification_flag_ongoing";
    String CATEGORY = "notification_category";
    String LOCAL = "is_notification_local";
    String PRIORITY = "notification_priority";
    String VISIBILITY = "notification_visibility";
    String HEADS_UP = "heads_up_notification";
    String USE_NOTIFICATION_SOUNDS = "use_notification_sounds";
    String USE_AUDIO_ATTRIBUTES = "use_audio_attributes";

    int LONG_MESSAGE = 1;
    int SHORT_MESSAGE = 2;
}
