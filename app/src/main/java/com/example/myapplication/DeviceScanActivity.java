package com.example.myapplication;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.example.myapplication.BluetoothLeService;
import com.example.myapplication.DeviceControlActivity;
import com.example.myapplication.R;
import com.example.myapplication.ui.notifications.NotificationsFragment;

import java.util.ArrayList;
import java.util.List;

public class DeviceScanActivity extends ListActivity {
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private final static String TAG = BluetoothLeService.class.getSimpleName();


    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_ENABLE_LOCATION = 2;
    // Stops scanning after 5 seconds.
    private static final long SCAN_PERIOD = 5000; //10000;
    private Context context;
    private com.example.myapplication.DeviceScanActivity binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        // Initializes list view adapter.
        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                                REQUEST_ENABLE_BT);
                    }
                }
                return;

            case REQUEST_ENABLE_LOCATION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                REQUEST_ENABLE_LOCATION);
                    }
                }
                return;
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
                    context = com.example.myapplication.DeviceScanActivity.this;

                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

                        onRequestPermissionsResult(REQUEST_ENABLE_LOCATION, null, new int[]{});
                        return;
                    }
                    Log.d(TAG, "Stuck here6");
                    scanner.stopScan(scanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
            /*
            // Start scan setup
            String[] names = new String[]{"BLE Device"};
            List<ScanFilter> filters = null;
            filters = new ArrayList<>();
            for (String name : names) {
                ScanFilter filter = new ScanFilter.Builder()
                        .setDeviceName(name)
                        .build();
                filters.add(filter);
            }*/

            // HM19 Testing

            String[] names = new String[]{"DSD TECH"};
            // Build filters list
            List<ScanFilter> filters = null;
            filters = new ArrayList<>();
            for (String name : names) {
                ScanFilter filter = new ScanFilter.Builder()
                        .setDeviceName(name)
                        .build();
                filters.add(filter);
            }

            /*
            //String[] peripheralAddresses = new String[]{"E4:E1:12:95:60:A2"};
            String[] peripheralAddresses = new String[]{"E4:E1:12:95:D8:C6"};

            // Build filters list
            List<ScanFilter> filters = null;
            if (peripheralAddresses != null) {
                filters = new ArrayList<>();
                for (String address : peripheralAddresses) {
                    ScanFilter filter = new ScanFilter.Builder()
                            .setDeviceAddress(address)
                            .build();
                    filters.add(filter);
                }
            }*/

            // Scan Settings
            ScanSettings scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    //.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    //.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    //.setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                    //.setReportDelay(0L)
                    .build();

            if (scanner != null) {
                scanner.startScan(filters, scanSettings, scanCallback);
                Log.d(TAG, "scan started");
            } else {
                Log.e(TAG, "could not get scanner object");
            }

        } else {
            mScanning = false;
            BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
            scanner.stopScan(scanCallback);
        }
        invalidateOptionsMenu();
    }

    // Device scan callback.
    private final ScanCallback scanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            final BluetoothDevice device = result.getDevice();

            // Start new activity right away after scan
            ControlActivity(device);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            // Ignore for now
        }

        @Override
        public void onScanFailed(int errorCode) {
            // Ignore for now
            Log.d(TAG, "Scan Failed");
            finish();
        }
    };


    protected void ControlActivity(BluetoothDevice device) {
        if (device == null) return;
        final Intent intent = new Intent(this, DeviceControlActivity.class);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions

            Log.d(TAG, "Error 256");
        }
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        Log.d(TAG, "Error 257: " + device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        Log.d(TAG, "Error 258: " + device.getAddress());

        // USER DATA
        //intent.putExtra(NotificationsFragment.USER_NAME, getIntent().getStringExtra(NotificationsFragment.USER_NAME));
        intent.putExtra(NotificationsFragment.USER_GENDER, getIntent().getStringExtra(NotificationsFragment.USER_GENDER));
        intent.putExtra(NotificationsFragment.USER_AGE, getIntent().getStringExtra(NotificationsFragment.USER_AGE));
        intent.putExtra(NotificationsFragment.USER_WEIGHT, getIntent().getStringExtra(NotificationsFragment.USER_WEIGHT));
        intent.putExtra(NotificationsFragment.CMD, getIntent().getStringExtra(NotificationsFragment.CMD));


        if (mScanning) {
            BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
            scanner.stopScan(scanCallback);
            mScanning = false;
        }
        startActivity(intent);
    }
}