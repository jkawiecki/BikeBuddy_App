package com.example.myapplication.ui.dashboard;

import static android.content.ContentValues.TAG;

import static com.example.myapplication.ui.home.HomeFragment.R_W;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.BluetoothLeService;
import com.example.myapplication.GoogleFitActivity;
import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentDashboardBinding;
import com.example.myapplication.ui.notifications.NotificationsFragment;

public class DashboardFragment extends Fragment {
    Context context;
    private FragmentDashboardBinding binding;

    @SuppressLint("SetTextI18n")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        context = root.getContext();

        Log.w(TAG, "Test Dash: " + NotificationsFragment.readData);
        String input = "";
        if(NotificationsFragment.readData.isEmpty()) {
            input = "0 0 0 0";
        } else {
            input = NotificationsFragment.readData;
        }
        String[] splitStr = input.split("\\s+");

        /*
        String str = "239 94 63 212";
        String[] splitStr = str.split("\\s+");
        Log.w(TAG, "Dist: " + splitStr[0]);
        Log.w(TAG, "AvgRPM: " + splitStr[1]);
        Log.w(TAG, "AvgHR: " + splitStr[2]);
        Log.w(TAG, "Cals: " + splitStr[3]);
         */

        // Attributes
        // Calories

        TextView distance = (TextView) root.findViewById(R.id.Distance);
        distance.setText("Distance (mi): " + Integer.parseInt(splitStr[0])/100.0);

        TextView avgRpm = (TextView) root.findViewById(R.id.rpm);
        avgRpm.setText("Avg RPM: " + Integer.parseInt(splitStr[1]));

        TextView avgHr = (TextView) root.findViewById(R.id.hr);
        avgHr.setText("Avg HR: " + Integer.parseInt(splitStr[2]));

        TextView calories = (TextView) root.findViewById(R.id.Calories);
        calories.setText("Calories: " + Math.abs(Integer.parseInt(splitStr[3])));

        Button upload = (Button) root.findViewById(R.id.button);
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, GoogleFitActivity.class);
                intent.putExtra(R_W, "write");

                startActivity(intent);
            }
        });

        return root;
    }

    /* Can also use the receiver broadcast
    public void onResume() {
        super.onResume();
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        context.registerReceiver(mGattUpdateReceiver, intentFilter);
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.w(TAG, "Rec");

            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                String[] values = intent.getStringExtra(BluetoothLeService.EXTRA_DATA).split(System.lineSeparator());
                if(values != null) {
                    Log.w(TAG, "Values2: " + values[0]);
                }
            }
        }
    };*/



@Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}