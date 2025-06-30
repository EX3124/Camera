package com.camera;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    public static MainActivity instance;

    AppCompatActivity activity;
    boolean allowcapture = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.main);

        instance = this;
        activity = this;

        ActivityCompat.requestPermissions(this, new String[]{"android.permission.CAMERA"}, 0);

        if (ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") == PackageManager.PERMISSION_GRANTED)
            startService(new Intent(this, CameraService.class));

        ImageButton capture = findViewById(R.id.capture);
        capture.setEnabled(false);
        capture.setAlpha(0f);
        capture.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    allowcapture = true;
                    v.setBackground(getDrawable(R.drawable.capture_press));
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                } else if (event.getAction() == MotionEvent.ACTION_UP && allowcapture) {
                    allowcapture = false;
                    v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                    v.setBackground(getDrawable(R.drawable.capture));
                }
                return false;
            }
        });
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setEnabled(false);
                CameraService.instance.capture();
            }
        });

        ImageButton front = findViewById(R.id.front);
        front.setEnabled(false);
        front.setAlpha(0f);
        front.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                front.setEnabled(false);
                CameraService.instance.switchLens();
            }
        });

        PreviewView preview = findViewById(R.id.preview);
        preview.setAlpha(0f);
        preview.animate().setDuration(1000).withEndAction(new Runnable() {
            @Override
            public void run() {
                preview.animate().alpha(1f).setDuration(250).start();
                capture.animate().alpha(1f).setDuration(250).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        capture.setEnabled(true);
                    }
                }).start();
                front.animate().alpha(1f).setDuration(250).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        front.setEnabled(true);
                    }
                }).start();
            }
        }).start();

        SensorManager sensorManager = getSystemService(SensorManager.class);
        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }

            @Override
            public void onSensorChanged(SensorEvent event) {
                CameraService.instance.imageCapture.setTargetRotation((int) event.values[0]);
            }
        }, sensorManager.getDefaultSensor(27), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            startService(new Intent(this, CameraService.class));
    }

    public int[] getScreenResolutions() {
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
    public int gcd(int a, int b) {
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }
}