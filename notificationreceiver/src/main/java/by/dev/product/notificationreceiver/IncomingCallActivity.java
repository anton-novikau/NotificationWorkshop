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
import android.widget.TextView;


public class IncomingCallActivity extends Activity implements View.OnClickListener {

    public static final String EXTRA_CALLER = "caller_name";

    private String mCallerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);

        Intent intent = getIntent();
        mCallerName = intent.getStringExtra(EXTRA_CALLER);
        TextView callMessage = (TextView) findViewById(R.id.caller_name);
        callMessage.setText(mCallerName);

        findViewById(R.id.answer_call).setOnClickListener(this);
        findViewById(R.id.decline_call).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.answer_call:
                answerCall();
                break;
            case R.id.decline_call:
                declineCall();
                break;
        }
    }

    private void answerCall() {
        Intent intent = new Intent(this, CallInProgressActivity.class);
        intent.putExtra(CallInProgressActivity.EXTRA_CALLER_NAME, mCallerName);
        startActivity(intent);
        finish();
    }

    private void declineCall() {
        Intent intent = new Intent(this, NotificationService.class);
        intent.putExtra(NotificationService.EXTRA_ACION, NotificationService.ACTION_CANCEL_CALL);
        startService(intent);
        finish();
    }
}
