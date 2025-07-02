package com.camera;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Size;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jp.wasabeef.glide.transformations.BlurTransformation;


public class CameraService extends Service implements LifecycleOwner {
    public static CameraService instance;
    private LifecycleRegistry lifecycleRegistry;
    ImageCapture imageCapture;
    private CameraSelector cameraLen;
    int[] selectedResolution = new int[2];
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        lifecycleRegistry = new LifecycleRegistry(this);
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);

        NotificationChannel serviceChannel = new NotificationChannel("0", getString(R.string.notifi_title), NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, "0")
                .setContentTitle(getString(R.string.notifi_title))
                .setContentText(getString(R.string.notifi_text))
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        startForeground(1, notification);

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        cameraLen = CameraSelector.DEFAULT_BACK_CAMERA;
        startCamera();

        return START_STICKY;
    }

    private void startCamera() {
        String[] resolution = getBestResolutions(MainActivity.instance.getScreenResolutions(), getCameraResolutions((cameraLen == CameraSelector.DEFAULT_BACK_CAMERA) ? "0" : "1")).split("x");
        selectedResolution[0] = Integer.parseInt(resolution[1]);
        selectedResolution[1] = Integer.parseInt(resolution[0]);
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder()
                        .setTargetResolution(new Size(selectedResolution[0], selectedResolution[1]))
                        .build();

                imageCapture = new ImageCapture.Builder()
                        .setTargetResolution(new Size(selectedResolution[0], selectedResolution[1]))
                        .build();
                PreviewView view = MainActivity.instance.activity.findViewById(R.id.preview);
                preview.setSurfaceProvider(view.getSurfaceProvider());

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraLen, preview, imageCapture);
            } catch (Exception ignore) {
                stopSelf();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
    }

    public void switchLens() {
        cameraLen = (cameraLen == CameraSelector.DEFAULT_BACK_CAMERA) ? CameraSelector.DEFAULT_FRONT_CAMERA : CameraSelector.DEFAULT_BACK_CAMERA;
        startCamera();

        PreviewView preview = MainActivity.instance.activity.findViewById(R.id.preview);
        Bitmap bitmap = preview.getBitmap();
        ImageView flip = MainActivity.instance.activity.findViewById(R.id.flip);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flip.setImageBitmap(bitmap);
            flip.setRenderEffect(RenderEffect.createBlurEffect(80f, 80f, Shader.TileMode.CLAMP));
        }else
            Glide.with(this).load(bitmap).transform(new BlurTransformation(16, 8)).into(flip);
        flip.setVisibility(View.VISIBLE);
        flip.setAlpha(1f);
        preview.setAlpha(0f);
        flip.animate().rotationX(-90f).scaleX(0.5f).scaleY(0.5f).setDuration(160).withEndAction(new Runnable() {
            @Override
            public void run() {
                flip.setRotationX(-270f);
                flip.animate().rotationX(-360f).scaleX(1f).scaleY(1f).setDuration(160).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        flip.animate().setDuration(300).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                flip.setRotationX(0f);
                                flip.setVisibility(View.GONE);
                                preview.setAlpha(1f);
                                MainActivity.instance.activity.findViewById(R.id.front).setEnabled(true);
                            }
                        }).start();
                    }
                }).start();
            }
        }).start();
    }
    public void capture() {
        if (imageCapture == null) return;

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, new SimpleDateFormat("yyyy-MM-dd HH-mm-ss SSS", Locale.getDefault()).format(System.currentTimeMillis()));
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/Camera");

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
        ).build();

        MainActivity.instance.activity.findViewById(R.id.preview).animate().alpha(0f).setDuration(100).withEndAction(new Runnable() {
            @Override
            public void run() {
                MainActivity.instance.activity.findViewById(R.id.preview).animate().alpha(1f).setDuration(100).start();
            }
        }).start();

        try {
            if (Settings.System.getInt(this.getContentResolver(), "camera_sounds_enabled") == 1)
                throw new Throwable();
        } catch (Throwable ignored) {
            AudioManager audioManager = getSystemService(AudioManager.class);
            MediaPlayer.create(this, R.raw.capture, new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build(), audioManager.generateAudioSessionId()).start();
        }

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                MainActivity.instance.activity.findViewById(R.id.capture).setEnabled(true);
                ImageView thumbnail = MainActivity.instance.activity.findViewById(R.id.thumbnail);
                Glide.with(MainActivity.instance.activity).load(outputFileResults.getSavedUri()).apply(RequestOptions.bitmapTransform(new CircleCrop())).into(thumbnail);
                MainActivity.instance.latestUri = outputFileResults.getSavedUri();
                FrameLayout gallery = MainActivity.instance.activity.findViewById(R.id.gallery);
                gallery.setScaleX(0.6f);
                gallery.setScaleY(0.6f);
                gallery.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        gallery.setEnabled(true);
                    }
                }).start();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {

            }
        });
    }

    private List<String> getCameraResolutions(String cameraId) {
        List<String> resolutions = new ArrayList<>();
        CameraManager cameraManager = getSystemService(CameraManager.class);
        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map != null) {
                Size[] sizes = map.getOutputSizes(ImageFormat.JPEG);
                for (Size size : sizes)
                    resolutions.add(size.toString());
            }

        } catch (Throwable ignored) { }
        return resolutions;
    }
    private String getBestResolutions(int[] screenConfig, List<String> resolutions){
        String maxResolution = "";
        int maxValue = 0;
        String bestMatch;

        for (String res : resolutions) {
            String[] dimensions = res.split("x");
            int width = Integer.parseInt(dimensions[0]);
            int height = Integer.parseInt(dimensions[1]);
            int value = width * height;

            if (value > maxValue) {
                maxValue = value;
                maxResolution = res;
            }
        }
        bestMatch = maxResolution;

        for (String resolution : resolutions) {
            String[] parts = resolution.split("x");
            int width = Integer.parseInt(parts[0]);
            int height = Integer.parseInt(parts[1]);

            int gcd = MainActivity.instance.gcd(width, height);
            int aspectRatioWidth = width / gcd;
            int aspectRatioHeight = height / gcd;

            if (aspectRatioWidth == screenConfig[2] && aspectRatioHeight == screenConfig[3]) {
                bestMatch = resolution;
                break;
            }
        }
        return bestMatch;
    }
}
