Documentation

This repo contains a proof of concept on how to run geospatial queries on an edge device like an Android phone. We use
the JTS library to perform the geospatial queries. 
https://github.com/locationtech/jts
https://locationtech.github.io/jts/

From this library we use the Geometry classes to represent a GPS point and curves. We also employ the STRtree class,
as it is JTS's implementation of the R-Tree data structure to efficiently perform geospatial queries. This data structure
allows for queries to be performed in logerithmic time rather than linear time.

The following is the general workflow. 
1. We first load in a geojson file containing all the curves in Georgia.
2. Next we load in all the curves into the R-Tree data structure. This will allow us to perform queries where we can find all curves that are within a certain bounding box, rather than manually looping through all the curves.
3. Next we load in all the GPS points in a run from a CSV.
4. We loop over all the GPS points and perform the following steps on each one:

    a. Convert the GPS point to Georgia CRS

    b. Create a bounding box around the GPS Point

    c. Query the R-Tree datastructure to find all curves that intersect with or are contained by the bounding box

    d. See which curve the GPS point falls into and record that curve ID. 


Files:
- `MainActivity.java` contains the entrance to the mobile application. This sets up the app with a dummy button and starts the data processing.
- `Spatial.java` contains all the code to read in geojson data, read in a csv of GPS data points, and perform geospatial queries.
- `Curve.java` contains a class definition to hold the 3 fields of a curve ID in 1 object.
- `Transform.java` contains a class with methods to convert from a source CRS to a target CRS.

Running application:
This program can be run either on an emulator or an actual phone. In both scenarios once the application is loaded, the results of the app will be stored in the 'Downloads' folder in the phone files. From here you can email the results file to yourself. The name of the file you are looking for is 'debug.csv'.

The application will load all relevant files in the background and perform all the geospatial queries in the background as well. Once it is done performing all the geospatial queries and writing the results to the 'debug.csv' file it will then display a screen with a button. This button is present just as a visual indicator that the application is done running, but does not functionally do anything.

Phone:
If running on an actual phone, ensure that the phone is setup for development. You can turn on Developer Options on the device by searching 'Developer Options' in the settings of the device, then turning the option on. Also be sure to turn on USB debugging under developer options as well.
Once the phone is plugged in, on the top right corner there should be a drop down with the name of the phone. If you click the play button, it will load the application on the phone.


If the app is not properly loading, try following the tutorial on this site: https://developer.android.com/studio/run/device

Emulator:
To run the app on an emulator, the emulator must be downloaded and installed first. Follow the tutorial here to set up and AVD and running the app in the emulator: https://developer.android.com/studio/run/emulator.
