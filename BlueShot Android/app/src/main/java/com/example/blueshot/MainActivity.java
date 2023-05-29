package com.example.blueshot;

import static android.os.Build.VERSION.SDK_INT;

import static com.example.blueshot.CapturedImageActivity.bmpImageFileLocation;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.google.android.material.snackbar.Snackbar;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";
    private final int SELECT_PHOTO = 1;
    public static final int REQUEST_CAMERA_ENABLE = 5;
    public static final int REQUEST_STORE_ENABLE = 6;
    public static final int REQUEST_READ_ENABLE = 7;

    public static boolean hasAskedCamera = false;
    public static boolean hasAskedUpload = false;
    private boolean doubleBackToExitPressedOnce = false;

    private Uri uri;
    public static Bitmap uploadedBitmap;
    public static Bitmap scaledBitmap;
    public static byte[] uploadByteArray;
    public static boolean uploadedPicture = false;


    private Toolbar toolbar;
    private TextView description, panel;
    private Button cameraButton, uploadButton, nextButton, showButton;

    private ActivityResultLauncher<Intent> requestStoragePermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (SDK_INT >= Build.VERSION_CODES.R) {
                        if (Environment.isExternalStorageManager()) {
                            showSnackBar("Write permission is ok");
                        } else {
                            showSnackBar("External storage access is necessary for the application");
                        }
                    }
                }
            });

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            finishAffinity();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        showSnackBar("Please click BACK again to exit");

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.i(TAG, "onCreate");

        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar_layout);
        setSupportActionBar(toolbar);

        String descriptionText = "Connect with your FPGA and let it edit your picture...";
        description = findViewById(R.id.description);
        description.setText(descriptionText);

        hasAskedCamera = false;
        hasAskedUpload = false;

        cameraButton = findViewById(R.id.cameraButton);
        uploadButton = findViewById(R.id.uploadButton);
        nextButton = findViewById(R.id.nextButton);

        panel = findViewById(R.id.panel);

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hasAskedCamera = true;
                hasAskedUpload = false;
                goToCameraActivity();
            }
        });

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hasAskedUpload = true;
                hasAskedCamera = false;
                goToUploadOption();
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FpgaActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.activity_transition_slide_in_right, R.anim.activity_transition_slide_out_left);
            }
        });

        showButton = findViewById(R.id.showChosenPictureButton);
        showButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, CapturedImageActivity.class);
                i.putExtra("parentActivity", "MainActivity");
                i.putExtra("showFinalPicture", true);
                startActivity(i);
                overridePendingTransition(R.anim.activity_transition_slide_in_left, R.anim.activity_transition_slide_out_left);
            }
        });
    }

    @Override
    protected void onStart() {
        updateLayout();
        //Log.i(TAG, "onStart");
        super.onStart();
    }

    @Override
    protected void onResume() {
        updateLayout();
        //Log.i(TAG, "onResume");
        super.onResume();
    }

    @Override
    protected void onPause() {
        //Log.i(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        hasAskedCamera = false;
        hasAskedUpload = false;
        //Log.i(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        uploadedPicture = false;
        //Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_PHOTO && resultCode == RESULT_OK && data != null && data.getData() != null) {
            uri = data.getData();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 8;
            try {
                uploadedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                showImage(uploadedBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (requestCode == SELECT_PHOTO && resultCode == RESULT_CANCELED) {

        }
    }

    private void showImage(Bitmap bitmap) throws IOException {
        scaledBitmap = Bitmap.createScaledBitmap(bitmap, 324, 576, true);

        if (AndroidBmpUtil.createBMPImage(scaledBitmap, "MainActivity")) {
            Intent i = new Intent(this, CapturedImageActivity.class);
            i.putExtra("parentActivity", "MainActivity");
            startActivity(i);
        }
        //bitmap.recycle();
    }


    public void updateLayout() {
        if (CameraActivity.savedPicture || uploadedPicture) {
            File bmpFile = new File(bmpImageFileLocation);
            if (bmpFile.exists()) {
                panel.setText("Your picture is ready to get edited");
                panel.setVisibility(View.VISIBLE);
                nextButton.setVisibility(View.VISIBLE);
                showButton.setVisibility(View.VISIBLE);
                return;
            }
        }
        panel.setVisibility(View.GONE);
        nextButton.setVisibility(View.GONE);
        showButton.setVisibility(View.GONE);
    }

    public void goToUploadOption() {
        if (!checkUploadPermissions()) {
            getUploadPermissions();
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK);
            //Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PHOTO);
        }
    }

    public boolean checkUploadPermissions() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            return ((ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED));
        } else {
            return ((ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &&
                    (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED));
        }
    }

    public void getUploadPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_ENABLE);
        } else if (SDK_INT < Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORE_ENABLE);
            }
        }
    }

    public void goToCameraActivity() {
        if (!checkCameraPermissions()) {
            getCameraPermissions();
        } else {
            Intent intent = new Intent(this, CameraActivity.class);
            startActivity(intent);
        }
    }

    public void getCameraPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_ENABLE);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_ENABLE);
        } else if (SDK_INT < Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORE_ENABLE);
            }
        }
    }

    public boolean checkCameraPermissions() {
       if (SDK_INT >= Build.VERSION_CODES.R) {
            return ((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) &&
                    (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED));
        } else {
            return ((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) &&
                    (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &&
                    (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED));
       }
    }

    /**
     * Checks if the user has accepted to provide the application with location access.
     * If this permission was not granted, it informs the user that this is a necessary feature.
     * Otherwise, it goes on and makes sure that Bluetooth is enabled.
     * If all requirements are granted, it starts measurements.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CAMERA_ENABLE:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    showSnackBar("Camera access is necessary for the application");
                } else {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_ENABLE);
                    } else {
                        if (hasAskedUpload) {
                            goToUploadOption();
                        } else if (hasAskedCamera) {
                            goToCameraActivity();
                        }
                    }
                }
                break;
            case REQUEST_STORE_ENABLE:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    showSnackBar("Storage access is necessary for saving the picture");
                } else {
                    if (hasAskedUpload) {
                        goToUploadOption();
                    } else if (hasAskedCamera) {
                        goToCameraActivity();
                    }
                }
                break;
            case REQUEST_READ_ENABLE:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    showSnackBar("Storage access is necessary for the application");
                } else {
                    if (hasAskedUpload) {
                        goToUploadOption();
                    } else if (hasAskedCamera) {
                        goToCameraActivity();
                    }
                }
                break;
        }
    }

    /**
     * Informs the user when something important has happened.
     * @param message the message shown to the user
     */
    public void showSnackBar(String message) {
        Snackbar.make(findViewById(R.id.mainActivityLayout), message, Snackbar.LENGTH_SHORT).show();
    }
}