package com.vypeensoft.fakecall;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Random;

public class DialingActivity extends AppCompatActivity {

    private ContactModel contact;
    private Handler handler = new Handler();
    private Runnable transitionRunnable;

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
            finish();
        });
    }

    private void startRingingSimulation() {
        // Random delay between 4 and 8 seconds
        int delay = (new Random().nextInt(5) + 4) * 1000;

        transitionRunnable = () -> {
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
