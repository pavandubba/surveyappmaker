package org.urbanlaunchpad.flocktracker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
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
	JSONObject jsurv = null;
	private AccountManager accountManager;
	final String SCOPE = "https://www.googleapis.com/auth/fusiontables";
	private static final int AUTHORIZATION_CODE = 1993;
	private static final int ACCOUNT_CODE = 1601;	
	private String token = null;
	private String username = null;
	 
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

		// Google credentials
		accountManager = AccountManager.get(this);
		chooseAccount();
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.bdownload:
			String jsonsurveystring = null;
			url = urlfield.getText().toString();
			filefeedback.setText("Downloading file: " + url
					+ "\n Please wait...");
//			downloadFile(url); /* Start the file downloading process. */
			filefeedback.setText(url
					+ "\n Downloaded succesfully \n Starting parsing of file.");
			try {
				jsonsurveystring = readFileAsString(Environment.getExternalStorageDirectory().getAbsolutePath() + "/FlockTracker/survey.json");
				filefeedback.setText("Contents of file: \n" + jsonsurveystring);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				filefeedback.setText("Error reading file.");
				
			}
			
			 try {
		            jsurv = new JSONObject(jsonsurveystring);
		            filefeedback.setText("Survey file succesfully parsed as JSON.");
		        } catch (JSONException e) {
		            Log.e("JSON Parser", "Error parsing data " + e.toString());
		            filefeedback.setText("Survey file not parsed, contains errors.");
		        }
			 
			break;
		case R.id.bcontinue:
//			startActivity(Surveyor.class);
			Intent i = new Intent(getApplicationContext(), Surveyor.class);
			i.putExtra("jsonsurvey",jsurv.toString());
			try {
				i.putExtra("token", token);
			} catch (Exception e) {}
			startActivity(i);
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

//	private void startActivity(Class<? extends Activity> activityClass) {
//		Intent intent = new Intent();
//		intent.setClassName(getPackageName(), activityClass.getName());
//		startActivity(intent);
//	}
	
	private void invalidateToken() {
		AccountManager accountManager = AccountManager.get(this);
		accountManager.invalidateAuthToken("com.google", token);
		token = null;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {
			if (requestCode == AUTHORIZATION_CODE) {
				requestToken();
			} else if (requestCode == ACCOUNT_CODE) {
				username = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
				
				// invalidate old tokens which might be cached. we want a fresh
				// one, which is guaranteed to work
				invalidateToken();

				requestToken();
			}
		}
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
	    FileInputStream is = new FileInputStream(filePath);
	    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "ISO-8859-1"));
	    String line, results = "";
	    while( ( line = reader.readLine() ) != null)
	    {
	        results += line;
	    }
	    reader.close();
	    return results;
	}
	
	private void chooseAccount() {
		// use https://github.com/frakbot/Android-AccountChooser for
		// compatibility with older devices
		Intent intent = AccountManager.newChooseAccountIntent(null, null,
				new String[] { "com.google" }, false, null, null, null, null);
		startActivityForResult(intent, ACCOUNT_CODE);
	}

	private void requestToken() {
		Account userAccount = null;
		for (Account account : accountManager.getAccountsByType("com.google")) {
			if (account.name.equals(username)) {
				userAccount = account;

				break;
			}
		}

		accountManager.getAuthToken(userAccount, "oauth2:" + SCOPE, null, this,
				new OnTokenAcquired(), null);
	}
	
	private class OnTokenAcquired implements AccountManagerCallback<Bundle> {

		@Override
		public void run(AccountManagerFuture<Bundle> future) {
			try {
				token = future.getResult().getString(
				        AccountManager.KEY_AUTHTOKEN);
			} catch (Exception e) {
//				throw new RuntimeException(e);
			}
		}
	}
}
