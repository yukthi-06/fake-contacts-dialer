package com.vypeensoft.fakecall;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class JsonStorageHelper {
    private static final String TAG = "JsonStorageHelper";
    private static final String BASE_PATH = Environment.getExternalStorageDirectory().getPath() + "/Vypeensoft/Contacts_Phone_Dialer/";
    private static final String SETTINGS_DIR = BASE_PATH + "settings/";
    private static final String AUDIO_DIR = BASE_PATH + "call_audio/";
    private static final String JSON_FILE = SETTINGS_DIR + "contacts.json";

    public static List<ContactModel> loadContacts(Context context) {
        ensureDirectoriesExist();
        File file = new File(JSON_FILE);
        if (!file.exists()) {
            return createDefaultContacts();
        }

        try (FileReader reader = new FileReader(file)) {
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<ContactModel>>() {}.getType();
            List<ContactModel> contacts = gson.fromJson(reader, listType);
            if (contacts == null) return new ArrayList<>();
            return contacts;
        } catch (IOException e) {
            Log.e(TAG, "Error reading JSON", e);
            return new ArrayList<>();
        }
    }

    private static void ensureDirectoriesExist() {
        File settingsDir = new File(SETTINGS_DIR);
        if (!settingsDir.exists()) {
            settingsDir.mkdirs();
        }
        File audioDir = new File(AUDIO_DIR);
        if (!audioDir.exists()) {
            audioDir.mkdirs();
        }
    }

    private static List<ContactModel> createDefaultContacts() {
        List<ContactModel> defaults = new ArrayList<>();
        defaults.add(new ContactModel("John", "+1 555 123 4567", "", "John.mp3"));
        defaults.add(new ContactModel("Emma", "+1 555 999 8888", "", "Emma.mp3"));
        defaults.add(new ContactModel("David Smith", "+1 555 444 3333", "", "David.mp3"));
        defaults.add(new ContactModel("Sarah Connor", "+1 555 000 1111", "", "Sarah.mp3"));

        saveContacts(defaults);
        return defaults;
    }

    public static void saveContacts(List<ContactModel> contacts) {
        ensureDirectoriesExist();
        try (FileWriter writer = new FileWriter(JSON_FILE)) {
            Gson gson = new Gson();
            gson.toJson(contacts, writer);
        } catch (IOException e) {
            Log.e(TAG, "Error saving JSON", e);
        }
    }

    public static String getAudioPath(String fileName) {
        return AUDIO_DIR + fileName;
    }
}
