package com.example.diplomka;

public class StreetData {

    public int id;
    public int from;
    public int to;
    public int sidewalk;
    public int sidewalk_width;
    public int green;
    public int comfort;

    public StreetData(int id, int from, int to, int sidewalk, int sidewalk_width, int green, int comfort) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.sidewalk = sidewalk;
        this.sidewalk_width = sidewalk_width;
        this.green = green;
        this.comfort = comfort;
    }

    public StreetData(int from, int to, int sidewalk, int sidewalk_width, int green, int comfort) {
        this.id = 0;
        this.from = from;
        this.to = to;
        this.sidewalk = sidewalk;
        this.sidewalk_width = sidewalk_width;
        this.green = green;
        this.comfort = comfort;
    }

    public StreetData() {
    }
}