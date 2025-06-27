package com.camera;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static MainActivity instance;

    AppCompatActivity activity;
    int[] selectedResolution = new int[2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.main);

        instance = this;
        activity = this;

        ActivityCompat.requestPermissions(this, new String[]{"android.permission.CAMERA"}, 0);

        if (ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") == PackageManager.PERMISSION_GRANTED) {
            String[] resolution = getBestResolutions(getScreenResolutions(), getCameraResolutions()).split("x");
            selectedResolution[0] = Integer.parseInt(resolution[1]);
            selectedResolution[1] = Integer.parseInt(resolution[0]);
            startService(new Intent(this, CameraService.class));
        }

        ImageButton button = findViewById(R.id.button);
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    CameraService.instance.takePhoto();
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY_RELEASE);
                }
                return true;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                String[] resolution = getBestResolutions(getScreenResolutions(), getCameraResolutions()).split("x");
                selectedResolution[0] = Integer.parseInt(resolution[1]);
                selectedResolution[1] = Integer.parseInt(resolution[0]);
                startService(new Intent(this, CameraService.class));
            }
        }
    }

    private List<String> getCameraResolutions() {
        List<String> resolutions = new ArrayList<>();
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map != null) {
                    Size[] sizes = map.getOutputSizes(ImageFormat.JPEG);
                    for (Size size : sizes) {
                        Log.e("com.camera", "Camera ID: " + cameraId + " - Supported resolution: " + size.toString());
                        resolutions.add(size.toString());
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return resolutions;
    }
    private int[] getScreenResolutions() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        int[] screenConfig = new int[4];
        /// screenConfig[0] 屏幕竖向像素
        /// screenConfig[1] 屏幕横向像素
        /// screenConfig[2] 屏幕竖向比值
        /// screenConfig[3] 屏幕横向比值

        display.getRealSize(size);

        screenConfig[0] = size.y;
        screenConfig[1] = size.x;

        int gcd = gcd(screenConfig[0], screenConfig[1]);
        screenConfig[2] = screenConfig[0] / gcd;
        screenConfig[3] = screenConfig[1] / gcd;
        Log.e("com.camera", "ScreenResolutions is " + screenConfig[0] + "x" + screenConfig[1] + ", " + screenConfig[2] + ":" + screenConfig[3]);

        return screenConfig;
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

            int gcd = gcd(width, height);
            int aspectRatioWidth = width / gcd;
            int aspectRatioHeight = height / gcd;

            if (aspectRatioWidth == screenConfig[2] && aspectRatioHeight == screenConfig[3]) {
                bestMatch = resolution;
                break;
            }
        }
        Log.e("com.camera", "BestResolution is " + bestMatch);
        return bestMatch;
    }
    private int gcd(int a, int b) {
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }
}