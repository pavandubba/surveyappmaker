package org.urbanlaunchpad.flocktracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

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
import org.urbanlaunchpad.flocktracker.Status_page_fragment.StatusPageUpdate;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;

public class Surveyor extends Activity implements
		Question_fragment.AnswerSelected, Question_fragment.PositionPasser,
		Question_navigator_fragment.NavButtonCallback,
		Start_trip_fragment.HubButtonCallback, StatusPageUpdate,
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
	private int totalsurveychapters;
	private JSONArray jchapterlist = null;
	JSONArray jquestionlist = null;
	JSONObject jchapter;
	JSONObject jquestion = null;
	private JSONObject aux = null;
	private Toast toast;
	private Integer questionposition;
	private Integer chapterposition;
	private Integer[] totalquestionsArray;
	String jumpString = null;
	String answerString = null;
	String token = null;
	private LocationClient mLocationClient;
	private final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	private Fragment navButtons;
	String[] columnlistNameString;
	String[] columnlistTypeString;
	private String[] questionIdlistString;
	private String[] questionKindlistString;
	Integer numberofquestions;
	Integer numberofcolumns;
	private Integer maleCount = 0;
	private Integer femaleCount = 0;
	private String surveyID;
	private String tripID;
	private boolean isTripStarted = false;
	private String TRIP_TABLE_ID = "1Q2mr8ni5LTxtZRRi3PNSYxAYS8HWikWqlfoIUK4";
	private String SURVEY_TABLE_ID = "11lGsm8B2SNNGmEsTmuGVrAy1gcJF9TQBo3G1Vw0";
	private String API_KEY = "AIzaSyB4Nn1k2sML-0aBN2Fk3qOXLF-4zlaNwmg";
	private JSONArray jtrackerquestions;
	private String username;
	private Integer trackerWait = 30000;
	private Integer tripQuestionposition = 0;
	private Calendar startTripTime = null;
	private double tripDistance = 0;
	private double totalDistanceBefore = 0;
	private int ridesCompleted = 0;
	private int surveysCompleted = 0;
	private Location startLocation;
	private Activity thisActivity;
	private Boolean askingTripQuestions = false;
	private Integer totalTripQuestions = 0;

	private enum EVENT_TYPE {
		MALE_UPDATE, FEMALE_UPDATE, UPDATE_STATS_PAGE, UPDATE_HUB_PAGE
	}

	@SuppressLint("HandlerLeak")
	private Handler messageHandler = new Handler() {

		public void handleMessage(Message msg) {
			if (msg.what == EVENT_TYPE.MALE_UPDATE.ordinal()) {
				// update male count
				TextView maleCountView = (TextView) findViewById(R.id.maleCount);
				maleCountView.setText(maleCount.toString());
				// update total count
				TextView totalCount = (TextView) findViewById(R.id.totalPersonCount);
				totalCount.setText("" + (maleCount + femaleCount));
			} else if (msg.what == EVENT_TYPE.FEMALE_UPDATE.ordinal()) {
				// update female count
				TextView femaleCountView = (TextView) findViewById(R.id.femaleCount);
				femaleCountView.setText(femaleCount.toString());
				// update total count
				TextView totalCount = (TextView) findViewById(R.id.totalPersonCount);
				totalCount.setText("" + (maleCount + femaleCount));
			} else if (msg.what == EVENT_TYPE.UPDATE_HUB_PAGE.ordinal()) {
				navButtons.getView().findViewById(R.id.previous_question_button)
				.setVisibility(View.INVISIBLE);
				
				// hide navigation buttons
				FragmentManager fragmentManager = getFragmentManager();

				FragmentTransaction transactionHide = fragmentManager
						.beginTransaction();
				transactionHide.hide(navButtons);
				transactionHide.commit();
				
				// update selected item and title, then close the drawer.
				ChapterDrawerList.setItemChecked(0, true);
				setTitle(ChapterTitles[0]);
				ChapterDrawerLayout.closeDrawer(ChapterDrawerList);
				
				// update male count
				TextView maleCountView = (TextView) findViewById(R.id.maleCount);
				maleCountView.setText(maleCount.toString());
				// update female count
				TextView femaleCountView = (TextView) findViewById(R.id.femaleCount);
				femaleCountView.setText(femaleCount.toString());
				// update total count
				TextView totalCount = (TextView) findViewById(R.id.totalPersonCount);
				totalCount.setText("" + (maleCount + femaleCount));
				
				if (isTripStarted) {
					ImageView gear = (ImageView) findViewById(R.id.start_trip_button);
					gear.setImageResource(R.drawable.ft_grn_st1);
					RotateAnimation anim = new RotateAnimation(0.0f, 360.0f,
							Animation.RELATIVE_TO_SELF, 0.5f,
							Animation.RELATIVE_TO_SELF, 0.5f);
					anim.setInterpolator(new LinearInterpolator());
					anim.setRepeatCount(Animation.INFINITE);
					anim.setDuration(700);
					// Start animating the image
					gear.startAnimation(anim);
				} else {
					ImageView gear = (ImageView) findViewById(R.id.start_trip_button);
					gear.setImageResource(R.drawable.ft_red_st);
					gear.setAnimation(null);	
				}
			} else if (msg.what == EVENT_TYPE.UPDATE_STATS_PAGE.ordinal()) {
				TextView tripTimeText = (TextView) findViewById(R.id.tripTime);
				if (tripTimeText != null) {
					TextView tripDistanceText = (TextView) findViewById(R.id.tripDistance);
					TextView surveysCompletedText = (TextView) findViewById(R.id.surveysCompleted);
					TextView ridesCompletedText = (TextView) findViewById(R.id.ridesCompleted);
					TextView totalDistanceText = (TextView) findViewById(R.id.totalDistance);
					TextView currentAddressText = (TextView) findViewById(R.id.currentAddress);
					TextView usernameText = (TextView) findViewById(R.id.user_greeting);

					if (startTripTime != null) {
						Calendar difference = Calendar.getInstance();
						difference.setTimeInMillis(difference.getTimeInMillis()
								- startTripTime.getTimeInMillis());
						tripTimeText.setText(difference.getTime().getMinutes()
								+ ":" + difference.getTime().getSeconds());
					} else {
						tripTimeText.setText(R.string.total_time);
					}
					ridesCompletedText.setText("" + ridesCompleted);
					totalDistanceText.setText(""
							+ String.format("%.2f", totalDistanceBefore
									+ tripDistance));
					tripDistanceText.setText(""
							+ String.format("%.2f", tripDistance));
					surveysCompletedText.setText("" + surveysCompleted);
					usernameText.setText("Hi " + username + "!");

					Geocoder geocoder = new Geocoder(thisActivity,
							Locale.getDefault());
					Location current = mLocationClient.getLastLocation();
					List<Address> addresses = null;
					try {
						addresses = geocoder.getFromLocation(
								current.getLatitude(), current.getLongitude(),
								1);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (addresses != null && addresses.size() > 0) {
						// Get the first address
						Address address = addresses.get(0);
						/*
						 * Format the first line of address (if available),
						 * city, and country name.
						 */
						String addressText = String.format(
								"%s, %s, %s",
								// If there's a street address, add it
								address.getMaxAddressLineIndex() > 0 ? address
										.getAddressLine(0) : "",
								// Locality is usually a city
								address.getLocality(),
								// The country of the address
								address.getCountryName());
						currentAddressText.setText(addressText);

					} else
						currentAddressText.setText(R.string.current_address);
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_surveyor);
		Bundle extras = getIntent().getExtras();
		thisActivity = this;
		if (extras != null) {
			username = extras.getString("username");
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

		// Obtaining Fusion Table IDs.

		try {
			TRIP_TABLE_ID = jsurv.getJSONObject("Tracker").getString("TableID");
			SURVEY_TABLE_ID = jsurv.getJSONObject("Survey")
					.getString("TableID");
		} catch (JSONException e3) {
			toast = Toast
					.makeText(
							getApplicationContext(),
							"Your project has messy Fusion Table IDs, uploading won't work.",
							Toast.LENGTH_SHORT);
			toast.show();
			e3.printStackTrace();
		}

		// Obtaining information about survey.

		try {
			jchapterlist = jsurv.getJSONObject("Survey").getJSONArray(
					"Chapters");
			totalsurveychapters = jchapterlist.length();
			ChapterTitles = new String[1 + totalsurveychapters];
			ChapterTitles[0] = "Status Page";
			for (int i = 1; i <= totalsurveychapters; ++i) {
				aux = jchapterlist.getJSONObject(i - 1);
				ChapterTitles[i] = aux.getString("Chapter");
			}
			// toast = Toast.makeText(getApplicationContext(), "Chapters " +
			// totalchapters, Toast.LENGTH_SHORT);
			// toast.show();
		} catch (JSONException e) {
			e.printStackTrace();
			toast = Toast.makeText(getApplicationContext(),
					"Chapters not parsed, check survey file.",
					Toast.LENGTH_SHORT);
			toast.show();
		}

		// Obtaining information about tracking.

		try {
			jtrackerquestions = jsurv.getJSONObject("Tracker").getJSONArray(
					"Questions");
			totalTripQuestions = jtrackerquestions.length();
		} catch (JSONException e2) {
			e2.printStackTrace();
			toast = Toast
					.makeText(
							getApplicationContext(),
							"Project does not contain tracker questions or questions were created erroneusly.",
							Toast.LENGTH_SHORT);
			toast.show();
		}

		// Filling number of questions per chapter.
		totalquestionsArray = new Integer[totalsurveychapters];
		for (int i = 0; i < totalsurveychapters; ++i) {
			try {
				aux = jchapterlist.getJSONObject(i);
				totalquestionsArray[i] = aux.getJSONArray("Questions").length();
			} catch (JSONException e) {
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

		navButtons = getFragmentManager().findFragmentById(
				R.id.survey_question_navigator_fragment);
		maleCount = 0;
		femaleCount = 0;

		if (savedInstanceState == null) {
			showHubPage();
		}

		mLocationClient = new LocationClient(this, this, this);

		// Creating a random survey ID

		surveyID = "S" + createID();
		// toast = Toast.makeText(getApplicationContext(), getResources()
		// .getString(R.string.survey_id) + " " + surveyID,
		// Toast.LENGTH_SHORT);
		// toast.show();

		// Checking existence of columns in the Fusion Tables.

		new Thread(new Runnable() {
			public void run() {
				columnCheck(SURVEY_TABLE_ID, "survey");
				columnCheck(TRIP_TABLE_ID, "trip");
			}
		}).start();

		// location tracking
		new Thread(new Runnable() {
			public void run() {
				while (true) {
					try {
						if (isTripStarted) {
							submitLocation();
							Thread.sleep(trackerWait);
						} else {
							Thread.sleep(5000);
						}
					} catch (ClientProtocolException e1) {
						e1.printStackTrace();
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
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

	public void startTrip() {
		isTripStarted = true;
		tripID = "T" + createID();
		FragmentManager fragmentManager = getFragmentManager();
	}

	public void submitLocation() throws ClientProtocolException, IOException {
		String columnnamesString = getnames("id", "nq", "Trip");
		String answerfinalString = getnames("Answer", "wq", "Trip");
		if (mLocationClient.isConnected()) {
			Location currentLocation = mLocationClient.getLastLocation();
			tripDistance += startLocation.distanceTo(currentLocation);
			startLocation = currentLocation;
	
			String latlng = LocationHelper.getLatLngAlt(currentLocation);
			// String TABLE_ID = "11lGsm8B2SNNGmEsTmuGVrAy1gcJF9TQBo3G1Vw0";
			String url = "https://www.googleapis.com/fusiontables/v1/query";
			String dateString = (String) android.text.format.DateFormat.format(
					"yyyy-MM-dd hh:mm:ss", new java.util.Date());
			String query = "INSERT INTO " + TRIP_TABLE_ID + " ("
					+ columnnamesString
					+ ",Location,Lat,Lng,Alt,Date,TripID,Username) VALUES ("
					+ answerfinalString + ",'<Point><coordinates>" + latlng
					+ "</coordinates></Point>','" + currentLocation.getLatitude()
					+ "','" + currentLocation.getLongitude() + "','"
					+ currentLocation.getAltitude() + "','" + dateString + "','"
					+ tripID + "','" + username + "');";
			String apiKey = "AIzaSyB4Nn1k2sML-0aBN2Fk3qOXLF-4zlaNwmg";
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(url);
			httppost.setHeader("Authorization", "Bearer " + token);
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("sql", query));
			nameValuePairs.add(new BasicNameValuePair("key", apiKey));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			HttpResponse response = httpclient.execute(httppost);
	
			Log.v("Submit trip response code", response.getStatusLine()
					.getStatusCode()
					+ " "
					+ response.getStatusLine().getReasonPhrase());
		}
	}

	public void stopTrip() {
		isTripStarted = false;
		tripID = "";
		startTripTime = null;

		messageHandler.sendEmptyMessage(EVENT_TYPE.UPDATE_HUB_PAGE.ordinal());
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
			if (position == 0)
				showHubPage();
			else
				selectChapter(chapterposition, questionposition);
		}
	}

	private void showHubPage() {

		FragmentManager fragmentManager = getFragmentManager();
		Fragment fragment = new Start_trip_fragment();

		FragmentTransaction transaction = fragmentManager.beginTransaction();
		transaction.replace(R.id.surveyor_frame, fragment);
		transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		transaction.commit();

		// update ui
		messageHandler.sendEmptyMessage(EVENT_TYPE.UPDATE_HUB_PAGE.ordinal());
	}

	private void showStatusPage() {
		FragmentManager fragmentManager = getFragmentManager();
		Fragment fragment = new Status_page_fragment();

		FragmentTransaction transaction = fragmentManager.beginTransaction();
		transaction.replace(R.id.surveyor_frame, fragment);
		transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		transaction.addToBackStack(null);
		transaction.commit();

		// update selected item and title, then close the drawer.
		ChapterDrawerList.setItemChecked(0, true);
		setTitle(ChapterTitles[0]);
		ChapterDrawerLayout.closeDrawer(ChapterDrawerList);

		messageHandler.sendEmptyMessage(EVENT_TYPE.UPDATE_STATS_PAGE.ordinal());
	}

	private void selectChapter(int position, int qposition) {
		navButtons.getView().findViewById(R.id.submit_survey_button)
				.setVisibility(View.VISIBLE);

		// update the main content by replacing fragments
		jchapter = null;
		try {
			jchapter = jchapterlist.getJSONObject(position - 1);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		// update selected item and title, then close the drawer.
		ChapterDrawerList.setItemChecked(position, true);
		setTitle(ChapterTitles[position]);
		ChapterDrawerLayout.closeDrawer(ChapterDrawerList);

		// Obtaining the question desired to send to fragment
		jquestion = getQuestion(qposition, jchapter);

		// Starting question fragment and passing json question information.
		ChangeQuestion(jquestion, position, qposition);

	}

	private JSONObject getQuestion(int position, JSONObject chapter) {
		JSONObject question = null;
		try {
			question = chapter.getJSONArray("Questions")
					.getJSONObject(position);
		} catch (JSONException e) {
			e.printStackTrace();
			toast = Toast.makeText(this, "Question " + position
					+ " does not exist in chapter.", Toast.LENGTH_SHORT);
			toast.show();
			String nullquestionhelper = "{\"Question\":\"No questions on chapter\"}";
			try {
				question = new JSONObject(nullquestionhelper);
			} catch (JSONException e1) {
				e1.printStackTrace();
			}

		}
		return question;
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
		if (fm.getBackStackEntryCount() > 1) {
			onPrevQuestionPressed();
			return;
		}

		// update title
		ChapterDrawerList.setItemChecked(0, true);
		setTitle(ChapterTitles[0]);
		ChapterDrawerLayout.closeDrawer(ChapterDrawerList);

		Log.i("MainActivity", "nothing on backstack, calling super");
		if (fm.getBackStackEntryCount() == 0)
			finish();
		super.onBackPressed();
	}

	public void onPrevQuestionPressed() {
		FragmentManager fm = getFragmentManager();
		if (fm.getBackStackEntryCount() > 2) {
			Log.i("MainActivity", "popping backstack");
			fm.popBackStackImmediate();
		} else if (fm.getBackStackEntryCount() == 2) {
			navButtons.getView().findViewById(R.id.previous_question_button)
					.setVisibility(View.INVISIBLE);
			fm.popBackStackImmediate();
		}
		ChapterDrawerList.setItemChecked(chapterposition, true);
		setTitle(ChapterTitles[chapterposition]);
		ChapterDrawerLayout.closeDrawer(ChapterDrawerList);
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

		// show navigation buttons and add new question
		if (!navButtons.isVisible()) {
			FragmentTransaction transactionShow = fragmentManager
					.beginTransaction();
			transactionShow.show(navButtons);
			transactionShow.commit();
		}

		transaction.replace(R.id.surveyor_frame, fragment);
		transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		if (!askingTripQuestions)
			transaction.addToBackStack(null);
		transaction.commit();
	}

	public void jumpFinder(String jumpString) {
		// Searches for a question with the same id as the jumpString value
		for (int i = 0; i < totalsurveychapters; ++i) {
			for (int j = 0; j < totalquestionsArray[i]; ++j) {
				String jumpAUX = null;
				try {
					jumpAUX = jsurv.getJSONObject("Survey")
							.getJSONArray("Chapters").getJSONObject(i)
							.getJSONArray("Questions").getJSONObject(j)
							.getString("id");
				} catch (JSONException e) {
					e.printStackTrace();
				}
				if (jumpString.equals(jumpAUX)) {
					chapterposition = i + 1;
					questionposition = j;
					break;
				}
			}
		}
	}

	public void submitSurvey() throws ClientProtocolException, IOException {
		String columnnamesString = getnames("id", "nq", "Survey");
		String answerfinalString = getnames("Answer", "wq", "Survey");

		Location currentLocation = mLocationClient.getLastLocation();
		String latlng = LocationHelper.getLatLngAlt(currentLocation);
		String url = "https://www.googleapis.com/fusiontables/v1/query";
		String dateString = (String) android.text.format.DateFormat.format(
				"yyyy-MM-dd hh:mm:ss", new java.util.Date());
		String query = "INSERT INTO "
				+ SURVEY_TABLE_ID
				+ " ("
				+ columnnamesString
				+ ",Location,Lat,Lng,Alt,Date,SurveyID,TripID,Username) VALUES ("
				+ answerfinalString + ",'<Point><coordinates>" + latlng
				+ "</coordinates></Point>','" + currentLocation.getLatitude()
				+ "','" + currentLocation.getLongitude() + "','"
				+ currentLocation.getAltitude() + "','" + dateString + "','"
				+ surveyID + "','" + tripID + "','" + username + "');";
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(url);
		httppost.setHeader("Authorization", "Bearer " + token);
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair("sql", query));
		nameValuePairs.add(new BasicNameValuePair("key", API_KEY));
		httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		HttpResponse response = httpclient.execute(httppost);
		if (response.getStatusLine().getStatusCode() == 200) {
			surveysCompleted++;
		}

		Log.v("Submit survey response code", response.getStatusLine()
				.getStatusCode()
				+ " "
				+ response.getStatusLine().getReasonPhrase());
	}

	public String getnames(String nametoget, String syntaxtype,
			String triporsurvey) {
		// If syntaxtype equals wq , quotes are aded, if it's nq , no quotes are
		// added.
		String addString = null;
		String namesString = null;
		JSONArray questionsArray = null;
		Integer totalchapters = null;
		Integer totalquestions = null;
		if (triporsurvey.equals("Survey")) {
			totalchapters = totalsurveychapters;
		} else if (triporsurvey.equals("Trip")) {
			totalchapters = 1;
		}
		for (int i = 0; i < totalchapters; ++i) {
			if (triporsurvey.equals("Survey")) {
				try {
					questionsArray = jsurv.getJSONObject("Survey")
							.getJSONArray("Chapters").getJSONObject(i)
							.getJSONArray("Questions");
					totalquestions = questionsArray.length();
				} catch (JSONException e) {
					e.printStackTrace();
				}
			} else if (triporsurvey.equals("Trip")) {
				try {
					questionsArray = jsurv.getJSONObject("Tracker")
							.getJSONArray("Questions");
					totalquestions = questionsArray.length();
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			for (int j = 0; j < totalquestions; ++j) {
				try {
					addString = questionsArray.getJSONObject(j).getString(
							nametoget);
				} catch (JSONException e) {
					// e.printStackTrace();
					addString = "";
				}
				if (i == 0 && j == 0) {
					if (syntaxtype.equals("wq")) {
						namesString = "'" + addString + "'";
					} else if (syntaxtype.equals("nq")) {
						namesString = addString;
					}
				} else {
					if (syntaxtype.equals("wq")) {
						namesString = namesString + ",'" + addString + "'";
					} else if (syntaxtype.equals("nq")) {
						namesString = namesString + "," + addString;
					}
				}
			}
		}
		Log.v("Names", triporsurvey + " " + namesString);
		// toast = Toast.makeText(this, "Names: " + namesString,
		// Toast.LENGTH_SHORT);
		// toast.show();
		return namesString;
	}

	/*
	 * Callback Handlers for Connecting to Google Play (Authentication)
	 */

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

	/*
	 * Fragment Event Listener Functions Below
	 */

	// Handler for which button was pressed in navigator fragment

	public void NavButtonPressed(NavButtonType type) {
		switch (type) {

		case PREVIOUS:
			if (askingTripQuestions == true) {
				tripQuestionposition = tripQuestionposition - 1;
			}
			onPrevQuestionPressed();
			break;
		case NEXT:
			navButtons.getView().findViewById(R.id.previous_question_button)
					.setVisibility(View.VISIBLE);
			if (askingTripQuestions == true) {
				if (tripQuestionposition + 1 == totalTripQuestions) {
					Toast.makeText(this, "Tracking is on!", Toast.LENGTH_SHORT)
							.show();
					askingTripQuestions = false;
					tripQuestionposition = 0;
					showHubPage();
					startTrip();
					break;
				} else if (jumpString != null) {
					// TODO Define a jump behavior.
					jumpFinder(jumpString);
				} else if (tripQuestionposition + 1 < totalTripQuestions) {
					++tripQuestionposition;
				}
				try {
					jquestion = jtrackerquestions
							.getJSONObject(tripQuestionposition);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				ChangeQuestion(jquestion, 0, tripQuestionposition);
			} else if (askingTripQuestions == false) {
				if ((questionposition + 1 == totalquestionsArray[chapterposition - 1])
						&& (chapterposition + 1 - 1 == totalsurveychapters)) {
					Toast.makeText(this,
							"You've reached the end of the survey.",
							Toast.LENGTH_SHORT).show();
					submitSurveyInterface();
					break;
				} else if (jumpString != null) {
					jumpFinder(jumpString);
				} else if (questionposition + 1 < totalquestionsArray[chapterposition - 1]) {
					++questionposition;
				} else if (questionposition + 1 >= totalquestionsArray[chapterposition - 1]) {
					questionposition = 0;
					++chapterposition;
				}
				selectChapter(chapterposition, questionposition);
			}

			break;
		case SUBMIT:
			submitSurveyInterface();
			break;
		}
	}

	// Handler to handle answers to survey questions
	public void AnswerRecieve(String answerStringRecieve,
			String jumpStringRecieve) {
		answerString = answerStringRecieve;
		jumpString = jumpStringRecieve;
		if (answerString != null) {
			if (askingTripQuestions == false) {
				try {
					jsurv.getJSONObject("Survey").getJSONArray("Chapters")
							.getJSONObject(chapterposition - 1)
							.getJSONArray("Questions")
							.getJSONObject(questionposition)
							.put("Answer", answerString);
					// toast = Toast.makeText(this, "Answer passed: " +
					// answerString,
					// Toast.LENGTH_SHORT);
					// toast.show();
				} catch (JSONException e) {
					e.printStackTrace();
				}
			} else if (askingTripQuestions == true) {
				try {
					jsurv.getJSONObject("Tracker").getJSONArray("Questions")
							.getJSONObject(tripQuestionposition)
							.put("Answer", answerString);
					// toast = Toast.makeText(this, "Answer passed: " +
					// jsurv.getJSONObject("Tracker")
					// .getJSONArray("Questions")
					// .getJSONObject(tripQuestionposition).getString("Answer"),
					// Toast.LENGTH_SHORT);
					// toast.show();
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}

		// toast = Toast.makeText(this, "After: Jump: " + jumpString +
		// "Answer: "
		// + answerString, Toast.LENGTH_SHORT);
		// toast.show();
	}

	// Handler to handle new survey position after answer to question
	public void PositionRecieve(Integer chapterpositionrecieve,
			Integer questionpositionrecieve) {
		questionposition = questionpositionrecieve;
		chapterposition = chapterpositionrecieve;
	}

	@Override
	public void updateStatusPage() {
		messageHandler.sendEmptyMessage(EVENT_TYPE.UPDATE_STATS_PAGE.ordinal());
	}

	@Override
	public void HubButtonPressed(HubButtonType type) {
		switch (type) {
		case UPDATE_PAGE:
			messageHandler.sendEmptyMessage(EVENT_TYPE.UPDATE_HUB_PAGE.ordinal());
			break;
		case TOGGLETRIP:
			if (isTripStarted) {
				stopTrip();
				tripDistance += startLocation.distanceTo(mLocationClient
						.getLastLocation());
				ridesCompleted++;
				totalDistanceBefore += tripDistance;
				tripDistance = 0;
			} else {
				startLocation = mLocationClient.getLastLocation();
				startTripTime = Calendar.getInstance();
				startTrip();

				// Obtaining the question desired to send to fragment
				try {
					jquestion = jtrackerquestions
							.getJSONObject(tripQuestionposition);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				askingTripQuestions = true;
				// Starting question fragment and passing json question
				// information.
				navButtons.getView().findViewById(R.id.previous_question_button)
				.setVisibility(View.INVISIBLE);
				navButtons.getView().findViewById(R.id.submit_survey_button)
						.setVisibility(View.INVISIBLE);
				ChangeQuestion(jquestion, 0, tripQuestionposition);
			}
			break;
		case NEWSURVEY:
			chapterposition = 1;
			questionposition = 0;
			selectChapter(chapterposition, questionposition);
			break;
		case STATISTICS:
			showStatusPage();
			break;
		case FEWERMEN:
			if (maleCount > 0) {
				maleCount--;
				updateCount("male");
			}
			break;
		case FEWERWOMEN:
			if (femaleCount > 0) {
				femaleCount--;
				updateCount("female");
			}
			break;
		case MOREMEN:
			maleCount++;
			updateCount("male");
			break;
		case MOREWOMEN:
			femaleCount++;
			updateCount("female");
			break;
		default:
			break;
		}
	}

	public void updateCount(String gender) {
		if (gender.equals("male")) {
			messageHandler.sendEmptyMessage(EVENT_TYPE.MALE_UPDATE.ordinal());
		} else if (gender.equals("female")) {
			messageHandler.sendEmptyMessage(EVENT_TYPE.FEMALE_UPDATE.ordinal());
		}
	}

	public void createcolumn(String name, String type, String TABLE_ID)
			throws ClientProtocolException, IOException {
		String url = "https://www.googleapis.com/fusiontables/v1/tables/"
				+ TABLE_ID + "/columns?key=" + API_KEY;

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
		nameValuePairs.add(new BasicNameValuePair("key", API_KEY));
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

	public String[] getquestionlist(JSONArray herearethequestions,
			String whattogetString, String survortrip) {
		Integer numberofquestions = 0;
		String[] questionStringArray;
		if (survortrip.equals("survey")) {
			for (int i = 0; i < totalsurveychapters; ++i) {
				for (int j = 0; j < totalquestionsArray[i]; ++j) {
					++numberofquestions;
				}
			}
		} else if (survortrip.equals("trip")) {
			numberofquestions = herearethequestions.length();
		}
		questionStringArray = new String[numberofquestions];

		if (survortrip.equals("survey")) {
			int auxcount = 0;
			for (int i = 0; i < totalsurveychapters; ++i) {
				for (int j = 0; j < totalquestionsArray[i]; ++j) {
					try {
						questionStringArray[auxcount] = herearethequestions
								.getJSONObject(i).getJSONArray("Questions")
								.getJSONObject(j).getString(whattogetString);
						// Log.v("ID", questionIDArray[auxcount]);
						++auxcount;
					} catch (JSONException e) {
						questionStringArray[auxcount] = "";
					}
				}
			}
		} else if (survortrip.equals("trip")) {
			for (int i = 0; i < numberofquestions; ++i) {
				try {
					questionStringArray[i] = herearethequestions.getJSONObject(
							i).getString(whattogetString);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		Log.v("Number of questions", numberofquestions.toString() + " "
				+ whattogetString);
		return questionStringArray;
	}

	public void columnCheck(String TABLE_ID, String survortrip) {
		// If survortrip euals "survey" it will check the survey table, if it's
		// "trip" it will check the tracker table.
		Boolean existsBoolean;
		String[] hardcolumnsStrings = null; // Columns that are in all projects.
		String[] hardcolumntypeStrings = null; // Types for the columns that are
												// in all projects.
		JSONArray whereTheQuestionsAre = null;
		if (survortrip.equals("survey")) {
			hardcolumnsStrings = new String[] { "Location", "Date", "Lat",
					"Alt", "Lng", "SurveyID", "TripID", "Username" };
			hardcolumntypeStrings = new String[] { "LOCATION", "DATETIME",
					"NUMBER", "NUMBER", "NUMBER", "STRING", "STRING", "STRING" };
			whereTheQuestionsAre = jchapterlist;
		} else if (survortrip.equals("trip")) {
			hardcolumnsStrings = new String[] { "Location", "Date", "Lat",
					"Alt", "Lng", "TripID", "Username" };
			hardcolumntypeStrings = new String[] { "LOCATION", "DATETIME",
					"NUMBER", "NUMBER", "NUMBER", "STRING", "STRING" };
			whereTheQuestionsAre = jtrackerquestions;
		}
		// Getting the types and names of the columns in the fusion table.
		try {
			columnlistNameString = getcolumnList(TABLE_ID, API_KEY, "name");
			columnlistTypeString = getcolumnList(TABLE_ID, API_KEY, "type");
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Checking for the existence of the hard columns on Fusion table.
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
					createcolumn(hardcolumnsStrings[i],
							hardcolumntypeStrings[i], TABLE_ID);
				} catch (ClientProtocolException e) {

					e.printStackTrace();
				} catch (IOException e) {

					e.printStackTrace();
				}
			}
		}
		// Checking for the existence of question columns on Fusion table.
		questionIdlistString = getquestionlist(whereTheQuestionsAre, "id",
				survortrip);
		questionKindlistString = getquestionlist(whereTheQuestionsAre, "Kind",
				survortrip);
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
					createcolumn(questionIdlistString[i], auxkind, TABLE_ID);
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

	public String createID() {
		String ID = null;
		Integer randy;
		for (int i = 0; i < 7; ++i) {
			randy = (int) (Math.random() * ((9) + 1));
			if (i == 0) {
				ID = randy.toString();
			} else {
				ID = ID + randy.toString();
			}
		}

		return ID;
	}

	public void resetSurvey() {
		surveyID = "S" + createID();
		// toast = Toast.makeText(getApplicationContext(), getResources()
		// .getString(R.string.survey_id) + " " + surveyID,
		// Toast.LENGTH_SHORT);
		// toast.show();
		for (int i = 0; i < totalsurveychapters; ++i) {
			for (int j = 0; j < totalquestionsArray[i]; j++) {
				try {
					jsurv.getJSONObject("Survey").getJSONArray("Chapters")
							.getJSONObject(i).getJSONArray("Questions")
							.getJSONObject(j).remove("Answer");
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void submitSurveyInterface() {
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					// Yes button clicked
					toast = Toast.makeText(getApplicationContext(),
							getResources()
									.getString(R.string.submitting_survey),
							Toast.LENGTH_SHORT);
					toast.show();
					new Thread(new Runnable() {
						public void run() {
							try {
								submitSurvey();
							} catch (ClientProtocolException e1) {
								e1.printStackTrace();
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
					}).start();
					toast = Toast
							.makeText(getApplicationContext(), getResources()
									.getString(R.string.survey_submitted),
									Toast.LENGTH_SHORT);
					toast.show();
					resetSurvey();
					showHubPage();
					break;
				case DialogInterface.BUTTON_NEGATIVE:
					// No button clicked
					break;
				}
			}
		};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(
				getResources().getString(R.string.submit_survey_question))
				.setPositiveButton(getResources().getString(R.string.yes),
						dialogClickListener)
				.setNegativeButton(getResources().getString(R.string.no),
						dialogClickListener).show();
	}

}