package org.urbanlaunchpad.flocktracker;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.urbanlaunchpad.flocktracker.Status_page_fragment.StatusPageUpdate;
import org.urbanlaunchpad.flocktracker.SurveyHelper.NextQuestionResult;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.content.LocalBroadcastManager;
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
		Hub_page_fragment.HubButtonCallback, StatusPageUpdate,
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener {
	private DrawerLayout ChapterDrawerLayout;
	private ListView ChapterDrawerList;
	private ActionBarDrawerToggle ChapterDrawerToggle;

	private CharSequence ChapterDrawerTitle;
	private CharSequence Title;
	private String username;

	private LocationClient mLocationClient;
	private final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	private Fragment navButtons;
	private Integer maleCount = 0;
	private Integer femaleCount = 0;
	private String surveyID;
	private String tripID;
	private boolean isTripStarted = false;
	private Integer trackerWait = 5000;
	private Calendar startTripTime = null;
	private double tripDistance = 0;
	private double totalDistanceBefore = 0;
	private int ridesCompleted = 0;
	private int surveysCompleted = 0;
	private Location startLocation;
	private Activity thisActivity;
	private Boolean askingTripQuestions = false;
	private boolean showingStatusPage = false;
	private boolean showingHubPage = false;
	private SurveyHelper surveyHelper;
	private Tracker tracker = null;
	
	private enum EVENT_TYPE {
		MALE_UPDATE, FEMALE_UPDATE, UPDATE_STATS_PAGE, UPDATE_HUB_PAGE, SHOW_NAV_BUTTONS
	}

	@SuppressLint("HandlerLeak")
	private Handler messageHandler = new Handler() {

		public void handleMessage(Message msg) {
			if (msg.what == EVENT_TYPE.SHOW_NAV_BUTTONS.ordinal()) {
				if (!showingStatusPage && !showingHubPage) {
					FragmentManager fragmentManager = getFragmentManager();
					FragmentTransaction transactionShow = fragmentManager
							.beginTransaction();
					transactionShow.show(navButtons);
					transactionShow.commit();
				}
			} else if (msg.what == EVENT_TYPE.MALE_UPDATE.ordinal()) {
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
				// hide navigation buttons
				FragmentManager fragmentManager = getFragmentManager();

				FragmentTransaction transactionHide = fragmentManager
						.beginTransaction();
				transactionHide.hide(navButtons);
				transactionHide.commit();

				// update selected item and title, then close the drawer.
				ChapterDrawerList.setItemChecked(0, true);
				setTitle(surveyHelper.getChapterTitles()[0]);
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

					// Get time difference
					if (startTripTime != null) {
						Calendar difference = Calendar.getInstance();
						difference.setTimeInMillis(difference.getTimeInMillis()
								- startTripTime.getTimeInMillis());
						tripTimeText.setText(difference.getTime().getMinutes()
								+ ":" + difference.getTime().getSeconds());
					} else {
						tripTimeText.setText(R.string.total_time);
					}

					// Get address
					Geocoder geocoder = new Geocoder(thisActivity,
							Locale.getDefault());
					Location current = mLocationClient.getLastLocation();
					List<Address> addresses = null;
					try {
						addresses = geocoder.getFromLocation(
								current.getLatitude(), current.getLongitude(),
								1);
					} catch (IOException e) {
						e.printStackTrace();
					}

					// Update our views
					ridesCompletedText.setText("" + ridesCompleted);
					totalDistanceText
							.setText(""
									+ String.format(
											"%.2f",
											(totalDistanceBefore + tripDistance) / 1000.0));
					tripDistanceText.setText(""
							+ String.format("%.2f", tripDistance / 1000.0));
					surveysCompletedText.setText("" + surveysCompleted);
					usernameText.setText("Hi " + username + "!");

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

					} else {
						currentAddressText.setText(R.string.current_address);
					}
				}
			}
		}
	};

	/*
	 * Activity Lifecycle Handlers
	 */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_surveyor);
		Bundle extras = getIntent().getExtras();
		thisActivity = this;
		if (extras != null) {
			username = extras.getString("username");
			surveyHelper = new SurveyHelper(username,
					extras.getString("token"), extras.getString("jsonsurvey"),
					getApplicationContext());
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
				R.layout.chapter_drawer_list_item, surveyHelper
						.getChapterTitles()));
		ChapterDrawerList.setOnItemClickListener(new DrawerItemClickListener());
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);
		ChapterDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
		ChapterDrawerLayout, /* DrawerLayout object */
		R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
		R.string.chapter_drawer_open, /* For accessibility */
		R.string.chapter_drawer_close /* For accessibility */) {
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

		// location tracking

		// new Thread(new Runnable() {
		// public void run() {
		// while (true) {
		// try {
		// if (isTripStarted) {
		// submitLocation();
		// Thread.sleep(trackerWait);
		// } else {
		// Thread.sleep(5000);
		// }
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
		// }
		// }
		// }).start();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// No call for super(). Bug on API Level > 11.
	}

	@Override
	protected void onStart() {
		super.onStart();
		// Connect the client.
		mLocationClient.connect();
	}

	@Override
	protected void onStop() {
		// Disconnecting the client invalidates it.
		mLocationClient.disconnect();
		super.onStop();
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
		if (!showingHubPage && !showingStatusPage) {
			int chapterPosition = surveyHelper.getChapterPosition();
			int questionPosition = surveyHelper.getQuestionPosition();

			if (questionPosition == 0) {
				showHubPage();
				return;
			}

			surveyHelper.updateSurveyPosition(chapterPosition,
					questionPosition - 1);
			showCurrentQuestion();
			return;
		}

		if (showingHubPage && surveyHelper.getChapterPosition() == 1
				&& surveyHelper.getQuestionPosition() == 0)
			finish();

		super.onBackPressed();
	}

	/*
	 * Starting and stopping trip logic
	 */

	public void startTrip() {
		isTripStarted = true;
		tripID = "T" + createID();
		newThreasubmitlocation();
		tracker = new Tracker(); 
		IntentFilter callInterceptorIntentFilter = new IntentFilter("android.intent.action.ANY_ACTION");
	    registerReceiver(tracker, callInterceptorIntentFilter);
		tracker.SetTracker(this, Surveyor.this);
		
//		tracker = new Tracker();
//		tracker.SetTracker(getBaseContext());
	}

	public void stopTrip() {
		newThreasubmitlocation();
		tracker.CancelTracker(this);
		isTripStarted = false;
		tripID = "";
		startTripTime = null;

		messageHandler.sendEmptyMessage(EVENT_TYPE.UPDATE_HUB_PAGE.ordinal());
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
		surveyHelper.resetSurvey();
	}

	/*
	 * Submitting survey and location logic
	 */

	public void submitSurvey() throws ClientProtocolException, IOException {
		if (surveyHelper.submitSurvey(mLocationClient.getLastLocation(),
				surveyID, tripID))
			surveysCompleted++;
	}

	public void submitLocation() {
		if (mLocationClient.isConnected()) {
			// Update status page information
			Location currentLocation = mLocationClient.getLastLocation();
			tripDistance += startLocation.distanceTo(currentLocation);
			startLocation = currentLocation;

			// Submit location
			try {
				surveyHelper.submitLocation(currentLocation, tripID);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * Drawer Logic
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
		ChapterDrawerToggle.onOptionsItemSelected(item);
		return true;
	}

	private class DrawerItemClickListener implements
			ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			surveyHelper.updateSurveyPosition(position, 0);

			if (position == 0)
				showHubPage();
			else {
				showingHubPage = false;
				showingStatusPage = false;
				messageHandler.sendEmptyMessage(EVENT_TYPE.SHOW_NAV_BUTTONS
						.ordinal());
				showCurrentQuestion();
			}
		}
	}

	/*
	 * Displaying different pages
	 */

	private void showHubPage() {
		// update title
		ChapterDrawerList.setItemChecked(0, true);
		setTitle(surveyHelper.getChapterTitles()[0]);
		ChapterDrawerLayout.closeDrawer(ChapterDrawerList);

		// Update fragments
		FragmentManager fragmentManager = getFragmentManager();
		Fragment fragment = new Hub_page_fragment();

		FragmentTransaction transaction = fragmentManager.beginTransaction();
		transaction.replace(R.id.surveyor_frame, fragment);
		transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		transaction.addToBackStack(null);
		transaction.commit();

		// update ui
		messageHandler.sendEmptyMessage(EVENT_TYPE.UPDATE_HUB_PAGE.ordinal());
	}

	private void showStatusPage() {
		showingStatusPage = true;

		FragmentManager fragmentManager = getFragmentManager();
		Fragment fragment = new Status_page_fragment();

		FragmentTransaction transaction = fragmentManager.beginTransaction();
		transaction.replace(R.id.surveyor_frame, fragment);
		transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		transaction.addToBackStack(null);
		transaction.commit();

		// update selected item and title, then close the drawer.
		ChapterDrawerList.setItemChecked(0, true);
		setTitle(surveyHelper.getChapterTitles()[0]);
		ChapterDrawerLayout.closeDrawer(ChapterDrawerList);

		messageHandler.sendEmptyMessage(EVENT_TYPE.UPDATE_STATS_PAGE.ordinal());
	}

	private void showCurrentQuestion() {
		navButtons.getView().findViewById(R.id.submit_survey_button)
				.setVisibility(View.VISIBLE);

		int chapterPosition;
		int questionPosition;
		String currentQuestion = null;

		if (askingTripQuestions) {
			chapterPosition = 0;
			questionPosition = surveyHelper.getChapterPosition();

			// get current trip question
			try {
				currentQuestion = surveyHelper.getCurrentTripQuestion()
						.toString();
			} catch (JSONException e) {
				e.printStackTrace();
			}

			// Hide submit
			navButtons.getView().findViewById(R.id.submit_survey_button)
					.setVisibility(View.INVISIBLE);
		} else {
			chapterPosition = surveyHelper.getChapterPosition();
			questionPosition = surveyHelper.getQuestionPosition();

			// update selected item and title, then close the drawer.
			ChapterDrawerList.setItemChecked(chapterPosition, true);
			setTitle(surveyHelper.getChapterTitles()[chapterPosition]);
			ChapterDrawerLayout.closeDrawer(ChapterDrawerList);

			// Get current question
			try {
				currentQuestion = surveyHelper.getCurrentQuestion().toString();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		// Starting question fragment and passing json question information.
		Fragment fragment = new Question_fragment();
		Bundle args = new Bundle();
		args.putString(Question_fragment.ARG_JSON_QUESTION,
				currentQuestion.toString());
		args.putInt(Question_fragment.ARG_CHAPTER_POSITION, chapterPosition);
		args.putInt(Question_fragment.ARG_QUESTION_POSITION, questionPosition);
		fragment.setArguments(args);

		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction transaction = fragmentManager.beginTransaction();

		// show navigation buttons and add new question
		if (!navButtons.isVisible()) {
			messageHandler.sendEmptyMessage(EVENT_TYPE.SHOW_NAV_BUTTONS
					.ordinal());
		}

		// selectively show previous question button
		if (questionPosition == 0) {
			navButtons.getView().findViewById(R.id.previous_question_button)
					.setVisibility(View.INVISIBLE);
		} else {
			navButtons.getView().findViewById(R.id.previous_question_button)
					.setVisibility(View.VISIBLE);
		}

		transaction.replace(R.id.surveyor_frame, fragment);
		transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		if (!askingTripQuestions)
			transaction.addToBackStack(null);
		transaction.commit();
	}

	@Override
	public void setTitle(CharSequence title) {
		Title = title;
		getActionBar().setTitle(Title);
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
	 * Question Navigation Event Handlers
	 */

	public void NavButtonPressed(NavButtonType type) {
		switch (type) {

		case PREVIOUS:
			surveyHelper.onPrevQuestionPressed(askingTripQuestions);
			showCurrentQuestion();
			break;
		case NEXT:
			NextQuestionResult result = surveyHelper
					.onNextQuestionPressed(askingTripQuestions);

			if (askingTripQuestions) {
				if (result == NextQuestionResult.END) {
					Toast.makeText(this, "Tracking is on!", Toast.LENGTH_SHORT)
							.show();
					askingTripQuestions = false;
					showHubPage();
					startTrip();
					break;
				}
			} else {
				if (result == NextQuestionResult.END) {
					Toast.makeText(this,
							"You've reached the end of the survey.",
							Toast.LENGTH_SHORT).show();
					submitSurveyInterface();
					break;
				}
			}

			showCurrentQuestion();
			break;
		case SUBMIT:
			submitSurveyInterface();
			break;
		}
	}

	/*
	 * Question Event Handlers
	 */

	public void AnswerRecieve(String answerStringReceive,
			String jumpStringReceive) {
		if (answerStringReceive != null) {
			if (!askingTripQuestions) {
				surveyHelper.answerCurrentQuestion(answerStringReceive);
			} else {
				surveyHelper.answerCurrentTrackerQuestion(answerStringReceive);
			}
		}

		if (jumpStringReceive != null) {
			surveyHelper.updateJumpString(jumpStringReceive);
		}
	}

	public void PositionRecieve(Integer chapterpositionrecieve,
			Integer questionpositionrecieve) {
		surveyHelper.updateSurveyPosition(chapterpositionrecieve,
				questionpositionrecieve);
	}

	/*
	 * Status Page Event Handlers
	 */

	@Override
	public void updateStatusPage() {
		// hide navigation buttons
		FragmentManager fragmentManager = getFragmentManager();

		FragmentTransaction transactionHide = fragmentManager
				.beginTransaction();
		transactionHide.hide(navButtons);
		transactionHide.commit();

		messageHandler.sendEmptyMessage(EVENT_TYPE.UPDATE_STATS_PAGE.ordinal());
	}

	@Override
	public void leftStatusPage() {
		showingStatusPage = false;
	}

	/*
	 * Hub Page Event Handlers
	 */

	@Override
	public void HubButtonPressed(HubButtonType type) {
		switch (type) {
		case SHOW_NAV_BUTTONS:
			showingHubPage = false;
			messageHandler.sendEmptyMessage(EVENT_TYPE.SHOW_NAV_BUTTONS
					.ordinal());
			break;
		case UPDATE_PAGE:
			showingHubPage = true;
			messageHandler.sendEmptyMessage(EVENT_TYPE.UPDATE_HUB_PAGE
					.ordinal());
			break;
		case TOGGLETRIP:
			if (isTripStarted) {
				stopTrip();

				// Update status page info
				tripDistance += startLocation.distanceTo(mLocationClient
						.getLastLocation());
				ridesCompleted++;
				totalDistanceBefore += tripDistance;
				tripDistance = 0;
			} else {
				startLocation = mLocationClient.getLastLocation();
				startTripTime = Calendar.getInstance();
				// startTrip();

				askingTripQuestions = true;

				// Starting question fragment and passing json question
				// information.
				surveyHelper.updateSurveyPosition(0, 0);
				showCurrentQuestion();
			}
			break;
		case NEWSURVEY:
			surveyHelper.updateSurveyPosition(1, 0);
			showCurrentQuestion();
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

	public void submitSurveyInterface() {
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					// Yes button clicked
					Toast toast = Toast.makeText(getApplicationContext(),
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
	
	public void newThreasubmitlocation(){
		new Thread(new Runnable() {
			public void run() {
				submitLocation();
			}
		}).start();
	}
	public void dummy(){
		Log.d("Dummy", "LOL");
	}
	
	@Override
	protected void onDestroy() {
		tracker.CancelTracker(this);
		super.onDestroy();
	}
	
//		public void SetTracker() {
//			Toast.makeText(this, "Setting tracker!", Toast.LENGTH_LONG)
//			.show();
//			Log.d("Tracker", "Setting tracker");
////			Log.d("Tracker", "Setting tracker, wait:" + trackerWait);		
//		    IntentFilter callInterceptorIntentFilter = new IntentFilter("android.intent.action.ANY_ACTION");
//		    LocalBroadcastManager.getInstance(this).registerReceiver(trackerBroadcastReceiver, callInterceptorIntentFilter);
//	        AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
//			Intent i = new Intent(Surveyor.this, Surveyor.class);
//			PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
//			am.setRepeating(AlarmManager.RTC_WAKEUP,
//					System.currentTimeMillis(), 5000, pi);
//			Log.d("Tracker", "Tracker working.");
//		}
//
//		public void CancelTracker() {
//			Toast.makeText(this, "Cancelling tracker!", Toast.LENGTH_LONG)
//					.show();
//			Intent intent = new Intent(this, Surveyor.class);
//			PendingIntent sender = PendingIntent
//					.getBroadcast(this, 0, intent, 0);
//			AlarmManager alarmManager = (AlarmManager) this
//					.getSystemService(Context.ALARM_SERVICE);
//			alarmManager.cancel(sender);
//		}

//	public class TrackerService extends Service
//	{
//	    Tracker tracker = new Tracker();
////	    public void onCreate()
////	    {
////	    	Log.d("Service", "Service created");
////	        super.onCreate();       
////	    }
//	    public int onStartCommand(Context context,Intent intent, int startId)
//	    {
//	    	super.onStartCommand(intent, 0, startId);
//			Toast.makeText(context, "Starting service!", Toast.LENGTH_LONG)
//			.show();
//	 	   Intent intent1 = new Intent(this.getApplicationContext(),Surveyor.class);
//	 	
//	 	   
//			Toast.makeText(context, "Service Started!", Toast.LENGTH_LONG)
//			.show();
//	        tracker.SetTracker(context);
//			return START_STICKY;
//	    }
//	    public void onDestroyCommand(){
//	    	super.onDestroy();
//	    	tracker.CancelTracker(getBaseContext());
//	    }
//
//	    @Override
//	    public IBinder onBind(Intent intent) 
//	    {
//	        return null;
//	    }
//	}

}
