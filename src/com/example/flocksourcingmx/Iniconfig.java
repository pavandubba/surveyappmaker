package com.example.flocksourcingmx;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class Iniconfig extends Activity implements View.OnClickListener {

	TextView filefeedback;
	EditText urlfield;
	String url;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_iniconfig);

		filefeedback = (TextView) findViewById(R.id.filefeedback);
		urlfield = (EditText) findViewById(R.id.eturlfield);

		Button download = (Button) findViewById(R.id.bdownload);
		Button cont = (Button) findViewById(R.id.bcontinue);
		ImageView ulp_link = (ImageView) findViewById(R.id.ulp_icon_link);
		
		download.setOnClickListener(this);
		cont.setOnClickListener(this);
		ulp_link.setOnClickListener(this);

	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.bdownload:
			JSONObject jsurv;
			String jsonsurveystring = null;
			url = urlfield.getText().toString();
			filefeedback.setText("Downloading file: " + url
					+ "\n Please wait...");
			downloadFile(url);
			filefeedback.setText(url
					+ "\n Downloaded succesfully \n Starting parsing of file.");
			try {
				jsonsurveystring = readFileAsString("/FlockTracker/survey.json");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			filefeedback.setText("Contents of file: \n" + jsonsurveystring);
			 try {
		            jsurv = new JSONObject(jsonsurveystring);
		            filefeedback.setText("Survey file succesfully parsed.");
		        } catch (JSONException e) {
		            Log.e("JSON Parser", "Error parsing data " + e.toString());
		            filefeedback.setText("Survey file not parsed, contains errors.");
		        }
			 
			break;
		case R.id.bcontinue:
			startActivity(Couhes.class);
			break;
		case R.id.ulp_icon_link:
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_VIEW);
			intent.addCategory(Intent.CATEGORY_BROWSABLE);
			intent.setData(Uri.parse("http://www.urbanlaunchpad.org/"));
			startActivity(intent);
			break;
		}
	}

	private void startActivity(Class<? extends Activity> activityClass) {
		Intent intent = new Intent();
		intent.setClassName(getPackageName(), activityClass.getName());
		startActivity(intent);
	}

	public void downloadFile(String urlstring) {
		// File downloader, it needs android 2.3 (Gingerbread) or later to work and 3.2 (Honeycomb) to show progress bar.
		File sdCard = Environment.getExternalStorageDirectory();
		String flockFolderName = "FlockTracker";
		File FlockFolder = new File(sdCard.getAbsolutePath() + "/" + flockFolderName);
		if(!FlockFolder.exists())
		{
			FlockFolder.mkdir(); 
		}		
		DownloadManager.Request request = new DownloadManager.Request(
				Uri.parse(urlstring));
		request.setDescription("" + urlstring);
		request.setTitle("Survey download");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			request.allowScanningByMediaScanner();
			request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		}
		request.setDestinationInExternalPublicDir(
				// Environment.DIRECTORY_DOWNLOADS, "name-of-the-file.ext");;
				"/" + flockFolderName, "survey.json");

		// get download service and enqueue file
		DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
		manager.enqueue(request);
	}
	
	public static String readFileAsString(String filePath) throws java.io.IOException
	{
	    BufferedReader reader = new BufferedReader(new FileReader(filePath));
	    String line, results = "";
	    while( ( line = reader.readLine() ) != null)
	    {
	        results += line;
	    }
	    reader.close();
	    return results;
	}

}
