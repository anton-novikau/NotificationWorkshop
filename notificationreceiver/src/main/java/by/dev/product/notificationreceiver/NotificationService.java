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
                ((ReceiverApplication) getApplication()).getSoundManager().stopRingtone();
                break;
            case ACTION_STOP_RINGTONE:
                ((ReceiverApplication) getApplication()).getSoundManager().stopRingtone();
                break;
            case ACTION_UNKNOWN: // fall through
            default:
                Log.w(LOG_TAG, "Action is not supported. ActionID = " + action);
                break;
        }
    }

    private void showNotification(Intent intent) {
        Notification.Builder builder = new Notification.Builder(this);
        // repeat sound only for calls
        boolean isCall = intent.getBooleanExtra(REPEAT_SOUND, false);
        builder.setSmallIcon(isCall ? R.drawable.stat_sys_phone_call : R.drawable.stat_notify_chat);
        String ticker = intent.getStringExtra(TICKER);
        builder.setTicker(ticker);
        String callerName = intent.getStringExtra(TITLE);
        builder.setContentTitle(callerName);
        String message = intent.getStringExtra(MESSAGE);
        builder.setContentText(message);
        int style = intent.getIntExtra(STYLE, SHORT_MESSAGE);
        if (style == LONG_MESSAGE) {
            Notification.BigTextStyle textStyle = new Notification.BigTextStyle(builder);
            textStyle.setBigContentTitle(callerName);
            textStyle.setSummaryText(getString(isCall ? R.string.call_summary : R.string.message_summary));
            textStyle.bigText(message);
            builder.setStyle(textStyle);
        }
        builder.setLocalOnly(intent.getBooleanExtra(LOCAL, false));
        builder.setOngoing(intent.getBooleanExtra(ONGOING_NOTIFICATION, false));
        int priority = intent.getIntExtra(PRIORITY, Notification.PRIORITY_DEFAULT);
        builder.setPriority(priority);
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

        boolean useNotificationSounds = intent.getBooleanExtra(USE_NOTIFICATION_SOUNDS, false);
        if (useNotificationSounds) {
            if (playSound) builder.setSound(ringtoneUri);
            if (vibrate) builder.setVibrate(VIBRATE_PATTERN);
        } else {
            ReceiverApplication app = (ReceiverApplication) getApplication();
            SoundManager sm = app.getSoundManager();
            boolean useAttrs = intent.getBooleanExtra(USE_AUDIO_ATTRIBUTES, false);
            if (playSound) sm.playRingtone(ringtoneUri, isCall, useAttrs);
            if (vibrate) sm.vibrate(VIBRATE_PATTERN, isCall, useAttrs);

            Intent deleteIntent = new Intent(this, NotificationService.class);
            deleteIntent.putExtra(EXTRA_ACION, ACTION_STOP_RINGTONE);
            builder.setDeleteIntent(PendingIntent.getService(this, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        }

        if (isCall) {
            Intent answerIntent = new Intent(this, CallInProgressActivity.class);
            answerIntent.putExtra(CallInProgressActivity.EXTRA_CALLER_NAME, callerName);
            answerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent answerPendingIntent = PendingIntent.getActivity(this, 0, answerIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(R.drawable.stat_sys_phone_call, getString(R.string.btn_answer), answerPendingIntent);

            Intent declineIntent = new Intent(this, NotificationService.class);
            declineIntent.putExtra(EXTRA_ACION, ACTION_CANCEL_CALL);
            PendingIntent declinePendingIntent = PendingIntent.getService(this, 0, declineIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(R.drawable.stat_sys_phone_call_end, getString(R.string.btn_decline), declinePendingIntent);

            Intent incomingCall = new Intent(this, IncomingCallActivity.class);
            incomingCall.putExtra(IncomingCallActivity.EXTRA_CALLER, callerName);
            incomingCall.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent incomingCallPending = PendingIntent.getActivity(this, 0, incomingCall, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setFullScreenIntent(incomingCallPending, true);
            builder.setContentIntent(incomingCallPending);
        } else {
            Intent messageIntent = new Intent(this, ChatActivity.class);
            messageIntent.putExtra(ChatActivity.EXTRA_MESSAGE, message);
            messageIntent.putExtra(ChatActivity.EXTRA_PARTICIPANT, callerName);
            messageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent messagePendingIntent = PendingIntent.getActivity(this, 0, messageIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            if (intent.getBooleanExtra(HEADS_UP, false)) builder.setFullScreenIntent(messagePendingIntent, priority == Notification.PRIORITY_MAX);
            builder.setContentIntent(messagePendingIntent);
        }

        // setup large user icon for connected wearable
        Notification.WearableExtender extender = new Notification.WearableExtender();
        extender.setBackground(BitmapFactory.decodeResource(getResources(), R.drawable.joan_doe_large));
        builder.extend(extender);

        Notification notification = builder.build();
        if (useNotificationSounds && isCall) notification.flags |= Notification.FLAG_INSISTENT;

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(notificationId, notification);
    }

    private static Uri getResourceUri(int resourceId, Context context) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + resourceId);
    }
}
