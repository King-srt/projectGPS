package com.example.lastmodulgps;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;


import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private TextView uid, latitude, longitude, altitude, akurasi, addressTextView;
    private Button btnFind, btnLogout;
    private ProgressBar bar;
    private FusedLocationProviderClient locationProviderClient;
    private LocationCallback locationCallback;
    private static final int PERMISSION_REQUEST_CODE = 10;
    private GoogleMap mMap;
    private FirebaseAuth mAuth;
    private FirebaseDatabase Data;
    private DatabaseReference theData;
    private com.google.firebase.firestore.FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latitude = findViewById(R.id.latitude);
        longitude = findViewById(R.id.longitude);
        altitude = findViewById(R.id.altitude);
        akurasi = findViewById(R.id.akurasi);
        addressTextView = findViewById(R.id.address_details);
        btnFind = findViewById(R.id.btn_find);
        bar = findViewById(R.id.progressBar);
        btnLogout = findViewById(R.id.logoutBtn);
        uid = findViewById(R.id.UID);
        db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        locationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mAuth = FirebaseAuth.getInstance();
        String url = "https://loginsso-b6b3a-default-rtdb.asia-southeast1.firebasedatabase.app/";
        Data = FirebaseDatabase.getInstance(url);
        theData = Data.getReference("Location");
        FirebaseUser user = mAuth.getCurrentUser();
        FragmentManager fragmentManager = getSupportFragmentManager();
        SupportMapFragment mapFragment = (SupportMapFragment) fragmentManager.findFragmentByTag("mapFragment");
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            fragmentManager.beginTransaction().replace(R.id.map_container, mapFragment, "mapFragment").commit();
            fragmentManager.executePendingTransactions();
        }
        mapFragment.getMapAsync(this);
        if (user != null) {
            uid.setText(user.getUid());
        }
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        updateLocationUI(location);
                        locationProviderClient.removeLocationUpdates(locationCallback);
                        bar.setVisibility(View.GONE);
                    }
                }
            }
        };

        btnFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getLocation();
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });


    }

    @SuppressLint("MissingPermission")
    private void getLocation() {
        bar.setVisibility(View.VISIBLE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
        } else {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Toast.makeText(this, "GPS tidak aktif!", Toast.LENGTH_SHORT).show();
                bar.setVisibility(View.GONE);
                return;
            }

            LocationRequest locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(1000)
                    .setFastestInterval(500);

            locationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private void updateLocationUI(Location location) {
        FirebaseUser user = mAuth.getCurrentUser();
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        latitude.setText(String.valueOf(lat));
        longitude.setText(String.valueOf(lng));
        altitude.setText(String.valueOf(location.getAltitude()));
        akurasi.setText(String.valueOf(location.getAccuracy()) + " meters");

        getLocationDetailsAndSaveToDatabase(user.getUid(), lat, lng, location);
    }

    private void getLocationDetailsAndSaveToDatabase(String userId, double lat, double lng, Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                String addressDetails = address.getAddressLine(0);
                addressTextView.setText(addressDetails);
                addressTextView.setVisibility(View.VISIBLE);

                Data data = new Data(String.valueOf(lat), String.valueOf(lng), addressDetails);
                db.collection("Locations").document(userId).set(data);
                theData.child(userId).setValue(data)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(MainActivity.this, "Data is Set", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(MainActivity.this, "Data isn't set", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                LatLng currentLatLng = new LatLng(lat, lng);
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(currentLatLng).title("Your Location"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
            } else {
                Toast.makeText(this, "Detail Lokasi tidak ditemukan.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal mendapatkan detail lokasi: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "Izin lokasi tidak diberikan!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class FirebaseFirestore {
    }
}
