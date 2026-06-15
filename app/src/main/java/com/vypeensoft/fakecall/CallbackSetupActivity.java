package com.vypeensoft.fakecall;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class CallbackSetupActivity extends AppCompatActivity {

    private Spinner spinnerContacts;
    private TextInputEditText etDelay;
    private Button btnSchedule;
    private List<ContactModel> contactList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_callback_setup);

        setupToolbar();
        initViews();
        loadContacts();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Schedule Call Back");
        }
    }

    private void initViews() {
        spinnerContacts = findViewById(R.id.spinner_contacts);
        etDelay = findViewById(R.id.et_delay);
        btnSchedule = findViewById(R.id.btn_schedule);

        btnSchedule.setOnClickListener(v -> scheduleCallback());
    }

    private void loadContacts() {
        contactList = JsonStorageHelper.loadContacts(this);
        List<String> names = new ArrayList<>();
        for (ContactModel contact : contactList) {
            names.add(contact.getName() + " (" + contact.getPhone() + ")");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerContacts.setAdapter(adapter);
    }

    private void scheduleCallback() {
        if (contactList.isEmpty()) {
            Toast.makeText(this, "No contacts available to schedule", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedIndex = spinnerContacts.getSelectedItemPosition();
        if (selectedIndex < 0 || selectedIndex >= contactList.size()) {
            Toast.makeText(this, "Please select a valid contact", Toast.LENGTH_SHORT).show();
            return;
        }

        ContactModel selectedContact = contactList.get(selectedIndex);

        String delayStr = etDelay.getText().toString().trim();
        if (delayStr.isEmpty()) {
            Toast.makeText(this, "Please enter a delay in seconds", Toast.LENGTH_SHORT).show();
            return;
        }

        int delaySeconds;
        try {
            delaySeconds = Integer.parseInt(delayStr);
            if (delaySeconds <= 0) {
                Toast.makeText(this, "Delay must be greater than 0 seconds", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid delay format", Toast.LENGTH_SHORT).show();
            return;
        }

        // Schedule using AlarmManager
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            Intent intent = new Intent(this, CallbackReceiver.class);
            intent.putExtra("contact", selectedContact);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    0,
                    intent,
                    flags
            );

            long triggerAtMillis = SystemClock.elapsedRealtime() + (delaySeconds * 1000L);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                    );
                } else {
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                    );
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                );
            }

            Toast.makeText(this, "Callback scheduled in " + delaySeconds + " seconds", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "AlarmManager not available", Toast.LENGTH_SHORT).show();
        }
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
