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

package by.dev.product.notificationreceiver;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


public class ChatActivity extends Activity {

    public static final String EXTRA_MESSAGE = "incoming_message";
    public static final String EXTRA_PARTICIPANT = "participant_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Intent intent = getIntent();

        ActionBar ab = getActionBar();
        if (ab != null) ab.setTitle(intent.getStringExtra(EXTRA_PARTICIPANT));

        TextView messageView = (TextView) findViewById(R.id.message);
        messageView.setText(intent.getStringExtra(EXTRA_MESSAGE));
        // cancel call notification and stop ringing
        Intent cancelCallNotification = new Intent(this, NotificationService.class);
        cancelCallNotification.putExtra(NotificationService.EXTRA_ACION, NotificationService.ACTION_CANCEL_MESSAGE);
        startService(cancelCallNotification);
    }
}
