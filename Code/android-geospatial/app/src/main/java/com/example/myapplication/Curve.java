package com.example.myapplication;

import org.locationtech.jts.geom.Geometry;

public class Curve {
    private String c_segid;
    private String c_sectid;
    private String c_id;
    private Geometry geometry;

    public Curve(String c_segid, String c_sectid, String c_id, Geometry geometry) {
        this.c_segid = c_segid;
        this.c_sectid = c_sectid;
        this.c_id = c_id;
        this.geometry = geometry;
    }

    public String getC_segid() {
        return c_segid;
    }

    public String getC_sectid() {
        return c_sectid;
    }

    public String getC_id() {
        return c_id;
    }

    public Geometry getGeometry() {
        return geometry;
    }
}
