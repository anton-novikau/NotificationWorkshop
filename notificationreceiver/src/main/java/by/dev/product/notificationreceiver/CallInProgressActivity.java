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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;


public class CallInProgressActivity extends Activity implements View.OnClickListener {

    public static final String EXTRA_CALLER_NAME = "caller_name";

    private Chronometer mTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_in_progress);

        TextView caller = (TextView) findViewById(R.id.caller_name);
        caller.setText(getIntent().getStringExtra(EXTRA_CALLER_NAME));
        mTimer = (Chronometer) findViewById(R.id.timer);
        mTimer.start();
        findViewById(R.id.hangup_call).setOnClickListener(this);
        // cancel call notification and stop ringing
        Intent cancelCallNotification = new Intent(this, NotificationService.class);
        cancelCallNotification.putExtra(NotificationService.EXTRA_ACION, NotificationService.ACTION_CANCEL_CALL);
        startService(cancelCallNotification);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.hangup_call) {
            mTimer.stop();
            Toast.makeText(this, R.string.call_finished, Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
