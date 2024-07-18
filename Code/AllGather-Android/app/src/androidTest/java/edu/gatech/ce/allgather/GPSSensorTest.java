//package edu.gatech.ce.allgather;
//
//import static androidx.test.espresso.Espresso.onView;
//import static androidx.test.espresso.assertion.ViewAssertions.matches;
//import static androidx.test.espresso.matcher.ViewMatchers.withId;
//import static androidx.test.espresso.matcher.ViewMatchers.withText;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyFloat;
//import static org.mockito.ArgumentMatchers.anyLong;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.doAnswer;
//import static org.mockito.Mockito.when;
//
//import android.content.Context;
//import android.location.Location;
//import android.location.LocationListener;
//import android.location.LocationManager;
//
//import androidx.test.core.app.ApplicationProvider;
//import androidx.test.ext.junit.runners.AndroidJUnit4;
//
//import org.junit.Before;
//import org.junit.Rule;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//
//import edu.gatech.ce.allgather.ui.camera.CameraFragment;
//
//
//@RunWith(AndroidJUnit4.class)
//class GPSSensorTest {
//
//    @Mock
//    private LocationManager mockLocationManager;
//
//    @Mock
//    private Location mockLocation;
//
//    private CameraFragment cameraFragmentActivity;
//
//    @Before
//    public void setup() {
//        MockitoAnnotations.initMocks(this);
//
//        // Mock the LocationManager in the application context
//        Context context = ApplicationProvider.getApplicationContext();
//        when(context.getSystemService(Context.LOCATION_SERVICE)).thenReturn(mockLocationManager);
//
//        // When requestLocationUpdates is called, trigger onLocationChanged with the mock location
//        doAnswer(invocation -> {
//            LocationListener listener = invocation.getArgument(3);
//            listener.onLocationChanged(mockLocation);
//            return null;
//        }).when(mockLocationManager).requestLocationUpdates(anyString(), anyLong(), anyFloat(), any(LocationListener.class));
//    }
//
//    @Test
//    public void testMainActivityState() {
//        // Use Espresso to check if the state of MainActivity has been changed to "GPS Obtained"
//        onView(withId(R.id.gpsSignalTextView)).check(matches(withText(R.string.gps_signal_connected)));
//    }
//}
