package com.example.myapplication;

import android.util.Log;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;

public class Spatial {

    //Function to read geojson file with curve data
    private List<Curve> read_geojson(InputStream is) throws IOException, org.locationtech.jts.io.ParseException, org.json.simple.parser.ParseException {
        List<Curve> curve_list = new ArrayList<>();
        Reader geoJsonReader = new InputStreamReader(is);

        JSONParser parser = new JSONParser();
        JSONObject geojson = (JSONObject) parser.parse(geoJsonReader);
        JSONArray features = (JSONArray) geojson.get("features");
        GeoJsonReader reader = new GeoJsonReader(new GeometryFactory(new PrecisionModel(), 2240));
        for (Object feature : features) {
            JSONObject properties = (JSONObject) ((JSONObject) feature).get("properties");
            String c_segid = (String) properties.get("c_segid");
            String c_sectid = Long.toString((Long) properties.get("c_sectid"));
            String c_id = Long.toString((Long) properties.get("c_id"));
            JSONObject geometry = (JSONObject) ((JSONObject) feature).get("geometry");
            String geojson_string = geometry.toJSONString();
            Geometry curve = reader.read(geojson_string);
            Curve new_curve = new Curve(c_segid, c_sectid, c_id, curve);
            curve_list.add(new_curve);
        }
        return curve_list;
    }

    //function to generate JTS R-tree data structure
    public STRtree generate_collection(InputStream is) {
        List<Curve> curveList = null;
        try {
            curveList = read_geojson(is);
        } catch (IOException e) {
            Log.d("logging", "Reading geojson failed");
            throw new RuntimeException(e);
        } catch (ParseException e) {
            Log.d("logging", "Reading geojson failed");
            throw new RuntimeException(e);
        } catch (org.json.simple.parser.ParseException e) {
            throw new RuntimeException(e);
        }
        Log.d("logging", "read in geojson");

        STRtree rtree = new STRtree();
        for (Curve curve: curveList) {
            rtree.insert(curve.getGeometry().getEnvelopeInternal(), curve);
        }
        return rtree;
    }

    //Read drive CSV and extract list of GPS coordinates
    public List<String[]> get_gps_point_list(InputStream is) throws IOException {
        Scanner gps_points = new Scanner(is);
        gps_points.nextLine();
        List<String[]> points = new ArrayList<>();

        while (gps_points.hasNextLine()) {
            String line = gps_points.nextLine();
            String[] array = line.split(",");
            String lat = array[2];
            String lon = array[3];
            String[] arr = {lat, lon};
            points.add(arr);
        }
        return points;
    }

    //Method 1 of geospatial querying methodology
    //Does not use buffered curves
    //Finds the closest curve
    public String[] get_nearest_curve(List<Curve> curves, Geometry gps_point) {
        double currDist = 0;
        double minDist = Double.MAX_VALUE;
        Curve selectedCurve = null;
        // Iterate through the features to check if the point is within any of the geometries
        for (Curve curve: curves) {
            Geometry curve_geometry = curve.getGeometry();
            currDist = curve_geometry.distance(gps_point);
            if (currDist < minDist) {
                minDist = currDist;
                selectedCurve = curve;
            }
        }

        if (selectedCurve == null) {
            return null;
        }
        String segid = selectedCurve.getC_segid();
        String sectid = selectedCurve.getC_sectid();
        String id = selectedCurve.getC_id();
        return new String[] {segid, sectid, id};
    }

    //Method 2: Preferred method as it is more accurate when compared to ground truth
    //uses buffered capped curves
    //finds curve that CONTAINS GPS point
    //If contained within multiple we return the closest curve
    public String[] get_nearest_curve_v2(List<Curve> curves, Geometry gps_point) {
        List<Curve> selectedCurves = new ArrayList<>();

        // Iterate through the features to check if the point is within any of the geometries
        for (Curve curve: curves) {
            Geometry curve_geometry = curve.getGeometry();
            if (curve_geometry.contains(gps_point)) {
                selectedCurves.add(curve);
            }
        }
        if (selectedCurves.size() == 0) {
            return null;
        }
        double currDist = 0;
        double minDist = Double.MAX_VALUE;
        Curve selectedCurve = null;

        for (Curve curve: selectedCurves) {
            Geometry curve_geometry = curve.getGeometry();
            currDist = curve_geometry.distance(gps_point);
            if (currDist < minDist) {
                minDist = currDist;
                selectedCurve = curve;
            }
        }
        String segid = selectedCurve.getC_segid();
        String sectid = selectedCurve.getC_sectid();
        String id = selectedCurve.getC_id();
        return new String[] {segid, sectid, id};
    }
}
