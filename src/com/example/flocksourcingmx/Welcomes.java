package com.example.flocksourcingmx;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Intent;

public class Welcomes extends Activity {

	private long splashDelay = 1000; // 1 second for debugging.
	// private long splashDelay = 6000; // 6 seconds.

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_welcomes);
		final MediaPlayer cuca_play = MediaPlayer.create(Welcomes.this,
				R.raw.cucaracha);
		findViewById(R.id.background).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						cuca_play.start();
					}
				});

		TimerTask finish_splash = new TimerTask() {
			@Override
			public void run() {
				Intent Iniconfig = new Intent().setClass(Welcomes.this,
						Iniconfig.class);
				startActivity(Iniconfig);
				finish();
			}
		};

		Timer timer = new Timer();
		timer.schedule(finish_splash, splashDelay);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.welcomes, menu);
		return true;
	}
}
