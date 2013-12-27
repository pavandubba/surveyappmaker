package org.urbanlaunchpad.flocktracker;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;

public class Welcomes extends Activity implements OnClickListener {

	// private long splashDelay = 1000; // 1 second for debugging.
	private long splashDelay = 5000; // 6 seconds.

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		// Remove notification bar
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setContentView(R.layout.activity_welcomes);

		final MediaPlayer cuca_play = MediaPlayer.create(Welcomes.this,
				R.raw.cucaracha);
		findViewById(R.id.app_big_icon_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						cuca_play.start();
					}
				});


		findViewById(R.id.mobility_futures_colaborative_button)
				.setOnClickListener(this);
		findViewById(R.id.mit_button).setOnClickListener(this);
		findViewById(R.id.urban_launchpad_button).setOnClickListener(this);

	}
	
	@Override
	protected void onResume() {
		super.onResume();
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
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.welcomes, menu);
		return true;
	}

	@Override
	public void onClick(View v) {
		Integer ID = v.getId();
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.addCategory(Intent.CATEGORY_BROWSABLE);
		if (ID == R.id.mobility_futures_colaborative_button){
			intent.setData(Uri
					.parse("http://dusp.mit.edu/transportation/project/mobility-futures-collaborative"));
			startActivity(intent);
		} else if (ID==R.id.mit_button ){
			intent.setData(Uri.parse("http://web.mit.edu/"));
			startActivity(intent);
		} else if (ID == R.id.urban_launchpad_button){
			intent.setData(Uri.parse("http://www.urbanlaunchpad.org/"));
			startActivity(intent);
		}
	}

}
