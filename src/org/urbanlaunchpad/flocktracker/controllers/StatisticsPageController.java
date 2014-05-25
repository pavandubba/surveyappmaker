package org.urbanlaunchpad.flocktracker.controllers;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;
import org.urbanlaunchpad.flocktracker.*;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

// TODO(adchia): finish refactoring
public class StatisticsPageController {
  public int surveysCompleted = 0;
  public Location startLocation;
  SurveyorActivity surveyorActivity;
  private Calendar startTripTime = null;
  private double tripDistance = 0; // distance in meters
  private double distanceDelta = 0;
  private double totalDistanceBefore = 0;
  private int ridesCompleted = 0;
  private List<Address> addresses;
  private static final double SECONDS_PER_HOUR = 3600;

  public StatisticsPageController(SurveyorActivity surveyorActivity) {
    this.surveyorActivity = surveyorActivity;
    // Load statistics from previous run-through
    totalDistanceBefore = IniconfigActivity.prefs.getFloat("tripDistanceBefore", 0);
    ridesCompleted = IniconfigActivity.prefs.getInt("ridesCompleted", 0);
    surveysCompleted = IniconfigActivity.prefs.getInt("surveysCompleted", 0);
  }

  public void updateStatusPage() {
    TextView tripTimeText = (TextView) surveyorActivity.findViewById(R.id.tripTime);
    if (tripTimeText != null) {
      TextView tripDistanceText = (TextView) surveyorActivity.findViewById(R.id.tripDistance);
      TextView totalDistanceText = (TextView) surveyorActivity.findViewById(R.id.totalDistance);
      TextView currentAddressText = (TextView) surveyorActivity.findViewById(R.id.currentAddress);
      TextView usernameText = (TextView) surveyorActivity.findViewById(R.id.user_greeting);
      TextView surveysCompletedText = (TextView) surveyorActivity.findViewById(R.id.surveysCompleted);
      TextView ridesCompletedText = (TextView) surveyorActivity.findViewById(R.id.ridesCompleted);

      // TODO Make this addaptable to different languages.
      usernameText.setText("Hi " + ProjectConfig.get().getUsername() + "!");
      surveysCompletedText.setText(Integer.toString(surveysCompleted));
      ridesCompletedText.setText(Integer.toString(ridesCompleted));
      tripTimeText.setText(getElapsedTime());
      currentAddressText.setText(getCurrentAddress());

      int distanceBeforeDecimal = (int) (tripDistance / 1000.0);
      int distanceAfterDecimal = (int) Math
          .round(100 * (tripDistance / 1000.0 - distanceBeforeDecimal));

      tripDistanceText.setText(Html.fromHtml("<b>"
          + String.format("%02d", distanceBeforeDecimal)
          + "</b>" + "."
          + String.format("%02d", distanceAfterDecimal)));
      totalDistanceText.setText(""
          + String.format(
          "%.2f",
          (totalDistanceBefore + tripDistance) / 1000.0));
    }
  }

  public void onPause() {
    IniconfigActivity.prefs.edit().putInt("ridesCompleted", ridesCompleted)
        .commit();
    IniconfigActivity.prefs.edit().putInt("surveysCompleted", surveysCompleted)
        .commit();
    IniconfigActivity.prefs.edit()
        .putFloat("totalDistanceBefore", (float) totalDistanceBefore)
        .commit();
  }

  public void onLocationChanged(boolean isTripStarted, Location location) {
    // update location + distance
    if (isTripStarted) {
      distanceDelta = startLocation.distanceTo(location);
      tripDistance += distanceDelta;
    }
    startLocation = location;
  }

  public void startTrip() {
    startTripTime = Calendar.getInstance();
  }

  public void stopTrip() {
    startTripTime = null;
    ridesCompleted++;
    totalDistanceBefore += tripDistance;
    tripDistance = 0;
  }

  // Get average speed over last tracker interval in kph
  public Double getSpeed() {
    return distanceDelta / (TrackerAlarm.TRACKER_INTERVAL) * SECONDS_PER_HOUR;
  }

  private CharSequence getElapsedTime() {
    if (startTripTime != null) {
      Calendar difference = Calendar.getInstance();
      difference.setTimeInMillis(difference.getTimeInMillis()
          - startTripTime.getTimeInMillis());
      return Html.fromHtml("<b>"
          + String.format("%02d", difference.getTime()
          .getMinutes())
          + "</b>:"
          + String.format("%02d", difference.getTime()
          .getSeconds()));
    } else {
      return Html.fromHtml("<b>00</b>:00");
    }
  }

  private CharSequence getCurrentAddress() {
    if (surveyorActivity.mLocationClient.isConnected()) {
      startLocation = surveyorActivity.mLocationClient.getLastLocation();
    }

    if (startLocation != null) {
      Log.d("Startlocation", "Not null");
      new Thread(new Runnable() {
        public void run() {
          try {
            Geocoder geocoder = new Geocoder(
                surveyorActivity, Locale.getDefault());
            // Location current =
            // mLocationClient.getLastLocation();
            addresses = geocoder.getFromLocation(
                startLocation.getLatitude(),
                startLocation.getLongitude(), 1);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }).start();
    }

    if (addresses != null && addresses.size() > 0) {
      // Get the first address
      Address address = addresses.get(0);

            /*
             * Format the first line of address (if available),
             * city, and country name.
             */
      String addressText = String.format("%s%s%s",
          address.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0) + ", " : "",
          address.getLocality() != null ? address.getLocality() + ", " : "",
          address.getCountryName()
      );

      return addressText;
    } else {
      return surveyorActivity.getString(R.string.default_address);
    }
  }
}
