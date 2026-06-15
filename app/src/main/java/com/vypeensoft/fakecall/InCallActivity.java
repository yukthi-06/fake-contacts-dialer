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

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class InCallActivity extends AppCompatActivity {

    private ContactModel contact;
    private Chronometer chronometer;
    private AudioPlayerHelper audioPlayerHelper;
    private boolean isMuted = false;
    private boolean isSpeakerOn = false;
    private boolean isKeypadOpen = false;
    private boolean isHoldActive = false;
    private boolean isVideoActive = false;

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
            CameraRecorderHelper recorder = CameraRecorderHelper.getInstance(this);
            if (recorder.isRecording()) {
                recorder.attachPreview(textureView);
            } else {
                recorder.startRecording(textureView);
            }
        } else {
            textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    CameraRecorderHelper recorder = CameraRecorderHelper.getInstance(InCallActivity.this);
                    if (recorder.isRecording()) {
                        recorder.attachPreview(textureView);
                    } else {
                        recorder.startRecording(textureView);
                    }
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
            } else {
                imgMute.setColorFilter(Color.WHITE);
            }
        });

        layoutKeypad.setOnClickListener(v -> {
            isKeypadOpen = !isKeypadOpen;
            if (isKeypadOpen) {
                imgKeypad.setColorFilter(Color.parseColor("#4CAF50"));
            } else {
                imgKeypad.setColorFilter(Color.WHITE);
            }
        });

        layoutSpeaker.setOnClickListener(v -> {
            isSpeakerOn = !isSpeakerOn;
            if (isSpeakerOn) {
                audioManager.setSpeakerphoneOn(true);
                imgSpeaker.setColorFilter(Color.parseColor("#4CAF50"));
            } else {
                audioManager.setSpeakerphoneOn(false);
                imgSpeaker.setColorFilter(Color.WHITE);
            }
        });

        layoutAddCall.setOnClickListener(v -> {
            imgAddCall.setColorFilter(Color.parseColor("#4CAF50"));
            v.postDelayed(() -> imgAddCall.setColorFilter(Color.WHITE), 200);
        });

        layoutHold.setOnClickListener(v -> {
            isHoldActive = !isHoldActive;
            if (isHoldActive) {
                imgHold.setColorFilter(Color.parseColor("#4CAF50"));
            } else {
                imgHold.setColorFilter(Color.WHITE);
            }
        });

        layoutVideoCall.setOnClickListener(v -> {
            isVideoActive = !isVideoActive;
            if (isVideoActive) {
                imgVideoCall.setColorFilter(Color.parseColor("#4CAF50"));
            } else {
                imgVideoCall.setColorFilter(Color.WHITE);
            }
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
