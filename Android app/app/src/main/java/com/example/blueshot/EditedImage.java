package com.example.blueshot;

import static com.example.blueshot.BluetoothLeService.byteFilePath;

import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class EditedImage extends AppCompatActivity {

    ImageView editedImageView;
    Button previousButton;
    Button beforeAfterButton;
    Bitmap editedImageBitmap, originalImageBitmap;
    TextView imageSubtitle;

    private boolean showingBefore = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edited_image);

        editedImageView = (ImageView) findViewById(R.id.editedImage);
        imageSubtitle = findViewById(R.id.pictureSubtitle);
        beforeAfterButton = findViewById(R.id.afterButton);

        editedImageBitmap = BitmapFactory.decodeFile(byteFilePath);
        originalImageBitmap = BitmapFactory.decodeFile(CapturedImageActivity.bmpImageFileLocation);
        editedImageView.setImageBitmap(originalImageBitmap);
        //editedImageView.setScaleType(ImageView.ScaleType.FIT_XY);

        setUpDescription();

        previousButton = findViewById(R.id.backToFpgaActivity);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        beforeAfterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setUpDescription();
            }
        });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_transition_slide_in_right, R.anim.activity_transition_slide_out_left);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void setUpDescription()
    {
        if (!showingBefore)
        {
            imageSubtitle.setText(R.string.afterSubtitle);
            beforeAfterButton.setText(R.string.before);
            editedImageView.setImageBitmap(editedImageBitmap);
            beforeAfterButton.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.previous), null, null, null);
            showingBefore = true;
        } else
        {
            imageSubtitle.setText(R.string.beforeSubtitle);
            beforeAfterButton.setText(R.string.after);
            editedImageView.setImageBitmap(originalImageBitmap);
            beforeAfterButton.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable(R.drawable.next), null);
            showingBefore = false;
        }
    }
}