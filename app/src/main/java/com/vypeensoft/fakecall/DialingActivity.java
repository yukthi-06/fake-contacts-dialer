package com.vypeensoft.fakecall;

import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.view.TextureView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Random;

public class DialingActivity extends AppCompatActivity {

    private ContactModel contact;
    private Handler handler = new Handler();
    private Runnable transitionRunnable;
    private android.media.MediaPlayer ringtonePlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialing);

        contact = (ContactModel) getIntent().getSerializableExtra("contact");
        if (contact == null) {
            finish();
            return;
        }

        setupUI();
        startRingingSimulation();
        setupCameraPreview();
        playRingtone();
    }

    private void playRingtone() {
        android.content.SharedPreferences prefs = getSharedPreferences("FakeCallPrefs", android.content.Context.MODE_PRIVATE);
        String uriStr = prefs.getString("ringtone_uri", null);
        android.net.Uri ringtoneUri = null;

        if (uriStr != null) {
            ringtoneUri = android.net.Uri.parse(uriStr);
        } else {
            ringtoneUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE);
        }

        if (ringtoneUri != null) {
            try {
                ringtonePlayer = new android.media.MediaPlayer();
                ringtonePlayer.setDataSource(this, ringtoneUri);
                ringtonePlayer.setLooping(true);
                ringtonePlayer.prepare();
                ringtonePlayer.start();
            } catch (Exception e) {
                android.util.Log.e("DialingActivity", "Error playing ringtone", e);
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

    private void setupCameraPreview() {
        TextureView textureView = findViewById(R.id.texture_preview);
        if (textureView.isAvailable()) {
            CameraRecorderHelper.getInstance(this).startRecording(textureView);
        } else {
            textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    CameraRecorderHelper.getInstance(DialingActivity.this).startRecording(textureView);
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
            });
        }
    }

    private void setupUI() {
        TextView txtName = findViewById(R.id.txt_dialing_name);
        TextView txtNumber = findViewById(R.id.txt_dialing_number);
        ImageView imgAvatar = findViewById(R.id.img_dialing_avatar);
        FloatingActionButton fabEnd = findViewById(R.id.fab_end_dialing);

        txtName.setText(contact.getName());
        txtNumber.setText(contact.getPhone());
        // In a real app, load contact photo here
        imgAvatar.setImageResource(R.drawable.ic_person_placeholder);

        fabEnd.setOnClickListener(v -> {
            stopSimulation();
            CameraRecorderHelper.getInstance(this).stopRecording();
            finish();
        });
    }

    private void startRingingSimulation() {
        // Random delay between 4 and 8 seconds
        int delay = (new Random().nextInt(5) + 4) * 1000;

        transitionRunnable = () -> {
            stopRingtone();
            Intent intent = new Intent(DialingActivity.this, InCallActivity.class);
            intent.putExtra("contact", contact);
            startActivity(intent);
            finish();
        };

        handler.postDelayed(transitionRunnable, delay);
    }

    private void stopSimulation() {
        if (transitionRunnable != null) {
            handler.removeCallbacks(transitionRunnable);
        }
        stopRingtone();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSimulation();
    }

    @Override
    public void onBackPressed() {
        // Disable back button during dialing to force end-call button usage or wait
        // super.onBackPressed();
    }
}
