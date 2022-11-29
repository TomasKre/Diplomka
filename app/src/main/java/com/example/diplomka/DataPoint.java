package com.example.diplomka;

public class DataPoint {

    public int id;
    public int session;
    public Long dt;
    public double lat;
    public double lon;
    public float noise;
    public int part;

    public DataPoint(int id, int ses, long dt, double lat, double lon, float noise, int part) {
        this.id = id;
        this.session = ses;
        this.dt = dt;
        this.lat = lat;
        this.lon = lon;
        this.noise = noise;
        this.part = part;
    }

    public DataPoint(int ses, long dt, double lat, double lon, float noise, int part) {
        this.id = 0;
        this.session = ses;
        this.dt = dt;
        this.lat = lat;
        this.lon = lon;
        this.noise = noise;
        this.part = part;
    }

    public DataPoint() {
    }
}