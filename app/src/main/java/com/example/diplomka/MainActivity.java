package com.example.diplomka;

import static com.example.diplomka.Tools.getDistanceInMeters;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;

import android.content.Intent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.android.gms.maps.model.LatLng;

public class MainActivity extends AppCompatActivity implements ISendDataActivity, ILocationListenActivity {

    private LocationManager locationManager;
    private LocationListener locationListener;
    private int session = 0;
    private int sessionOfItem;
    private DataModel dm;
    private final int minTimeMs = 2500;
    private final int minDistanceM = 5;
    private int[] permissionsRequests = new int[3];
    private View popupAsyncView;
    private PopupWindow popupAsyncWindow;
    private ListView dataWindow;
    private int startX;
    private int startLockX;
    private boolean locked = false;
    private final int maxLockMove = 150;
    private int startArrowX;
    // čas a vzdálenost pro nasekání cest
    private final int maxTimeMs = 300000;
    private final float maxDistanceM = 100;
    private boolean checked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        incrementSession();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button locationButton = findViewById(R.id.location_permission);
        Button microphoneButton = findViewById(R.id.microphone_permission);
        Button storageButton = findViewById(R.id.storage_permission);
        Button realtimeMapButton = findViewById(R.id.realtime_measure_button);
        ImageView lock = findViewById(R.id.lock);
        ImageView infoButton = findViewById(R.id.info_button);
        Switch onOffSwitch = findViewById(R.id.measure_switch);
        dataWindow = findViewById(R.id.data_window);

        dm = new DataModel(this);

        checkPermissionButtons();
        locationButton.setOnClickListener(v -> requestLocationPermission());
        microphoneButton.setOnClickListener(v -> requestMicrophonePermissions());
        storageButton.setOnClickListener(v -> requestExternalStoragePermissions());
        realtimeMapButton.setOnClickListener(v -> openRealtimeMapIntent());
        lock.setOnTouchListener((v, event) -> lockTouchEvent((ImageView) v, event));
        infoButton.setOnClickListener(v -> infoButtonClickListener());
        onOffSwitch.setOnClickListener(v -> {
            Log.d("Switch Button", "onClick");
            onOffSwitchClickListener((Switch) v);
        });

