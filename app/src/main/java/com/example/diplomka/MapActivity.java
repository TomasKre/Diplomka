package com.example.diplomka;

import static com.example.diplomka.Tools.getDistanceInMeters;
import static com.example.diplomka.Tools.getHumanDate;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.diplomka.databinding.ActivityMapBinding;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnPolylineClickListener, ISendDataActivity {

    private GoogleMap mMap;
    private ActivityMapBinding binding;
    private DataModel dm;
    private String msg;
    private int session;
    private List<DataPoint> dataPoints;
    private List<StreetData> dataStreets;
    private List<Polyline> polylineList;
    private List<DataPoint> markers;
    private int maxPart;
    private int allPaths = 0;
    private int greenPaths = 0;
    private Context ctx;
    private View popupAsyncView;
    private PopupWindow popupAsyncWindow;

    //mezi gps měřeními
    private static final int maxTimeMs = 300000;
    private static final float maxDistanceM = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ctx = getApplicationContext();
        dm = new DataModel(ctx);

        //getExtra
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if(extras == null) {
                msg = null;
            } else {
                msg = extras.getString("item");
            }
        } else {
            msg = (String) savedInstanceState.getSerializable("item");
        }

        session = Integer.parseInt(msg.split("\\)")[0]);
        dataPoints = dm.getDataPoints(session);
        dataStreets = dm.getStreetData(session);
        polylineList = new ArrayList<>();
        markers = new ArrayList<>();
        maxPart = 0;

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Button send_button = findViewById(R.id.send_button);
        send_button.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.send_data_title);
            builder.setMessage(MessageFormat.format(getString(R.string.send_data_message_long), session));

            builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                sendStreetDataAndDataPointsToServer(session);
                dialog.dismiss();
                createLoadingPopup();
            });
            builder.setNegativeButton(R.string.no, (dialog, which) -> {
                // Do nothing
                dialog.dismiss();
            });
            AlertDialog alert = builder.create();
            alert.show();
        });
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        Log.d("Map", getResources().getString(R.string.night_mode));
        if (getResources().getString(R.string.night_mode).equals("night")) {
            try {
                // Customise the styling of the base map using a JSON object defined in a raw resource file.
                boolean success = googleMap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(
                                this, R.raw.map_style_night));
                if (!success) {
                    Log.e("Map", "Style parsing failed.");
                }
            } catch (Resources.NotFoundException e) {
                Log.e("Map", "Can't find style. Error: ", e);
            }
        }

        googleMap.setOnMapLongClickListener(latLng -> onMapLongClick(latLng));

        LatLng lastPosition = null;
        Long lastDatetimeMillis = (long) 0;
        int lastId = 0;
        int lastPart = 0;
        int id_from = 0;
        int part = 1;
        DataPoint lastDataPoint = null;
        // Případně color ve tvaru int 0xAARRGGBB
        PolylineOptions polylineOptions = new PolylineOptions().clickable(true).color(ContextCompat.getColor(this, R.color.denied));
        for (DataPoint dataPoint : dataPoints) {
            if (lastPosition != null) {
                LatLng position = new LatLng(dataPoint.lat, dataPoint.lon);
                if (dataPoint.dt - lastDatetimeMillis < maxTimeMs) {
                    polylineOptions.add(position);
                    if (dataPoint.part != lastPart) {
                        for (StreetData dataStreet : dataStreets) {
                            if (dataStreet.part == lastPart) {
                                if (dataStreet.isInput) {
                                    polylineOptions.color(ContextCompat.getColor(ctx, R.color.accepted));
                                    greenPaths++;
                                }
                                allPaths++;
                                Polyline polyline = googleMap.addPolyline(polylineOptions);
                                polylineList.add(polyline);
                                polylineOptions = new PolylineOptions().clickable(true)
                                        .color(ContextCompat.getColor(this, R.color.denied));
                                polylineOptions.add(position);
                                if (dataPoint.part > maxPart)
                                    maxPart = dataPoint.part;
                                mMap.addMarker(new MarkerOptions().position(position)
                                        .title(getHumanDate(dataPoint.dt)));
                                markers.add(dataPoint);
                            }
                        }
                        lastPart = dataPoint.part;
                    }
                } else {
                    Polyline polyline = googleMap.addPolyline(polylineOptions);
                    polylineList.add(polyline);
                    polylineOptions = new PolylineOptions().clickable(true).color(ContextCompat.getColor(this, R.color.denied));
                    polylineOptions.add(position);
                    lastPart = dataPoint.part;
                    if (dataPoint.part > maxPart)
                        maxPart = dataPoint.part;
                    mMap.addMarker(new MarkerOptions().position(position).title(getHumanDate(dataPoint.dt)));
                    markers.add(dataPoint);
                    allPaths++;
                }
            } else {
                LatLng position = new LatLng(dataPoint.lat, dataPoint.lon);
                polylineOptions.add(position);
                lastPart = dataPoint.part;
                if (dataPoint.part > maxPart)
                    maxPart = dataPoint.part;
                mMap.addMarker(new MarkerOptions().position(position).title(getHumanDate(dataPoint.dt)));
                markers.add(dataPoint);
            }
            lastPosition = new LatLng(dataPoint.lat, dataPoint.lon);
            lastDatetimeMillis = dataPoint.dt;
            lastDataPoint = dataPoint;
        }
        for (StreetData dataStreet : dataStreets) {
            if (dataStreet.part == lastPart) {
                if (dataStreet.isInput) {
                    polylineOptions.color(ContextCompat.getColor(ctx, R.color.accepted));
                    greenPaths++;
                }
                allPaths++;
                Polyline polyline = googleMap.addPolyline(polylineOptions);
                polylineList.add(polyline);
                polylineOptions = new PolylineOptions().clickable(true)
                        .color(ContextCompat.getColor(this, R.color.denied));
                mMap.addMarker(new MarkerOptions().position(lastPosition)
                        .title(getHumanDate(lastDatetimeMillis)));
                markers.add(lastDataPoint);
            }
        }

        // Custom map marker takto:
        /*mMap.addMarker(new MarkerOptions().position(lastPosition).title(getHumanDate(dataPoint.dt))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker)));*/

        // Místo pouhého spojování bodů lze nakreslit cestu https://abhiandroid.com/programming/googlemaps

        checkSendButton();
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastPosition, 18.0f));
        //lze vylepšit zazoomováním na těžiště bodů, místo na poslední bod
        // případně i zoomem dle max N-S a W-E

        // Set listeners for click events.
        googleMap.setOnPolylineClickListener(this);
    }

    //@Override
    public void onMapLongClick(LatLng clickedLatLng) {
        // Iterate through the list of polylines and find the one that is closest to the long press location
        double minDistanceLines = Double.MAX_VALUE;
        double minDistancePoints;
        Polyline closestPolyline = null;
        LatLng nearestPoint = null;
        LatLng nearestPointLine = null;
        for (Polyline polyline : polylineList) {
            minDistancePoints = Double.MAX_VALUE;
            for (LatLng point : polyline.getPoints()) {
                double distancePoints = getDistanceInMeters(clickedLatLng, point);
                if (distancePoints < minDistancePoints) {
                    nearestPoint = point;
                    minDistancePoints = distancePoints;
                }
            }
            double distanceLines = getDistanceInMeters(clickedLatLng, nearestPoint);
            if (distanceLines < minDistanceLines) {
                minDistanceLines = distanceLines;
                closestPolyline = polyline;
                nearestPointLine = nearestPoint;
            }
        }
        if (minDistanceLines > 100.0F) {
            Toast.makeText(this, R.string.map_faraway_long_click, Toast.LENGTH_LONG).show();
            return;
        }
        if (closestPolyline != null) {
            boolean isMarkered = false;
            //Možný update přidat k datapointům info jestli je v nich marker? Místo 2 forů
            for (DataPoint marker : markers) {
                if (nearestPointLine.latitude == marker.lat && nearestPointLine.longitude == marker.lon) {
                    isMarkered = true;
                    break;
                }
            }
            // Pokud nejbližší bod longclicku nemá marker projdi body dané části a najdi, který je to bod
            if (!isMarkered) {
                for (DataPoint dataPoint : dataPoints) {
                    if (nearestPointLine.latitude == dataPoint.lat && nearestPointLine.longitude == dataPoint.lon) {
                        // Přidej marker
                        mMap.addMarker(new MarkerOptions().position(nearestPointLine).title(getHumanDate(dataPoint.dt)));
                        markers.add(dataPoint);
                        ++maxPart;

                        // Načti body dané session a čášti, uprav na nové části
                        List<DataPoint> oldPoints = dm.getDataPoints(session, dataPoint.part);
                        dm.updateSplitStreetData(dataPoint.id, dataPoint.part, maxPart);
                        dm.updateSplitDataPoints(dataPoint.session, dataPoint.dt, dataPoint.part, maxPart);

                        // Zjištění, zda byla čára user inputovaná
                        boolean isInput = false;
                        PolylineOptions polylineOptions;
                        if (closestPolyline.getColor() != ContextCompat.getColor(this, R.color.denied)) {
                            isInput = true;
                            polylineOptions = new PolylineOptions().clickable(true).color(ContextCompat.getColor(this, R.color.accepted));
                            greenPaths++;
                        } else {
                            polylineOptions = new PolylineOptions().clickable(true).color(ContextCompat.getColor(this, R.color.denied));
                        }
                        allPaths++;

                        // Algoritmus lze zjednodušit oproti onMapReady, jelikož podmínky času a vzdálenosti jsou již splněny
                        // Projdi všechny body dané části a utvoř nové 2 polyline, starou odstraň
                        Polyline polyline;
                        List<LatLng> polylinePoints = closestPolyline.getPoints();
                        for (DataPoint oldPoint : oldPoints) {
                            LatLng latLng = new LatLng(oldPoint.lat, oldPoint.lon);
                            if (polylinePoints.size() > 1)
                                polylinePoints.remove(latLng);
                            polylineOptions.add(latLng);
                            if (oldPoint.lat == nearestPointLine.latitude && oldPoint.lon == nearestPointLine.longitude) {
                                polyline = mMap.addPolyline(polylineOptions);
                                polylineList.add(polyline);
                                if (isInput) {
                                    polylineOptions = new PolylineOptions().clickable(true).color(ContextCompat.getColor(this, R.color.accepted));
                                } else {
                                    polylineOptions = new PolylineOptions().clickable(true).color(ContextCompat.getColor(this, R.color.denied));
                                }
                                polylineOptions.add(latLng);
                            }
                        }
                        // Postupné odstraňování bodů staré polyline a poslední bod, který zůstal v listu by měl být ten poslední
                        polylineOptions.add(polylinePoints.get(0));
                        polyline = mMap.addPolyline(polylineOptions);
                        polylineList.add(polyline);
                        polylineList.remove(closestPolyline);
                        closestPolyline.remove();
                        checkSendButton();
                        break;
                    }
                }
            } else {
                Toast.makeText(this, R.string.map_marker_long_click, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onPolylineClick(@NonNull Polyline polyline) {
        List<LatLng> geoPoints = polyline.getPoints();
        LatLng pointA = geoPoints.get(0);
        LatLng pointB = geoPoints.get(geoPoints.size() - 1);
        int from = 0, to = 0;
        // Asi není nejelegantnější řešení, ale cesty nejspíše nebudou mít počet bodů v řádech tisíců
        // Jsou pouze z jedné session
        for (DataPoint dataPoint : dataPoints) {
            if(dataPoint.lat == pointA.latitude && dataPoint.lon == pointA.longitude) {
                from = dataPoint.id;
            }
            if(dataPoint.lat == pointB.latitude && dataPoint.lon == pointB.longitude) {
                to = dataPoint.id;
            }
        }

        dataStreets = dm.getStreetData(session);
        StreetData streetDataClicked = null;
        for (StreetData streetData:dataStreets) {
            if (streetData.from == from && streetData.to == to)
                streetDataClicked = streetData;
        }

        // inflate the layout of the popup window
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_input_data, null);

        Spinner spinnerSidewalk = (Spinner) popupView.findViewById(R.id.sidewalk_spinner);
        ArrayAdapter<CharSequence> adapterSidewalk = ArrayAdapter.createFromResource(this,
                R.array.sidewalk_array, android.R.layout.simple_spinner_item);
        adapterSidewalk.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSidewalk.setAdapter(adapterSidewalk);

        Spinner spinnerSidewalkWidth = (Spinner) popupView.findViewById(R.id.sidewalk_width_spinner);
        ArrayAdapter<CharSequence> adapterSidewalkWidth = ArrayAdapter.createFromResource(this,
                R.array.sidewalk_width_array, android.R.layout.simple_spinner_item);
        adapterSidewalkWidth.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSidewalkWidth.setAdapter(adapterSidewalkWidth);

        Spinner spinnerGreen = (Spinner) popupView.findViewById(R.id.green_spinner);
        ArrayAdapter<CharSequence> adapterGreen = ArrayAdapter.createFromResource(this,
                R.array.green_array, android.R.layout.simple_spinner_item);
        adapterGreen.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGreen.setAdapter(adapterGreen);

        Spinner spinnerComfort = (Spinner) popupView.findViewById(R.id.safespace_spinner);
        ArrayAdapter<CharSequence> adapterComfort = ArrayAdapter.createFromResource(this,
                R.array.comfort_array, android.R.layout.simple_spinner_item);
        adapterComfort.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerComfort.setAdapter(adapterComfort);

        if (streetDataClicked != null) {
            spinnerSidewalk.setSelection(streetDataClicked.sidewalk);
            spinnerSidewalkWidth.setSelection(streetDataClicked.sidewalk_width);
            spinnerGreen.setSelection(streetDataClicked.green);
            spinnerSidewalkWidth.setSelection(streetDataClicked.sidewalk_width);
        }

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        // focusable true by default
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height);

        Button buttonSave = (Button) popupView.findViewById(R.id.save_button);
        Button buttonCancel = (Button) popupView.findViewById(R.id.cancel_button);
        if (from != 0 && to != 0) {
            int finalFrom = from;
            int finalTo = to;
            buttonSave.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_CANCEL:
                        case MotionEvent.ACTION_DOWN:
                            break;
                        case MotionEvent.ACTION_UP:
                            popupWindow.dismiss();
                            dm.updateStreetData(finalFrom, finalTo, spinnerSidewalk.getSelectedItemPosition(),
                                    spinnerSidewalkWidth.getSelectedItemPosition(), spinnerGreen.getSelectedItemPosition(),
                                    spinnerComfort.getSelectedItemPosition());
                            if(polyline.getColor() == ContextCompat.getColor(ctx, R.color.denied)) {
                                greenPaths++;
                                checkSendButton();
                                polyline.setColor(ContextCompat.getColor(ctx, R.color.accepted));
                            }
                            break;
                    }
                    return true;
                }
            });
        }

        buttonCancel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_DOWN:
                        break;
                    case MotionEvent.ACTION_UP:
                        popupWindow.dismiss();
                        break;
                }
                return true;
            }
        });

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0);
    }

    private void checkSendButton() {
        Log.v("Map paths", "Green paths: " + greenPaths + "/" + allPaths);
        Button send_button = findViewById(R.id.send_button);
        if (allPaths == greenPaths && allPaths > 0) {
            send_button.setClickable(true);
            send_button.setBackground(ContextCompat.getDrawable(this, R.drawable.button_save_border));
        } else {
            send_button.setClickable(false);
            send_button.setBackground(ContextCompat.getDrawable(this, R.drawable.button_deny_border));
        }
    }

    private void sendStreetDataAndDataPointsToServer(int session) {
        ArrayList<DataPoint> points = dm.getDataPoints(session);
        ArrayList<StreetData> streets = dm.getStreetData(session);
        ArrayList<FullData> out = new ArrayList<>();

        StreetData lastStreet = null;
        for (DataPoint point:
             points) {
            int id = point.id;
            boolean last = false;
            for (StreetData street:
                 streets) {
                if (id >= street.from && id <= street.to) {
                    last = true;
                    lastStreet = street;
                    if (id < street.to) {
                        out.add(new FullData(point.dt, point.lat, point.lon, point.noise, street.sidewalk,
                                street.sidewalk_width, street.green, street.comfort));
                        last = false;
                        break;
                    }
                }
            }
            if (last) {
                out.add(new FullData(point.dt, point.lat, point.lon, point.noise, lastStreet.sidewalk,
                        lastStreet.sidewalk_width, lastStreet.green, lastStreet.comfort));
            }
        }

        FullDataNamedArray finalJson = new FullDataNamedArray(out);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        String arrayToJson = "";
        try {
            arrayToJson = objectMapper.writeValueAsString(finalJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        HTTP http = new HTTP(this,getString(R.string.target_server_upload_fulldata));
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

        // Set info text
        TextView textView = popupAsyncView.findViewById(R.id.info_text);
        textView.setText(R.string.HTTP_send_sending);

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        // focusable true by default
        popupAsyncWindow = new PopupWindow(popupAsyncView, width, height);
        popupAsyncWindow.showAtLocation(popupAsyncView, Gravity.CENTER, 0, 0);

        Button send_button = findViewById(R.id.send_button);
        send_button.setClickable(false);
    }

    public void finishLoadingPopup() {
        ImageView imageView = popupAsyncView.findViewById(R.id.image_progress);
        // Stop animation and change source image
        imageView.setAnimation(null);
        imageView.setImageResource(R.drawable.check_mark);

        // Change info text
        TextView textView = popupAsyncView.findViewById(R.id.info_text);
        textView.setText(R.string.HTTP_send_success);

        dm.deleteDataPointsBySession(session);
        dm.deleteStreetDataBySession(session);

        Handler handler = new Handler();
        handler.postDelayed(() -> {
            Button send_button = findViewById(R.id.send_button);
            send_button.setClickable(true);
            popupAsyncWindow.dismiss();
            finish();
            }, 5000);
    }

    public void cancelLoadingPopup() {
        ImageView imageView = popupAsyncView.findViewById(R.id.image_progress);
        // Stop animation and change source image
        imageView.setAnimation(null);
        imageView.setImageResource(R.drawable.cross);

        // Change info text
        TextView textView = popupAsyncView.findViewById(R.id.info_text);
        textView.setText(R.string.HTTP_send_unsuccess);

        Handler handler = new Handler();
        handler.postDelayed(() -> {
            Button send_button = findViewById(R.id.send_button);
            send_button.setClickable(true);
            popupAsyncWindow.dismiss();
            }, 5000);
    }
}