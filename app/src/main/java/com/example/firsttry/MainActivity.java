package com.example.firsttry;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.util.Log;
import android.widget.CompoundButton;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.Manifest;
import android.widget.Toast;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executor;

// android firebase
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


import java.util.Map;
public class MainActivity extends AppCompatActivity {
    private static final int DEFAULT_UPDATE_INTERVAL = 5;
    private static final int FAST_UPDATE_INTERVAL = 1;
    private static final int PERMISSION_FINE_LOCATION = 99;
    private static final int PERMISSION_COARSE_LOCATION = 99;
    TextView tv_lat, tv_lon, tv_altitude, tv_accuracy, tv_speed, tv_Sensor, tv_address, tv_updates;
    Switch sw_locationsupdates, sw_gps;

    //this is the heart and soul of this application
    FusedLocationProviderClient fusedLocationProviderClient;

    //veriable to remember  if we ARE tracking location or not fusedLocationProviderClient
    LocationRequest locationRequest;
    boolean updateOn = false;
    LocationCallback locationCallBack;

    //location request is a config file for all setting related to
    // Declare DatabaseReference
    private DatabaseReference mDatabase;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_altitude = findViewById(R.id.tv_altitude);
        tv_accuracy = findViewById(R.id.tv_accuracy);
        tv_speed = findViewById(R.id.tv_speed);
        tv_Sensor = findViewById(R.id.tv_sensor);
        tv_address = findViewById(R.id.tv_address);
        tv_updates = findViewById(R.id.tv_updates);
        sw_gps = findViewById(R.id.sw_gps);
        sw_locationsupdates = findViewById(R.id.sw_locationsupdates); // Replace 'R.id.sw_locationsupdates' with your actual ID

        //firebase ..................
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // set all the properties of  Location request

        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000 * DEFAULT_UPDATE_INTERVAL);
        locationRequest.setFastestInterval(1000 * FAST_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        locationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                //save the location
                updateUIValues(locationResult.getLastLocation());
            }
        };


        sw_gps.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    tv_Sensor.setText("Using GPS sensors");
                } else {
                    locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                    tv_Sensor.setText("Using Towers + WiFi");
                }
            }
        });

        sw_locationsupdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sw_locationsupdates.isChecked()) {
                    startLocationUpdates();
                } else {
                    stopLocationUpdates();
                }
            }
        });


        updateGps();


    }

    private void stopLocationUpdates() {
        tv_updates.setText("Location is not being tracked");
        tv_lat.setText("Not tracking location");
        tv_lon.setText("Not tracking location");
        tv_speed.setText("Not tracking location");
        tv_address.setText("Not tracking location");
        tv_accuracy.setText("Not tracking location");
        tv_altitude.setText("Not tracking location");
        tv_Sensor.setText("Not tracking location");

        fusedLocationProviderClient.removeLocationUpdates(locationCallBack);
    }


    private void startLocationUpdates() {
        tv_updates.setText("Location is being tracked");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
        updateGps();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                startLocationUpdates();
            } else {
                // Permission denied.
                Toast.makeText(this, "Location permissions are required for this app to work", Toast.LENGTH_SHORT).show();
            }
        }
        // Repeat for PERMISSION_COARSE_LOCATION if you have separate request codes
    }


    private void updateGps() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission is already granted, perform location-related tasks
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    // We got permission. Check if location is not null before updating UI
                    if (location != null) {
                        updateUIValues(location);
                    } else {
                        // Handle case where location is null
                        tv_address.setText("Location not available");
                    }

                }
            });
        } else {
            // Request permissions if not already granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
                }
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_COARSE_LOCATION);
                }
            }

        }
    }

    private void updateUIValues(Location location) {
        tv_lat.setText(String.valueOf(location.getLatitude()));
        tv_lon.setText(String.valueOf(location.getLongitude()));
        tv_accuracy.setText(String.valueOf(location.getAccuracy())); // Fixed to set accuracy correctly

        // Correctly setting altitude and speed
        if(location.hasAltitude()) {
            tv_altitude.setText(String.valueOf(location.getAltitude()*-1));
        } else {
            tv_altitude.setText("Not available");
        }

        if(location.hasSpeed()) {
            tv_speed.setText(String.valueOf(location.getSpeed())); // Fixed to set speed correctly
        } else {
            tv_speed.setText("Not available");
        }

        String pattern = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern, Locale.getDefault());
        String date = simpleDateFormat.format(new Date());

        // Get the address using the Geocoder
        String address = "Not available";
        Geocoder geocoder = new Geocoder(MainActivity.this);
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && addresses.size() > 0) {
                address = addresses.get(0).getAddressLine(0);
            }
        } catch (IOException e) {
            Log.e("Geocoder", "Failed to get address", e);
        }
        tv_address.setText(address);

        // Create a HashMap or a Model class to represent your location object
        Map<String, Object> locationUpdate = new HashMap<>();
        locationUpdate.put("latitude", location.getLatitude());
        locationUpdate.put("longitude", location.getLongitude());
        locationUpdate.put("accuracy", location.getAccuracy());
        locationUpdate.put("altitude", location.hasAltitude() ? location.getAltitude() : "Not available");
        locationUpdate.put("speed", location.hasSpeed() ? location.getSpeed() : "Not available");
        locationUpdate.put("timestamp", date);  // Add the current date and time
        locationUpdate.put("address", address);  // Add the address

        // Update the values in Firebase
        mDatabase.child("locations").push().setValue(locationUpdate)
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Location updated successfully."))
                .addOnFailureListener(e -> {
                    // This will log the error message if the database write fails
                    Log.d("Firebase", "Failed to update location.", e);
                    // Consider showing a message to the user as well
                    Toast.makeText(MainActivity.this, "Failed to update location: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });

    }

}