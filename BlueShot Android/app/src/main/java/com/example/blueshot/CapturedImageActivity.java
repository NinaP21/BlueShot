package com.example.blueshot;

import static com.example.blueshot.CameraActivity.rotatedBitmap;
import static com.example.blueshot.CameraActivity.savedPicture;
import static com.example.blueshot.MainActivity.uploadedPicture;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CapturedImageActivity extends AppCompatActivity {

    private Button rejectButton, acceptButton, previousButton, discardButton;
    private ImageView imageView;

    private final int DELAY = 2000; //milliseconds
    private Handler handler = new Handler();

    public static File bmpImageFolder;
    public static String bmpImageFileLocation;
    private String parentActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captured_image);

        imageView = findViewById(R.id.capturedImage);

        rejectButton = findViewById(R.id.rejectPicture);
        acceptButton = findViewById(R.id.acceptPicture);
        discardButton = findViewById(R.id.discardButton);

        rejectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideDecisionButtons();
                onBackPressed();
            }
        });

        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideDecisionButtons();
                saveBmpImage();
            }
        });

        previousButton = findViewById(R.id.backToCameraActivity);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        discardButton.setVisibility(View.GONE);
        discardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discardButton.setClickable(false);
                ObjectAnimator.ofFloat(discardButton, "alpha", 0f).setDuration(100).start();
                discardBitmap();
            }
        });
    }

    @Override
    protected void onStart() {
        Bitmap chosenImageBitmap = null;
        Bundle bundle = getIntent().getExtras();
        parentActivity = bundle.getString("parentActivity");

        if (bundle.getBoolean("showFinalPicture", false) && parentActivity.equals("MainActivity")) {
            acceptButton.setVisibility(View.GONE);
            rejectButton.setVisibility(View.GONE);
            discardButton.setVisibility(View.VISIBLE);
            if (savedPicture)
                chosenImageBitmap = BitmapFactory.decodeByteArray(CameraActivity.byteArray, 0, CameraActivity.byteArray.length);
            else if (uploadedPicture)
                chosenImageBitmap = BitmapFactory.decodeByteArray(MainActivity.uploadByteArray, 0, MainActivity.uploadByteArray.length);
        } else {
            showDecisionButtons();
            if (parentActivity.equals("MainActivity")) {
                chosenImageBitmap = BitmapFactory.decodeByteArray(MainActivity.uploadByteArray, 0, MainActivity.uploadByteArray.length);
            } else if (parentActivity.equals("CameraActivity")) {
                chosenImageBitmap = BitmapFactory.decodeByteArray(CameraActivity.byteArray, 0, CameraActivity.byteArray.length);
            }
        }
        imageView.setImageBitmap(chosenImageBitmap);
        //imageView.setScaleType(ImageView.ScaleType.FIT_XY);

        super.onStart();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_transition_slide_in_right, R.anim.activity_transition_slide_out_left);
    }

    public void discardBitmap() {
        File bmpFile = new File(bmpImageFileLocation);
        if (bmpFile.exists()) {
            if (bmpFile.delete()) {
                if (savedPicture) {
                    CameraActivity.byteArray = null;
                    savedPicture = false;
                }
                else if (uploadedPicture) {
                    MainActivity.uploadByteArray = null;
                    uploadedPicture = false;
                }
                showMessage("Picture successfully deleted");
            } else {
                showMessage("Your picture has already been deleted");

            }
        }
    }

    public void showDecisionButtons() {
        rejectButton.setClickable(true);
        acceptButton.setClickable(true);
        ObjectAnimator.ofFloat(acceptButton, "alpha", 1f).setDuration(200).start();
        ObjectAnimator.ofFloat(rejectButton, "alpha", 1f).setDuration(200).start();
    }

    public void hideDecisionButtons() {
        rejectButton.setClickable(false);
        acceptButton.setClickable(false);
        ObjectAnimator.ofFloat(acceptButton, "alpha", 0f).setDuration(100).start();
        ObjectAnimator.ofFloat(rejectButton, "alpha", 0f).setDuration(100).start();
    }

    public void saveBmpImage() {
        Bitmap currentBitmap;
        if (parentActivity.equals("CameraActivity")) {
            currentBitmap = rotatedBitmap;
        } else if (parentActivity.equals("MainActivity")) {
            currentBitmap = MainActivity.uploadedBitmap;
        } else {
            return ;
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        createImageFiles();
        try {
            String bmpFilePath = createBmpImageFile(timeStamp);

            if ( !AndroidBmpUtil.save(parentActivity, bmpFilePath) ) {
                Log.e("ERROR", "Saving picture in .bmp format failed");
                showMessage("Could not save image. Please try again");
            } else {
                Log.i("Captured Image state", "Saving picture in .bmp format succeeded");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        currentBitmap.recycle();

        if (parentActivity.equals("CameraActivity")) {
            savedPicture = true;
            uploadedPicture = false;
            showMessage("Picture saved successfully");
        } else if (parentActivity.equals("MainActivity")) {
            savedPicture = false;
            uploadedPicture = true;
            showMessage("Picture uploaded successfully");
        }
    }

    public String createBmpImageFile(String name) throws IOException {
        String imageFileName = "BMP_" + name + ".bmp";
        File imageFile = new File(bmpImageFolder, imageFileName);
        if ( !imageFile.createNewFile() ) {
            Log.e("ERROR", "BMP image file creation failed");
            return null;
        }

        bmpImageFileLocation = imageFile.getAbsolutePath();
        return imageFile.getAbsolutePath();
    }

    public void createImageFiles() {
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File appFolder = new File(storageDirectory, "BlueShot");

        if ( !appFolder.exists() ) {
            if ( !appFolder.mkdirs() ) {
                Log.e("Error", "in creating BlueShot folder");
            }
        }
        File appDirectory = appFolder.getAbsoluteFile();

        bmpImageFolder = new File(appDirectory, "BMP Images");
        if ( !bmpImageFolder.exists() ) {
            if ( !bmpImageFolder.mkdirs() ) {
                Log.e("Error", "in creating BMP Images folder");
            }
        }
    }

    private void showMessage(String message) {
        showSnackBar(message);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(CapturedImageActivity.this, MainActivity.class);
                startActivity(intent);
            }
        }, DELAY);
    }

    /**
     * Informs the user when something important has happened.
     * @param message the message shown to the user
     */
    public void showSnackBar(String message) {
        Snackbar.make(findViewById(R.id.CapturedImageActivityLayout), message, Snackbar.LENGTH_SHORT).show();
    }
}