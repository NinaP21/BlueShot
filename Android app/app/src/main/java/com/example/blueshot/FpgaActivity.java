package com.example.blueshot;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import java.util.List;


public class FpgaActivity extends AppCompatActivity {

    public static final int REQUEST_ENABLE_BLUETOOTH_CONNECT = 1;
    public static final int REQUEST_ENABLE_BLUETOOTH_SCAN = 2;
    public static final int REQUEST_FINE_LOC_ENABLE = 3;
    public static final int REQUEST_CHECK_SETTINGS = 4;

    public static long sendingTime = 0;

    public static boolean boundService = false;

    private final String TAG = "FpgaActivity";

    public static final String TRANSPARENT_UART_SERVICE = "49535343-fe7d-4ae5-8fa9-9fafd205e455";
    public static final String TRANSPARENT_UART_TX = "49535343-1e4d-4bd9-ba61-23c647249616";
    public static final String TRANSPARENT_UART_RX = "49535343-8841-43f4-a8d4-ecbe34729bb3";

    public static BluetoothGattCharacteristic transparentUartRx, transparentUartTx;
    public static BluetoothAdapter bluetoothAdapter;
    public static BluetoothManager bluetoothManager;

    private BluetoothLeService bluetoothService;
    private List<BluetoothGattCharacteristic> characteristics;
    public static boolean scanning = false;
    public static int connectionState;
    public static int progressStatus = 0;
    private long start = 0;

    private LocationManager locationManager;
    private LocationProvider locationProvider;

    private Button fpgaButton, previousButton, showButton, logsButton;
    private TextView description, progressTextView;
    private ProgressBar progressBar;
    private Toolbar toolbar;

