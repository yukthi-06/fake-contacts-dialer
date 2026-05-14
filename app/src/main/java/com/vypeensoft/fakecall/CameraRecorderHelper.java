package com.vypeensoft.fakecall;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CameraRecorderHelper {
    private static final String TAG = "CameraRecorderHelper";
    private static CameraRecorderHelper instance;
    private Context context;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private MediaRecorder mediaRecorder;
    private Size videoSize;
    private boolean isRecording = false;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private CameraRecorderHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized CameraRecorderHelper getInstance(Context context) {
        if (instance == null) {
            instance = new CameraRecorderHelper(context);
        }
        return instance;
    }

    public void startRecording(TextureView textureView) {
        if (isRecording) return;
        startBackgroundThread();
        openCamera(textureView);
    }

    public void attachPreview(TextureView textureView) {
        if (!isRecording || cameraDevice == null || textureView.getSurfaceTexture() == null) return;
        try {
            if (captureSession != null) {
                captureSession.stopRepeating();
                // We don't close the session, just change the target if possible, 
                // but Camera2 usually requires a new session for new surfaces.
                // For simplicity, we'll just stop and restart if needed, 
                // but let's try just updating the request first.
            }
            
            Surface textureSurface = new Surface(textureView.getSurfaceTexture());
            Surface recorderSurface = mediaRecorder.getSurface();

            final CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            builder.addTarget(textureSurface);
            builder.addTarget(recorderSurface);

            captureSession.setRepeatingRequest(builder.build(), null, backgroundHandler);
        } catch (Exception e) {
            Log.e(TAG, "Error attaching preview", e);
        }
    }

    public void stopRecording() {
        if (!isRecording) return;
        try {
            if (captureSession != null) {
                captureSession.stopRepeating();
                captureSession.close();
                captureSession = null;
            }
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.reset();
                mediaRecorder.release();
                mediaRecorder = null;
            }
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording", e);
        }
        isRecording = false;
        stopBackgroundThread();
    }

    @SuppressLint("MissingPermission")
    private void openCamera(TextureView textureView) {
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIdList = manager.getCameraIdList();
            if (cameraIdList == null || cameraIdList.length == 0) {
                Log.e(TAG, "No cameras found");
                return;
            }
            String cameraId = cameraIdList[0]; // Back camera
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            Size[] sizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    .getOutputSizes(MediaRecorder.class);
            if (sizes == null || sizes.length == 0) {
                Log.e(TAG, "No video sizes found");
                return;
            }
            videoSize = sizes[0];

            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    startPreviewAndRecording(textureView);
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    cameraDevice = null;
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera access exception", e);
        }
    }

    private void startPreviewAndRecording(TextureView textureView) {
        try {
            setupMediaRecorder();
            Surface textureSurface = new Surface(textureView.getSurfaceTexture());
            Surface recorderSurface = mediaRecorder.getSurface();

            final CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            builder.addTarget(textureSurface);
            builder.addTarget(recorderSurface);

            List<Surface> surfaces = new ArrayList<>();
            surfaces.add(textureSurface);
            surfaces.add(recorderSurface);

            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    captureSession = session;
                    try {
                        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
                        captureSession.setRepeatingRequest(builder.build(), null, backgroundHandler);
                        mediaRecorder.start();
                        isRecording = true;
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Failed to start repeating request", e);
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "Capture session configuration failed");
                }
            }, backgroundHandler);
        } catch (Exception e) {
            Log.e(TAG, "Error starting preview and recording", e);
        }
    }

    private void setupMediaRecorder() throws IOException {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        
        File outputFile = getOutputMediaFile();
        if (outputFile == null) {
            throw new IOException("Could not create output file");
        }
        mediaRecorder.setOutputFile(outputFile.getAbsolutePath());
        
        mediaRecorder.setVideoEncodingBitRate(10000000);
        mediaRecorder.setVideoFrameRate(30);
        
        if (videoSize == null) {
            videoSize = new Size(1280, 720); // Default if somehow null
        }
        mediaRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
        
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.prepare();
    }

    private File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "Vypeensoft/Contacts_Phone_Dialer/recordings");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while stopping background thread", e);
            }
        }
    }
}
