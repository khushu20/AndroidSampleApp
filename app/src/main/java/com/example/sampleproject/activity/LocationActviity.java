package com.example.sampleproject.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.sampleproject.R;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;

public class LocationActviity extends AppCompatActivity {
    int locationInterval = 1000;
    private static final int REQUEST_LOCATION_TURN_ON = 2000;
    LocationActviity locationActviity = this;
    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;
    private ActivityResultLauncher<IntentSenderRequest> resolutionForResult;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //calling location request for peroid of time
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, locationInterval)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(locationInterval)
                .setMaxUpdateDelayMillis(locationInterval)
                .build();
        getCurrentLocation();
        getmovementlocation();

        resolutionForResult = registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
            if(result.getResultCode() == RESULT_OK){
                getCurrentLocation();
            }else {
                turnOnGPS();
            }
        });

    }
//on location allowed do any need full funtionality
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isGPSEnabled()) {
                    getCurrentLocation();
                } else {
                    turnOnGPS();
                }
            } else {
                getCurrentLocation();
            }
        } else {
            getCurrentLocation();
        }


    }
//broadcast receiver for receiving the location off state when disabled from settings or notificationbar
    private final BroadcastReceiver gpsreceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (intent != null && intent.getAction() != null && intent.getAction().matches(LocationManager.PROVIDERS_CHANGED_ACTION)) {
                    LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                    boolean isGpsEnabled = false;
                    if (locationManager != null) {
                        isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                    }
                    if (locationManager != null) {
                        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                        Log.e("error", String.valueOf(isNetworkEnabled));
                    }
                    if (!isGpsEnabled) {
                        getCurrentLocation();
                    }
                }
            } catch (Exception e) {
                Log.e("error", e.toString());
            }
        }
    };

//check permissions and getlocation location
    private void getCurrentLocation() {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(locationActviity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                if (isGPSEnabled()) {

                    LocationServices.getFusedLocationProviderClient(locationActviity)
                            .requestLocationUpdates(locationRequest, new LocationCallback() {
                                @Override
                                public void onLocationResult(@NonNull LocationResult locationResult) {
                                    super.onLocationResult(locationResult);

                                    LocationServices.getFusedLocationProviderClient(locationActviity)
                                            .removeLocationUpdates(this);

                                    if (locationResult.getLocations().size() > 0) {
                                        int index = locationResult.getLocations().size() - 1;
                                        double latitude = locationResult.getLocations().get(index).getLatitude();
                                        double longitude = locationResult.getLocations().get(index).getLongitude();

                                        Log.e("location", latitude + " " + longitude);
                                    }
                                }
                            }, Looper.getMainLooper());


                } else {
                    turnOnGPS();
                }

            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }
//check location and movement of the location
    private void getmovementlocation() {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(locationActviity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                if (isGPSEnabled()) {

                    LocationCallback locationCallback = new LocationCallback() {
                        @Override
                        public void onLocationResult(@NonNull LocationResult locationResult) {
                            for (Location location : locationResult.getLocations()) {
                                Toast.makeText(locationActviity, "" + location.getLatitude() + " " + location.getLongitude(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    };
                    fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
//                    fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> Toast.makeText(locationActviity, "" + location.getLatitude() + " " + location.getLongitude(), Toast.LENGTH_SHORT).show());

                    fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                            locationCallback,
                            Looper.getMainLooper());
                } else {
                    turnOnGPS();
                }

            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }
    private void turnOnGPS() {


        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(getApplicationContext())
                .checkLocationSettings(builder.build());

        result.addOnCompleteListener(task -> {

            try {
                LocationSettingsResponse response = task.getResult(ApiException.class);
                Log.e("LOCATION response", String.valueOf(response));

            } catch (ApiException e) {

                switch (e.getStatusCode()) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:

                        try {
                            ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                            resolvableApiException.startResolutionForResult(locationActviity, REQUEST_LOCATION_TURN_ON);
                            IntentSenderRequest intentSenderRequest = new IntentSenderRequest.Builder((resolvableApiException).getResolution()).build();
                            resolutionForResult.launch(intentSenderRequest);
                        } catch (IntentSender.SendIntentException ex) {
                            ex.printStackTrace();
                        }
                        break;

                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        //Device does not have location
                        break;
                }
            }
        });

    }

    private boolean isGPSEnabled() {
        boolean isEnabled;
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return isEnabled;

    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(gpsreceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(gpsreceiver);
    }
}
