package com.example.diplomka;

public class FullData {
    public Long dt;
    public double lat;
    public double lon;
    public float noise;
    public int sidewalk;
    public int sidewalk_width;
    public int green;
    public int comfort;

    public FullData(long dt, double lat, double lon, float noise, int sidewalk, int sidewalk_width,
                    int green, int comfort) {
        this.dt = dt;
        this.lat = lat;
        this.lon = lon;
        this.noise = noise;
        this.sidewalk = sidewalk;
        this.sidewalk_width = sidewalk_width;
        this.green = green;
        this.comfort = comfort;
    }
}
