package org.urbanlaunchpad.flocktracker.controllers;

import android.location.Location;
import com.google.android.gms.location.LocationListener;
import org.urbanlaunchpad.flocktracker.ProjectConfig;
import org.urbanlaunchpad.flocktracker.models.Metadata;

public class MetadataController implements LocationListener{
  private Metadata metadata;

  public MetadataController() {
    this.metadata = ProjectConfig.get().getMetadata();
  }

  @Override
  public void onLocationChanged(Location location) {
    metadata.setLatitude(location.getLatitude());
    metadata.setLongitude(location.getLongitude());
    metadata.setAltitude(location.getAltitude());
  }
}
