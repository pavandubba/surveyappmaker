package org.urbanlaunchpad.flocktracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.Arrays;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.drive.DriveScopes;

public class Iniconfig extends Activity implements View.OnClickListener {

	TextView usernameField;
	TextView projectNameField;
	ImageView cont;
	EditText input;
	String projectName = "";
	String jsonsurveystring;
	JSONObject jsurv = null;
	private String username = "";
	AlertDialog.Builder alert;
	private boolean debison = false; // If true, a test project will be loaded
										// by default.
	public static GoogleAccountCredential credential;

	static final int REQUEST_ACCOUNT_PICKER = 1;
	static final int REQUEST_PERMISSIONS = 2;
	static final String FUSION_TABLE_SCOPE = "https://www.googleapis.com/auth/fusiontables";

	private enum EVENT_TYPE {
		GOT_USERNAME, GOT_PROJECT_NAME, PARSED_CORRECTLY, PARSED_INCORRECTLY, INPUT_NAME
	}

	@SuppressLint("HandlerLeak")
	private Handler messageHandler = new Handler() {

		public void handleMessage(Message msg) {
			if (msg.what == EVENT_TYPE.GOT_USERNAME.ordinal()) {
				// have input a name. update it on interface
				usernameField.setText(username);
			} else if (msg.what == EVENT_TYPE.GOT_PROJECT_NAME.ordinal()) {
				// have input a project name. update it on interface
				projectNameField.setText(projectName);
			} else if (msg.what == EVENT_TYPE.PARSED_CORRECTLY.ordinal()) {
				// got survey!
				Toast toast = Toast.makeText(getApplicationContext(),
						"survey parsed!", Toast.LENGTH_SHORT);
				toast.show();
			} else if (msg.what == EVENT_TYPE.PARSED_INCORRECTLY.ordinal()) {
				// got bad/no survey!
				Toast toast = Toast.makeText(getApplicationContext(),
						"Could not get survey", Toast.LENGTH_SHORT);
				toast.show();
				jsurv = null;
			} else if (msg.what == EVENT_TYPE.INPUT_NAME.ordinal()) {
				input.setText(projectName);
				alert.setView(input);
				// want to display alert to get project name
				alert.show();
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
			} else {
				Log.e("Survey Parser", "Error parsing survey");
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_iniconfig);

		// initialize fields
		usernameField = (TextView) findViewById(R.id.usernameText);
		projectNameField = (TextView) findViewById(R.id.projectNameText);

		// initialize dialog for inputting project name
		alert = new AlertDialog.Builder(this);
		alert.setTitle("Select project");
		input = new EditText(this);
		alert.setView(input);

		// set listener for ok when user inputs project name
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// save the project name
				projectName = input.getText().toString().trim();

				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
				dialog.dismiss();

				if (!projectName.isEmpty()) {
					// update our interface with project name
					messageHandler.sendEmptyMessage(EVENT_TYPE.GOT_PROJECT_NAME
							.ordinal());

					parseSurvey();
				}
			}
		});

		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
						dialog.dismiss();
					}
				});

		// set listeners for rows and disable continue button
		View projectNameSelectRow = findViewById(R.id.projectNameRow);
		View usernameSelectRow = findViewById(R.id.usernameRow);
		cont = (ImageView) findViewById(R.id.bcontinue);
		usernameSelectRow.setOnClickListener(this);
		projectNameSelectRow.setOnClickListener(this);
		cont.setOnClickListener(this);

		// Debug mode, passes project without internet connection.
		DebuggingIsOn(debison);

		// get credential with scopes
		credential = GoogleAccountCredential.usingOAuth2(
				getApplicationContext(),
				Arrays.asList(new String[] { FUSION_TABLE_SCOPE,
						DriveScopes.DRIVE }));

	}

	@Override
	public void onClick(View view) {
		Integer id = view.getId();

		if (id == R.id.usernameRow) {
			// Google credentials
			// chooseAccount();
			startActivityForResult(credential.newChooseAccountIntent(),
					REQUEST_ACCOUNT_PICKER);
		} else if (id == R.id.projectNameRow) {
			if (username.isEmpty()) {
				Toast toast = Toast.makeText(getApplicationContext(),
						"Select user first please", Toast.LENGTH_SHORT);
				toast.show();
				return;
			}

			input = new EditText(this);
			alert.setView(input);

			// Show the popup dialog to get the project name
			messageHandler.sendEmptyMessage(EVENT_TYPE.INPUT_NAME.ordinal());
		} else if (id == R.id.bcontinue) {
			if (jsurv == null) {
				Toast toast = Toast.makeText(getApplicationContext(),
						"Invalid user/project!", Toast.LENGTH_SHORT);
				toast.show();
				return;
			}

			// Go to survey
			Intent i = new Intent(getApplicationContext(), Surveyor.class);
			i.putExtra("jsonsurvey", jsurv.toString());
			i.putExtra("username", username);
			startActivity(i);
		}
	}

	/*
	 * Survey getting helper functions
	 */

	public String getSurvey(String tableId) throws ClientProtocolException,
			IOException {
		String MASTER_TABLE_ID = "1isCCC51fe6nWx27aYWKfZWmk9w2Zj6a4yTyQ5c4";
		String query = URLEncoder.encode("SELECT survey_json FROM "
				+ MASTER_TABLE_ID + " WHERE table_id = '" + tableId + "'",
				"UTF-8");
		String apiKey = "AIzaSyB4Nn1k2sML-0aBN2Fk3qOXLF-4zlaNwmg";
		String url = "https://www.googleapis.com/fusiontables/v1/query?key="
				+ apiKey + "&sql=" + query;
		Log.v("Get survey query", url);

		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(url);
		try {
			httpget.setHeader("Authorization",
					"Bearer " + credential.getToken());
			HttpResponse response = httpclient.execute(httpget);

			Log.v("Token", credential.getToken());
			Log.v("Get survey response code", response.getStatusLine()
					.getStatusCode()
					+ " "
					+ response.getStatusLine().getReasonPhrase());

			// receive response as inputStream
			InputStream inputStream = response.getEntity().getContent();

			// convert inputstream to string
			if (inputStream != null)
				return convertInputStreamToString(inputStream);
			else
				return null;
		} catch (UserRecoverableAuthException e) {
			UserRecoverableAuthException exception = (UserRecoverableAuthException) e;
			Intent authorizationIntent = exception.getIntent();
			startActivityForResult(authorizationIntent, REQUEST_PERMISSIONS);
		} catch (GoogleAuthException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	// convert inputstream to String
	private static String convertInputStreamToString(InputStream inputStream)
			throws IOException {
		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(inputStream, "UTF-8"));
		String line = "";
		String result = "";
		while ((line = bufferedReader.readLine()) != null)
			result += line;

		inputStream.close();
		return result;
	}

	public void parseSurvey() {
		// get and parse survey
		new Thread(new Runnable() {
			public void run() {
				try {
					jsonsurveystring = getSurvey(projectName);
					Log.v("response", jsonsurveystring);

					try {
						JSONObject array = new JSONObject(
								jsonsurveystring);
						String rows = array.getJSONArray("rows")
								.toString();
						String jsonRows = rows.substring(
								rows.indexOf("{"),
								rows.lastIndexOf("}") + 1);

						// properly format downloaded string
						jsonRows = jsonRows.replaceAll("\\\\n", "");
						jsonRows = jsonRows.replace("\\", "");
						Log.v("JSON Parser string", jsonRows);
						jsurv = new JSONObject(jsonRows);
						messageHandler
								.sendEmptyMessage(EVENT_TYPE.PARSED_CORRECTLY
										.ordinal());
					} catch (JSONException e) {
						Log.e("JSON Parser", "Error parsing data "
								+ e.toString());
						messageHandler
								.sendEmptyMessage(EVENT_TYPE.PARSED_INCORRECTLY
										.ordinal());
					}
				} catch (ClientProtocolException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}).start();
	}
	
	/*
	 * Username selection helper functions
	 */

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case REQUEST_ACCOUNT_PICKER:
			if (resultCode == RESULT_OK && data != null
					&& data.getExtras() != null) {
				username = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
				credential.setSelectedAccountName(username);

				// update our username field
				messageHandler.sendEmptyMessage(EVENT_TYPE.GOT_USERNAME
						.ordinal());
			}
			break;
		case REQUEST_PERMISSIONS:
			if (resultCode == RESULT_OK)
				parseSurvey();
			else {
		        startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
			}
			break;
		}
	}

	private void DebuggingIsOn(boolean deb) {
		// Debug mode, passes project without internet connection.
		// Stuff for debugging without internet connection.
		if (deb) {
			projectName = "My fake project is back";
			username = "fakeuser123@youdontknowwhere.us";
			try {
				jsurv = new JSONObject(
						"{ \"Tracker\": { \"Questions\": [ { \"id\": \"q1\", \"Question\": \"Cami�n\", \"Kind\": \"MC\", \"Other\": true, \"Answers\": [ { \"Answer\": \"Ruta 56\" }, { \"Answer\": \"Ruta 15\" } ] } ], \"TableID\": \"1Q2mr8ni5LTxtZRRi3PNSYxAYS8HWikWqlfoIUK4\" }, \"Survey\": { \"Chapters\": [ { \"Chapter\": \"SITUACI�N DEL LOTE Y VIVIENDA\", \"Questions\": [ { \"id\": \"q1\", \"Question\": \"�Cu�l es el uso de su lote?\", \"Kind\": \"MC\", \"Other\": true, \"Answers\": [ { \"Answer\": \"Habitacional\" }, { \"Answer\": \"Mixto\" } ] }, { \"id\": \"q2\", \"Question\": \"�Cu�l es la superficie del lote?\", \"Kind\": \"ON\" }, { \"id\": \"q3\", \"Question\": \"�Cu�l es la superficie construida de su lote?\", \"Kind\": \"ON\" }, { \"id\": \"q4\", \"Question\": \"�Cu�ntas viviendas est�n construidas en este predio?\", \"Kind\": \"ON\" }, { \"id\": \"q5\", \"Question\": \"�Cu�ntas familias comparten esta vivienda?\", \"Kind\": \"ON\" }, { \"id\": \"q6\", \"Question\": \"Este predio es:\", \"Kind\": \"MC\", \"Other\": true, \"Jump\": \"q6d\", \"Answers\": [ { \"Answer\": \"Propio\" }, { \"Answer\": \"Rentado 6b\", \"Jump\": \"q6b\" }, { \"Answer\": \"Prestado 6d\" }, { \"Answer\": \"Compartido 6d\" }, { \"Answer\": \"Lo cuida 6d\" }, { \"Answer\": \"Secesi�n de derechos 6d\" }, { \"Answer\": \"Otra tenencia 6d\" } ] }, { \"id\": \"q6a\", \"Question\": \"�A trav�s de qui�n adquiri�/rent�/ocup� el lote? 6d\", \"Kind\": \"MC\", \"Other\": true, \"Jump\": \"q6d\", \"Answers\": [ { \"Answer\": \"Fraccionador 6d\" }, { \"Answer\": \"Lider 6d\" }, { \"Answer\": \"Comunitario 6d\" }, { \"Answer\": \"Ejidatario o comunero 6d\" }, { \"Answer\": \"Funcionario 6d\" } ] }, { \"id\": \"q6b\", \"Question\": \"Si renta, �Cu�nto paga mensualmente? 7\", \"Kind\": \"OT\" }, { \"id\": \"q6d\", \"Question\": \"�Qu� documentos de posesi�n y/o propiedad tiene?\", \"Kind\": \"OT\" }, { \"id\": \"q7\", \"Question\": \"�Cu�nto tiempo lleva viviendo aqu�?\", \"Kind\": \"ON\" }, { \"id\": \"q8\", \"Question\": \"�Est� el predio en alg�n proceso de regularizaci�n?\", \"Kind\": \"MC\", \"Other\": true, \"Answers\": [ { \"Answer\": \"S�\" }, { \"Answer\": \"No\" } ] }, { \"id\": \"q9\", \"Question\": \"�Sabe que adquiri� un lote en zona no apta para vivienda?\", \"Kind\": \"MC\", \"Other\": true, \"Answers\": [ { \"Answer\": \"S�\" }, { \"Answer\": \"No\" } ] } ] } ], \"TableID\": \"11lGsm8B2SNNGmEsTmuGVrAy1gcJF9TQBo3G1Vw0\" } }");
			} catch (JSONException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			messageHandler.sendEmptyMessage(EVENT_TYPE.PARSED_CORRECTLY
					.ordinal());
			messageHandler.sendEmptyMessage(EVENT_TYPE.GOT_USERNAME.ordinal());
			messageHandler.sendEmptyMessage(EVENT_TYPE.GOT_PROJECT_NAME
					.ordinal());
		}
	}
}