    /**
     * Informs the user that the device's Bluetooth has been turned OFF.
     */
    private final BroadcastReceiver fpgaActivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_OFF) {
                    scanning = false;
                    connectionState = STATE_DISCONNECTED;
                    showSnackBar("Bluetooth is OFF");
                    Log.i("WARNING", "Bluetooth has been turned off. Any scanning or connection state has stopped.");
                }
            }
        }
    };

    /**
     * Gets executed when the device's Bluetooth is disabled so the application asks the user
     * to enable it. If the user agrees and they have previously chosen to start the measurements,
     * then the program begins the procedure to start location tracking.
     * However, if the user doesn't agree to enable Bluetooth, then the application informs them
     * that this is necessary for BlueShot's function.
     *
     */
    private ActivityResultLauncher<Intent> requestBluetoothPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_CANCELED) {
                        showSnackBar("Bluetooth access is necessary for the application");
                    } else
                        connectWithFPGA();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_fpga);

        overridePendingTransition(R.anim.activity_transition_slide_in_right, R.anim.activity_transition_slide_out_left);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationProvider = locationManager.getProvider(LocationManager.GPS_PROVIDER);

        toolbar = findViewById(R.id.toolbar_layout);
        setSupportActionBar(toolbar);

        String descriptionText = "Connect with your FPGA and let it edit your picture...";
        description = findViewById(R.id.description);
        description.setText(descriptionText);

        fpgaButton = findViewById(R.id.scanButton);
        fpgaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.hasAskedCamera = false;
                MainActivity.hasAskedUpload = false;
                if (CameraActivity.savedPicture || MainActivity.uploadedPicture) {
                    connectWithFPGA();
                }
            }
        });

        previousButton = findViewById(R.id.previousButton);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        showButton = findViewById(R.id.showButton);
        showButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FpgaActivity.this, EditedImage.class);
                startActivity(intent);
                overridePendingTransition(R.anim.activity_transition_slide_in_left, R.anim.activity_transition_slide_out_left);
            }
        });

        logsButton = findViewById(R.id.showLogs);
        logsButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(FpgaActivity.this, LogsActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.activity_transition_slide_in_left, R.anim.activity_transition_slide_out_left);
                    }
                }
        );

        progressBar = findViewById(R.id.progressBar);
        progressTextView = findViewById(R.id.progessTextView);
    }

    @Override
    protected void onStart() {
        //Log.i(TAG, "onStart");
        super.onStart();

        initialiseBle();

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(fpgaActivityReceiver, filter);
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());

        progressBar.setVisibility(View.GONE);
        progressTextView.setVisibility(View.GONE);
        showButton.setVisibility(View.GONE);
        logsButton.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        //Log.i(TAG, "onResume");
        super.onResume();
    }

    @Override
    protected void onPause() {
        //Log.i(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        //Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(gattUpdateReceiver);
        if (boundService)
            unbindService(serviceConnection);
        connectionState = STATE_DISCONNECTED;
        //Log.i(TAG, "onStop");
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(FpgaActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityIfNeeded(intent, 0);
    }


    public void setUpProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
        progressTextView.setVisibility(View.VISIBLE);
        progressTextView.setText("Scanning...");

        new Thread(new Runnable() {
            public void run() {
                while (progressStatus <= progressBar.getMax()) {
                    progressBar.setProgress(progressStatus);
                    if (progressStatus == progressBar.getMax()) {
                        Log.i("progress bar", "max value");
                        break;
                    }
                    // Update the progress bar and display the
                    //current value in the text view
                }
            }
        }).start();
    }

    public void connectWithFPGA() {
        if (!checkBlePermissions()) {
            getBlePermissions();
        } else {
            if (connectionState == STATE_CONNECTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {}
                unbindService(serviceConnection);
            }
            progressStatus = 0;
            setUpProgressBar();
            Intent gattServiceIntent = new Intent(FpgaActivity.this, BluetoothLeService.class);
            if (bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)) {
                boundService = true;
            }
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
        boolean bluetoothConnectNotGranted = (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED);
        boolean bluetoothScanNotGranted = (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED);
        switch (requestCode) {
            case REQUEST_FINE_LOC_ENABLE:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    showSnackBar("Precise location access is necessary for the application");
                } else if ((grantResults.length > 0) && (bluetoothConnectNotGranted)) {
                    requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_ENABLE_BLUETOOTH_CONNECT);
                } else if ((grantResults.length > 0) && (bluetoothScanNotGranted)) {
                    requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, REQUEST_ENABLE_BLUETOOTH_SCAN);
                } else if ((grantResults.length > 0) && !isGpsEnabled()) {
                    enableGps();
                } else if ((grantResults.length > 0) && (!bluetoothAdapter.isEnabled())) {
                    enableBluetooth();
                } else if ((grantResults.length > 0) && (!scanning))
                    connectWithFPGA();
                break;
            case REQUEST_ENABLE_BLUETOOTH_CONNECT:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    showSnackBar("Something wrong happened in Bluetooth access");
                } else if ((grantResults.length > 0) && (bluetoothScanNotGranted)) {
                    requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, REQUEST_ENABLE_BLUETOOTH_SCAN);
                } else if ((grantResults.length > 0) && !isGpsEnabled()) {
                    enableGps();
                } else if (!bluetoothAdapter.isEnabled()) {
                    enableBluetooth();
                } else if (!scanning)
                    connectWithFPGA();
                break;
            case REQUEST_ENABLE_BLUETOOTH_SCAN:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    showSnackBar("Something wrong happened in Bluetooth access");
                } else if ((grantResults.length > 0) && !isGpsEnabled()) {
                    enableGps();
                } else if (!bluetoothAdapter.isEnabled()) {
                    enableBluetooth();
                } else if (!scanning)
                    connectWithFPGA();
                break;
        }
    }

    /**
     * Checks if the application has access to the device's approximate location and if the Bluetooth is enabled.
     * @return true if all permissions are granted
     */
    public boolean checkBlePermissions() {
        return ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) &&
                (bluetoothAdapter.isEnabled()) &&
                isGpsEnabled());
    }

    public void enableBluetooth() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        requestBluetoothPermissionLauncher.launch(intent);
    }

    /**
     * Makes sure that firstly the user will be asked to give the application access to the device's location
     * and then to check whether the Bluetooth is ON or OFF.
     */
    public void getBlePermissions() {
        boolean bluetoothConnectNotGranted = (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED);
        boolean bluetoothScanNotGranted = (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED);
        boolean coarseLocationNotGranted = (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED);

        if (coarseLocationNotGranted) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOC_ENABLE);
        } else if (bluetoothConnectNotGranted) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_ENABLE_BLUETOOTH_CONNECT);
        } else if (bluetoothScanNotGranted) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, REQUEST_ENABLE_BLUETOOTH_SCAN);
        } else if (!isGpsEnabled()) {
            enableGps();
        } else if (!bluetoothAdapter.isEnabled()) {
            enableBluetooth();
        }
    }

    public void enableGps() {

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000) // 10 seconds
                .setFastestInterval(5000);

        LocationSettingsRequest.Builder locationRequestBuilder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        locationRequestBuilder.setNeedBle(false);
        locationRequestBuilder.setAlwaysShow(true);

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> locationTask = settingsClient.checkLocationSettings(locationRequestBuilder.build());

        /*
        When the Task completes, your app can check the location settings by looking
        at the status code from the LocationSettingsResponse object.
        To get even more details about the current state of the relevant location settings,
        your app can call the LocationSettingsResponse object's getLocationSettingsStates() method.
         */

        locationTask.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(FpgaActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        Log.i("Exception thrown ", sendEx.toString());
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                if (!bluetoothAdapter.isEnabled()) {
                    enableBluetooth();
                } else if (!scanning) {
                    connectWithFPGA();
                }
            } else {
                showSnackBar("Please enable your GPS");
            }
        }
    }

    public boolean isGpsEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * Informs the user when something important has happened.
     * @param message the message shown to the user
     */
    public void showSnackBar(String message) {
        Snackbar.make(findViewById(R.id.fpgaActivityLayout), message, Snackbar.LENGTH_SHORT).show();
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bluetoothService = ((BluetoothLeService.LocalBinder) service).getService();

            if (bluetoothService != null) {
                // call functions on service to check connection and connect to devices
                if (bluetoothService.initialize()) {
                    Log.i("Error", "Unable to initialize Bluetooth");
                    finish();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i("service", " disconnected");
            bluetoothService = null;
        }
    };

    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                connectionState = STATE_CONNECTED;
                progressStatus = 10;
                progressTextView.setText("Connecting to FPGA...");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                connectionState = STATE_DISCONNECTED;
                progressStatus = 0;
                progressTextView.setText("Error...");
                progressBar.setIndeterminate(false);
                progressBar.setProgressDrawable(getDrawable(R.drawable.progress_bar));
                showSnackBar("BLE has stopped working. Please try again.");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                if (bluetoothService != null) {
                    try {
                        readServices(bluetoothService.getSupportedGattServices());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else if (BluetoothLeService.ACTION_SENT_PHOTO.equals(action)) {
                sendingTime = (System.currentTimeMillis() - start)/1000;
                Log.i(TAG , "sending picture lasted " + sendingTime + " sec");
                progressStatus = 60;
                progressTextView.setText("Receiving edited photo...");
                Log.i("DEBUG", "ACTION_SENT_PHOTO received");
            } else if (BluetoothLeService.ACTION_RECEIVED_PHOTO.equals(action)) {
                progressStatus = 100;
                progressTextView.setText("Done");
                progressBar.setIndeterminate(false);
                progressBar.setProgressDrawable(getDrawable(R.drawable.progress_bar));
                showButton.setVisibility(View.VISIBLE);
                logsButton.setVisibility(View.VISIBLE);
                Log.i("DEBUG", "ACTION_RECEIVED_PHOTO received");
            } else if (BluetoothLeService.ACTION_SCANNING_RESTARTED.equals(action)) {
                setUpProgressBar();
                showSnackBar("Connection failed. BlueShot is trying again");
                Log.i("DEBUG", "ACTION_SCANNING_RESTARTED received");
            } else if (BluetoothLeService.ACTION_SCANNING_FAILED.equals(action)) {
                progressBar.setVisibility(View.GONE);
                progressTextView.setVisibility(View.GONE);
                showSnackBar("Scanning failed. Please restart your Bluetooth");
                Log.i("DEBUG", "ACTION_SCANNING_FAILED received");
            }
        }
    };

    public void readServices(List<BluetoothGattService> services) throws InterruptedException {
        if (services.isEmpty()) {
            Log.i("Services:   ", "No services to show");
        } else {
            for (int i = 0; i < services.size(); i++) {
                String uuid = services.get(i).getUuid().toString();
                if (!uuid.equals(TRANSPARENT_UART_SERVICE)) {
                    continue;
                }

                characteristics = bluetoothService.getSupportedGattCharacteristics(services.get(i));
                Log.i("Microchip Transparent UART service", " has " + characteristics.size() + " characteristics.");

                if (ActivityCompat.checkSelfPermission(FpgaActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {}
                int j = 0;
                while (j < characteristics.size()) {
                        if (ActivityCompat.checkSelfPermission(FpgaActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {}
                        readCharacteristic(characteristics.get(j));
                        j++;
                }
                progressTextView.setText("Sending photo...");
                progressStatus = 40;
            }
        }
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) throws InterruptedException {

        if (!characteristic.getUuid().toString().equals(TRANSPARENT_UART_TX) &&
                !characteristic.getUuid().toString().equals(TRANSPARENT_UART_RX)) {
            return ;
        }

        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            bluetoothService.setCharacteristicNotification(characteristic, true);
        }

        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
            //bluetoothService.setCharacteristicIndication(characteristic, true);
        }

        if (characteristic.getUuid().toString().equals(TRANSPARENT_UART_TX)) {
            transparentUartTx = characteristic;
            Log.i("Microchip Transparent UART TX", "has just been found");
        } else if (characteristic.getUuid().toString().equals(TRANSPARENT_UART_RX)) {
            transparentUartRx = characteristic;
            Log.i("Microchip Transparent UART RX", "has just been found");
            try {
                Thread.sleep(350);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            bluetoothService.writeCharacteristic(transparentUartRx);
            Log.i("Image transfer state", "Started");
            start = System.currentTimeMillis();
        }
    }

    /**
     * Sets up Bluetooth API variables for device's Bluetooth management
     */
    public void initialiseBle() {
        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CHARACTERISTICS_READ);
        intentFilter.addAction(BluetoothLeService.ACTION_SENT_PHOTO);
        intentFilter.addAction(BluetoothLeService.ACTION_RECEIVED_PHOTO);
        intentFilter.addAction(BluetoothLeService.ACTION_SCANNING_RESTARTED);
        intentFilter.addAction(BluetoothLeService.ACTION_SCANNING_FAILED);
        return intentFilter;
    }
}