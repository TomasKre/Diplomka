package com.example.diplomka;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

public class SoundMeter {

    private AudioRecord audioRecord;
    private int minSize;
    private Context context;
    private final String TAG = "SoundMeter";
    public static double REFERENCE = 0.00002;

    public SoundMeter (Context ctx) {
        context = ctx;
    }

    public void start() {
        minSize = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minSize);
            audioRecord.startRecording();
            return;
        }
        Toast.makeText(context, "Není povoleno nahrávání zvuku.", Toast.LENGTH_LONG).show();
    }

    public void stop() {
        if (audioRecord != null) {
            audioRecord.stop();
        }
    }

    public double getAmplitude() {
        Log.v(TAG, "getAmplitude");
        short[] buffer = new short[minSize];
        audioRecord.read(buffer, 0, minSize);
        int max = 0;
        int average = 0;
        for (short s : buffer)
        {
            int energy = Math.abs(s);
            average += energy;
            if (energy > max)
            {
                max = Math.abs(s);
            }
        }
        double x = average / minSize;
        double pressure = x / 51805.5336; //the value 51805.5336 can be derived from asuming that x=32767=0.6325 Pa and x=1 = 0.00002 Pa (the reference value)
        double db = (20 * Math.log10(pressure / REFERENCE));
        Log.v(TAG, "avarage = " + x);
        Log.v(TAG, "db = " + db);
        return db;
    }

}