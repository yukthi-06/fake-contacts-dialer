package com.vypeensoft.fakecall;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
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
    private TextureView pendingTextureView;

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
        this.pendingTextureView = textureView;
        startBackgroundThread();
        openCamera();
    }

    public void attachPreview(TextureView textureView) {
        this.pendingTextureView = textureView;
        if (!isRecording || cameraDevice == null || textureView.getSurfaceTexture() == null) return;
        
        // Re-run the preview setup on the background thread
        backgroundHandler.post(() -> startPreviewAndRecording(textureView));
    }

    public void stopRecording() {
        if (!isRecording) return;
        isRecording = false;
        try {
            if (captureSession != null) {
                captureSession.stopRepeating();
                captureSession.close();
                captureSession = null;
            }
            if (mediaRecorder != null) {
                try {
                    mediaRecorder.stop();
                } catch (RuntimeException e) {
                    Log.e(TAG, "MediaRecorder stop failed", e);
                }
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
        stopBackgroundThread();
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            String backCameraId = null;
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics chars = manager.getCameraCharacteristics(id);
                Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    backCameraId = id;
                    break;
                }
            }

            if (backCameraId == null) {
                Log.e(TAG, "No back camera found");
                return;
            }

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(backCameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            videoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));

            manager.openCamera(backCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    if (pendingTextureView != null) {
                        startPreviewAndRecording(pendingTextureView);
                    }
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
        if (cameraDevice == null || textureView.getSurfaceTexture() == null) return;
        
        try {
            // If already recording, close current session before switching surface
            if (captureSession != null) {
                captureSession.stopRepeating();
                captureSession.close();
                captureSession = null;
            }

            if (!isRecording) {
                setupMediaRecorder();
            }

            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(videoSize.getWidth(), videoSize.getHeight());
            Surface textureSurface = new Surface(texture);
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
                        builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
                        
                        captureSession.setRepeatingRequest(builder.build(), null, backgroundHandler);
                        
                        if (!isRecording) {
                            mediaRecorder.start();
                            isRecording = true;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to start session/recording", e);
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "Session configuration failed");
                }
            }, backgroundHandler);
        } catch (Exception e) {
            Log.e(TAG, "Error in startPreviewAndRecording", e);
        }
    }

    private void setupMediaRecorder() throws IOException {
        if (mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
        }
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        
        File outputFile = getOutputMediaFile();
        if (outputFile == null) throw new IOException("File creation failed");
        mediaRecorder.setOutputFile(outputFile.getAbsolutePath());
        
        mediaRecorder.setVideoEncodingBitRate(2500000);
        mediaRecorder.setVideoFrameRate(24);
        mediaRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOrientationHint(90);
        mediaRecorder.prepare();
    }

    private Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        return choices[choices.length - 1]; // Smallest
    }

    private File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "Vypeensoft/Contacts_Phone_Dialer/recordings");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) return null;
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
    }

    private void startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = new HandlerThread("CameraBackground");
            backgroundThread.start();
            backgroundHandler = new Handler(backgroundThread.getLooper());
        }
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Thread join interrupted", e);
            }
        }
    }
}
