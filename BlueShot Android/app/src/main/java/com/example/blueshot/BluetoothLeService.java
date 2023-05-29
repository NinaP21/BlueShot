package com.example.blueshot;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

public class BluetoothLeService extends Service {

    public final static String ACTION_GATT_CONNECTED =
            "com.example.blueshot.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.blueshot.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.blueshot.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_GATT_CHARACTERISTICS_READ =
            "com.example.blueshot.ACTION_GATT_CHARACTERISTICS_READ";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.blueshot.ACTION_DATA_AVAILABLE";
    public final static String ACTION_SENT_PHOTO =
            "com.example.blueshot.ACTION_SENT_PHOTO";
    public final static String ACTION_RECEIVED_PHOTO =
            "com.example.blueshot.ACTION_RECEIVED_PHOTO";
    public final static String ACTION_SCANNING_RESTARTED =
            "com.example.blueshot.ACTION_SCANNING_RESTARTED";
    public final static String ACTION_SCANNING_FAILED =
            "com.example.blueshot.ACTION_SCANNING_FAILED";

    public static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static final String FPGA_ADDRESS = "80:1F:12:B6:BA:96";

    private final String TAG = "BluetoothLeService";

    //public static final int FILE_SIZE = CameraActivity.byteArray.length;

    public static final int IMAGE_SIZE = AndroidBmpUtil.IMAGE_WIDTH * AndroidBmpUtil.IMAGE_HEIGHT;
    public static final int HEADER_SIZE = 1078;

    public static BluetoothAdapter bluetoothAdapter;
    public static BluetoothManager bluetoothManager;
    public static BluetoothLeScanner bluetoothLeScanner;
    public static BluetoothGatt bluetoothGatt;
    private boolean firstWrite = true;
    private boolean done = false;

    private ByteBuffer buffer;
    private File appFolder;
    public static String byteFilePath;
    public static File byteFile;

    public static int totalReceivedBytes = 0;

    public static int CURRENT_MTU_SIZE;
    public static long recvTime = 0;

    private Queue<BluetoothGattCharacteristic> characteristicsQueue = new LinkedList<>();
    private Queue<byte[]> valueQueue = new LinkedList<>();
    private boolean recvFirstTime = true;
    private long startRecv = 0;
    // Stops scanning after 1 minute.
    private static final long SCAN_PERIOD = 60000;
    private static final long DELAY = 350;
    private Handler handler = new Handler();
    private Binder binder = new LocalBinder();

    private ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
                    super.onScanResult(callbackType, result);

                    if (ActivityCompat.checkSelfPermission(BluetoothLeService.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    }
                    //String name = result.getDevice().getName();
                    String address = result.getDevice().getAddress();
                    if (address.equals(FPGA_ADDRESS)) {
                        stopScanning();
                        connect(FPGA_ADDRESS);
                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                    broadcastUpdate(ACTION_SCANNING_FAILED);
                    Log.i("Scan Error Code", "    " + errorCode);
                    if (errorCode == SCAN_FAILED_APPLICATION_REGISTRATION_FAILED) {
                        Log.i("Error", "Please restart your Bluetooth");
                    }
                }
            };

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            if (recvFirstTime)
            {
                Log.i("Image receiving state", "Started");
                startRecv = System.currentTimeMillis();
                recvFirstTime = false;
            }


