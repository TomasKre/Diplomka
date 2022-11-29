package com.example.diplomka;

public class StreetData {

    public int id;
    public int from;
    public int to;
    public int part;
    public int sidewalk;
    public int sidewalk_width;
    public int green;
    public int comfort;
    public boolean isInput;

    public StreetData(int id, int from, int to, int part, int sidewalk, int sidewalk_width,
                      int green, int comfort, boolean isInput) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.part = part;
        this.sidewalk = sidewalk;
        this.sidewalk_width = sidewalk_width;
        this.green = green;
        this.comfort = comfort;
        this.isInput = isInput;
    }

    public StreetData(int from, int to, int part, int sidewalk, int sidewalk_width, int green,
                      int comfort, boolean isInput) {
        this.id = 0;
        this.from = from;
        this.to = to;
        this.part = part;
        this.sidewalk = sidewalk;
        this.sidewalk_width = sidewalk_width;
        this.green = green;
        this.comfort = comfort;
        this.isInput = isInput;
    }

    public StreetData() {
    }
}