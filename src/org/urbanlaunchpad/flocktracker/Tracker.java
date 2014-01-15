package org.urbanlaunchpad.flocktracker;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

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

		if (surveyor != null) {
			new Thread(new Runnable() {
				public void run() {
					surveyor.saveLocation();
					synchronized (Surveyor.submissionQueue) {
						while (Surveyor.savingSubmission) {
							try {
								Surveyor.submissionQueue.wait();
							} catch (Exception e) {
								e.printStackTrace();
								return;
							}
						}

						if (!Surveyor.submittingSubmission) {
							surveyor.spawnSubmission();
						}
					}
				}
			}).start();
		} else {
			Intent intentAlarm = new Intent(context, Tracker.class);
			PendingIntent sender = PendingIntent.getBroadcast(context, 1,
					intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT);
			AlarmManager alarmManager = (AlarmManager) context
					.getSystemService(Context.ALARM_SERVICE);
			alarmManager.cancel(sender);
		}

		wl.release();
	}

}