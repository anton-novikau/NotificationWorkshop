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

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import static by.dev.product.NotificationConfig.*;

public class NotificationService extends IntentService {
    private static final String SERVICE_NAME = "NotificationService";

    private static final String LOG_TAG = "NotificationService";

    public static final String EXTRA_ACION = "service_action";

    public static final int ACTION_SHOW_NOTIFICATION = 0;
    public static final int ACTION_CANCEL_CALL = 1;
    public static final int ACTION_CANCEL_MESSAGE = 2;
    private static final int ACTION_STOP_RINGTONE = 3;
    private static final int ACTION_UNKNOWN = -1;

    private static final String JOAN_DOE_CONTACT = "tel:+375290778899";

    private static final int ID_CALL = 1;
    private static final int ID_MESSAGE = 2;

    private static final Uri MSG_NOTIFICATION_URI = Settings.System.DEFAULT_NOTIFICATION_URI;
    private static final long[] VIBRATE_PATTERN = {0, 250, 250, 250};

    /**
     * Creates an instance of NotificationService.
     */
    public NotificationService() {
        super(SERVICE_NAME);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int action = intent.getIntExtra(EXTRA_ACION, ACTION_UNKNOWN);
        switch (action) {
            case ACTION_SHOW_NOTIFICATION:
                showNotification(intent);
                break;
            case ACTION_CANCEL_MESSAGE:
                ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(ID_MESSAGE);
                break;
            case ACTION_CANCEL_CALL:
                ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(ID_CALL);
                break;
            case ACTION_STOP_RINGTONE:
                ReceiverApplication app = (ReceiverApplication) getApplication();
                app.getSoundManager().stopRingtone();
                break;
            case ACTION_UNKNOWN: // fall through
            default:
                Log.w(LOG_TAG, "Action is not supported. ActionID = " + action);
                break;
        }
    }

    private void showNotification(Intent intent) {
        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_small_notification);
        String ticker = intent.getStringExtra(TICKER);
        builder.setTicker(ticker);
        int style = intent.getIntExtra(STYLE, SHORT_MESSAGE);
        switch (style) {
            case LONG_MESSAGE:
                Notification.BigTextStyle textStyle = new Notification.BigTextStyle(builder);
                textStyle.setBigContentTitle(intent.getStringExtra(TITLE));
                textStyle.setSummaryText(ticker);
                textStyle.bigText(intent.getStringExtra(MESSAGE));
                builder.setStyle(textStyle);
                break;
            case SHORT_MESSAGE: // fall through
                builder.setContentTitle(intent.getStringExtra(TITLE));
                builder.setContentText(intent.getStringExtra(MESSAGE));
            default:
                break;
        }
        builder.setLocalOnly(intent.getBooleanExtra(LOCAL, false));
        builder.setOngoing(intent.getBooleanExtra(ONGOING_NOTIFICATION, false));
        builder.setPriority(intent.getIntExtra(PRIORITY, Notification.PRIORITY_DEFAULT));
        builder.setVisibility(intent.getIntExtra(VISIBILITY, Notification.VISIBILITY_PUBLIC));
        String category = intent.getStringExtra(CATEGORY);
        builder.setCategory(category);
        builder.addPerson(JOAN_DOE_CONTACT);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.joan_doe_small));

        Uri ringtoneUri;
        int notificationId;
        if (Notification.CATEGORY_CALL.equals(category)) {
            notificationId = ID_CALL;
            ringtoneUri = getResourceUri(R.raw.long_ringtone, this);
        } else {
            notificationId = ID_MESSAGE;
            ringtoneUri = MSG_NOTIFICATION_URI;
        }

        boolean playSound = intent.getBooleanExtra(SOUND, false);
        boolean vibrate = intent.getBooleanExtra(VIBRATE, false);
        boolean loop = intent.getBooleanExtra(REPEAT_SOUND, false);
        boolean useNotificationSounds = intent.getBooleanExtra(USE_NOTIFICATION_SOUNDS, false);
        if (useNotificationSounds) {
            if (playSound) builder.setSound(ringtoneUri);
            if (vibrate) builder.setVibrate(VIBRATE_PATTERN);
        } else {
            ReceiverApplication app = (ReceiverApplication) getApplication();
            SoundManager sm = app.getSoundManager();
            if (playSound) sm.playRingtone(ringtoneUri, loop);
            if (vibrate) sm.vibrate(VIBRATE_PATTERN, loop);

            Intent deleteIntent = new Intent(this, NotificationService.class);
            deleteIntent.putExtra(EXTRA_ACION, ACTION_STOP_RINGTONE);
            builder.setDeleteIntent(PendingIntent.getService(this, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        }
        // TODO: add actions

        // setup large user icon for connected wearable
        Notification.WearableExtender extender = new Notification.WearableExtender();
        extender.setBackground(BitmapFactory.decodeResource(getResources(), R.drawable.joan_doe_large));
        builder.extend(extender);

        Notification notification = builder.build();
        if (useNotificationSounds && loop) notification.flags |= Notification.FLAG_INSISTENT;

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(notificationId, notification);
    }

    private static Uri getResourceUri(int resourceId, Context context) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + resourceId);
    }
}
