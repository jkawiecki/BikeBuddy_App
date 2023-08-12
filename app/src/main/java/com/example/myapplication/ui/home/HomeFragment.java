package com.example.myapplication.ui.home;

import static android.app.Activity.RESULT_OK;
import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Cartesian;

import com.anychart.core.cartesian.series.Line;
import com.anychart.data.Mapping;
import com.anychart.data.Set;
import com.anychart.enums.Anchor;
import com.anychart.enums.MarkerType;
import com.anychart.enums.TooltipPositionMode;
import com.anychart.graphics.vector.Stroke;
import com.example.myapplication.BluetoothLeService;
import com.example.myapplication.GoogleFitActivity;
import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentHomeBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.SessionReadRequest;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    public static List retrieved_data = new ArrayList<>();
    View root;

    private boolean authInProgress = false;
    public static final String R_W = "R_W";

    Context context;
    List<Value[]> data = new ArrayList<>();

    FitnessOptions fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_SPEED, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_WRITE)
            .addDataType(DataType.TYPE_CYCLING_WHEEL_RPM, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_CYCLING_WHEEL_RPM, FitnessOptions.ACCESS_WRITE)
            .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_WRITE)
            .addDataType(DataType.TYPE_SPEED, FitnessOptions.ACCESS_WRITE)
            .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_WRITE)
            .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_WRITE)
            .addDataType(DataType.TYPE_HEART_POINTS, FitnessOptions.ACCESS_WRITE)

            .build();

    enum FitActionRequestCode {
        READ_BIKE_SESSION,
        WRITE_BIKE_SESSION
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        root = binding.getRoot();
        context = root.getContext();

        checkPermissionsAndRun(FitActionRequestCode.READ_BIKE_SESSION);
        return root;
    }

    private void checkPermissionsAndRun(FitActionRequestCode requestCode) {
        if (permissionApproved()) {
            fitSignIn(requestCode);
        } else {
            //requestRuntimePermissions(fitActionRequestCode);
            Log.i(TAG, "Requesting permissions");
            // Request permission from user
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                    requestCode.ordinal());
        }
    }

    private void fitSignIn(FitActionRequestCode requestCode) {

        if (GoogleSignIn.hasPermissions(getAccount(), fitnessOptions)) {
            performActionForRequestCode(requestCode);
        } else {
            // Permission not granted
            Log.i(TAG, "here " + requestCode.ordinal());
            GoogleSignIn.requestPermissions(
                    this, // your activity
                    requestCode.ordinal(),
                    getAccount(),
                    fitnessOptions);
        }
    }

    // Handles OAuth sign in flow
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_OK) {
            FitActionRequestCode action = FitActionRequestCode.values()[requestCode];
            performActionForRequestCode(action);
        } else {
            // oAuthErrorMsg(requestCode, resultCode);
            String message = "There was an error signing into Fit. Request code was: "
                    + requestCode + " Result code was: " + resultCode;
            Log.e(TAG, message);
        }
    }

    private void performActionForRequestCode(FitActionRequestCode requestCode) {
        switch (requestCode) {
            case READ_BIKE_SESSION:
                readBikingSessions();
                break;
        }
    }

    private GoogleSignInAccount getAccount() {
        return GoogleSignIn.getAccountForExtension(context, fitnessOptions);
    }

    /*
        READ BIKING SESSION DATA
     */
    private void readBikingSessions() {

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("EST"));
        Date now = new Date();
        cal.setTime(now);

        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.HOUR, -48); // Specify time to go back (make sure you have 5 points)
        long startTime = cal.getTimeInMillis();

        // Build session read requestion
        SessionReadRequest readRequest = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            readRequest = new SessionReadRequest.Builder()
                    .read(DataType.TYPE_ACTIVITY_SEGMENT)
                    .read(DataType.TYPE_DISTANCE_DELTA)
                    .read(DataType.TYPE_CYCLING_WHEEL_RPM)
                    .read(DataType.TYPE_HEART_POINTS)
                    .read(DataType.TYPE_CALORIES_EXPENDED)
                    .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                    .readSessionsFromAllApps()
                    .build();
        }

        Fitness.getSessionsClient(getContext(), getAccount())
                .readSession(readRequest)
                .addOnSuccessListener(response -> {
                    // Get list of sessions matching criteria
                    List<Session> sessions = response.getSessions();
                    Log.i(TAG, "Num of sessions retrieved: " + sessions.size());
                    for (Session session : sessions) {

                        List<DataSet> dataSets = response.getDataSet(session);
                        data.add(dumpDataSets(dataSets));
                    }
                    dumpBikeSession(data);
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to read session", e));
    }

    private void dumpBikeSession(List<Value[]> data) {
        Log.w(TAG, "Data: " + data);
        displayData();
    }

    private Value[] dumpDataSets(List<DataSet> dataSets) {
        Value[] out = new Value[4];
        for(DataSet dataSet : dataSets) {
            if(dumpDataSet(dataSet) != null) {
                Pair<String, Value> pair = dumpDataSet(dataSet);

                switch (pair.first) {
                    case "distance":
                        out[0] = pair.second;
                        break;
                    case "rpm":
                        out[1] = pair.second;
                        break;
                    case "intensity":
                        out[2] = pair.second;
                        break;
                    case "calories":
                        out[3] = pair.second;
                        break;
                }
            }
        }
        return out;
    }

    // DataSet Parsing
    private Pair<String, Value> dumpDataSet(DataSet dataSet) {

        Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType());
        for (DataPoint dp : dataSet.getDataPoints()) {
            Log.i(TAG,"Data point:");
            Log.i(TAG,"\tType: " + dp.getDataType());
            Log.i(TAG,"\tStart: " + getStartTimeString(dp));
            Log.i(TAG,"\tEnd: " + getEndTimeString(dp));
            for (Field field : dp.getDataType().getFields()) {
                Log.i(TAG,"\tField: " + field.getName() + " Type: " + field.getFormat() + " Value: " + dp.getValue(field) );
                if(field.getName().equals("distance") || field.getName().equals("rpm") || field.getName().equals("intensity") || field.getName().equals("calories")) {
                    Pair<String, Value> pair = new Pair<>(field.getName(), dp.getValue(field));
                    return pair;
                }
            }
        }
        return null;
    }

    private String getStartTimeString(DataPoint dp) {
        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        return dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS));
    }

    private String getEndTimeString(DataPoint dp) {
        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        return dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS));
    }

    private Boolean permissionApproved() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        } else {
            return true;
        }
    }

    private class CustomDataEntry extends ValueDataEntry {

        CustomDataEntry(String x, Number value, Number value2) {
            super(x, value);
            setValue("value2", value2);
            //setValue("value3", value3);
        }
    }

    private void displayData() {

        AnyChartView anyChartView = root.findViewById(R.id.any_chart_view);
        anyChartView.setProgressBar(root.findViewById(R.id.progress_bar));

        Cartesian cartesian = AnyChart.line();

        cartesian.animation(true);

        cartesian.padding(10d, 20d, 5d, 20d);

        cartesian.crosshair().enabled(true);
        cartesian.crosshair()
                .yLabel(true)
                // TODO ystroke
                .yStroke((Stroke) null, null, null, (String) null, (String) null);

        cartesian.tooltip().positionMode(TooltipPositionMode.POINT);

        cartesian.title("Recent RPM and Calories");

        cartesian.yAxis(0);
        cartesian.xAxis(0).labels().padding(5d, 5d, 5d, 5d);

        Log.w(TAG, "Size: " + data.size());
        double s1_2;
        double s1_4;
        double s2_2;
        double s2_4;
        double s3_2;
        double s3_4;
        double s4_2;
        double s4_4;
        double s5_2;
        double s5_4;

        Value[] s1 = data.get(data.size()-5);
        Value[] s2 = data.get(data.size()-4);
        Value[] s3 = data.get(data.size()-3);
        Value[] s4 = data.get(data.size()-2);
        Value[] s5 = data.get(data.size()-1);

        s1_2 = s1[1].asFloat();
        s1_4 = s1[3].asFloat();

        s2_2 = s2[1].asFloat();
        s2_4 = s2[3].asFloat();

        s3_2 = s3[1].asFloat();
        s3_4 = s3[3].asFloat();

        s4_2 = s4[1].asFloat();
        s4_4 = s4[3].asFloat();

        s5_2 = s5[1].asFloat();
        s5_4 = s5[3].asFloat();


        List<DataEntry> seriesData = new ArrayList<>();
        seriesData.add(new CustomDataEntry("1", s1_2, s1_4));//, //(Number) s1[3].asFloat()));
        seriesData.add(new CustomDataEntry("2", s2_2, s2_4));//, (Number) s2[3].asFloat()));
        seriesData.add(new CustomDataEntry("3", s3_2, s3_4));//, (Number) s3[3].asFloat()));
        seriesData.add(new CustomDataEntry("4", s4_2, s4_4));//, (Number) s3[3].asFloat()));
        seriesData.add(new CustomDataEntry("5", s5_2, s5_4));//, (Number) s3[3].asFloat()));

        Set set = Set.instantiate();
        set.data(seriesData);
        Mapping series1Mapping = set.mapAs("{ x: 'x', value: 'value' }");
        Mapping series2Mapping = set.mapAs("{ x: 'x', value: 'value2' }");
        //Mapping series3Mapping = set.mapAs("{ x: 'x', value: 'value3' }");
        //Mapping series4Mapping = set.mapAs("{ x: 'x', value: 'value4' }");

        Line series1 = cartesian.line(series1Mapping);
        series1.name("Avg RPM");
        series1.hovered().markers().enabled(true);
        series1.hovered().markers()
                .type(MarkerType.CIRCLE)
                .size(4d);
        series1.tooltip()
                .position("right")
                .anchor(Anchor.LEFT_CENTER)
                .offsetX(5d)
                .offsetY(5d);

        Line series2 = cartesian.line(series2Mapping);
        series2.name("Calories");
        series2.hovered().markers().enabled(true);
        series2.hovered().markers()
                .type(MarkerType.CIRCLE)
                .size(4d);
        series2.tooltip()
                .position("right")
                .anchor(Anchor.LEFT_CENTER)
                .offsetX(5d)
                .offsetY(5d);

        cartesian.legend().enabled(true);
        cartesian.legend().fontSize(13d);
        cartesian.legend().padding(0d, 0d, 10d, 0d);

        anyChartView.setChart(cartesian);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
