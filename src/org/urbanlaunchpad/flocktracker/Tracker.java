package org.urbanlaunchpad.flocktracker;


import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

public class Tracker extends BroadcastReceiver {
	private Surveyor surveyor = null;

	@SuppressLint("Wakelock")
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("Tracker", "Starting alarm");
		PowerManager pm = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(
				PowerManager.PARTIAL_WAKE_LOCK, "");
		wl.acquire();
		
//		newThreasubmitlocation();
//		surveyor = new Surveyor();
		// surveyor.dummy();
		// surveyor.getApplicationContext();
//		this.surveyor = context.getApplicationContext().;
		// this.surveyor = (Surveyor) context;
		// surveyor.dummy();
		surveyor.newThreasubmitlocation();

		Toast.makeText(context, "Uploading location!", Toast.LENGTH_LONG)
				.show();

		wl.release();
	}

	public void SetTracker(Context context, Surveyor main) {
		this.surveyor = main;
		Toast.makeText(context, "Setting tracker!", Toast.LENGTH_LONG)
		.show();
		Log.d("Tracker", "Setting tracker");
//		Log.d("Tracker", "Setting tracker, wait:" + trackerWait);				       
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(this.surveyor, Tracker.class);
		PendingIntent pi = PendingIntent.getBroadcast(this.surveyor, 0, i, 0);
		am.setRepeating(AlarmManager.RTC_WAKEUP,
				System.currentTimeMillis(), 5000, pi);
		Log.d("Tracker", "Tracker working.");
	}

	public void CancelTracker(Context context) {
		Toast.makeText(context, "Cancelling tracker!", Toast.LENGTH_LONG)
				.show();
		Intent intent = new Intent(this.surveyor, Tracker.class);
		PendingIntent sender = PendingIntent
				.getBroadcast(context, 0, intent, 0);
		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(sender);
	}

}