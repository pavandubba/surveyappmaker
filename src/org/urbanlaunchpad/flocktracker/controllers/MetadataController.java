package org.urbanlaunchpad.flocktracker.controllers;

import android.location.Location;
import com.google.android.gms.location.LocationListener;
import org.urbanlaunchpad.flocktracker.models.Metadata;

public class MetadataController implements LocationListener{
  private Metadata metadata;

  public MetadataController(Metadata metadata) {
    this.metadata = metadata;
  }

  @Override
  public void onLocationChanged(Location location) {
    metadata.setLatitude(location.getLatitude());
    metadata.setLongitude(location.getLongitude());
    metadata.setAltitude(location.getAltitude());
  }
}
