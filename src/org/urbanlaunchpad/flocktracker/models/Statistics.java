package org.urbanlaunchpad.flocktracker.models;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.text.Html;
import android.text.Spanned;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.TrackerAlarm;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class Statistics {

  private Context context;

  public int surveysCompleted = 0;
  private Calendar startTripTime = null;
  private double tripDistance = 0; // distance in meters
  private double distanceDelta = 0;
  private double totalDistanceBefore = 0;
  private int ridesCompleted = 0;
  private List<Address> addresses;
  private Metadata metadata;

  private static final double SECONDS_PER_HOUR = 3600;

  public Statistics(Context context, Metadata metadata) {
    this.context = context;
    this.metadata = metadata;
  }

  public CharSequence getCurrentAddress() {
    if (metadata.getCurrentLocation() != null) {
      new Thread(new Runnable() {
        public void run() {
          try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            addresses = geocoder.getFromLocation(metadata.getCurrentLocation().getLatitude(),
                metadata.getCurrentLocation().getLongitude(), 1);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }).start();
    }

    if (addresses != null) {
      // Get the first address
      Address address = addresses.get(0);

      String addressText = String.format("%s%s%s",
          address.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0) + ", " : "",
          address.getLocality() != null ? address.getLocality() + ", " : "",
          address.getCountryName()
      );

      return addressText;
    } else {
      return context.getString(R.string.default_address);
    }
  }

  public CharSequence getElapsedTime() {
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

  public int getSurveysCompleted() {
    return surveysCompleted;
  }

  public void setSurveysCompleted(int surveysCompleted) {
    this.surveysCompleted = surveysCompleted;
  }

  public Calendar getStartTripTime() {
    return startTripTime;
  }

  public void setStartTripTime(Calendar startTripTime) {
    this.startTripTime = startTripTime;
  }

  public Spanned getFormattedTripDistance() {
    int distanceBeforeDecimal = (int) (tripDistance / 1000.0);
    int distanceAfterDecimal = (int) Math.round(100 * (tripDistance / 1000.0 - distanceBeforeDecimal));
    return Html.fromHtml("<b>" + String.format("%02d", distanceBeforeDecimal) + "</b>" + "."
        + String.format("%02d", distanceAfterDecimal));
  }

  public double getTripDistance() {
    return tripDistance;
  }

  public void setTripDistance(double tripDistance) {
    this.tripDistance = tripDistance;
  }

  public double getDistanceDelta() {
    return distanceDelta;
  }

  public void setDistanceDelta(double distanceDelta) {
    this.distanceDelta = distanceDelta;
  }

  public String getFormattedTotalDistance() {
    return String.format("%.2f", (totalDistanceBefore + tripDistance) / 1000.0);
  }

  public double getTotalDistanceBefore() {
    return totalDistanceBefore;
  }

  public void setTotalDistanceBefore(double totalDistanceBefore) {
    this.totalDistanceBefore = totalDistanceBefore;
  }

  public int getRidesCompleted() {
    return ridesCompleted;
  }

  public void setRidesCompleted(int ridesCompleted) {
    this.ridesCompleted = ridesCompleted;
  }

  // Get average speed over last tracker interval in kph
  public double getSpeed() {
    return distanceDelta / (TrackerAlarm.TRACKER_INTERVAL) * SECONDS_PER_HOUR;
  }

  public void updateLocation(boolean isTripStarted, Location location) {
    // update location + distance
    if (isTripStarted) {
      distanceDelta = metadata.getCurrentLocation().distanceTo(location);
      tripDistance += distanceDelta;
    }
  }
}
