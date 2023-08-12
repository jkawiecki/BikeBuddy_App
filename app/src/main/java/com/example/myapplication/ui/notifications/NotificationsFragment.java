package com.example.myapplication.ui.notifications;

import static android.content.ContentValues.TAG;

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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.BluetoothLeService;
import com.example.myapplication.DeviceControlActivity;
import com.example.myapplication.DeviceScanActivity;
import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentNotificationsBinding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class NotificationsFragment extends Fragment {
    Context context;
    private FragmentNotificationsBinding binding;
    EditText name, gender, age, weight;
    public static final String USER_NAME = "USER_NAME";
    public static final String USER_GENDER = "USER_GENDER";
    public static final String USER_AGE = "USER_AGE";
    public static final String USER_WEIGHT = "USER_WEIGHT";
    public static final String CMD = "CMD";

    public static String readData = "";
    public static String filename = "temp";
    public static double max_hr = 0.0;


    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        NotificationsViewModel notificationsViewModel =
                new ViewModelProvider(this).get(NotificationsViewModel.class);

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        context = root.getContext();

        // Attributes
        name = (EditText) root.findViewById(R.id.user_name);
        gender = (EditText) root.findViewById(R.id.user_gender);
        age = (EditText) root.findViewById(R.id.user_age);
        weight = (EditText) root.findViewById(R.id.user_weight);

        Button save_test = (Button) root.findViewById(R.id.save);
        save_test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Name: " + name.getText().toString());
                Log.d(TAG, "Gender: " + gender.getText().toString());
                Log.d(TAG, "Age: " + age.getText().toString());
                Log.d(TAG, "Weight: " + weight.getText().toString());
            }
        });

        Button write = (Button) root.findViewById(R.id.ble_scanner);
        write.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Logs "start" time of ride in cache file
                logStartTime();

                Intent intent = new Intent(context, DeviceScanActivity.class);
                intent.putExtra(USER_GENDER, gender.getText().toString());
                intent.putExtra(USER_AGE, age.getText().toString());
                intent.putExtra(USER_WEIGHT, weight.getText().toString());
                intent.putExtra(CMD, "write");

                startActivity(intent);
            }
        });

        Button read = (Button) root.findViewById(R.id.hm19);
        read.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get max hr
                int years = 25;
                if(!(age.getText().toString().isEmpty())) {
                    years = Integer.parseInt(age.getText().toString());
                }
                max_hr = 205.8 - (0.685 * years);
                Log.d(TAG, "max_hr: " + max_hr);

                Intent intent = new Intent(context, DeviceScanActivity.class);
                intent.putExtra(CMD, "read");
                startActivity(intent);
            }
        });

        // Testing
        //readData = "12 94 63 7";
        return root;
    }

    public void onResume() {
        super.onResume();
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        context.registerReceiver(mGattUpdateReceiver, intentFilter);
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                String[] values = intent.getStringExtra(BluetoothLeService.EXTRA_DATA).split(System.lineSeparator());
                if(values != null) {
                    //Log.w(TAG, "ValuesN: " + values[0]);
                    readData = values[0];
                }
            }
        }
    };

    public void logStartTime() {
        // Add time start
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("EST"));
        Date now = new Date();
        cal.setTime(now);
        long startTime = cal.getTimeInMillis();

        File file = new File(context.getCacheDir(), filename);
        try {
            FileOutputStream stream = new FileOutputStream(file);
            String write = String.valueOf(startTime);
            try {
                stream.write(write.getBytes());
            } finally {
                stream.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

@Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
