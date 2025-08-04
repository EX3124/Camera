package com.camera;

import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;

public class MainActivity extends AppCompatActivity {
    public static MainActivity instance;

    AppCompatActivity activity;
    Uri LatestUri;
    int SensorRotation;
    String action;
    
    private boolean AllowCapture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.main);

        instance = this;
        activity = this;
        action = getIntent().getAction();

        ActivityCompat.requestPermissions(this, new String[]{"android.permission.CAMERA"}, 0);

        if (ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") == PackageManager.PERMISSION_GRANTED)
            startService(new Intent(this, CameraService.class));

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

        ImageButton capture = findViewById(R.id.capture);
        capture.setEnabled(false);
        capture.setAlpha(0f);
        capture.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    AllowCapture = true;
                    v.setBackground(getDrawable(R.drawable.capture_press));
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                } else if (event.getAction() == MotionEvent.ACTION_UP && AllowCapture) {
                    AllowCapture = false;
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
                front.setEnabled(false);
                CameraService.instance.capture();
                v.announceForAccessibility(getString(R.string.talkback_captured));
            }
        });

        FrameLayout gallery = findViewById(R.id.gallery);
        gallery.setEnabled(false);
        gallery.setAlpha(0f);
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent().setAction("com.android.camera.action.REVIEW").setDataAndType(LatestUri,"image/png"));
            }
        });

        try (Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Images.Media._ID}, MediaStore.Images.Media.RELATIVE_PATH + "=? AND " + MediaStore.Images.Media.MIME_TYPE + "=?", new String[]{"DCIM/Camera/", "image/png"}, MediaStore.Images.Media.DATE_ADDED + " DESC")) {
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                LatestUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
            }
        } catch (Throwable ignored) { }

        PreviewView viewfinder = findViewById(R.id.viewfinder);
        viewfinder.setAlpha(0f);
        viewfinder.animate().setDuration(1000).withEndAction(new Runnable() {
            @Override
            public void run() {
                viewfinder.animate().alpha(1f).setDuration(250).start();
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
                if (LatestUri != null) {
                    ImageView thumbnail = findViewById(R.id.thumbnail);
                    Glide.with(activity).load(LatestUri).apply(RequestOptions.bitmapTransform(new CircleCrop())).into(thumbnail);
                    gallery.animate().alpha(1f).setDuration(250).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            gallery.setEnabled(true);
                        }
                    }).start();
                }
            }
        }).start();

        FrameLayout direction = findViewById(R.id.direction);
        ConstraintLayout.LayoutParams DirectionLayout = (ConstraintLayout.LayoutParams) direction.getLayoutParams();
        direction.setAlpha(0f);
        direction.setScaleX(0.6f);
        direction.setScaleY(0.6f);

        ImageButton confirm = findViewById(R.id.confirm);
        confirm.setVisibility(View.GONE);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                setResult(RESULT_OK, new Intent().setData(LatestUri));
                stopService(new Intent(activity, CameraService.class));
                finish();
            }
        });

        ImageButton retake = findViewById(R.id.retake);
        retake.setVisibility(View.GONE);
        retake.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                direction.setVisibility(View.VISIBLE);
                capture.setVisibility(View.VISIBLE);
                front.setVisibility(View.VISIBLE);
                viewfinder.setVisibility(View.VISIBLE);
                findViewById(R.id.preview).setVisibility(View.GONE);
                confirm.setVisibility(View.GONE);
                v.setVisibility(View.GONE);
            }
        });

        SensorManager sensorManager = getSystemService(SensorManager.class);
        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) { }
            @Override
            public void onSensorChanged(SensorEvent event) {

                DirectionLayout.topMargin = 0;
                DirectionLayout.leftMargin = 0;
                DirectionLayout.rightMargin = 0;
                DirectionLayout.bottomMargin = 0;
                DirectionLayout.topToTop = ConstraintLayout.LayoutParams.UNSET;
                DirectionLayout.startToStart = ConstraintLayout.LayoutParams.UNSET;
                DirectionLayout.endToEnd = ConstraintLayout.LayoutParams.UNSET;
                DirectionLayout.bottomToBottom = ConstraintLayout.LayoutParams.UNSET;
                DirectionLayout.bottomToTop = ConstraintLayout.LayoutParams.UNSET;

                if (event.values[0] == 1f) {
                    front.animate().rotation(90f).setDuration(200).start();
                    gallery.animate().rotation(90f).setDuration(200).start();
                    confirm.animate().rotation(90f).setDuration(200).start();
                    retake.animate().rotation(90f).setDuration(200).start();
                    direction.setRotation(90f);
                    DirectionLayout.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
                    DirectionLayout.bottomToTop = R.id.capture;
                    DirectionLayout.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
                    SensorRotation = 1;
                } else if (event.values[0] == 2f) {
                    front.animate().rotation(180f).setDuration(200).start();
                    gallery.animate().rotation(180f).setDuration(200).start();
                    confirm.animate().rotation(180f).setDuration(200).start();
                    retake.animate().rotation(180f).setDuration(200).start();
                    direction.setRotation(180f);
                    DirectionLayout.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                    DirectionLayout.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
                    DirectionLayout.bottomToTop = R.id.capture;
                    DirectionLayout.bottomMargin = 240;
                    SensorRotation = 2;
                } else if (event.values[0] == 3f) {
                    if (SensorRotation == 0) {
                        front.setRotation(360f);
                        gallery.setRotation(360f);
                        confirm.setRotation(360f);
                        retake.setRotation(360f);
                    }
                    front.animate().rotation(270f).setDuration(200).start();
                    gallery.animate().rotation(270f).setDuration(200).start();
                    confirm.animate().rotation(270f).setDuration(200).start();
                    retake.animate().rotation(270f).setDuration(200).start();
                    direction.setRotation(270f);
                    DirectionLayout.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
                    DirectionLayout.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                    DirectionLayout.bottomToTop = R.id.capture;
                    SensorRotation = 3;
                } else {
                    if (SensorRotation == 3) {
                        front.animate().rotation(360f).setDuration(200).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                front.setRotation(0f);
                            }
                        }).start();
                        gallery.animate().rotation(360f).setDuration(200).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                gallery.setRotation(0f);
                            }
                        }).start();
                        confirm.animate().rotation(360f).setDuration(200).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                confirm.setRotation(0f);
                            }
                        }).start();
                        retake.animate().rotation(360f).setDuration(200).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                retake.setRotation(0f);
                            }
                        }).start();
                    } else {
                        front.animate().rotation(0f).setDuration(200).start();
                        gallery.animate().rotation(0f).setDuration(200).start();
                        confirm.animate().rotation(0f).setDuration(200).start();
                        retake.animate().rotation(0f).setDuration(200).start();
                    }
                    direction.setRotation(0f);
                    DirectionLayout.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
                    DirectionLayout.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                    DirectionLayout.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
                    DirectionLayout.topMargin = 240;
                    SensorRotation = 0;
                }

                direction.setAlpha(0f);
                direction.setScaleX(0.6f);
                direction.setScaleY(0.6f);
                direction.animate().cancel();
                direction.setLayoutParams(DirectionLayout);

                ImageView arrow = findViewById(R.id.arrow);
                arrow.setTranslationY(0f);
                arrow.setRotationY(0f);
                arrow.animate().cancel();

                if (viewfinder.getAlpha() == 1f)
                    direction.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        arrow.animate().setDuration(200).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                arrow.animate().yBy(-10f).rotationY(180f).setDuration(500).withEndAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        arrow.animate().yBy(20f).setDuration(1000).withEndAction(new Runnable() {
                                            @Override
                                            public void run() {
                                                arrow.animate().yBy(-20f).rotationY(360f).setDuration(1000).withEndAction(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        arrow.animate().yBy(20f).setDuration(1000).withEndAction(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                arrow.animate().yBy(-20f).rotationY(540f).setDuration(500).withEndAction(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        arrow.animate().yBy(10f).setDuration(1000).withEndAction(new Runnable() {
                                                                            @Override
                                                                            public void run() {
                                                                                arrow.animate().setDuration(1000).withEndAction(new Runnable() {
                                                                                    @Override
                                                                                    public void run() {
                                                                                        direction.animate().alpha(0f).scaleX(0.6f).scaleY(0.6f).setDuration(200).start();
                                                                                    }
                                                                                }).start();
                                                                            }
                                                                        }).start();
                                                                    }
                                                                }).start();
                                                            }
                                                        }).start();
                                                    }
                                                }).start();
                                            }
                                        }).start();
                                    }
                                }).start();
                            }
                        }).start();
                    }
                }).start();
            }
        }, sensorManager.getDefaultSensor(27), SensorManager.SENSOR_DELAY_NORMAL);
    }
    @Override
    protected void onResume() {
        super.onResume();
        try (Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Images.Media._ID}, MediaStore.Images.Media.RELATIVE_PATH + "=? AND " + MediaStore.Images.Media.MIME_TYPE + "=?", new String[]{"DCIM/Camera/", "image/png"}, MediaStore.Images.Media.DATE_ADDED + " DESC")) {
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                LatestUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                ImageView thumbnail = findViewById(R.id.thumbnail);
                Glide.with(activity).load(LatestUri).apply(RequestOptions.bitmapTransform(new CircleCrop())).into(thumbnail);
            } else {
                FrameLayout gallery = findViewById(R.id.gallery);
                gallery.setEnabled(false);
                gallery.setAlpha(0f);
            }
        } catch (Throwable ignored) { }
        if (!action.equals("android.intent.action.MAIN")) {
            LatestUri = null;
            findViewById(R.id.gallery).setVisibility(View.GONE);
        }
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