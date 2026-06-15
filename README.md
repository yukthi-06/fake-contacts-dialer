# Fake Contacts Dialer

A premium Android application built in Java that simulates a fake contacts list and fake calling experience.

## Features
- **Contacts List**: Modern Android-style contacts list with search functionality.
- **Fake Dialing**: Simulates the outgoing call screen with a "Calling..." state and random ringing duration (4-8 seconds).
- **Fake In-Call**: Simulates an active call with a timer, contact info, and audio playback.
- **Local Storage**: Reads/writes contacts to `/sdcard/Vypeensoft/Contacts_Phone_Dialer/settings/contacts.json`.
- **Audio Playback**: Plays corresponding MP3 files from `/sdcard/Vypeensoft/Contacts_Phone_Dialer/audio_samples/` during calls.
- **Automatic Setup**: Creates required directories and default contacts on first launch.
- **Permission Handling**: Supports Android 8.0 up to Android 14+ with proper runtime permission requests.

## Setup Instructions
1. Open the project in **Android Studio**.
2. Build and run on an Android device or emulator (API 26+).
3. On first launch, the app will request storage permissions.
4. Once granted, it will create the following structure on your SD card:
   - `/Vypeensoft/Contacts_Phone_Dialer/settings/contacts.json`
   - `/Vypeensoft/Contacts_Phone_Dialer/audio_samples/`
5. To hear audio during a call:
   - Place MP3 files in the `audio_samples` folder.
   - The file names should match the `audio` field in `contacts.json` (e.g., `John.mp3`).

## Technical Details
- **Language**: Java
- **UI Framework**: Material Design Components (XML Layouts)
- **JSON Parsing**: Gson
- **Audio**: MediaPlayer
- **Architecture**: Modular (Activities, Adapters, Models, Helpers)

## Permissions
- `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE`: To access and store the JSON file.
- `MANAGE_EXTERNAL_STORAGE`: For Android 11+ to access the `/sdcard` directory directly.
- `READ_MEDIA_AUDIO`: For Android 13+ audio access.