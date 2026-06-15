package com.vypeensoft.fakecall;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.File;

public class SettingsActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_RINGTONE = 200;
    private static final String PREFS_NAME = "FakeCallPrefs";
    private static final String KEY_RINGTONE_URI = "ringtone_uri";
    private static final String KEY_RINGTONE_NAME = "ringtone_name";

    private TextView tvRingtoneName;
    private Button btnPreviewRingtone;
    private Button btnResetRingtone;
    private LinearLayout layoutSelectRingtone;

    private MediaPlayer mediaPlayer;
    private boolean isPlayingPreview = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setupToolbar();
        initViews();
        loadSettings();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }
    }

    private void initViews() {
        tvRingtoneName = findViewById(R.id.tv_ringtone_name);
        btnPreviewRingtone = findViewById(R.id.btn_preview_ringtone);
        btnResetRingtone = findViewById(R.id.btn_reset_ringtone);
        layoutSelectRingtone = findViewById(R.id.layout_select_ringtone);

        layoutSelectRingtone.setOnClickListener(v -> selectRingtoneFile());

        btnPreviewRingtone.setOnClickListener(v -> {
            if (isPlayingPreview) {
                stopPreview();
            } else {
                startPreview();
            }
        });

        btnResetRingtone.setOnClickListener(v -> resetRingtone());
    }

    private void selectRingtoneFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        startActivityForResult(intent, REQUEST_CODE_PICK_RINGTONE);
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String name = prefs.getString(KEY_RINGTONE_NAME, "Default Ringtone");
        tvRingtoneName.setText(name);
    }

    private void resetRingtone() {
        stopPreview();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_RINGTONE_URI);
        editor.remove(KEY_RINGTONE_NAME);
        editor.apply();

        tvRingtoneName.setText("Default Ringtone");
        Toast.makeText(this, "Ringtone reset to default", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_RINGTONE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    // Try to persist permissions
                    final int takeFlags = data.getFlags()
                            & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    try {
                        getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    } catch (SecurityException e) {
                        // Ignore if we can't persist
                    }

                    String name = getFileName(uri);
                    if (name == null || name.isEmpty()) {
                        name = "Selected Audio File";
                    }

                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(KEY_RINGTONE_URI, uri.toString());
                    editor.putString(KEY_RINGTONE_NAME, name);
                    editor.apply();

                    tvRingtoneName.setText(name);
                    Toast.makeText(this, "Ringtone updated", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to select file", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void startPreview() {
        stopPreview();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String uriStr = prefs.getString(KEY_RINGTONE_URI, null);
        Uri ringtoneUri = null;

        if (uriStr != null) {
            ringtoneUri = Uri.parse(uriStr);
        } else {
            ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        }

        if (ringtoneUri == null) {
            Toast.makeText(this, "No default ringtone found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, ringtoneUri);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();

            isPlayingPreview = true;
            btnPreviewRingtone.setText("Stop Preview");
        } catch (Exception e) {
            Toast.makeText(this, "Error playing preview", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopPreview() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) {
                // Ignore
            }
            mediaPlayer = null;
        }
        isPlayingPreview = false;
        btnPreviewRingtone.setText("Play Preview");
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPreview();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
