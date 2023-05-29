package com.example.blueshot;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CameraActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private Button cameraButton;
    private TextureView cameraView;

    public static boolean savedPicture = false;
    public static byte[] byteArray;
    public static byte[] headerByteArray;
    public static byte[] imageArray;
    private final String TAG = "CameraActivity";

    private CameraCharacteristics cameraCharacteristics;
    private CameraManager cameraManager;
    private ImageReader imageReader;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder previewCaptureRequestBuilder;
    private CameraCaptureSession cameraCaptureSession;
    private Size[] imageDimensions;
    private String cameraId;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private Image image;
    public static Bitmap rotatedBitmap;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    public static boolean checkSDK() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }

    private CameraDevice.StateCallback cameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened( CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
            Log.i("Camera state", "opened");
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            cameraDevice = null;
            Log.i("Camera state", "closed");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
            Log.i("Camera state", "error");
        }
    };

    private void createCameraPreview() {
        try {
            SurfaceTexture surfaceTexture = cameraView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(imageDimensions[0].getWidth(), imageDimensions[0].getHeight());

            Surface previewSurface = new Surface(surfaceTexture);

            previewCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewCaptureRequestBuilder.addTarget(previewSurface);

            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            if(cameraDevice == null) {
                                return;
                            }
                            try {
                                cameraCaptureSession = session;
                                previewCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                                cameraCaptureSession.setRepeatingRequest(previewCaptureRequestBuilder.build(), null, backgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            Log.e("ERROR", "CameraCaptureSession configuration failed");
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            setupCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) { }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) { }
    };


    private ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @RequiresApi(api = Build.VERSION_CODES.Q)
        @Override
        public void onImageAvailable(ImageReader reader) {
            image = reader.acquireLatestImage();
            try {
                showCapturedImage();
            } catch (CameraAccessException | IOException e) {
                e.printStackTrace();
            }
        }
    };

    private void showCapturedImage() throws CameraAccessException, IOException {
        ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();

        byteBuffer.clear();
        byte[] bytes = new byte[byteBuffer.capacity()];
        //Log.i(TAG, "initial size of captured image is " + byteBuffer.capacity() + " bytes");
        byteBuffer.get(bytes);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 8;
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

        int orientation = getJpegOrientation(CameraActivity.this.getWindowManager().getDefaultDisplay().getRotation());
        int rotation = ORIENTATIONS.get(orientation);

        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);
        rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);

        if (AndroidBmpUtil.createBMPImage(rotatedBitmap, "CameraActivity")) {
            Intent i = new Intent(this, CapturedImageActivity.class);
            i.putExtra("parentActivity", "CameraActivity");
            startActivity(i);
        }

        bitmap.recycle();
        scaledBitmap.recycle();
    }

    private void setupCamera() {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            for (String mCameraId : cameraManager.getCameraIdList()) {
                cameraCharacteristics = cameraManager.getCameraCharacteristics(mCameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = mCameraId;
                    openCamera();
                    break;
                }
            }

            StreamConfigurationMap configs = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            imageDimensions = configs.getOutputSizes(ImageFormat.JPEG);
            imageReader = ImageReader.newInstance(imageDimensions[0].getWidth(), imageDimensions[0].getHeight(), ImageFormat.JPEG, 1);
            imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        try {
            cameraButton.setClickable(true);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {}
            cameraManager.openCamera(cameraId, cameraDeviceStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home ){
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.i(TAG, "onCreate");

        setContentView(R.layout.activity_camera);

        toolbar = findViewById(R.id.toolbar_layout);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        cameraView = findViewById(R.id.cameraView);
        cameraView.setSurfaceTextureListener(surfaceTextureListener);

        cameraButton = findViewById(R.id.capturePicture);
        cameraButton.setClickable(false);
        cameraButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            cameraButton.setClickable(false);
                            ObjectAnimator.ofFloat(cameraButton, "alpha", 0f).setDuration(150).start();
                            captureImage();
                        } catch (CameraAccessException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    @Override
    protected void onStart() {
        //Log.i(TAG, "onStart");
        cameraButton.setClickable(true);
        super.onStart();
    }

    @Override
    protected void onResume() {
        startBackgroundThread();
        if (cameraView.isAvailable()) {
            ObjectAnimator.ofFloat(cameraButton, "alpha", 1f).setDuration(150).start();
            cameraButton.setClickable(true);
            setupCamera();
        } else {
            cameraView.setSurfaceTextureListener(surfaceTextureListener);
        }
        //Log.i(TAG, "onResume");
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (cameraDevice != null) {
            cameraDevice.close();
        }
        try {
            stopBackgroundThread();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //Log.i(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        //Log.i(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        //Log.i(TAG, "onDestroy");
        savedPicture = false;
        super.onDestroy();
    }

    protected void startBackgroundThread() {
        backgroundThread = new HandlerThread("Camera Background");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    protected void stopBackgroundThread() throws InterruptedException {
        backgroundThread.quitSafely();
        backgroundThread.join();
        backgroundThread = null;
        backgroundHandler = null;
    }

    private void captureImage() throws CameraAccessException, InterruptedException {
        if (cameraDevice == null) {
            Log.e(TAG, "Camera device is null");
            onBackPressed();
        }

        List<Surface> outputSurfaces = new ArrayList<>(2);
        outputSurfaces.add(imageReader.getSurface());
        outputSurfaces.add(new Surface(cameraView.getSurfaceTexture()));

        CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        captureRequestBuilder.addTarget(imageReader.getSurface());

        final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
            }
        };

        cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.capture(captureRequestBuilder.build(), captureListener, backgroundHandler);
                        //session.close();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                }
            }, backgroundHandler);

    }

    private int getJpegOrientation(int deviceOrientation) {
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) return 0;
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        return  (sensorOrientation + deviceOrientation + 270) % 360;
    }
}