            totalReceivedBytes = totalReceivedBytes + characteristic.getValue().length;
            //Log.i("total received bytes", " " + totalReceivedBytes);
            if (totalReceivedBytes >= IMAGE_SIZE && !done) {
                int lastLength = totalReceivedBytes - IMAGE_SIZE;
                int lastIndex = characteristic.getValue().length - lastLength - 1;
                Log.i("lastlength is ", " " + lastLength);
                recvTime = (System.currentTimeMillis() - startRecv)/1000;
                buffer.put(characteristic.getValue(), 0, lastIndex);
                try {
                    byteFilePath = createByteFile();
                    FileOutputStream fos = new FileOutputStream(byteFilePath);
                    fos.write(buffer.array());
                    fos.close();
                    done = true;
                    Log.i("File with received bytes", "completed");
                    broadcastUpdate(ACTION_RECEIVED_PHOTO);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else if (!done) {
                buffer.put(characteristic.getValue());
            }
            //Log.i("total received bytes", "  " + totalReceivedBytes);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("Graftike o descriptor", " " + descriptor.getUuid().toString());
            } else if (status == BluetoothGatt.GATT_FAILURE) {
                Log.i("DEN Graftike o descriptor", " " + descriptor.getUuid().toString());
            }
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyRead(gatt, txPhy, rxPhy, status);
            Log.i("transmitter phy is", " " + txPhy);
            Log.i("receiver phy is", " " + rxPhy);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    // successfully connected to the GATT Server
                    bluetoothGatt = gatt;
                    broadcastUpdate(ACTION_GATT_CONNECTED);
                    Log.i("Connection State", "Connected");
                    if (ActivityCompat.checkSelfPermission(BluetoothLeService.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    }
                    int GATT_MAX_MTU_SIZE = 160;
                    gatt.requestMtu(GATT_MAX_MTU_SIZE);
                    try {
                        Thread.sleep(DELAY);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    gatt.discoverServices();
                    try {
                        Thread.sleep(DELAY);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // disconnected from the GATT Server
                    Log.i("Connection State", "Disconnected");
                    broadcastUpdate(ACTION_GATT_DISCONNECTED);
                    gatt.close();
                }
            } else {
                if (status == 113) {
                    Log.i("Connection status", "Error, Time out!");
                }
                if (status == 19) {
                    Log.i("Connection status", "Device has stopped connection on purpose");
                }
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
                Log.i("Connection State", "Disconnected. Device has stopped connection itself");
                gatt.close();
                bluetoothGatt = null;

                //startScanning();
                //broadcastUpdate(ACTION_SCANNING_RESTARTED);
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                CURRENT_MTU_SIZE = mtu;
                Log.i("MTU is now", "  " + CURRENT_MTU_SIZE);
            } else if (status == BluetoothGatt.GATT_FAILURE) {
                CURRENT_MTU_SIZE = mtu;
                Log.e("ERROR", "MTU could not be changed");
            }
            try {
                createQueue();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else if (status == BluetoothGatt.GATT_FAILURE) {
                Log.i("ERROR", "onServicesDiscovered received error: " + status);
                if (ActivityCompat.checkSelfPermission(BluetoothLeService.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                }
                gatt.disconnect();
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                //Log.i("new value length", "  " + characteristic.getValue().length);
                //convertToHex(characteristic.getValue());
                if (!valueQueue.isEmpty()) {
                    try {
                        //Log.i("remaining queue elements", "  " + valueQueue.size());
                        writeCharacteristic(characteristic);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    broadcastUpdate(ACTION_SENT_PHOTO);
                }
            } else if (status == BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH) {
                Log.e("TAG", "onCharacteristicWrite() A write operation failed due to invalid attribute length");
            } else {
                Log.e("TAG", "onCharacteristicWrite() A write operation failed. Status = " + status);
            }
        }
    };

    public void stopScanning() {
        if (FpgaActivity.scanning) {
            FpgaActivity.scanning = false;
            Log.i("Scanning ", "has stopped.");
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {}
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }

    /**
     * Sets up Bluetooth API variables for device's Bluetooth management
     */
    public void initialiseBle() {
        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null) return null;
        return bluetoothGatt.getServices();
    }

    public List<BluetoothGattCharacteristic> getSupportedGattCharacteristics(BluetoothGattService service) {
        if (service == null) return null;
        characteristicsQueue.addAll(service.getCharacteristics());
        return service.getCharacteristics();
    }

    public void createByteFileDirectory() {
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

        appFolder = new File(storageDirectory, "BlueShot");

        if ( !appFolder.exists() ) {
            if ( !appFolder.mkdirs() ) {
                Log.e("Error", "in creating BlueShot folder");
            }
        }
    }

    public String createByteFile() throws IOException {

        String name = "NEW_BMP_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".bmp";
        byteFile = new File(appFolder, name);
        if ( !byteFile.createNewFile() ) {
            Log.e("ERROR", "BMP image file creation failed");
            return null;
        }
        return  byteFile.getAbsolutePath();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        Log.i(TAG, "IMAGE_SIZE is " + IMAGE_SIZE);
        initialiseBle();

        firstWrite = true;

        buffer = ByteBuffer.allocate(HEADER_SIZE + IMAGE_SIZE);

        //Log.i("buffer length is ", " "+buffer.capacity());
        buffer.put(CameraActivity.headerByteArray);
        createByteFileDirectory();

        startScanning();

        return binder;
    }

    public void createQueue() throws IOException {

        int k = 0;
        int l;
        do {
            l = CURRENT_MTU_SIZE - 3;
        } while(CURRENT_MTU_SIZE == 0);
        //Log.i("length of stream to send is", " " + CameraActivity.imageArray.length);
        valueQueue.add(Arrays.copyOfRange(CameraActivity.imageArray, k, l));

        while (true) {
            //Log.i("k and l are", " " + k + "   " + l);
            k = k + CURRENT_MTU_SIZE - 3;
            if (l + CURRENT_MTU_SIZE - 3 > CameraActivity.imageArray.length) {
                l = CameraActivity.imageArray.length;
                valueQueue.add(Arrays.copyOfRange(CameraActivity.imageArray, k, l));
                break;
            } else {
                l = l + CURRENT_MTU_SIZE - 3;
            }
            //Log.i(" k =", " " + k + "  and l = " + l);
            valueQueue.add(Arrays.copyOfRange(CameraActivity.imageArray, k, l));
        }
        Log.i("queue size is", " " + valueQueue.size());
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind");

        if (FpgaActivity.scanning) {
            stopScanning();
            FpgaActivity.scanning = false;
        }
        closeConnection();
        FpgaActivity.boundService = false;
        totalReceivedBytes = 0;
        CURRENT_MTU_SIZE = 0;
        return super.onUnbind(intent);
    }

    private void startScanning() {
        if (FpgaActivity.scanning) {
            stopScanning();
        }
        // Stops scanning after a predefined scan period.
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(BluetoothLeService.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {}
                if (FpgaActivity.scanning) {
                    FpgaActivity.scanning = false;
                    Log.i("Scanning ", "has stopped.");
                    bluetoothLeScanner.stopScan(leScanCallback);
                    broadcastUpdate(ACTION_SCANNING_FAILED);
                }
            }
        }, SCAN_PERIOD);

        if (BluetoothAdapter.checkBluetoothAddress(FPGA_ADDRESS)) {
            List<ScanFilter> filters = new ArrayList<>();
            ScanSettings settings = (new ScanSettings.Builder())
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            ScanFilter filter = new ScanFilter.Builder()
                    .setDeviceAddress(FPGA_ADDRESS)
                    .build();
            filters.add(filter);
            FpgaActivity.scanning = true;
            bluetoothLeScanner.startScan(filters, settings, leScanCallback);

            Log.i("Scanning ", "has started.");
        }
    }

    private void closeConnection() {
        if (bluetoothGatt == null) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {}
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    public boolean initialize() {
        if (bluetoothAdapter == null) {
            Log.i("Error", "Unable to obtain a BluetoothAdapter.");
            return true;
        }
        return false;
    }

    public boolean connect(final String address) {
        if (bluetoothAdapter == null || address == null) {
            Log.i("Error", "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        try {
            final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            (new Handler(Looper.getMainLooper())).post(new Runnable() {
                @Override
                public void run() {
                    if (ActivityCompat.checkSelfPermission(BluetoothLeService.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    }
                    device.connectGatt(BluetoothLeService.this, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
                }
            });
            Log.i("Trying to connect with device", "  " + address);
        } catch (IllegalArgumentException exception) {
            Log.i("Error", "Device not found with provided address.");
            return false;
        }
        return true;
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {}
        if ( !bluetoothGatt.setCharacteristicNotification(characteristic, enabled) ) {
            Log.e("ERROR", "setCharacteristicNotification() failed for characteristic " + characteristic.getUuid());
            return;
        }
        try {
            Thread.sleep(DELAY);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
        if (descriptor == null) {
            Log.e("Descriptor", "is null ");
            return ;
        }

        if (enabled) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        bluetoothGatt.writeDescriptor(descriptor);
        try {
            Thread.sleep(DELAY);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setCharacteristicIndication(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {}
        if ( !bluetoothGatt.setCharacteristicNotification(characteristic, enabled) ) {
            Log.e("ERROR", "setCharacteristicNotification() failed for characteristic " + characteristic.getUuid());
            return;
        }
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
        if (descriptor == null) {
            Log.e("Descriptor", "is null ");
            return ;
        }

        if (!enabled) {
            return ;
        }

        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        bluetoothGatt.writeDescriptor(descriptor);
        try {
            Thread.sleep(DELAY);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) throws InterruptedException {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {}

        /*if (firstWrite) {
            Thread.sleep(DELAY);
            byte[] passwordKey = "pictureBytesStarting".getBytes();
            characteristic.setValue(passwordKey);
            Log.i("Image transfer state", "Started");
            firstWrite = false;
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            bluetoothGatt.writeCharacteristic(characteristic);
            Thread.sleep(DELAY);
        } else { */
        characteristic.setValue(valueQueue.poll());
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        bluetoothGatt.writeCharacteristic(characteristic);
        //}

    }

    public void convertToHex(byte[] value) {
        char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[value.length * 2];
        for (int j = 0; j < value.length; j++) {
            int v = value[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        //return hexChars;
        System.out.println(hexChars.length);
    }
}