package com.example.diplomka;

public class DataPoint {

    public int id;
    public Long dt;
    public float lat;
    public float lon;
    public float noise;

    public DataPoint(int id, long dt, float lat, float lon, float noise) {
        this.id = id;
        this.dt = dt;
        this.lat = lat;
        this.lon = lon;
        this.noise = noise;
    }

    public DataPoint(long dt, float lat, float lon, float noise) {
        this.id = 0;
        this.dt = dt;
        this.lat = lat;
        this.lon = lon;
        this.noise = noise;
    }

    public DataPoint() {

    }
}