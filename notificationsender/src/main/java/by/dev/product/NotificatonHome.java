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

import android.app.Activity;
import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.Spinner;

import by.dev.product.notificationsender.R;

import static by.dev.product.notificationsender.NotificationConfig.*;


public class NotificatonHome extends Activity implements View.OnClickListener {

    private static final String ACTION_NOTIFICATION_SEND = "by.dev.product.action.NOTIFICATION";

    private Spinner mPriorities;
    private Spinner mVisibility;
    private CheckBox mUseHeadsUp;
    private CheckBox mUseNotificationToPlay;
    private CheckBox mPlaySound;
    private CheckBox mVibrate;
    private CheckBox mLocal;
    private CheckBox mUseAudioAttrs;
    private RadioGroup mMessageType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notificaton_home);

        mUseHeadsUp = (CheckBox) findViewById(R.id.use_heads_up);
        mUseNotificationToPlay = (CheckBox) findViewById(R.id.use_notification_to_play);
        mPlaySound = (CheckBox) findViewById(R.id.play_sound);
        mVibrate = (CheckBox) findViewById(R.id.vibrate);
        mLocal = (CheckBox) findViewById(R.id.local);
        mUseAudioAttrs = (CheckBox) findViewById(R.id.use_attrs);
        mMessageType = (RadioGroup) findViewById(R.id.message_type);
        mPriorities = (Spinner) findViewById(R.id.priorities_spinner);
        mVisibility = (Spinner) findViewById(R.id.visibility_spinner);

        if (savedInstanceState == null) {
            mUseNotificationToPlay.setEnabled(false);
            mUseAudioAttrs.setEnabled(false);
        }

        ArrayAdapter<Priority> prioritiesAdapter = new ArrayAdapter<Priority>(this, android.R.layout.simple_spinner_item, Priority.values());
        prioritiesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ArrayAdapter<Visibility> visibilityAdapter = new ArrayAdapter<Visibility>(this, android.R.layout.simple_spinner_item, Visibility.values());
        visibilityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPriorities.setAdapter(prioritiesAdapter);
        mVisibility.setAdapter(visibilityAdapter);

        mPlaySound.setOnClickListener(this);
        mVibrate.setOnClickListener(this);
        mUseNotificationToPlay.setOnClickListener(this);
        findViewById(R.id.send_call).setOnClickListener(this);
        findViewById(R.id.send_message).setOnClickListener(this);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        setUseNotificatonBuilderState();
        setAudioAttrsState();
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        switch (viewId) {
            case R.id.send_call:
                initCall();
                break;
            case R.id.send_message:
                initMessage();
                break;
            case R.id.play_sound: // fall through
            case R.id.vibrate: // fall through
                setUseNotificatonBuilderState();
                setAudioAttrsState();
                break;
            case R.id.use_notification_to_play:
                setAudioAttrsState();
                break;
        }
    }

    private void setUseNotificatonBuilderState() {
        setCheckBoxState(mPlaySound.isChecked() || mVibrate.isChecked(), mUseNotificationToPlay);
    }

    private void setAudioAttrsState() {
        setCheckBoxState(!mUseNotificationToPlay.isChecked() && (mPlaySound.isChecked() || mVibrate.isChecked()), mUseAudioAttrs);
    }

    private static void setCheckBoxState(boolean enabled, CheckBox view) {
        if (!enabled) view.setChecked(false);
        view.setEnabled(enabled);
    }

    private void initCall() {
        Bundle config = createGeneralConfig();
        config.putString(TITLE, getString(R.string.caller_name));
        config.putString(MESSAGE, getString(R.string.call_message));
        config.putString(TICKER, getString(R.string.call_ticker));
        config.putBoolean(HEADS_UP, true);
        config.putString(CATEGORY, Notification.CATEGORY_CALL);
        config.putBoolean(ONGOING_NOTIFICATION, true);
        config.putBoolean(REPEAT_SOUND, true);

        Intent intent = new Intent(ACTION_NOTIFICATION_SEND);
        intent.putExtras(config);
        sendBroadcast(intent);
    }

    private void initMessage() {
        Bundle config = createGeneralConfig();
        int selectedType = mMessageType.getCheckedRadioButtonId();
        String caller = getString(R.string.caller_name);
        String msg = getString(selectedType == R.id.long_message ? R.string.long_message : R.string.short_message);
        String ticker = caller + ": " + msg;
        config.putInt(STYLE, selectedType == R.id.long_message ? LONG_MESSAGE : SHORT_MESSAGE);
        config.putString(TITLE, caller);
        config.putString(MESSAGE, msg);
        config.putString(TICKER, ticker);
        config.putBoolean(HEADS_UP, mUseHeadsUp.isChecked());
        config.putString(CATEGORY, Notification.CATEGORY_MESSAGE);


        Intent intent = new Intent(ACTION_NOTIFICATION_SEND);
        intent.putExtras(config);
        sendBroadcast(intent);
    }

    private Bundle createGeneralConfig() {
        Bundle config = new Bundle();
        Priority selectedPriority = (Priority) mPriorities.getSelectedItem();
        config.putInt(PRIORITY, selectedPriority.priority);
        Visibility selectedVisibility = (Visibility) mVisibility.getSelectedItem();
        config.putInt(VISIBILITY, selectedVisibility.visibility);
        config.putBoolean(LOCAL, mLocal.isChecked());
        config.putBoolean(SOUND, mPlaySound.isChecked());
        config.putBoolean(VIBRATE, mVibrate.isChecked());
        config.putBoolean(USE_NOTIFICATION_SOUNDS, mUseNotificationToPlay.isChecked());
        config.putBoolean(USE_AUDIO_ATTRIBUTES, mUseAudioAttrs.isChecked());
        return config;
    }

    enum Priority {
        DEFAULT(Notification.PRIORITY_DEFAULT), MIN(Notification.PRIORITY_MIN), MAX(Notification.PRIORITY_MAX);

        final int priority;

        Priority(int priority) {
            this.priority = priority;
        }
    }

    enum Visibility {
        PUBLIC("Public", Notification.VISIBILITY_PUBLIC), PRIVATE("Private", Notification.VISIBILITY_PRIVATE), SECRET("Secret", Notification.VISIBILITY_SECRET);

        final String label;
        final int visibility;

        Visibility(String label, int visibility) {
            this.label = label;
            this.visibility = visibility;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
