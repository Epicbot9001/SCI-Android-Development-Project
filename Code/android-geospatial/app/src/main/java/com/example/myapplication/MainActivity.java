package com.example.myapplication;


import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.content.pm.PackageManager;
import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;

import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.myapplication.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.io.WKTReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.InputStream;
import java.util.List;



public class MainActivity extends AppCompatActivity {

    private final String EPSG2240 = "PROJCS[\"NAD83 / Georgia West (ftUS)\",GEOGCS[\"NAD83\",DATUM[\"North_American_Datum_1983\",SPHEROID[\"GRS 1980\",6378137,298.257222101,AUTHORITY[\"EPSG\",\"7019\"]],AUTHORITY[\"EPSG\",\"6269\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4269\"]],UNIT[\"US survey foot\",0.3048006096012192,AUTHORITY[\"EPSG\",\"9003\"]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",30],PARAMETER[\"central_meridian\",-84.16666666666667],PARAMETER[\"scale_factor\",0.9999],PARAMETER[\"false_easting\",2296583.333],PARAMETER[\"false_northing\",0],AUTHORITY[\"EPSG\",\"2240\"],AXIS[\"X\",EAST],AXIS[\"Y\",NORTH]]";

    private final String EPSG4326 = "GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]]";

    private final String PROJ_4_2240 = "+proj=tmerc +lat_0=30 +lon_0=-84.16666666666667 +k=0.9999 +x_0=699999.9998983998 +y_0=0 +ellps=GRS80 +datum=NAD83 +to_meter=0.3048006096012192 +no_defs";
    private final String PROJ_4_4326 = "+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs";

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        writeToFile(runGeospatial());
    }

    public StringBuilder runGeospatial() {
        long startTime = System.nanoTime();
        long endTime = System.nanoTime();

        Log.d("logging", "start");
        // Create object handling performing all reading of geojson, and geospatial queries
        Spatial spatial = new Spatial();
        InputStream is;
        //Open geojson containing all curves in georgia with capped buffer
        //This is file is required for method 2 of geospatial queries. For method 1 use 'curves.geojson'
        try {
            is = getAssets().open("curve_inventory.geojson");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //Create r-tree datastructure holding all the curves from read in geojson
        STRtree collection = spatial.generate_collection(is);

        Log.d("logging", "made r-tree datastructure");
        //Create CRS transform object to convert from GPS CRS to georgia CRS
        Transform gps_georgia_crs = new Transform(PROJ_4_4326, PROJ_4_2240);
        Log.d("logging", "created transform");

        List<String[]> gps_points;
        InputStream drive;
        //Open csv containing all GPS points from a drive
        try {
            drive = getAssets().open("drive.csv");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //Extract list of GPS points from CSV
        try {
            gps_points = spatial.get_gps_point_list(drive);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Log.d("logging", "finished reading in csv");
        endTime = System.nanoTime();
        Log.d("logging", Double.toString((endTime - startTime) / 1000000.0));
        WKTReader wktReader = new WKTReader();
        final double MAX_SEARCH_DISTANCE = 100;
        StringBuilder output = new StringBuilder();
        //Creating output CSV that will be analyzed later
        output.append("lat,lon,c_segid,c_sectid,c_id,time(ms)\n");

        //We will loop through each GPS point and perform the following operations
        // 1) read it in as well known text.
        // 2) Apply CRS transform from GPS CRS to georgia CRS
        // 3) Apply buffer radius around GPS point and create a bounding box
        // 4) Query the r tree data structure to return all curves that intersect with our bounding box. (R tree ensures this query is a logarithmic operation)
        // 5) Of the nearest curves, find which buffered curve the GPS point is within and closest too
        // 6) Output curve id, GPS coordinate, and time taken to output stringbuilder
        for (String[] gps_point : gps_points) {
            startTime = System.nanoTime();
            String lat = gps_point[0];
            String lon = gps_point[1];
            String point_wkt = String.format("POINT (%s %s)", lon, lat);
            Geometry pointToCheck = null;
            try {
                pointToCheck = wktReader.read(point_wkt);
            } catch (org.locationtech.jts.io.ParseException e) {
                throw new RuntimeException(e);
            }

            Geometry geo = gps_georgia_crs.transformPoint(pointToCheck);

            Envelope boundingBox = geo.buffer(MAX_SEARCH_DISTANCE).getEnvelopeInternal();

            List<Curve> nearby_curves = collection.query(boundingBox);
            String[] curveID = spatial.get_nearest_curve_v2(nearby_curves, geo);

            endTime = System.nanoTime();
            if (curveID != null) {
                output.append(String.format("%s,%s,%s,%s,%s,%f\n", lat, lon, curveID[0], curveID[1], curveID[2], (endTime - startTime) / 1000000.0));
            } else {
                output.append(String.format("%s,%s,%s,%s,%s,%f\n", lat, lon, "null", "null", "null", (endTime - startTime) / 1000000.0));
            }
        }
        Log.d("logging", "done looping over gps points");
        return output;
    }

    public void writeToFile(StringBuilder output) {
        //Ask user permissions to access writing to external storage
        ActivityCompat.requestPermissions(this,
                new String[]{WRITE_EXTERNAL_STORAGE},
                PackageManager.PERMISSION_GRANTED);
        StorageManager storageManager = (StorageManager) getSystemService(STORAGE_SERVICE);
        StorageVolume storageVolume = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            storageVolume = storageManager.getStorageVolumes().get(0);
        }
        File fileOutput = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            fileOutput = new File(storageVolume.getDirectory().getPath() + "/Download/debug.csv");
        }
        Log.d("logging", "creating file");
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(fileOutput);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        try {
            fout.write(output.toString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Log.d("logging", "wrote file");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}