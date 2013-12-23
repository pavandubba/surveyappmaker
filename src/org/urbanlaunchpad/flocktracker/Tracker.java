package org.urbanlaunchpad.flocktracker;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

public class Tracker extends BroadcastReceiver {
	static Surveyor surveyor;
	
	@SuppressLint("Wakelock")
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("Tracker", "Starting alarm");
		PowerManager pm = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(
				PowerManager.PARTIAL_WAKE_LOCK, "");
		wl.acquire();

		new Thread(new Runnable() {
			public void run() {
				surveyor.submitLocation();
			}
		}).start();
		
		Toast.makeText(context, "Uploading location!", Toast.LENGTH_LONG)
				.show();

		wl.release();
	}
	
}