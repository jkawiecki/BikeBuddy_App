package com.example.myapplication;

import static android.content.ContentValues.TAG;

import static com.example.myapplication.ui.home.HomeFragment.R_W;
import static com.example.myapplication.ui.notifications.NotificationsFragment.filename;
import static com.example.myapplication.ui.notifications.NotificationsFragment.max_hr;

import android.Manifest;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.ui.home.HomeFragment;
import com.example.myapplication.ui.notifications.NotificationsFragment;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.SessionInsertRequest;
import com.google.android.gms.fitness.request.SessionReadRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class GoogleFitActivity extends Activity {

    Context context;



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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        context = getApplicationContext();
        final Intent intent = getIntent();

        String r_w = intent.getStringExtra(R_W);
        if(r_w.equals("read")) {
            checkPermissionsAndRun(FitActionRequestCode.READ_BIKE_SESSION);
        }

        if(r_w.equals("write")) {
            checkPermissionsAndRun(FitActionRequestCode.WRITE_BIKE_SESSION);
        }
        Log.w(TAG, "HERE");

        //finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        finish();
    }

    private void checkPermissionsAndRun(FitActionRequestCode requestCode) {
        if (permissionApproved()) {
            fitSignIn(requestCode);
        } else {
            //requestRuntimePermissions(fitActionRequestCode);
            Log.i(TAG, "Requesting permissions");
            // Request permission from user
            ActivityCompat.requestPermissions(this,
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
            case WRITE_BIKE_SESSION:
                insertSession();
                break;
        }
    }

    private GoogleSignInAccount getAccount() {
        return GoogleSignIn.getAccountForExtension(context, fitnessOptions);
    }

    /*
        INSERT BIKING DATA
     */
    private void insertSession() {
        SessionInsertRequest insertRequest = createBikeSessionInsertRequest();

        Fitness.getSessionsClient(this, getAccount())
                .insertSession(insertRequest)
                .addOnSuccessListener(unused -> {
                            Log.w(TAG, "Session insert successful!");
                            end();
                        })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error inserting session: ", e);
                    end();
                });
    }

    private void end() {
        finish();
    }



    private SessionInsertRequest createBikeSessionInsertRequest() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("EST"));
        Date now = new Date();
        cal.setTime(now);

        long startTime = getStartTime();
        Log.w(TAG, "Start: " + startTime);
        long endTime = cal.getTimeInMillis();
        double[] data = getInsertData();

        // Build Data source
        // Activity Segment
        DataSource bikeDataSource = new DataSource.Builder()
                .setAppPackageName(context.getPackageName())
                .setDataType(DataType.TYPE_ACTIVITY_SEGMENT)
                .setType(DataSource.TYPE_RAW)
                .setStreamName("Biking")
                .build();
        DataPoint bike = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            bike = DataPoint.builder(bikeDataSource)
                    .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                    .setActivityField(Field.FIELD_ACTIVITY, FitnessActivities.BIKING)
                    .build();
        }
        DataSet bikeDataSet = DataSet.builder(bikeDataSource)
                .addAll(Arrays.asList(bike))
                .build();

        //Distance
        DataSource distDataSource = new DataSource.Builder()
                .setAppPackageName(context.getPackageName())
                .setDataType(DataType.TYPE_DISTANCE_DELTA)
                .setType(DataSource.TYPE_RAW)
                .setStreamName("Distance")
                .build();
        // input distance in meters
        float dist = (float) (((float) data[0]) * 1609.24);
        DataPoint distDP = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            distDP = DataPoint.builder(distDataSource)
                    .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                    .setField(Field.FIELD_DISTANCE, dist)
                    .build();
        }
        DataSet distDataSet = DataSet.builder(distDataSource)
                .addAll(Arrays.asList(distDP))
                .build();

        //Avg RPM
        DataSource rpmDataSource = new DataSource.Builder()
                .setAppPackageName(context.getPackageName())
                .setDataType(DataType.TYPE_CYCLING_WHEEL_RPM)
                .setType(DataSource.TYPE_RAW)
                .setStreamName("RPM")
                .build();
        float rpm = (float) data[1];
        DataPoint wheelRPM = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            wheelRPM = DataPoint.builder(rpmDataSource)
                    .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                    .setField(Field.FIELD_RPM, rpm)
                    .build();
        }
        DataSet rpmDataSet = DataSet.builder(rpmDataSource)
                .addAll(Arrays.asList(wheelRPM))
                .build();

        //Avg HR
        DataSource hrDataSource = new DataSource.Builder()
                .setAppPackageName(context.getPackageName())
                .setDataType(DataType.TYPE_HEART_POINTS)
                .setType(DataSource.TYPE_RAW)
                .setStreamName("HR")
                .build();

        int mins = (int) (((endTime - startTime)/1000)/60);
        double heart_rate = data[2];
        double hPoints;
        if(0 < (heart_rate/max_hr) && (heart_rate/max_hr) < 0.5) {
            hPoints = mins/2;
        } else if(0.5 <= (heart_rate/max_hr) && (heart_rate/max_hr) < 0.7) {
            hPoints = mins;
        } else {
            hPoints = 2*mins;
        }
        DataPoint hr = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            hr = DataPoint.builder(hrDataSource)
                    .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                    .setField(Field.FIELD_INTENSITY, (float) hPoints)
                    .build();
        }
        DataSet hrDataSet = DataSet.builder(hrDataSource)
                .addAll(Arrays.asList(hr))
                .build();

        DataSource calDataSource = new DataSource.Builder()
                .setAppPackageName(context.getPackageName())
                .setDataType(DataType.TYPE_CALORIES_EXPENDED)
                .setType(DataSource.TYPE_RAW)
                .setStreamName("Calories")
                .build();
        float cals = (float) data[3];
        DataPoint calories = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            calories = DataPoint.builder(calDataSource)
                    .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                    .setField(Field.FIELD_CALORIES, cals)
                    .build();
        }
        DataSet calDataSet = DataSet.builder(calDataSource)
                .addAll(Arrays.asList(calories))
                .build();

        // Build Session
        Session session = new Session.Builder()
                .setName("Bike: " + startTime)
                //.setDescription("Bike around Shoreline")
                .setIdentifier("Bike: " + startTime)
                .setActivity(FitnessActivities.BIKING)
                .setStartTime(startTime, TimeUnit.MILLISECONDS)
                .setEndTime(endTime, TimeUnit.MILLISECONDS)
                .build();

        // Request to insert session
        SessionInsertRequest insertRequest = new SessionInsertRequest.Builder()
                .setSession(session)
                .addDataSet(distDataSet)
                .addDataSet(rpmDataSet)
                .addDataSet(hrDataSet)
                .addDataSet(calDataSet)
                .addDataSet(bikeDataSet)
                .build();

        return insertRequest;
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
            // Permission is not granted
            return false;
        } else {
            return true;
        }
    }

    public long getStartTime() {
        File file = new File(context.getCacheDir(), filename);

        int length = (int) file.length();
        byte[] bytes = new byte[length];

        try {
            FileInputStream in = new FileInputStream(file);
            try {
                in.read(bytes);
            } finally {
                in.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        file.delete();
        context.deleteFile(filename);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("EST"));
        Date now = new Date();
        cal.setTime(now);

        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.MINUTE, 15); // Specify time to go back (make sure you have 5 points)
        long start_error = cal.getTimeInMillis();

        String contents = new String(bytes);
        if(contents.isEmpty()) {
            Log.w(TAG, "Contents empty, using 15minytes as the timer");
            contents = Long.toString(start_error);
        }
        return Long.parseLong(contents);
    }

    public double[] getInsertData() {
        //Log.w(TAG, "Test Insert: " + NotificationsFragment.readData);
        String input = "";
        if(NotificationsFragment.readData.isEmpty()) {
            input = "0 0 0 0";
        } else {
            input = NotificationsFragment.readData;
        }
        String[] splitStr = input.split("\\s+");
        double[] out = new double[]{Integer.parseInt(splitStr[0])/100.0, Integer.parseInt(splitStr[1]), Integer.parseInt(splitStr[2]), Math.abs(Integer.parseInt(splitStr[3]))};

        return out;
    }

    /*
    private void broadcastUpdate(final List<Value[]> data) {
        final Intent intent = new Intent(DATA_AVAILABLE);
        intent.putExtra(EXTRA_DATA, data);

        //Log.i("BlueToothService: ", "BroadcastUpdate ActionOnly: " + action);
        sendBroadcast(intent);
    }*/

    @Override
    protected void onPause() {
        super.onPause();
        //unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unbindService(mServiceConnection);
        //mBluetoothLeService = null;
    }





}
