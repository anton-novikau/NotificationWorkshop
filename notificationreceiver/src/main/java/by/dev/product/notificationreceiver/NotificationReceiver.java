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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static by.dev.product.notificationreceiver.NotificationService.EXTRA_ACION;
import static by.dev.product.notificationreceiver.NotificationService.ACTION_SHOW_NOTIFICATION;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, NotificationService.class);
        serviceIntent.putExtra(EXTRA_ACION, ACTION_SHOW_NOTIFICATION);
        serviceIntent.putExtras(intent);
        context.startService(serviceIntent);
    }
}
