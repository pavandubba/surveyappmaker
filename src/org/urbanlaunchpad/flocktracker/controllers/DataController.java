package org.urbanlaunchpad.flocktracker.controllers;

import android.location.Location;
import com.google.android.gms.location.LocationListener;
import org.urbanlaunchpad.flocktracker.models.Metadata;
import org.urbanlaunchpad.flocktracker.models.Statistics;

public class DataController {
  private Metadata metadata;
  private Statistics statistics;

  public DataController(Metadata metadata, Statistics statistics) {
    this.metadata = metadata;
    this.statistics = statistics;
  }

  public void onLocationChanged(boolean isTripStarted, Location location) {
    statistics.updateLocation(isTripStarted, location);
    metadata.setCurrentLocation(location);
  }
}
