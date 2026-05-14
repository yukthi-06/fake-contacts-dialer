package com.vypeensoft.fakecall;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.TextureView;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class InCallActivity extends AppCompatActivity {

    private ContactModel contact;
    private Chronometer chronometer;
    private AudioPlayerHelper audioPlayerHelper;

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
