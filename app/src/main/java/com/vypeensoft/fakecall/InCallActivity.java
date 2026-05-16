package com.vypeensoft.fakecall;

import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.TextureView;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class InCallActivity extends AppCompatActivity {

    private ContactModel contact;
    private Chronometer chronometer;
    private AudioPlayerHelper audioPlayerHelper;
    private boolean isMuted = false;
    private boolean isSpeakerOn = false;
    private boolean isKeypadOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_call);

        contact = (ContactModel) getIntent().getSerializableExtra("contact");
        if (contact == null) {
            finish();
            return;
        }

        audioPlayerHelper = new AudioPlayerHelper();
        setupUI();
        startCall();
        setupCameraPreview();
    }

    private void setupCameraPreview() {
        TextureView textureView = findViewById(R.id.texture_preview);
        if (textureView.isAvailable()) {
            CameraRecorderHelper.getInstance(this).attachPreview(textureView);
        } else {
            textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    CameraRecorderHelper.getInstance(InCallActivity.this).attachPreview(textureView);
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
        TextView txtName = findViewById(R.id.txt_incall_name);
        TextView txtNumber = findViewById(R.id.txt_incall_number);
        ImageView imgAvatar = findViewById(R.id.img_incall_avatar);
        FloatingActionButton fabEnd = findViewById(R.id.fab_end_call);
        chronometer = findViewById(R.id.chronometer);

        txtName.setText(contact.getName());
        txtNumber.setText(contact.getPhone());
        // In a real app, load contact photo here
        imgAvatar.setImageResource(R.drawable.ic_person_placeholder);

        fabEnd.setOnClickListener(v -> endCall());

        setupActionButtons();
    }

    private void setupActionButtons() {
        LinearLayout layoutMute = findViewById(R.id.layout_mute);
        LinearLayout layoutKeypad = findViewById(R.id.layout_keypad);
        LinearLayout layoutSpeaker = findViewById(R.id.layout_speaker);
        LinearLayout layoutAddCall = findViewById(R.id.layout_add_call);
        LinearLayout layoutHold = findViewById(R.id.layout_hold);
        LinearLayout layoutVideoCall = findViewById(R.id.layout_video_call);
        
        ImageView imgMute = findViewById(R.id.img_mute);
        ImageView imgKeypad = findViewById(R.id.img_keypad);
        ImageView imgSpeaker = findViewById(R.id.img_speaker);
        ImageView imgAddCall = findViewById(R.id.img_add_call);
        ImageView imgHold = findViewById(R.id.img_hold);
        ImageView imgVideoCall = findViewById(R.id.img_video_call);

        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        layoutMute.setOnClickListener(v -> {
            isMuted = !isMuted;
            if (isMuted) {
                imgMute.setColorFilter(Color.parseColor("#4CAF50")); // Green for active
                Toast.makeText(this, "Microphone muted", Toast.LENGTH_SHORT).show();
            } else {
                imgMute.setColorFilter(Color.WHITE);
                Toast.makeText(this, "Microphone unmuted", Toast.LENGTH_SHORT).show();
            }
        });

        layoutKeypad.setOnClickListener(v -> {
            isKeypadOpen = !isKeypadOpen;
            if (isKeypadOpen) {
                imgKeypad.setColorFilter(Color.parseColor("#4CAF50"));
                Toast.makeText(this, "Keypad shown", Toast.LENGTH_SHORT).show();
            } else {
                imgKeypad.setColorFilter(Color.WHITE);
                Toast.makeText(this, "Keypad hidden", Toast.LENGTH_SHORT).show();
            }
        });

        layoutSpeaker.setOnClickListener(v -> {
            isSpeakerOn = !isSpeakerOn;
            if (isSpeakerOn) {
                audioManager.setSpeakerphoneOn(true);
                imgSpeaker.setColorFilter(Color.parseColor("#4CAF50"));
                Toast.makeText(this, "Speakerphone on", Toast.LENGTH_SHORT).show();
            } else {
                audioManager.setSpeakerphoneOn(false);
                imgSpeaker.setColorFilter(Color.WHITE);
                Toast.makeText(this, "Speakerphone off", Toast.LENGTH_SHORT).show();
            }
        });

        layoutAddCall.setOnClickListener(v -> {
            Toast.makeText(this, "Adding call...", Toast.LENGTH_SHORT).show();
        });

        layoutHold.setOnClickListener(v -> {
            Toast.makeText(this, "Call on hold", Toast.LENGTH_SHORT).show();
        });

        layoutVideoCall.setOnClickListener(v -> {
            Toast.makeText(this, "Switching to video...", Toast.LENGTH_SHORT).show();
        });
    }

    private void startCall() {
        // Start timer
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();

        // Start audio playback
        String audioPath = JsonStorageHelper.getAudioPath(contact.getAudio());
        audioPlayerHelper.playAudio(this, audioPath);
    }

    private void endCall() {
        chronometer.stop();
        audioPlayerHelper.stopAudio();
        CameraRecorderHelper.getInstance(this).stopRecording();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioPlayerHelper != null) {
            audioPlayerHelper.stopAudio();
        }
        CameraRecorderHelper.getInstance(this).stopRecording();
    }

    @Override
    public void onBackPressed() {
        // Disable back button to force end-call button usage
        // super.onBackPressed();
    }
}
