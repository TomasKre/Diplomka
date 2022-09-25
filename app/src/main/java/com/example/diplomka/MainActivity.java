package com.example.diplomka;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import android.content.Intent;

public class MainActivity extends AppCompatActivity {

    private LocationManager locationManager;
    private LocationListener locationListener;

    public int session;
    private DataModel dm;

    ListView dataWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("session_prefs", 0);
        session = sharedPreferences.getInt("session", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("session", ++session);
        editor.apply();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button locationButton = findViewById(R.id.location_permission);
        Button microphoneButton = findViewById(R.id.microphone_permission);
        Button storageButton = findViewById(R.id.storage_permission);

        dataWindow = findViewById(R.id.data_window);

        dm = new DataModel(this);

        locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);

        //Check if GPS is enabled or not

        try {
            locationListener = new LocationChangeListener(this, this);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 30000, 50, locationListener);
            }
        } catch (Exception e) {
            Log.v("Location", "Není povoleno použití GPS");
        }

        locationButton.setOnClickListener(v -> requestLocationPermission());

        microphoneButton.setOnClickListener(v -> requestMicrophonePermissions());

        storageButton.setOnClickListener(v -> requestExternalStoragePermissions());

        showData(dm);
    }

    private final int MY_PERMISSIONS_EXTERNAL_STORAGE = 3;
    private final int MY_PERMISSIONS_ACCESS_LOCATION = 2;
    private final int MY_PERMISSIONS_RECORD_AUDIO = 1;

    protected void requestExternalStoragePermissions() {
        try {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.MANAGE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "Prosím, povolte využití přístupu k datům.", Toast.LENGTH_LONG).show();

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_EXTERNAL_STORAGE);

                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_EXTERNAL_STORAGE);
                }
            }
            else if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE)//další podmínka
                    == PackageManager.PERMISSION_GRANTED) {

                //saveData();
            }
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    protected void requestLocationPermission() {
        try {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                //When permission is not granted by user, show them message why this permission is needed.
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Toast.makeText(this, "Prosím, povolte využití polohy.", Toast.LENGTH_LONG).show();

                    //Give user option to still opt-in the permissions
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSIONS_ACCESS_LOCATION);

                } else {
                    // Show user dialog to grant permission to record audio
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSIONS_ACCESS_LOCATION);
                }
            }
            //If permission is granted, then go ahead recording audio
            else if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                //recordLocation();
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 30000, 50, locationListener);
            }
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    protected void requestMicrophonePermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            //When permission is not granted by user, show them message why this permission is needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, "Prosím, povolte využití mikrofonu.", Toast.LENGTH_LONG).show();

                //Give user option to still opt-in the permissions
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);

            } else {
                // Show user dialog to grant permission to record audio
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);
            }
        }
        //If permission is granted, then go ahead recording audio
        else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {

            //recordAudio();
        } else {
            // Show user dialog to grant permission to record audio
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MY_PERMISSIONS_RECORD_AUDIO);
        }
    }

    //Handling callback
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.v("onRequestPermissionsResult", "requestCode: " + requestCode);
        Log.v("onRequestPermissionsResult", "permissions: " + Arrays.toString(permissions));
        Log.v("onRequestPermissionsResult", "grantResults: " + Arrays.toString(grantResults));
        switch (requestCode) {
            case MY_PERMISSIONS_RECORD_AUDIO: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (locationListener == null) {
                        try {
                            locationListener = new LocationChangeListener(this, this);
                            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                locationManager.requestLocationUpdates(
                                        LocationManager.GPS_PROVIDER, 30000, 50, locationListener);
                            }
                        } catch (Exception e) {
                            Log.v("Location", "Není povoleno použití GPS (Microphone permissions)");
                        }
                    }
                } else {
                    Toast.makeText(this, "Zakázáno používat mikrofon.", Toast.LENGTH_LONG).show();
                }
                return;
            }
            case MY_PERMISSIONS_ACCESS_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (locationListener == null) {
                        try {
                            locationListener = new LocationChangeListener(this, this);
                            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                locationManager.requestLocationUpdates(
                                        LocationManager.GPS_PROVIDER, 30000, 50, locationListener);
                            }
                        } catch (Exception e) {
                            Log.v("Location", "Není povoleno použití GPS (Location permissions)");
                        }
                    }
                } else {
                    Toast.makeText(this, "Zakázano používat přesnou polohu.", Toast.LENGTH_LONG).show();

                }
                return;
            }
            case MY_PERMISSIONS_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    // TODO: index 0 - manage nepovolen, 1 a 2 read a write povoleny - proč?
                    if (locationListener == null) {
                        try {
                            locationListener = new LocationChangeListener(this, this);
                            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                locationManager.requestLocationUpdates(
                                        LocationManager.GPS_PROVIDER, 30000, 50, locationListener);
                            }
                        } catch (Exception e) {
                            Log.v("Location", "Není povoleno použití GPS (Storage permissions)");
                        }
                    }
                } else {
                    Toast.makeText(this, "Zakázáno používat externí úložiště.", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    public void showData(DataModel dm) {
        ArrayList<String> dataLines = dm.getGroupedDataAsStrings();

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, dataLines);
        dataWindow.setAdapter(adapter);

        dataWindow.setOnItemClickListener((adapterView, view, position, l) -> {
            String value = adapter.getItem(position);
            openMapIntent(value);
        });
    }

    public void openMapIntent(String msg) {
        Intent intent = new Intent(this, MapActivity.class);
        //intent.putExtra(ID, String.valueOf(id));
        intent.putExtra("item", msg);
        startActivity(intent);
    }

    //TODO: Data přidat ID k ukládání session
}