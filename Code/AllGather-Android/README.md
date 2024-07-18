# AllGather


Android app for collecting roadway data through a dashboard mounted mobile device

## Running

Built APKs are available [here](code/android/allgather/app/build/outputs/apk/debug).
1. Connect Android device to a computer using USB cable.
2. Swipe down to see notifications. There will be a notification to select what the USB cable does. Tap on it and make sure "File Transfer" is chosen.
3. The phone's internal storage will show up as a drive on your computer. Copy and paste one of the APKs to the `download` directory.
4. Open the Download directory on the phone. There will either be a File Manager app or Downloads app to achieve this. 
5. Tap on the APK to install it. Provide all necessary permissions.
6. Tap on the installed app to run it.

## Data Storage

### Acceleration File

It's a csv file. Open with any text editor. **DO NOT OPEN WITH EXCEL** because it removes significant digits from the timestamp data.

The first row gives the orientation of the smartphone. See v1.2 update. 

The next row is the header. Rows after that is the csv data. Here are the columns:
- `timestamp_nanosecond`: system nanosecond timestamp when data point was taken.
- `local_timestamp_milliseconds`: system UTC timestamp in milliseconds since epoch (UNIX time). 
- `accel_x_mps2`: linear acceleration in +x direction in SI units.
- `accel_y_mps2`: linear acceleration in +y direction in SI units.
- `accel_z_mps2`: linear acceleration in +z direction in SI units.
- `rotation_x_sin_theta_by_2`: xsin(&theta;/2) component of unit quaternion
- `rotation_y_sin_theta_by_2`: ysin(&theta;/2) component of unit quaternion
- `rotation_z_sin_theta_by_2`: zsin(&theta;/2) component of unit quaternion
- `rotation_cos_theta_by_2`: cos(&theta;/2) component of unit quaternion
- `angvelocity_x_radps`: angular velocity around +x axis in rad/s
- `angvelocity_y_radps`: angular velocity around +y axis in rad/s
- `angvelocity_z_radps`: angular velocity around +z axis in rad/s
- `yaw`: yaw in degree
- `roll`: roll in degree
- `pitch`: pitch in degree

For details about the reference system, see [here](https://developer.android.com/guide/topics/sensors/sensors_overview#sensors-coords) and [here](https://developer.android.com/guide/topics/sensors/sensors_motion.html#sensors-motion-rotate)

### Location File

It's a csv file. Open with any text editor.  **DO NOT OPEN WITH EXCEL** because it removes significant digits from the timestamp data.

First row is headers. After that is the csv data. Here are the columns:
- `timestamp_utc_gps`: GPS UTC timestamp in milliseconds since epoch (UNIX time).
- `timestamp_utc_local`: system UTC timestamp in milliseconds since epoch (UNIX time).
- `latitude_dd`: latitude in decimal degrees.
- `longitude_dd`: longitude in decimal degrees.
- `altitude_m`: altitude above mean sea level in meters.
- `bearing_deg`: bearing in degrees.
- `accuracy_m`: accuracy in meters.

### Video

Video is .mp4 file format. H264 codec. 30 fps. A recording can have multiple video files as the recording is restarted after a file size reaches approx 1 GB. The file name is in the format`<a>_<b>.mp4` where `<a)` is the timestamp when the overall recording was started. This string is treated as the unique identifier for the recording (we assume no 2 recordings will start at the same millisecond). `<b>` is the system UNIX time in milliseconds since epoch at which this particular file was started.

### Connecting everything together

System time is used to register all 3 folders together. 
- Each record in the acceleration file has the local UNIX time in the column `local_timestamp_milliseconds`
- Each record in the location file has the local UNIX time in the column `timestamp_utc_local`
- Each video file has the local UNIX time of the first frame in the file name (`<b>`). The frame rate is 30fps. So local UNIX time of each frame can be calculated. Note that there is some time gap when a video 1GB video is stopped and the next one started. So don't just use the first video in a recording's timestamp. For a frame, use the timestamp of the video to which it belongs to.

Note that although system time will be the same across all 3 data folders, the absolute value of the system time itself might be wrong if the device's local time is wrong (maybe internal clock died because of not charging for a long time, etc.). It's not a big deal since the actual time is still available from the GPS time.

### Data Format Update Log

##### v2.0
Now Use CameraX API instead of Camera2.  
Now use liveData API to store the data.  
Improved UI and temporarily removed the setting view.  
Added Deprecated Folder to store the old APIs.  
Added Yaw, Pitch and Roll in the output acc file.  

##### v1.5
Before this version, location files saved the GPS time while acceleration and video name were according to local time, which can have a slight offset (or a large one if local time is wrong). Now the location file has a column `utc_timestamp_local` which contains the local time. So now everything has a local timestamp. GPS time is still saved in the location file as `utc_timestamp_gps`.

##### v1.4
Encryption now made optional. Not changeable by UI, only by [code](code/android/allgather/app/src/main/java/edu/gatech/ce/allgather/AllGatherApplication.kt).

When encryption is off, location and acceleration are saved to csv files. When encrytion is on, see [v1.3](#v1.3).

##### v1.3
Encryption implemented. Acceleration files now have the  `.acx` extension. Location files now have the `.lox` extension.

To decrypt files, run [Decryptor](tools/decryptor/). Compile using `javac Decryptor` and run using `java Decryptor [key] [path to folder with .lox, .acx files]`
The key is `gtbuzz2018`

##### v1.2
In acceleration file, first line now contains orientation. Orientation is coded as follows:
- 0 = upright portrait
- 1 = landscape left (with the phone facing you and in upright portrait orientation, rotate 90 degrees counter clockwise)
- 2 = upside down portrait (not used)
- 3 = landscape right (with the phone facing you and in upright portrait orientation, rotate 90 degrees clockwise)

Example: If the orientation is landscape left, the first line of the acceleration file should now read:

`orientation,1`
##### v1.1
Data shifted to folder format root/driverID/phoneID/

##### v1.0

## Mapbox Vision Integration
Experimental, please see mapbox vision branch.

## ISSUES

### Overheat

Exposing to the sunlight will cause most smartphones to overheat. Once overheated, the smartphone will shutdown and the recording will be lost. To prevent overheat, place smartphone in the shade if possible. A white phone, AC and window vent will also help.



