package com.example.sampleproject.activity;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.example.sampleproject.R;
import com.example.sampleproject.databinding.ActivityMainBinding;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;

public class FragmentLocation extends Fragment {
    private ActivityMainBinding binding;
    LocationRequest locationRequest;
    int locationInterval = 1000;
    private static final int REQUEST_LOCATION_TURN_ON = 2000;
    private double latitude;
    private double longitude;
    private ActivityResultLauncher<IntentSenderRequest> resolutionForResult;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.inflate(inflater, R.layout.activity_main, container, false);
        View view = binding.getRoot();
        binding.setLifecycleOwner(requireActivity());
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, locationInterval)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(locationInterval)
                .setMaxUpdateDelayMillis(locationInterval)
                .build();
        requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        getlocation();
        return view;
    }
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        resolutionForResult = registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
            if(result.getResultCode() == RESULT_OK){
                getlocation();
            }else {
                turnOnGPS();
            }
        });
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
                        getlocation();
                    }
                    else {
                        turnOnGPS();
                    }
                }
            } catch (Exception e) {
                Log.e("error", e.toString());
            }
        }
    };

    //check permissions and  location
    private void getlocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (isGPSEnabled()) {
                    LocationServices.getFusedLocationProviderClient(requireActivity()).requestLocationUpdates(locationRequest, new LocationCallback() {
                        @Override
                        public void onLocationResult(@NonNull LocationResult locationResult) {
                            super.onLocationResult(locationResult);
                            LocationServices.getFusedLocationProviderClient(requireActivity()).removeLocationUpdates(this);
                            if (locationResult.getLocations().size() > 0) {
                                int index = locationResult.getLocations().size() - 1;
                                latitude = locationResult.getLocations().get(index).getLatitude();
                                longitude = locationResult.getLocations().get(index).getLongitude();
                                Log.e("location", latitude + " " + longitude);
                            }
                        }
                    }, Looper.getMainLooper());
                } else {
                    turnOnGPS();
                }

            } else {
                requireActivity().requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    private void turnOnGPS() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);
        Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(requireActivity()).checkLocationSettings(builder.build());
        result.addOnCompleteListener(task -> {
            try {
                LocationSettingsResponse response = task.getResult(ApiException.class);
                Log.e("LOCATION response", String.valueOf(response));
                Toast.makeText(requireActivity(), "GPS is already tured on", Toast.LENGTH_SHORT).show();
            } catch (ApiException e) {
                switch (e.getStatusCode()) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                            resolvableApiException.startResolutionForResult(requireActivity(), REQUEST_LOCATION_TURN_ON);
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
        LocationManager locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
        isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return isEnabled;
    }

    @Override
    public void onStart() {
        super.onStart();
        requireActivity().registerReceiver(gpsreceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
    }

    @Override
    public void onStop() {
        super.onStop();
        requireActivity().unregisterReceiver(gpsreceiver);
    }

}

