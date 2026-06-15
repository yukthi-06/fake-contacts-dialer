package com.vypeensoft.fakecall;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class CallbackReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "incoming_call_channel";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
        ContactModel contact = (ContactModel) intent.getSerializableExtra("contact");
        if (contact == null) return;

        Intent incomingIntent = new Intent(context, IncomingCallActivity.class);
        incomingIntent.putExtra("contact", contact);
        incomingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Try starting activity directly (works if app is in foreground)
        try {
            context.startActivity(incomingIntent);
        } catch (Exception e) {
            // Ignore, we will use fullScreenIntent as background activity fallback
        }

        // Send a High-Priority notification with fullScreenIntent to bypass Android 10+ background limits
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Incoming Calls",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Shows incoming fake calls");
                notificationManager.createNotificationChannel(channel);
            }

            int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    incomingIntent,
                    pendingFlags
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_call) // use existing call icon
                    .setContentTitle("Incoming call")
                    .setContentText(contact.getName() + " is calling")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setFullScreenIntent(fullScreenPendingIntent, true)
                    .setAutoCancel(true);

            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }
}
