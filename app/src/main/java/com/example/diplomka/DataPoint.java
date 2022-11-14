package com.example.diplomka;

public class DataPoint {

    public int id;
    public int session;
    public Long dt;
    public float lat;
    public float lon;
    public float noise;

    public DataPoint(int id, int ses, long dt, float lat, float lon, float noise) {
        this.id = id;
        this.session = ses;
        this.dt = dt;
        this.lat = lat;
        this.lon = lon;
        this.noise = noise;
    }

    public DataPoint(int ses, long dt, float lat, float lon, float noise) {
        this.id = 0;
        this.session = ses;
        this.dt = dt;
        this.lat = lat;
        this.lon = lon;
        this.noise = noise;
    }

    public DataPoint() {
    }
}