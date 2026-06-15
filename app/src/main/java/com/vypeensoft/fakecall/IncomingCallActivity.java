package com.vypeensoft.fakecall;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class IncomingCallActivity extends AppCompatActivity {

    private static final String TAG = "IncomingCallActivity";
    private static final int TIMEOUT_MILLIS = 30000; // 30 seconds timeout

    private ContactModel contact;
    private MediaPlayer ringtonePlayer;
    private Handler handler = new Handler();
    private Runnable timeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);

        contact = (ContactModel) getIntent().getSerializableExtra("contact");
        if (contact == null) {
            finish();
            return;
        }

        setupUI();
        playRingtone();
        setupTimeout();
    }

    private void setupUI() {
        TextView txtName = findViewById(R.id.txt_incoming_name);
        TextView txtNumber = findViewById(R.id.txt_incoming_number);
        ImageView imgAvatar = findViewById(R.id.img_incoming_avatar);
        FloatingActionButton fabDecline = findViewById(R.id.fab_decline);
        FloatingActionButton fabAnswer = findViewById(R.id.fab_answer);

        txtName.setText(contact.getName());
        txtNumber.setText(contact.getPhone());
        imgAvatar.setImageResource(R.drawable.ic_person_placeholder);

        fabDecline.setOnClickListener(v -> declineCall());
        fabAnswer.setOnClickListener(v -> answerCall());
    }

    private void playRingtone() {
        SharedPreferences prefs = getSharedPreferences("FakeCallPrefs", Context.MODE_PRIVATE);
        String uriStr = prefs.getString("ringtone_uri", null);
        Uri ringtoneUri = null;

        if (uriStr != null) {
            ringtoneUri = Uri.parse(uriStr);
        } else {
            ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        }

        if (ringtoneUri != null) {
            try {
                ringtonePlayer = new MediaPlayer();
                ringtonePlayer.setDataSource(this, ringtoneUri);
                ringtonePlayer.setLooping(true);
                ringtonePlayer.prepare();
                ringtonePlayer.start();
            } catch (Exception e) {
                Log.e(TAG, "Error playing ringtone", e);
            }
        }
    }

    private void stopRingtone() {
        if (ringtonePlayer != null) {
            try {
                if (ringtonePlayer.isPlaying()) {
                    ringtonePlayer.stop();
                }
                ringtonePlayer.release();
            } catch (Exception e) {
                // Ignore
            }
            ringtonePlayer = null;
        }
    }

    private void setupTimeout() {
        timeoutRunnable = () -> {
            Log.d(TAG, "Incoming call timed out");
            declineCall();
        };
        handler.postDelayed(timeoutRunnable, TIMEOUT_MILLIS);
    }

    private void cancelTimeout() {
        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
        }
    }

    private void dismissNotification() {
        android.app.NotificationManager notificationManager = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(1001);
        }
    }

    private void declineCall() {
        cancelTimeout();
        stopRingtone();
        dismissNotification();
        finish();
    }

    private void answerCall() {
        cancelTimeout();
        stopRingtone();
        dismissNotification();

        Intent intent = new Intent(this, InCallActivity.class);
        intent.putExtra("contact", contact);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimeout();
        stopRingtone();
    }

    @Override
    public void onBackPressed() {
        // Disable back button for realistic incoming call screen behavior
        // super.onBackPressed();
    }
}
