package com.example.diplomka;

public interface ILocationListenActivity {
    void locationChanged(long timestamp, double latitude, double longitude, double noise, int accuracyInMeters);
}
