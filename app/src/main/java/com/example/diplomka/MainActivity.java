package com.example.diplomka;

import static com.example.diplomka.MapActivity.getDistanceInMeters;
import static com.example.diplomka.MapActivity.getHumanDate;

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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Intent;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MainActivity extends AppCompatActivity {

    private LocationManager locationManager;
    private LocationListener locationListener;

    public int session;
    private DataModel dm;
    private int minTimeMs = 2500;
    private int minDistanceM = 5;
    private int[] permissionsRequests;

    ListView dataWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("session_prefs", 0);
        session = sharedPreferences.getInt("session", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("session", ++session);
        editor.apply();// TODO: odmazat? nebo pouze

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // TODO: Dialog na smazání a odeslání dat měření

        permissionsRequests = new int[3]; // TODO: upravit aby se nastavovalo na 0 při zapnutí aplikace pokud jsou perms -1

        Button locationButton = findViewById(R.id.location_permission);
        Button microphoneButton = findViewById(R.id.microphone_permission);
        Button storageButton = findViewById(R.id.storage_permission);
        ImageView infoButton = findViewById(R.id.info_button);
        Switch onOffSwitch = findViewById(R.id.measure_switch);
        dataWindow = findViewById(R.id.data_window);

        dm = new DataModel(this);
        dm.deleteSoloDataPoints();

        checkPermissionButtons();
        locationButton.setOnClickListener(v -> requestLocationPermission());
        microphoneButton.setOnClickListener(v -> requestMicrophonePermissions());
        storageButton.setOnClickListener(v -> requestExternalStoragePermissions());
        infoButton.setOnClickListener(v -> infoButtonClickListener());
        onOffSwitch.setOnClickListener(v -> onOffSwitchClickListener((Switch) v));
        showData(dm);

        dm.updateDataPoints(74, 6);
        dm.updateDataPoints(75, 6);
    }

    private final int MY_PERMISSIONS_EXTERNAL_STORAGE = 3;
    private final int MY_PERMISSIONS_ACCESS_LOCATION = 2;
    private final int MY_PERMISSIONS_RECORD_AUDIO = 1;

    protected void requestExternalStoragePermissions() {
        try {
            if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE},MY_PERMISSIONS_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Storage request error " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    protected void requestLocationPermission() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_ACCESS_LOCATION);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Location request error " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    protected void requestMicrophonePermissions() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_RECORD_AUDIO);

            }
        } catch (Exception e) {
            Toast.makeText(this, "Microphone request error " + e.getMessage(), Toast.LENGTH_LONG).show();
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
                if (permissionsRequests[MY_PERMISSIONS_RECORD_AUDIO - 1]++ == 0) {
                    if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED))
                        Toast.makeText(this, "Bez záznamu hlasitosti nebude měření fungovat.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Nelze opakovaně žádat o stejná oprávnění. Prosím, povolte oprávnění v nastevení telefonu.", Toast.LENGTH_LONG).show();
                }
                break;
            }
            case MY_PERMISSIONS_ACCESS_LOCATION: {
                if (permissionsRequests[MY_PERMISSIONS_ACCESS_LOCATION - 1]++ == 0) {
                    if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED))
                        Toast.makeText(this, "Bez přesné polohy nebude měření fungovat.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Nelze opakovaně žádat o stejná oprávnění. Prosím, povolte oprávnění v nastevení telefonu.", Toast.LENGTH_LONG).show();
                }
                break;
            }
            case MY_PERMISSIONS_EXTERNAL_STORAGE: {
                if (permissionsRequests[MY_PERMISSIONS_EXTERNAL_STORAGE - 1]++ == 0) {
                    if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED))
                        Toast.makeText(this, "Bez použití externího úložiště budou data ukládána v paměti telefonu.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Nelze opakovaně žádat o stejná oprávnění. Prosím, povolte oprávnění v nastevení telefonu.", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    public void showData(DataModel dm) {
        ArrayList<String> dataLines = dm.getGroupedDataPointsAsStrings();

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, dataLines);
        dataWindow.setAdapter(adapter);

        dataWindow.setOnItemClickListener((adapterView, view, position, l) -> {
            String value = adapter.getItem(position);
            int sessionOfItem = Integer.parseInt(value.split("\\)")[0]);
            if (sessionOfItem != session) {
                RadioButton open = findViewById(R.id.radio_open);
                RadioButton send = findViewById(R.id.radio_send);
                RadioButton delete = findViewById(R.id.radio_delete);
                if (open.isChecked()) {
                    openMapIntent(value);
                } else if (send.isChecked()) {
                    sendDataPointsToServer(sessionOfItem);
                } else if (delete.isChecked()) {
                    dm.deleteDataPointsBySession(sessionOfItem);
                    showData(dm);
                }
            } else {
                Toast.makeText(this, "Nelze rozkliknout právě probíhající měření.", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void openMapIntent(String msg) {
        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra("item", msg);
        startActivity(intent);
    }

    private void sendDataPointsToServer(int sessionOfItem) {
        Toast.makeText(this, "Není naimplementováno.", Toast.LENGTH_LONG).show();
    }

    private void infoButtonClickListener() {
        Intent intent = new Intent(this, InfoActivity.class);
        startActivity(intent);
    }

    private void onOffSwitchClickListener(Switch v) {
        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }
        if (locationListener == null) {
            locationListener = new LocationChangeListener(this, this);
        }
        if(v.isChecked()) {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeMs, minDistanceM, locationListener);
                            return;
                        }
                    }
                }
                Toast.makeText(this, "Nejsou povolena všechna oprávnění.", Toast.LENGTH_LONG).show();
                v.setChecked(false);
            } catch (Exception e) {
                Toast.makeText(this, "Error switch" + e.getMessage(), Toast.LENGTH_LONG).show();
                v.setChecked(false);
            }
        } else {
            // Odregistrace updatů lokace
            locationManager.removeUpdates(locationListener);
            // Kontrola a odstranění osamocených data pointů
            dm.deleteSoloDataPoints();
            // Nasekání cest
            LatLng lastPosition = null;
            Long lastDatetimeMillis = (long) 0;
            int lastId = 0;
            int id_from = 0;
            int part = 1;
            DataPoint lastDataPoint = null;
            double meters = 0.0;
            // čas a vzdálenost pro nasekání cest
            final int maxTimeMs = 300000;
            final float maxDistanceM = 100;
            List<DataPoint> dataPoints = dm.getDataPoints(session);
            for (DataPoint dataPoint : dataPoints) {
                if(lastPosition != null) {
                    if (dataPoint.dt - lastDatetimeMillis < maxTimeMs) {
                        meters += getDistanceInMeters(lastPosition.latitude, dataPoint.lat,
                                lastPosition.longitude, dataPoint.lon);
                        if (meters >= maxDistanceM) {
                            meters = 0;
                            dm.addStreetData(id_from, dataPoint.id, part++, 0, 0,
                                    0, 0, 0);
                            id_from = dataPoint.id;
                        }
                        dm.updateDataPoints(dataPoint.id, part);
                    } else {
                        meters = 0;
                        dm.addStreetData(id_from, dataPoint.id, part++, 0, 0,
                                0, 0, 0);
                        id_from = dataPoint.id;
                        dm.updateDataPoints(dataPoint.id, part);
                    }
                } else {
                    id_from = dataPoint.id;
                    dm.updateDataPoints(dataPoint.id, part);
                }
                lastPosition = new LatLng(dataPoint.lat, dataPoint.lon);
                lastDatetimeMillis = dataPoint.dt;
                lastId = dataPoint.id;
            }
            if (meters > 0) {
                dm.addStreetData(id_from, lastId, part, 0, 0,
                        0, 0, 0);
            }
            // Inkrement session
            SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("session_prefs", 0);
            session = sharedPreferences.getInt("session", 0);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("session", ++session);
            editor.apply();
        }
    }

    public void checkPermissionButtons() {
        Button locationButton = findViewById(R.id.location_permission);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button_save));
            locationButton.setClickable(false);
        } else {
            locationButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button_deny));
            locationButton.setClickable(true);
        }
        Button microphoneButton = findViewById(R.id.microphone_permission);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            microphoneButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button_save));
            microphoneButton.setClickable(false);
        } else {
            microphoneButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button_deny));
            microphoneButton.setClickable(true);
        }
        Button storageButton = findViewById(R.id.storage_permission);
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            storageButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button_save));
            storageButton.setClickable(false);
        } else {
            storageButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button_deny));
            storageButton.setClickable(true);
        }
    }
}