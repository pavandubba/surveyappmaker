package org.urbanlaunchpad.flocktracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;

public class Surveyor extends Activity implements
		Question_fragment.AnswerSelected, Question_fragment.PositionPasser,
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener {
	private DrawerLayout ChapterDrawerLayout;
	private ListView ChapterDrawerList;
	private ActionBarDrawerToggle ChapterDrawerToggle;

	private CharSequence ChapterDrawerTitle;
	private CharSequence Title;
	private static String[] ChapterTitles;

	private String jsonsurveystring;
	private JSONObject jsurv = null;
	private int totalchapters;
	private JSONArray jchapterlist = null;
	JSONArray jquestionlist = null;
	JSONObject jchapter;
	JSONObject jquestion = null;
	private JSONObject aux = null;
	private Toast toast;
	private View nextquestionbutton;
	private View previousquestionbutton;
	private View submitbutton;
	private Integer questionposition;
	private Integer chapterposition;
	private Integer[] totalquestionsArray;
	String jumpString = null;
	String answerString = null;
	String token = null;
	String columnnamesString;
	String answerfinalString;
	private LocationClient mLocationClient;
	private final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	String[] columnlistNameString;
	String[] columnlistTypeString;
	private String[] questionIdlistString;
	private String[] questionKindlistString;
	Integer numberofquestions;
	Integer numberofcolumns;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_surveyor);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			jsonsurveystring = extras.getString("jsonsurvey");
			token = extras.getString("token");
			try {
				jsurv = new JSONObject(jsonsurveystring);
				// toast = Toast.makeText(getApplicationContext(),
				// "json recieved"
				// + jsonsurveystring, Toast.LENGTH_SHORT);
				// toast.show();
			} catch (JSONException e) {
				Log.e("JSON Parser", "Error parsing data, check survey file."
						+ e.toString());
			}
		}

		// Obtaining information about survey.

		try {
			jchapterlist = jsurv.getJSONArray("Survey");
			totalchapters = jchapterlist.length();
			ChapterTitles = new String[totalchapters];
			for (int i = 0; i < totalchapters; ++i) {
				aux = jchapterlist.getJSONObject(i);
				ChapterTitles[i] = aux.getString("Chapter");
			}
			// toast = Toast.makeText(getApplicationContext(), "Chapters " +
			// totalchapters, Toast.LENGTH_SHORT);
			// toast.show();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			toast = Toast.makeText(getApplicationContext(),
					"Chapters not parsed, check survey file.",
					Toast.LENGTH_SHORT);
			toast.show();
		}

		// Filling number of questions per chapter.
		totalquestionsArray = new Integer[totalchapters];
		for (int i = 0; i < totalchapters; ++i) {
			try {
				aux = jchapterlist.getJSONObject(i);
				totalquestionsArray[i] = aux.getJSONArray("Questions").length();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				totalquestionsArray[i] = 0;
			}
			// toast = Toast.makeText(this, "No of questions on chapter " + i
			// +":"+totalquestionsArray[i], Toast.LENGTH_SHORT);
			// toast.show();
		}

		// Navigation drawer information.

		Title = ChapterDrawerTitle = getTitle();
		ChapterDrawerLayout = (DrawerLayout) findViewById(R.id.chapter_drawer_layout);
		ChapterDrawerList = (ListView) findViewById(R.id.chapter_drawer);

		// set a custom shadow that overlays the main content when the drawer
		// opens
		ChapterDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
				GravityCompat.START);
		// set up the drawer's list view with items and click listener
		ChapterDrawerList.setAdapter(new ArrayAdapter<String>(this,
				R.layout.chapter_drawer_list_item, ChapterTitles));
		ChapterDrawerList.setOnItemClickListener(new DrawerItemClickListener());
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);
		ChapterDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
		ChapterDrawerLayout, /* DrawerLayout object */
		R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
		R.string.chapter_drawer_open, /* For accessibility */
		R.string.chapter_drawer_close /* For accessibility */
		) {
			public void onDrawerClosed(View view) {
				getActionBar().setTitle(Title);
			}

			public void onDrawerOpened(View drawerView) {
				getActionBar().setTitle(ChapterDrawerTitle);
			}
		};
		ChapterDrawerLayout.setDrawerListener(ChapterDrawerToggle);

		if (savedInstanceState == null) {
			chapterposition = 0;
			questionposition = 0;
			selectChapter(chapterposition, questionposition);
		}

		// Next and previous question navigation.

		nextquestionbutton = (View) findViewById(R.id.next_question_button);

		nextquestionbutton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (jumpString != null) {
					jumpFinder(jumpString);
				} else if (questionposition + 1 < totalquestionsArray[chapterposition]) {
					++questionposition;
				} else if (questionposition + 1 >= totalquestionsArray[chapterposition]) {
					questionposition = 0;
					++chapterposition;
				}

				selectChapter(chapterposition, questionposition);
			}
		});

		previousquestionbutton = (View) findViewById(R.id.previous_question_button);

		previousquestionbutton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

		// Submit button behavior.

		submitbutton = (View) findViewById(R.id.submit_survey_button);
		submitbutton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				new Thread(new Runnable() {
					public void run() {
						try {
							submitSurvey();
						} catch (ClientProtocolException e1) {

							e1.printStackTrace();
						} catch (IOException e1) {

							e1.printStackTrace();
						}

						columnCheck();
					}
				}).start();

			}
		});

		mLocationClient = new LocationClient(this, this, this);
	}

	/*
	 * Called when the Activity becomes visible.
	 */
	@Override
	protected void onStart() {
		super.onStart();
		// Connect the client.
		mLocationClient.connect();
	}

	/*
	 * Called when the Activity is no longer visible.
	 */
	@Override
	protected void onStop() {
		// Disconnecting the client invalidates it.
		mLocationClient.disconnect();
		super.onStop();
	}

	/*
	 * Handle results returned to this Activity by other Activities started with
	 * startActivityForResult(). In particular, the method onConnectionFailed()
	 * in LocationUpdateRemover and LocationUpdateRequester may call
	 * startResolutionForResult() to start an Activity that handles Google Play
	 * services problems. The result of this call returns here, to
	 * onActivityResult.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {

		// Choose what to do based on the request code
		switch (requestCode) {

		// If the request code matches the code sent in onConnectionFailed
		case CONNECTION_FAILURE_RESOLUTION_REQUEST:

			switch (resultCode) {
			// If Google Play services resolved the problem
			case Activity.RESULT_OK:

				// Log the result
				Log.d("Location", "Resolved connection");
				break;

			// If any other result was returned by Google Play services
			default:
				// Log the result
				Log.e("Location", "Could not resolve connection");

				break;
			}

			// If any other request code was received
		default:
			// Report that this Activity received an unknown requestCode
			Log.e("Surveyor activity", "unknown request code");
			break;
		}
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		// To make the action bar home/up action should open or close the
		// drawer.
		if (ChapterDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		return true;
	}

	private class DrawerItemClickListener implements
			ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			chapterposition = position;
			questionposition = 0;
			selectChapter(chapterposition, questionposition);
		}
	}

	private void selectChapter(int position, int qposition) {
		// update the main content by replacing fragments
		jchapter = null;
		try {
			jchapter = jchapterlist.getJSONObject(position);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// update selected item and title, then close the drawer.
		ChapterDrawerList.setItemChecked(position, true);
		setTitle(ChapterTitles[position]);
		ChapterDrawerLayout.closeDrawer(ChapterDrawerList);

		// Obtaining the question desired to send to fragment
		getQuestion(qposition);

		// Starting question fragment and passing json question information.
		ChangeQuestion(jquestion, chapterposition, questionposition);

	}

	private void getQuestion(int position) {
		try {
			jquestion = jchapter.getJSONArray("Questions").getJSONObject(
					position);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			toast = Toast.makeText(this, "Question " + position
					+ " does not exist in chapter.", Toast.LENGTH_SHORT);
			toast.show();
			String nullquestionhelper = "{\"Question\":\"No questions on chapter\"}";
			try {
				jquestion = new JSONObject(nullquestionhelper);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}

	}

	@Override
	public void setTitle(CharSequence title) {
		Title = title;
		getActionBar().setTitle(Title);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		ChapterDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggles
		ChapterDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public void onBackPressed() {
		FragmentManager fm = getFragmentManager();
		if (fm.getBackStackEntryCount() > 0) {
			Log.i("MainActivity", "popping backstack");
			fm.popBackStack();
		} else {
			Log.i("MainActivity", "nothing on backstack, calling super");
			super.onBackPressed();
		}
	}

	public void ChangeQuestion(JSONObject jquestion, Integer chapterposition,
			Integer questionposition) {
		// Starting question fragment and passing json question information.
		Fragment fragment = new Question_fragment();
		Bundle args = new Bundle();
		args.putString(Question_fragment.ARG_JSON_QUESTION,
				jquestion.toString());
		args.putInt(Question_fragment.ARG_CHAPTER_POSITION, chapterposition);
		args.putInt(Question_fragment.ARG_QUESTION_POSITION, questionposition);
		fragment.setArguments(args);

		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction transaction = fragmentManager.beginTransaction();
		transaction.replace(R.id.surveyor_frame, fragment);
		transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		transaction.addToBackStack(null);
		transaction.commit();

	}

	public void AnswerRecieve(String answerStringRecieve,
			String jumpStringRecieve) {
		answerString = answerStringRecieve;
		jumpString = jumpStringRecieve;
		if (answerString != null) {
			try {
				jsurv.getJSONArray("Survey").getJSONObject(chapterposition)
						.getJSONArray("Questions")
						.getJSONObject(questionposition)
						.put("Answer", answerString);
				// toast = Toast.makeText(this, "Answer passed: " +
				// answerString,
				// Toast.LENGTH_SHORT);
				// toast.show();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		toast = Toast.makeText(this, "After: Jump: " + jumpString + "Answer: "
				+ answerString, Toast.LENGTH_SHORT);
		toast.show();
	}

	public void PositionRecieve(Integer chapterpositionrecieve,
			Integer questionpositionrecieve) {
		questionposition = questionpositionrecieve;
		chapterposition = chapterpositionrecieve;
	}

	public void jumpFinder(String jumpString) {
		// Searches for a question with the same id as the jumpString value
		for (int i = 0; i < totalchapters; ++i) {
			for (int j = 0; j < totalquestionsArray[i]; ++j) {
				String jumpAUX = null;
				try {
					jumpAUX = jsurv.getJSONArray("Survey").getJSONObject(i)
							.getJSONArray("Questions").getJSONObject(j)
							.getString("id");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (jumpString.equals(jumpAUX)) {
					chapterposition = i;
					questionposition = j;
					break;
				}
			}
		}
	}

	public void submitSurvey() throws ClientProtocolException, IOException {
		columnnamesString = getnames("id", "nq");
		answerfinalString = getnames("Answer", "wq");

		Location currentLocation = mLocationClient.getLastLocation();
		String latlng = LocationHelper.getLatLngAlt(currentLocation);
		String TABLE_ID = "11lGsm8B2SNNGmEsTmuGVrAy1gcJF9TQBo3G1Vw0";
		String url = "https://www.googleapis.com/fusiontables/v1/query";
		String dateString = (String) android.text.format.DateFormat.format(
				"yyyy-MM-dd hh:mm:ss", new java.util.Date());
		String query = "INSERT INTO " + TABLE_ID + " (" + columnnamesString
				+ ",Location,Lat,Lng,Alt,Date) VALUES (" + answerfinalString
				+ ",'<Point><coordinates>" + latlng
				+ "</coordinates></Point>','" + currentLocation.getLatitude()
				+ "','" + currentLocation.getLongitude() + "','"
				+ currentLocation.getAltitude() + "','" + dateString + "');";
		String apiKey = "AIzaSyB4Nn1k2sML-0aBN2Fk3qOXLF-4zlaNwmg";
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(url);
		httppost.setHeader("Authorization", "Bearer " + token);
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair("sql", query));
		nameValuePairs.add(new BasicNameValuePair("key", apiKey));
		httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		HttpResponse response = httpclient.execute(httppost);

		Log.v("Submit survey response code", response.getStatusLine()
				.getStatusCode()
				+ " "
				+ response.getStatusLine().getReasonPhrase());
	}

	public String getnames(String nametoget, String syntaxtype) {
		// If syntaxtype equals wq , quotes are aded, if it's nq , no quotes are
		// added.
		String addString = null;
		String namesString = null;
		for (int i = 0; i < totalchapters; ++i) {
			for (int j = 0; j < totalquestionsArray[i]; ++j) {
				try {
					addString = jsurv.getJSONArray("Survey").getJSONObject(i)
							.getJSONArray("Questions").getJSONObject(j)
							.getString(nametoget);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					// e.printStackTrace();
					addString = "";
				}
				if (i == 0 && j == 0) {
					if (syntaxtype.equals("wq")) {
						namesString = "'" + addString + "'";
					} else {
						namesString = addString;
					}
				} else {
					if (syntaxtype.equals("wq")) {
						namesString = namesString + ",'" + addString + "'";
					} else {
						namesString = namesString + "," + addString;
					}
				}
			}
		}
		// toast = Toast.makeText(this, "Names: " + namesString,
		// Toast.LENGTH_SHORT);
		// toast.show();
		return namesString;
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		if (connectionResult.hasResolution()) {
			try {
				// Start an Activity that tries to resolve the error
				connectionResult.startResolutionForResult(this,
						CONNECTION_FAILURE_RESOLUTION_REQUEST);
				/*
				 * Thrown if Google Play services canceled the original
				 * PendingIntent
				 */
			} catch (IntentSender.SendIntentException e) {
				// Log the error
				e.printStackTrace();
			}
		} else {
			/*
			 * If no resolution is available, display a dialog to the user with
			 * the error.
			 */
			Toast.makeText(this, connectionResult.getErrorCode(),
					Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onDisconnected() {
		// Display the connection status
		Toast.makeText(this, "Disconnected. Please re-connect.",
				Toast.LENGTH_SHORT).show();
	}

	public void createcolumn(String name, String type)
			throws ClientProtocolException, IOException {
		String TABLE_ID = "11lGsm8B2SNNGmEsTmuGVrAy1gcJF9TQBo3G1Vw0";
		String apiKey = "AIzaSyB4Nn1k2sML-0aBN2Fk3qOXLF-4zlaNwmg";
		String url = "https://www.googleapis.com/fusiontables/v1/tables/"
				+ TABLE_ID + "/columns?key=" + apiKey;

		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(url);
		httppost.setHeader("Authorization", "Bearer " + token);
		httppost.setHeader("Content-Type", "application/json");
		JSONObject object = new JSONObject();
		try {
			object.put("name", name);
			object.put("type", type);
		} catch (Exception ex) {
		}
		String columnString = object.toString();
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
		nameValuePairs.add(new BasicNameValuePair("key", apiKey));
		httppost.setEntity(new StringEntity(columnString, "UTF-8"));
		HttpResponse response = httpclient.execute(httppost);

		Log.v("Create column response code", response.getStatusLine()
				.getStatusCode()
				+ " "
				+ response.getStatusLine().getReasonPhrase());
	}

	public String[] getcolumnList(String TABLE_ID, String apiKey,
			String whatotogetString) throws ClientProtocolException,
			IOException {
		// Returns the column list (of maximum MAX items) of a given fusion
		// tables table as a JSON string.
		String MAX = "500";
		String url = "https://www.googleapis.com/fusiontables/v1/tables/"
				+ TABLE_ID + "/columns?key=" + apiKey + "&maxResults=" + MAX;
		String[] columnlistStringArray = null;
		JSONObject jcolumns = null;
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(url);
		httpget.setHeader("Authorization", "Bearer " + token);
		HttpResponse response = httpclient.execute(httpget);
		String columnlistJSONString = EntityUtils
				.toString(response.getEntity());
		Log.v("Column list response code", response.getStatusLine()
				.getStatusCode()
				+ " "
				+ response.getStatusLine().getReasonPhrase());
		Integer total = 0;
		try {
			jcolumns = new JSONObject(columnlistJSONString);
			total = jcolumns.getInt("totalItems");
			columnlistStringArray = new String[total];
		} catch (JSONException e) {
		}
		if (total > 0) {
			for (int i = 0; i < total; ++i) {
				try {
					columnlistStringArray[i] = jcolumns.getJSONArray("items")
							.getJSONObject(i).getString(whatotogetString);
				} catch (JSONException e) {
					columnlistStringArray[i] = "";
				}
				// Log.v("ID", columnlistStringArray[i]);
			}
		}
		Log.v("Number of columns", total.toString() + " " + whatotogetString);
		return columnlistStringArray;
	}

	public String[] getquestionlist(JSONObject jsonsurv, String whattgetString) {
		Integer numberofquestions = 0;
		String[] questionStringArray;
		for (int i = 0; i < totalchapters; ++i) {
			for (int j = 0; j < totalquestionsArray[i]; ++j) {
				++numberofquestions;
			}
		}
		questionStringArray = new String[numberofquestions];
		int auxcount = 0;
		for (int i = 0; i < totalchapters; ++i) {
			for (int j = 0; j < totalquestionsArray[i]; ++j) {
				try {
					questionStringArray[auxcount] = jsonsurv
							.getJSONArray("Survey").getJSONObject(i)
							.getJSONArray("Questions").getJSONObject(j)
							.getString(whattgetString);
					// Log.v("ID", questionIDArray[auxcount]);
					++auxcount;
				} catch (JSONException e) {
					questionStringArray[auxcount] = "";
				}
			}
		}
		Log.v("Number of questions", numberofquestions.toString() + " "
				+ whattgetString);
		return questionStringArray;
	}

	public void columnCheck() {
		Boolean existsBoolean;
		String[] hardcolumnsStrings = { "Location", "Date", "Lat", "Alt", "Lng" }; // Columns
																					// that
																					// are
																					// in
																					// all
																					// projects.
		String[] hardcolumntypeStrings = { "LOCATION", "DATETIME", "NUMBER",
				"NUMBER", "NUMBER" }; // Types for the columns that are in all
										// projects.
		try {
			columnlistNameString = getcolumnList(
					"11lGsm8B2SNNGmEsTmuGVrAy1gcJF9TQBo3G1Vw0",
					"AIzaSyB4Nn1k2sML-0aBN2Fk3qOXLF-4zlaNwmg", "name");
			columnlistTypeString = getcolumnList(
					"11lGsm8B2SNNGmEsTmuGVrAy1gcJF9TQBo3G1Vw0",
					"AIzaSyB4Nn1k2sML-0aBN2Fk3qOXLF-4zlaNwmg", "type");
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		int numberofhardcolumns = hardcolumnsStrings.length;
		for (int i = 0; i < numberofhardcolumns; ++i) {
			existsBoolean = searchinStringArray(columnlistNameString,
					hardcolumnsStrings[i]);
			if (existsBoolean == true) {
				// If the question id and the column name is the same,
				// checking if the column types are well set.
				changecolumntype();
			} else if (existsBoolean == false) {
				try {
					createcolumn(hardcolumnsStrings[i], hardcolumntypeStrings[i]);
				} catch (ClientProtocolException e) {

					e.printStackTrace();
				} catch (IOException e) {

					e.printStackTrace();
				}
			}
		}

		questionIdlistString = getquestionlist(jsurv, "id");
		questionKindlistString = getquestionlist(jsurv, "Kind");
		numberofquestions = questionIdlistString.length;
		numberofcolumns = columnlistNameString.length;
		String auxkind = null;
		for (int i = 0; i < numberofquestions; ++i) {
			if ((questionKindlistString[i].equals("MC") || questionKindlistString[i]
					.equals("CB")) || questionKindlistString[i].equals("OT")) {
				auxkind = "STRING";
			} else if (questionKindlistString[i].equals("ON")) {
				auxkind = "NUMBER";
			} else {
				auxkind = "STRING";
			}
			existsBoolean = searchinStringArray(columnlistNameString,
					questionIdlistString[i]);
			if (existsBoolean == true) {
				// If the question id and the column name is the same,
				// checking if the column types are well set.
				changecolumntype();
			} else if (existsBoolean == false) {
				try {
					createcolumn(questionIdlistString[i], auxkind);
				} catch (ClientProtocolException e) {

					e.printStackTrace();
				} catch (IOException e) {

					e.printStackTrace();
				}
			}

		}

	}

	public Boolean searchinStringArray(String[] Array, String string) {
		int lenght = Array.length;
		if (lenght > 0) {
			for (int i = 0; i < lenght; ++i) {
				if (Array[i].equals(string)) {
					break;
				} else if (i + 1 == lenght) {
					return false;
				}
			}
		} else {
			return false;
		}
		return true;
	}

	public void changecolumntype() {

	}

}