        onOffSwitch.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Switch switchSlider = (Switch) v;
                // Normální chování view (posun atp.)
                switchSlider.onTouchEvent(event);
                // K změně stavu slouží click event, toto je ošetření Touch eventu
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Uložit stav na začátku move eventu
                        Log.d("Switch Button", "onTouch Event:DOWN");
                        checked = switchSlider.isChecked();
                        break;
                    case MotionEvent.ACTION_UP:
                        // Vrátit do původního stavu
                        Log.d("Switch Button", "onTouch Event:UP");
                        switchSlider.setChecked(checked);
                        break;
                }
                return true;
            }
        });

    }

    private boolean lockTouchEvent(ImageView lock, MotionEvent event) {
        int x = (int) event.getX();
        ImageView arrows = findViewById(R.id.slide_arrow);
        WindowManager.LayoutParams WMLP = getWindow().getAttributes();
        switch (event.getAction()) {
            case MotionEvent.ACTION_CANCEL:
                return false;
            case MotionEvent.ACTION_DOWN:
                // Zaznamenání polohy při dotyku na zámek
                startX = x;
                startLockX = (int) lock.getX();
                startArrowX = (int) arrows.getX();
                Log.d("Lock DOWN", "Touch X:" + x);
                Log.d("Lock DOWN", "Image X:" + startLockX);
                break;
            case MotionEvent.ACTION_MOVE:
                // Pohyb obrázkem na ose X při slidu po zámku
                int newX = Math.max(-maxLockMove, Math.min(maxLockMove, x - startX));
                Log.v("Lock MOVE", "X - startX:" + x);
                if (x - startX > 0 && !locked) {
                    lock.setX(startLockX + newX);
                }
                if (startX - x > 0 && locked) {
                    lock.setX(startLockX + newX);
                }
                break;
            case MotionEvent.ACTION_UP:
                Log.d("Lock UP", "X:" + x);
                if (locked) {
                    // Odemčení UI
                    if (startX - x > maxLockMove) {
                        // Odebrání šipek pod zámkem
                        arrows.setRotation(0);
                        arrows.setX(startArrowX + maxLockMove);
                        // Rozsvícení obrazovky
                        WMLP.screenBrightness = 1.0f;
                        getWindow().setAttributes(WMLP);
                        // Odemčení UI
                        enableUI(findViewById(R.id.main_layout), true);
                        enableUI(findViewById(R.id.radio_group), true);
                        locked = false;
                        // Přenastavení pozice a zdroje ImageView
                        lock.setX(startLockX - maxLockMove);
                        if (getResources().getString(R.string.night_mode).equals("night")) {
                            lock.setImageResource(R.drawable.lock_unlocked_white);
                        } else {
                            lock.setImageResource(R.drawable.lock_unlocked);
                        }
                    } else {
                        // Vrácení na původní pozici při neúplném pohybu
                        lock.setX(startLockX);
                    }
                } else {
                    // Zamčení UI
                    if (x - startX > maxLockMove) {
                        // Navrácení šipek pod zámek
                        arrows.setRotation(180);
                        arrows.setX(startArrowX - maxLockMove);
                        // Zhasnutí obrazovky
                        WMLP.screenBrightness = 0.0f;
                        getWindow().setAttributes(WMLP);
                        // Zamčení UI
                        enableUI(findViewById(R.id.main_layout), false);
                        enableUI(findViewById(R.id.radio_group), false);
                        locked = true;
                        // Přenastavení pozice a zdroje ImageView
                        lock.setX(startLockX + maxLockMove);
                        if (getResources().getString(R.string.night_mode).equals("night")) {
                            lock.setImageResource(R.drawable.lock_locked_white);
                        } else {
                            lock.setImageResource(R.drawable.lock_locked);
                        }
                    } else {
                        // Vrácení na původní pozici při neúplném pohybu
                        lock.setX(startLockX);
                    }
                }
                break;
        }
        return true;
    }

    @Override
    public void onResume() {
        Log.v("Activity lifecycle", "onResume");
        checkPermissionButtons();
        showData(dm);
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.v("Activity lifecycle", "onPause");
        // Vrácení zhasínání obrazovky na system default
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Odregistrace updatů lokace
        if (locationManager != null)
            locationManager.removeUpdates(locationListener);
        // Kontrola a odstranění osamocených data pointů a cest
        dm.deleteSoloDataPoints();
        // Nasekání cest
        chopPathIntoParts();
        // Odčeknutí switche
        Switch measureSwitch = findViewById(R.id.measure_switch);
        measureSwitch.setChecked(false);
        // Inkrement session
        incrementSession();
        // Překreslení dat
        showData(dm);
        super.onPause();
    }

    private final int MY_PERMISSIONS_EXTERNAL_STORAGE = 3;
    private final int MY_PERMISSIONS_ACCESS_LOCATION = 2;
    private final int MY_PERMISSIONS_RECORD_AUDIO = 1;

    protected void requestExternalStoragePermissions() {
        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE},MY_PERMISSIONS_EXTERNAL_STORAGE);
        }
    }

    protected void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_ACCESS_LOCATION);
        }
    }

    protected void requestMicrophonePermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_RECORD_AUDIO);
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
                        Toast.makeText(this, R.string.microphone_perm_deny, Toast.LENGTH_LONG).show();
                } else {
                    sendUserToSettings();
                }
                checkPermissionMicrophone();
                break;
            }
            case MY_PERMISSIONS_ACCESS_LOCATION: {
                if (permissionsRequests[MY_PERMISSIONS_ACCESS_LOCATION - 1]++ == 0) {
                    if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED))
                        Toast.makeText(this, R.string.location_perm_deny, Toast.LENGTH_LONG).show();
                } else {
                    sendUserToSettings();
                }
                checkPermissionLocation();
                break;
            }
            case MY_PERMISSIONS_EXTERNAL_STORAGE: {
                if (permissionsRequests[MY_PERMISSIONS_EXTERNAL_STORAGE - 1]++ == 0) {
                    if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED))
                        Toast.makeText(this, R.string.storage_perm_deny, Toast.LENGTH_LONG).show();
                } else {
                    sendUserToSettings();
                }
                checkPermissionStorage();
                break;
            }
        }
    }

    public void sendUserToSettings() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Oprávnění k lokaci");
        builder.setMessage("Nelze opakovaně žádat o stejná oprávnění. Chcete povolit oprávnění v nastavení telefonu?");
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // User clicked "Yes"
                // Send them to settings
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", getPackageName(), null));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // User clicked "No"
                // Do nothing
            }
        });
        builder.show();
    }

    public void showData(DataModel dm) {
        ArrayList<String> dataLines = dm.getGroupedDataPointsAsStrings();

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, dataLines);
        dataWindow.setAdapter(adapter);

        dataWindow.setOnItemClickListener((adapterView, view, position, l) -> {
            String value = adapter.getItem(position);
            sessionOfItem = Integer.parseInt(value.split("\\)")[0]);
            if (sessionOfItem != session) {
                RadioButton open = findViewById(R.id.radio_open);
                RadioButton send = findViewById(R.id.radio_send);
                RadioButton delete = findViewById(R.id.radio_delete);
                if (open.isChecked()) {
                    openMapIntent(value);
                } else if (send.isChecked()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Odeslat data");
                    builder.setMessage("Opravdu chcete odeslat záznamy s id " + sessionOfItem + "?");

                    builder.setPositiveButton("Ano", (dialog, which) -> {
                        sendDataPointsToServer();
                        dialog.dismiss();
                        createLoadingPopup();
                    });
                    builder.setNegativeButton("Ne", (dialog, which) -> {
                        // Do nothing
                        dialog.dismiss();
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                } else if (delete.isChecked()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Smazat záznam");
                    builder.setMessage("Opravdu chcete smazat záznam " + sessionOfItem + "?");

                    builder.setPositiveButton("Ano", (dialog, which) -> {
                        dm.deleteDataPointsBySession(sessionOfItem);
                        dm.deleteStreetDataBySession(sessionOfItem);
                        showData(dm);
                        dialog.dismiss();
                    });
                    builder.setNegativeButton("Ne", (dialog, which) -> {
                        // Do nothing
                        dialog.dismiss();
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
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

    public void openRealtimeMapIntent() {
        // Pokud je vše povoleno, začni měřit
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                if ((ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                        && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(this, RealtimeMapActivity.class);
                    intent.putExtra("session", session);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Nejsou povolena všechna oprávnění.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Nejsou povolena všechna oprávnění.", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Nejsou povolena všechna oprávnění.", Toast.LENGTH_LONG).show();
        }
    }

    private void infoButtonClickListener() {
        Intent intent = new Intent(this, InfoActivity.class);
        startActivity(intent);
    }

    @SuppressLint("MissingPermission") // permission v manifestu je, zřejmě nějaký bug, protože ze začátku se nezobrazovalo
    private void onOffSwitchClickListener(Switch v) {
        Log.d("Switch Button", "onOff function");
        Log.d("Switch Button", "Checked:" + v.isChecked());
        return;
        /*if(v.isChecked()) {
            if (locationManager == null)
                locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (locationListener == null)
                locationListener = new LocationChangeListener(this, this);
            // Pokud je vše povoleno, začni měřit
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    if ((ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                            && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        // Nastavení flagu obrazovky, aby nezhasínala
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        // Registrace listeneru na location updaty
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeMs, minDistanceM, locationListener);
                        return;
                    }
                }
            }
            Toast.makeText(this, "Nejsou povolena všechna oprávnění.", Toast.LENGTH_LONG).show();
            v.setChecked(false);
        } else {
            // Vrácení zhasínání obrazovky na system default
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            // Odregistrace updatů lokace
            if (locationManager != null)
                locationManager.removeUpdates(locationListener);
            // Kontrola a odstranění osamocených data pointů
            dm.deleteSoloDataPoints();
            // Nasekání cest
            chopPathIntoParts();
            // Odčeknutí switche
            v.setChecked(false);
            // Inkrement session
            incrementSession();
            // Překreslení dat
            showData(dm);
        }*/
    }

    private void chopPathIntoParts() {
        LatLng lastPosition = null;
        Long lastDatetimeMillis = (long) 0;
        int lastId = 0;
        int id_from = 0;
        int part = 1;
        double meters = 0.0;
        List<DataPoint> dataPoints = dm.getDataPoints(session);
        for (DataPoint dataPoint : dataPoints) {
            if(lastPosition != null) {
                if (dataPoint.dt - lastDatetimeMillis < maxTimeMs) {
                    meters += getDistanceInMeters(lastPosition.latitude, dataPoint.lat,
                            lastPosition.longitude, dataPoint.lon);
                    if (meters >= maxDistanceM) {
                        meters = 0;
                        dm.addStreetData(session, id_from, dataPoint.id, part++, 0, 0,
                                0, 0, 0);
                        id_from = dataPoint.id;
                    }
                    dm.updateDataPoints(dataPoint.id, part);
                } else {
                    meters = 0;
                    dm.addStreetData(session, id_from, dataPoint.id, part++, 0, 0,
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
            dm.addStreetData(session, id_from, lastId, part, 0, 0,
                    0, 0, 0);
        }
    }

    private void incrementSession() {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("session_prefs", 0);
        if (session == 0)
            session = sharedPreferences.getInt("session", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("session", ++session);
        editor.apply();
    }

    private void enableUI(ViewGroup target, boolean locked) {
        int lockId = findViewById(R.id.lock).getId();
        for (int i = 0; i < target.getChildCount(); i++) {
            View child = target.getChildAt(i);
            if (child.getId() != lockId) {
                child.setEnabled(locked);
            }
        }
    }

    public void checkPermissionButtons() {
        checkPermissionLocation();
        checkPermissionMicrophone();
        checkPermissionStorage();
    }

    public void checkPermissionLocation() {
        Button locationButton = findViewById(R.id.location_permission);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button_save));
            locationButton.setClickable(false);
        } else {
            locationButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button_deny));
            locationButton.setClickable(true);
        }
    }

    public void checkPermissionMicrophone() {
        Button microphoneButton = findViewById(R.id.microphone_permission);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            microphoneButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button_save));
            microphoneButton.setClickable(false);
        } else {
            microphoneButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button_deny));
            microphoneButton.setClickable(true);
        }
    }

    public void checkPermissionStorage() {
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

    private void sendDataPointsToServer() {
        ArrayList<DataPoint> points = dm.getDataPoints(sessionOfItem);
        DataPointNamedArray finalJson = new DataPointNamedArray(points);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        String arrayToJson = "";
        try {
            arrayToJson = objectMapper.writeValueAsString(finalJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        HTTP http = new HTTP(this,"http://ulice.nti.tul.cz:5000/upload/points");
        AsyncTask<String, Void, String> result = http.execute(arrayToJson);
        Log.v("HTTP Async", result.getStatus().toString());
    }

    public void createLoadingPopup() {
        // inflate the layout of the popup window
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        popupAsyncView = inflater.inflate(R.layout.popup_async_task, null);
        ImageView imageView = popupAsyncView.findViewById(R.id.image_progress); //Initialize ImageView via FindViewById or programatically

        RotateAnimation anim = new RotateAnimation(0.0f, 360.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        // Setup anim with desired properties
        anim.setInterpolator(new LinearInterpolator());
        anim.setRepeatCount(Animation.INFINITE); // repeat animation indefinitely
        anim.setDuration(1250); // animation cycle length in milliseconds

        // Start animation
        imageView.startAnimation(anim);

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        // focusable true by default
        popupAsyncWindow = new PopupWindow(popupAsyncView, width, height);
        popupAsyncWindow.showAtLocation(popupAsyncView, Gravity.CENTER, 0, 0);

        // disable the activity
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    public void finishLoadingPopup() {
        ImageView imageView = popupAsyncView.findViewById(R.id.image_progress);
        // Stop animation and change source image
        imageView.setAnimation(null);
        imageView.setImageResource(R.drawable.check_mark);

        // Change info text
        TextView textView = popupAsyncView.findViewById(R.id.info_text);
        textView.setText("Odesláno, díky!");

        dm.deleteDataPointsBySession(session);
        dm.deleteStreetDataBySession(session);

        Handler handler = new Handler();
        handler.postDelayed(() -> {
            // enable the activity
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            dm.deleteDataPointsBySession(sessionOfItem);
            dm.deleteStreetDataBySession(sessionOfItem);
            showData(dm);
            popupAsyncWindow.dismiss();
        }, 5000);
    }

    public void cancelLoadingPopup() {
        ImageView imageView = popupAsyncView.findViewById(R.id.image_progress);
        // Stop animation and change source image
        imageView.setAnimation(null);
        imageView.setImageResource(R.drawable.cross);

        // Change info text
        TextView textView = popupAsyncView.findViewById(R.id.info_text);
        textView.setText("Odeslání se nezdařilo.");

        Handler handler = new Handler();
        handler.postDelayed(() -> {
            // enable the activity
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            popupAsyncWindow.dismiss();
        }, 5000);
    }

    @Override
    public void locationChanged(long timestamp, double latitude, double longitude, double noise, int accuracyInMeters) {
        dm.addDataPoints(timestamp, session, latitude, longitude, noise, 0);
        if (accuracyInMeters >= 0) {
            TextView accuracyTextView = findViewById(R.id.accuracy_value);
            accuracyTextView.setText(Integer.toString(accuracyInMeters));
        }
        showData(dm);
    }
}