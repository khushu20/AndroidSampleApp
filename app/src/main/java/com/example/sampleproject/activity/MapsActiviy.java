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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;

import com.example.sampleproject.R;
import com.example.sampleproject.databinding.ActivityMapsBinding;
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
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;

public class MapsActiviy extends AppCompatActivity implements OnMapReadyCallback {
    private static final int REQUEST_LOCATION_TURN_ON = 2000;
    MapsActiviy activiy = this;
    double latitude;
    double longitude;
    private GoogleMap mMap;
    ActivityMapsBinding binding;
    LocationRequest locationRequest;
    int locationInterval = 1000;
    Marker mCurrLocationMarker;
    FusedLocationProviderClient fusedLocationProviderClient;
    SupportMapFragment mapFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_maps);
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, locationInterval)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(locationInterval)
                .setMaxUpdateDelayMillis(locationInterval)
                .build();
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                mMap = googleMap;
                getCurrentLocation();
            });
        }
        //getmovementlocation();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        getCurrentLocation();
        mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
        mMap.setOnMapClickListener(latLng -> {
            mMap.clear();
            latitude = latLng.latitude;
            longitude = latLng.longitude;
            mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
            CameraUpdate point = CameraUpdateFactory.newLatLng(latLng);
            mMap.moveCamera(point);
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

            Log.e("location", latitude + " " + longitude);
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
            if (ActivityCompat.checkSelfPermission(activiy, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
                if (isGPSEnabled()) {
                    LocationServices.getFusedLocationProviderClient(activiy)
                            .requestLocationUpdates(locationRequest, new LocationCallback() {
                                @Override
                                public void onLocationResult(@NonNull LocationResult locationResult) {
                                    super.onLocationResult(locationResult);
                                    LocationServices.getFusedLocationProviderClient(activiy).removeLocationUpdates(this);
                                    if (locationResult.getLocations().size() > 0) {
                                        mMap.clear();
                                        int index = locationResult.getLocations().size() - 1;
                                         latitude = locationResult.getLocations().get(index).getLatitude();
                                         longitude = locationResult.getLocations().get(index).getLongitude();
                                        LatLng location = new LatLng(latitude, longitude);
                                        mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(location).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
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
            if (ActivityCompat.checkSelfPermission(activiy, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                if (isGPSEnabled()) {

                    LocationCallback locationCallback = new LocationCallback() {
                        @Override
                        public void onLocationResult(@NonNull LocationResult locationResult) {
                            for (Location locations : locationResult.getLocations()) {
                                mMap.clear();
                                 latitude = locations.getLatitude();
                                 longitude = locations.getLongitude();
                                LatLng location = new LatLng(latitude, longitude);
                                mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(location).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
                                Log.e("location", latitude + " " + longitude);
                            }
                        }
                    };
                    fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
//                    fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> Toast.makeText(activiy, "" + location.getLatitude() + " " + location.getLongitude(), Toast.LENGTH_SHORT).show());

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
                Toast.makeText(activiy, "GPS is already tured on", Toast.LENGTH_SHORT).show();

            } catch (ApiException e) {

                switch (e.getStatusCode()) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:

                        try {
                            ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                            resolvableApiException.startResolutionForResult(activiy, REQUEST_LOCATION_TURN_ON);
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
