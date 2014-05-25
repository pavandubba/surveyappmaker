package org.urbanlaunchpad.flocktracker.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import org.urbanlaunchpad.flocktracker.R;

public final class LocationUtil {

  public static String getLngLatAlt(Location location) {
    if (location == null) {
      return "";
    }

    // Return the latitude and longitude as strings
    return location.getLongitude() + "," + location.getLatitude() + "," + location.getAltitude();
  }

  public static void checkLocationConfig(final Context context) {
    LocationManager lm = null;
    AlertDialog.Builder dialog;
    boolean gps_enabled = false;
    boolean network_enabled = false;
    if (lm == null) {
      lm = (LocationManager) context
          .getSystemService(Context.LOCATION_SERVICE);
    }

    try {
      gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
    } catch (Exception ex) {
    }

    try {
      network_enabled = lm
          .isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    } catch (Exception ex) {
    }

    if (!gps_enabled && !network_enabled) {
      dialog = new AlertDialog.Builder(context);
      dialog.setMessage(context.getResources().getString(
          R.string.gps_network_not_enabled));
      dialog.setPositiveButton(
          context.getResources().getString(
              R.string.open_location_settings),
          new DialogInterface.OnClickListener() {

            @Override
            public void onClick(
                DialogInterface paramDialogInterface,
                int paramInt) {
              Intent myIntent = new Intent(
                  Settings.ACTION_LOCATION_SOURCE_SETTINGS);
              myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              context.startActivity(myIntent);
              // get gps
            }
          }
      );

      dialog.setNegativeButton(
          context.getString(R.string.cancel_location_settings),
          new DialogInterface.OnClickListener() {

            @Override
            public void onClick(
                DialogInterface paramDialogInterface,
                int paramInt) {

            }
          }
      );
      dialog.show();
    }
  }
}
