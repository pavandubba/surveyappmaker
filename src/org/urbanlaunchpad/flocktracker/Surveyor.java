package org.urbanlaunchpad.flocktracker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.urbanlaunchpad.flocktracker.Status_page_fragment.StatusPageUpdate;
import org.urbanlaunchpad.flocktracker.SurveyHelper.NextQuestionResult;
import org.urbanlaunchpad.flocktracker.SurveyHelper.Tuple;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class Surveyor extends Activity implements
		Question_fragment.AnswerSelected,
		Question_navigator_fragment.NavButtonCallback,
		Hub_page_fragment.HubButtonCallback, StatusPageUpdate,
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener {

	// Drawer fields
	private DrawerLayout ChapterDrawerLayout;
	private ListView FixedNavigationList;
	private ListView ChapterDrawerList;
	private LinearLayout drawer;
	private ActionBarDrawerToggle ChapterDrawerToggle;
	private CharSequence ChapterDrawerTitle;
	private CharSequence Title;
	private List<RowItem> rowItems;
	public static final Integer INCOMPLETE_CHAPTER = R.drawable.complete_red;
	public static final Integer COMPLETE_CHAPTER = R.drawable.complete_green;
	public static final Integer HALF_COMPLETE_CHAPTER = R.drawable.complete_orange;

	private String username;
	private LocationClient mLocationClient;
	private final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	private Fragment navButtons;
	private Integer maleCount = 0;
	private Integer femaleCount = 0;
	private String surveyID;
	private String tripID;
	private boolean isTripStarted = false;
	private Calendar startTripTime = null;
	private double tripDistance = 0;
	private double totalDistanceBefore = 0;
	private int ridesCompleted = 0;
	private int surveysCompleted = 0;
	private Location startLocation;
	private List<Address> addresses;
	private Activity thisActivity;
	public static Boolean askingTripQuestions = false;
	private Boolean inLoop = false;
	private boolean showingStatusPage = false;
	private boolean showingHubPage = false;
	private SurveyHelper surveyHelper;
	public static GoogleDriveHelper driveHelper;
	static final Integer TRACKER_INTERVAL = 30000; // Tracker working every 30
													// seconds.
	static final String HUB_PAGE_TITLE = "Hub Page";
	static final String STATISTICS_PAGE_TITLE = "Statistics";
	private Question_fragment currentQuestionFragment;
	public static boolean submittingSubmission = false;
	public static boolean savingSurveySubmission = false;
	public static boolean savingTrackerSubmission = false;

	public static final String TRACKER_TYPE = "Tracker";
	public static final String SURVEY_TYPE = "Survey";

	// Milliseconds per second
	private static final int MILLISECONDS_PER_SECOND = 1000;
	// Update frequency in seconds
	public static final int UPDATE_INTERVAL_IN_SECONDS = 5;
	// Update frequency in milliseconds
	private static final long UPDATE_INTERVAL = MILLISECONDS_PER_SECOND
			* UPDATE_INTERVAL_IN_SECONDS;
	// The fastest update frequency, in seconds
	private static final int FASTEST_INTERVAL_IN_SECONDS = 1;
	// A fast frequency ceiling in milliseconds
	private static final long FASTEST_INTERVAL = MILLISECONDS_PER_SECOND
			* FASTEST_INTERVAL_IN_SECONDS;

	// Define an object that holds accuracy and frequency parameters
	LocationRequest mLocationRequest;

	// Stored queues of surveys to submit
	public static HashSet<String> surveySubmissionQueue;
	public static HashSet<String> trackerSubmissionQueue;

	private enum EVENT_TYPE {
		MALE_UPDATE, FEMALE_UPDATE, UPDATE_STATS_PAGE, UPDATE_HUB_PAGE, SHOW_NAV_BUTTONS, SUBMITTED_SURVEY, SUBMIT_FAILED
	}

	@SuppressLint("HandlerLeak")
	private Handler messageHandler = new Handler() {

		@SuppressWarnings("deprecation")
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
				askingTripQuestions = false;

				// hide navigation buttons
				FragmentManager fragmentManager = getFragmentManager();

				FragmentTransaction transactionHide = fragmentManager
						.beginTransaction();
				transactionHide.hide(navButtons);
				transactionHide.commit();

				// update male count
				TextView maleCountView = (TextView) findViewById(R.id.maleCount);
				if (maleCountView != null)
					maleCountView.setText(maleCount.toString());

				// update female count
				TextView femaleCountView = (TextView) findViewById(R.id.femaleCount);
				if (femaleCountView != null)
					femaleCountView.setText(femaleCount.toString());

				// update total count
				TextView totalCount = (TextView) findViewById(R.id.totalPersonCount);
				if (totalCount != null)
					totalCount.setText("" + (maleCount + femaleCount));

				if (isTripStarted) {
					ImageView gear = (ImageView) findViewById(R.id.start_trip_button);
					gear.setImageResource(R.drawable.ft_grn_st1);
				} else {
					ImageView gear = (ImageView) findViewById(R.id.start_trip_button);
					gear.setImageResource(R.drawable.ft_red_st);
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
						tripTimeText.setText(Html.fromHtml("<b>"
								+ String.format("%02d", difference.getTime()
										.getMinutes())
								+ "</b>"
								+ ":"
								+ String.format("%02d", difference.getTime()
										.getSeconds())));
					} else {
						tripTimeText.setText(Html.fromHtml("<b>00</b>" + ":"
								+ "00"));
					}

					// Get address
					if (mLocationClient.isConnected()) {
						startLocation = mLocationClient.getLastLocation();
					}
					if (startLocation != null) {
						Log.d("Startlocation", "Not null");
						new Thread(new Runnable() {
							public void run() {
								try {
									Geocoder geocoder = new Geocoder(
											thisActivity, Locale.getDefault());
									// Location current =
									// mLocationClient.getLastLocation();
									addresses = geocoder.getFromLocation(
											startLocation.getLatitude(),
											startLocation.getLongitude(), 1);
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}).start();

						if (addresses != null && addresses.size() > 0) {
							// Get the first address
							Address address = addresses.get(0);
							/*
							 * Format the first line of address (if available),
							 * city, and country name.
							 */
							String addressText = String
									.format("%s%s%s",
									// If there's a street address, add
									// it
											address.getMaxAddressLineIndex() > 0 ? address
													.getAddressLine(0) + ", "
													: "",
											// Locality is usually a city
											address.getLocality() != null ? address
													.getLocality() + ", "
													: "",
											// The country of the address
											address.getCountryName());
							currentAddressText.setText(addressText);

						} else {
							currentAddressText
									.setText(R.string.default_address);
						}
					}

					int distanceBeforeDecimal = (int) (tripDistance / 1000.0);
					int distanceAfterDecimal = (int) Math
							.round(100 * (tripDistance / 1000.0 - distanceBeforeDecimal));

					// Update our views
					ridesCompletedText.setText("" + ridesCompleted);
					tripDistanceText.setText(Html.fromHtml("<b>"
							+ String.format("%02d", distanceBeforeDecimal)
							+ "</b>" + "."
							+ String.format("%02d", distanceAfterDecimal)));
					totalDistanceText
							.setText(""
									+ String.format(
											"%.2f",
											(totalDistanceBefore + tripDistance) / 1000.0));
					surveysCompletedText.setText("" + surveysCompleted);
					usernameText.setText("Hi " + username + "!");
				}
			} else if (msg.what == EVENT_TYPE.SUBMITTED_SURVEY.ordinal()) {
				Toast toast = Toast.makeText(getApplicationContext(),
						getResources().getString(R.string.survey_submitted),
						Toast.LENGTH_SHORT);
				toast.show();
				surveyHelper.updateSurveyPosition(
						SurveyHelper.HUB_PAGE_CHAPTER_POSITION,
						SurveyHelper.HUB_PAGE_QUESTION_POSITION);
				showHubPage();
			} else if (msg.what == EVENT_TYPE.SUBMIT_FAILED.ordinal()) {
				Toast toast = Toast.makeText(getApplicationContext(),
						getResources().getString(R.string.submit_failed),
						Toast.LENGTH_SHORT);
				toast.show();
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
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		if (extras != null) {
			username = extras.getString("username");
			surveyHelper = new SurveyHelper(username,
					extras.getString("jsonsurvey"), getApplicationContext());
		}
		driveHelper = new GoogleDriveHelper(this);

		// Navigation drawer information.
		Title = ChapterDrawerTitle = getTitle();
		ChapterDrawerLayout = (DrawerLayout) findViewById(R.id.chapter_drawer_layout);
		ChapterDrawerList = (ListView) findViewById(R.id.chapter_drawer);
		FixedNavigationList = (ListView) findViewById(R.id.fixed_navigation);
		drawer = (LinearLayout) findViewById(R.id.drawer);
		rowItems = new ArrayList<RowItem>();
		for (String chapterTitle : surveyHelper.getChapterTitles()) {
			RowItem rowItem = new RowItem(INCOMPLETE_CHAPTER, chapterTitle);
			rowItems.add(rowItem);
		}

		// set a custom shadow that overlays the main content when the drawer
		// opens
		ChapterDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
				GravityCompat.START);
		// set up the drawer's list view with items and click listener
		FixedNavigationList.setAdapter((new ArrayAdapter<String>(this,
				R.layout.old_chapter_list_item, new String[] { HUB_PAGE_TITLE,
						STATISTICS_PAGE_TITLE })));
		FixedNavigationList
				.setOnItemClickListener(new FixedNavigationItemClickListener());
		ChapterDrawerList.setAdapter(new DrawerListViewAdapter(this,
				R.layout.chapter_drawer_list_item, rowItems));
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

		mLocationRequest = LocationRequest.create();
		// Use high accuracy
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		// Set the update interval to 5 seconds
		mLocationRequest.setInterval(UPDATE_INTERVAL);
		// Set the fastest update interval to 1 second
		mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

		// Creating a random survey ID

		surveyID = "S" + createID();

		// location tracking
		Tracker.surveyor = this;

		// Load statistics from previous run-through
		totalDistanceBefore = Iniconfig.prefs.getFloat("tripDistanceBefore", 0);
		ridesCompleted = Iniconfig.prefs.getInt("ridesCompleted", 0);
		surveysCompleted = Iniconfig.prefs.getInt("surveysCompleted", 0);

		trackerSubmissionQueue = new HashSet<String>(
				Iniconfig.prefs.getStringSet("trackerSubmissionQueue",
						new HashSet<String>()));
		surveySubmissionQueue = new HashSet<String>(
				Iniconfig.prefs.getStringSet("surveySubmissionQueue",
						new HashSet<String>()));
		if (!surveySubmissionQueue.isEmpty()
				|| !trackerSubmissionQueue.isEmpty()) {
			spawnSubmission();
		}

		// Check for location services.
		SurveyHelper.checkLocationConfig(this);

	}

	// Spawn a thread that continuously pops off a survey to submit
	public void spawnSubmission() {
		new Thread(new Runnable() {
			@SuppressWarnings("unchecked")
			public void run() {
				while (true) {
					synchronized (trackerSubmissionQueue) {
						if (trackerSubmissionQueue.isEmpty()) {
							submittingSubmission = false;
						} else {
							submittingSubmission = true;
						}
					}

					if (!submittingSubmission) {
						synchronized (surveySubmissionQueue) {
							if (surveySubmissionQueue.isEmpty()) {
							} else {
								submittingSubmission = true;
							}
						}
					}

					if (!submittingSubmission) {
						Log.d("Spawn submission queue", "Queue is empty");
						break;
					}

					// Iterate through queues to submit surveys
					submitFromQueue(surveySubmissionQueue,
							"surveySubmissionQueue");
					submitFromQueue(trackerSubmissionQueue,
							"trackerSubmissionQueue");
				}
			}
		}).start();
	}

	public void submitFromQueue(HashSet<String> queue, String queueName) {
		synchronized (queue) {

			Iterator<String> i = queue.iterator();
			if (i.hasNext()) {
				boolean success = false;
				try {
					JSONObject submission = new JSONObject(i.next());
					String tripIDString = submission.has("tripID") ? submission
							.getString("tripID") : "";
					String surveyIDString = submission.has("surveyID") ? submission
							.getString("surveyID") : "";
					success = surveyHelper.submitSubmission(
							submission.getString("jsurv"),
							submission.getString("lat"),
							submission.getString("lng"),
							submission.getString("alt"),
							submission.getString("imagePaths"), surveyIDString,
							tripIDString, submission.getString("timestamp"),
							submission.getString("type"),
							submission.getString("maleCount"),
							submission.getString("femaleCount"),
							submission.getString("totalCount"));
				} catch (Exception e) {
					e.printStackTrace();
				}

				// Finished submitting. Remove from queue and
				// commit
				if (success) {
					Log.d("Spawn submission queue", "Submission success.");
					i.remove();
					Iniconfig.prefs
							.edit()
							.putStringSet(queueName,
									(Set<String>) queue.clone()).commit();

				} else { // no connection, sleep for a while and try
					// again
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

			}
		}
	}

	@Override
	protected void onPause() {
		Iniconfig.prefs.edit().putInt("ridesCompleted", ridesCompleted)
				.commit();
		Iniconfig.prefs.edit().putInt("surveysCompleted", surveysCompleted)
				.commit();
		Iniconfig.prefs.edit()
				.putFloat("totalDistanceBefore", (float) totalDistanceBefore)
				.commit();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		cancelTracker();
		surveyHelper.resetTracker();
		mLocationClient.disconnect();
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// No call for super(). Bug on API Level > 11.
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
		if (askingTripQuestions) {
			if (surveyHelper.prevTrackingPositions.empty()
					|| surveyHelper.getTripQuestionPosition() == 0) {
				surveyHelper
						.updateTrackerPositionOnBack(SurveyHelper.HUB_PAGE_QUESTION_POSITION);
				showHubPage();
				return;
			}
			// Pop last question off
			Integer prevPosition = surveyHelper.prevTrackingPositions.pop();
			surveyHelper.updateTrackerPositionOnBack(prevPosition);
			showCurrentQuestion();
			return;
		}

		if (surveyHelper.prevPositions.isEmpty()) {
			finish();
			return;
		}

		Tuple prevPosition = surveyHelper.prevPositions.pop();
		// Pop last question off
		surveyHelper.updateSurveyPositionOnBack(prevPosition.chapterPosition,
				prevPosition.questionPosition);

		if (surveyHelper.wasJustAtHubPage(prevPosition)) {
			showHubPage();
			return;
		} else if (surveyHelper.wasJustAtStatsPage(prevPosition)) {
			showStatusPage();
			return;
		}

		showCurrentQuestion();
		return;
	}

	/*
	 * Starting and stopping trip logic
	 */

	public void startTrip() {
		isTripStarted = true;
		mLocationClient.connect();
		tripID = "T" + createID();
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

		for (RowItem rowItem : rowItems) {
			rowItem.setImageId(INCOMPLETE_CHAPTER);
		}
	}

	public void resetTrackerSurvey() {
		askingTripQuestions = false;
		surveyHelper.prevTrackingPositions = new Stack<Integer>();
	}

	/*
	 * Submitting survey and location logic
	 */

	public void saveSurvey() {
		savingSurveySubmission = true;
		// connect if not tracking
		if (!mLocationClient.isConnected()) {
			mLocationClient.connect();
		}
		if (mLocationClient.isConnected()) {
			new Thread(new Runnable() {
				public void run() {
					String jsurvString = surveyHelper.jsurv.toString();
					JSONObject imagePaths = new JSONObject();
					for (Tuple key : SurveyHelper.prevImages.keySet()) {
						try {
							imagePaths.put(key.toString(),
									SurveyHelper.prevImages.get(key).getPath());
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
					resetSurvey();
					surveyHelper.jumpString = null;

					// save location tagged survey
					surveyHelper.saveSubmission(
							mLocationClient.getLastLocation(), surveyID,
							tripID, jsurvString, imagePaths, SURVEY_TYPE,
							maleCount.toString(), femaleCount.toString(),
							((Integer) (maleCount + femaleCount)).toString());
					// disconnect if not tracking or not currently
					// submitting
					// surveys
					if (!isTripStarted && !submittingSubmission)
						mLocationClient.disconnect();

					surveysCompleted++;
				}
			}).start();

			messageHandler.sendEmptyMessage(EVENT_TYPE.SUBMITTED_SURVEY
					.ordinal());
		}
	}

	public void saveLocation() {
		// connect if not tracking
		Log.d("Save Location", "" + mLocationClient.isConnected());
		if (!mLocationClient.isConnected()) {
			mLocationClient.connect();
		}
		if (mLocationClient.isConnected()) {
			new Thread(new Runnable() {
				public void run() {
					try {
						surveyHelper.jsurv
								.put("Tracker", surveyHelper.jtracker);
					} catch (JSONException e1) {
						e1.printStackTrace();
					}

					String jsurvString = surveyHelper.jsurv.toString();
					JSONObject imagePaths = new JSONObject();
					for (Integer key : SurveyHelper.prevTrackerImages.keySet()) {
						try {
							imagePaths.put(key.toString(),
									SurveyHelper.prevTrackerImages.get(key)
											.getPath());
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}

					// save location tagged survey
					surveyHelper.saveSubmission(startLocation, surveyID,
							tripID, jsurvString, imagePaths, "Tracker",
							maleCount.toString(), femaleCount.toString(),
							((Integer) (maleCount + femaleCount)).toString());
				}
			}).start();
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

		case GoogleDriveHelper.REQUEST_ACCOUNT_PICKER:
			if (resultCode == RESULT_OK && intent != null
					&& intent.getExtras() != null) {
				driveHelper.requestAccountPicker(intent);
			}
			break;
		case GoogleDriveHelper.REQUEST_AUTHORIZATION:
			if (resultCode == Activity.RESULT_OK) {
				if (askingTripQuestions) {
					SurveyHelper.prevTrackerImages.put(
							surveyHelper.getTripQuestionPosition(),
							driveHelper.fileUri);
				} else {
					SurveyHelper.prevImages.put(
							new Tuple(surveyHelper.getChapterPosition(),
									surveyHelper.getQuestionPosition()),
							driveHelper.fileUri);
				}
				currentQuestionFragment.ImageLayout();
			} else {
				startActivityForResult(
						Iniconfig.credential.newChooseAccountIntent(),
						GoogleDriveHelper.REQUEST_ACCOUNT_PICKER);
			}
			break;
		case GoogleDriveHelper.CAPTURE_IMAGE:
			try {
				Bitmap imageBitmap = BitmapFactory.decodeFile(
						driveHelper.fileUri.getPath(), null);
				float rotation = ImageHelper.rotationForImage(this,
						Uri.fromFile(new File(driveHelper.fileUri.getPath())));
				if (rotation != 0) {
					Matrix matrix = new Matrix();
					matrix.preRotate(rotation);
					imageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0,
							imageBitmap.getWidth(), imageBitmap.getHeight(),
							matrix, true);
				}

				imageBitmap.compress(CompressFormat.JPEG, 25,
						new FileOutputStream(driveHelper.fileUri.getPath()));
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (resultCode == Activity.RESULT_OK) {
				if (askingTripQuestions) {
					SurveyHelper.prevTrackerImages.put(
							surveyHelper.getTripQuestionPosition(),
							driveHelper.fileUri);
				} else {
					SurveyHelper.prevImages.put(
							new Tuple(surveyHelper.getChapterPosition(),
									surveyHelper.getQuestionPosition()),
							driveHelper.fileUri);
				}
				currentQuestionFragment.ImageLayout();
			}
			break;

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

	private class FixedNavigationItemClickListener implements
			ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Log.d("Clicked on fixed position", position + "");
			if (askingTripQuestions)
				resetTrackerSurvey();

			if (position == 0) {
				surveyHelper.updateSurveyPosition(
						SurveyHelper.HUB_PAGE_CHAPTER_POSITION,
						SurveyHelper.HUB_PAGE_QUESTION_POSITION);
				showHubPage();
			} else {
				surveyHelper.updateSurveyPosition(
						SurveyHelper.STATS_PAGE_CHAPTER_POSITION,
						SurveyHelper.STATS_PAGE_QUESTION_POSITION);
				showStatusPage();
			}
		}
	}

	private class DrawerItemClickListener implements
			ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Log.d("Clicked on drawer position", position + "");
			if (askingTripQuestions)
				resetTrackerSurvey();
			surveyHelper.updateSurveyPosition(position, 0);
			surveyHelper.jumpString = null;
			showingHubPage = false;
			showingStatusPage = false;
			showCurrentQuestion();
		}
	}

	/*
	 * Displaying different pages
	 */

	private void showHubPage() {
		showingHubPage = true;
		showingStatusPage = false;

		// Update fragments
		FragmentManager fragmentManager = getFragmentManager();
		Fragment fragment = new Hub_page_fragment();

		FragmentTransaction transaction = fragmentManager.beginTransaction();
		transaction.replace(R.id.surveyor_frame, fragment);
		transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		transaction.addToBackStack(null);
		transaction.commit();

		// update selected item and title, then close the drawer.
		FixedNavigationList.setItemChecked(0, true);
		ChapterDrawerList.setItemChecked(-1, true);
		setTitle(HUB_PAGE_TITLE);
		ChapterDrawerLayout.closeDrawer(drawer);

		// update ui
		messageHandler.sendEmptyMessage(EVENT_TYPE.UPDATE_HUB_PAGE.ordinal());
	}

	private void showStatusPage() {
		showingHubPage = false;
		showingStatusPage = true;

		FragmentManager fragmentManager = getFragmentManager();
		Fragment fragment = new Status_page_fragment();

		FragmentTransaction transaction = fragmentManager.beginTransaction();
		transaction.replace(R.id.surveyor_frame, fragment);
		transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		transaction.addToBackStack(null);
		transaction.commit();

		// update selected item and title, then close the drawer.
		FixedNavigationList.setItemChecked(1, true);
		ChapterDrawerList.setItemChecked(-1, true);
		setTitle(STATISTICS_PAGE_TITLE);
		ChapterDrawerLayout.closeDrawer(drawer);
	}

	private void showCurrentQuestion() {
		showingHubPage = false;
		showingStatusPage = false;

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
			FixedNavigationList.setItemChecked(-1, true);
			ChapterDrawerList.setItemChecked(chapterPosition, true);
			setTitle(surveyHelper.getChapterTitles()[chapterPosition]);
			ChapterDrawerLayout.closeDrawer(drawer);

			// Get current question
			try {
				currentQuestion = surveyHelper.getCurrentQuestion().toString();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		// Starting question fragment and passing json question information.
		currentQuestionFragment = new Question_fragment();
		Bundle args = new Bundle();
		args.putString(Question_fragment.ARG_JSON_QUESTION,
				currentQuestion.toString());
		args.putInt(Question_fragment.ARG_CHAPTER_POSITION, chapterPosition);
		args.putInt(Question_fragment.ARG_QUESTION_POSITION, questionPosition);
		currentQuestionFragment.setArguments(args);

		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction transaction = fragmentManager.beginTransaction();

		// show navigation buttons and add new question
		if (!navButtons.isVisible()) {
			messageHandler.sendEmptyMessage(EVENT_TYPE.SHOW_NAV_BUTTONS
					.ordinal());
		}

		// selectively show previous question button
		if ((askingTripQuestions && surveyHelper.getTripQuestionPosition() == 0)
				|| (!askingTripQuestions && questionPosition == 0)) {
			navButtons.getView().findViewById(R.id.previous_question_button)
					.setVisibility(View.INVISIBLE);
		} else {
			navButtons.getView().findViewById(R.id.previous_question_button)
					.setVisibility(View.VISIBLE);
		}

		transaction.replace(R.id.surveyor_frame, currentQuestionFragment);
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

		mLocationClient.requestLocationUpdates(mLocationRequest,
				new LocationListener() {
					@Override
					public void onLocationChanged(final Location location) {
						// update location + distance
						if (isTripStarted) {
							tripDistance += startLocation.distanceTo(location);
						}
						startLocation = location;
					}
				});

		if (savingSurveySubmission) { // connecting for submitting survey and
										// not
			// tracking
			new Thread(new Runnable() {
				public void run() {
					saveSurvey();
				}
			}).start();
		} else { // connecting for tracking
			startLocation = mLocationClient.getLastLocation();
			Toast.makeText(this, "Tracking is on!", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onDisconnected() {
		// Display the connection status
		Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
	}

	/*
	 * Question Navigation Event Handlers
	 */

	public void NavButtonPressed(NavButtonType type) {
		switch (type) {

		case PREVIOUS:
			currentQuestionFragment.saveState();
			surveyHelper.onPrevQuestionPressed(askingTripQuestions);
			onBackPressed();
			break;
		case NEXT:
			currentQuestionFragment.saveState();
			NextQuestionResult result = surveyHelper.onNextQuestionPressed(
					askingTripQuestions, inLoop);

			if (askingTripQuestions) {
				if (result == NextQuestionResult.END) {
					askingTripQuestions = false;
					surveyHelper.updateSurveyPosition(
							SurveyHelper.HUB_PAGE_CHAPTER_POSITION,
							SurveyHelper.HUB_PAGE_QUESTION_POSITION);
					surveyHelper.prevTrackingPositions = new Stack<Integer>();
					showHubPage();
					startTrip();
					startTripTime = Calendar.getInstance();
					startTracker();
					break;
				}
			} else {
				if (result == NextQuestionResult.CHAPTER_END) {
					rowItems.get(surveyHelper.getChapterPosition() - 1)
							.setImageId(COMPLETE_CHAPTER);
				} else if (result == NextQuestionResult.END) {
					Toast.makeText(this,
							"You've reached the end of the survey.",
							Toast.LENGTH_SHORT).show();
					submitSurveyInterface();
					break;
				}

				RowItem rowItem = rowItems.get(surveyHelper
						.getChapterPosition());
				if (rowItem.getImageId() != COMPLETE_CHAPTER)
					rowItem.setImageId(HALF_COMPLETE_CHAPTER);
			}

			showCurrentQuestion();
			break;
		case SUBMIT:
			currentQuestionFragment.saveState();
			submitSurveyInterface();
			break;
		}
	}

	/*
	 * Question Event Handlers
	 */

	public void AnswerRecieve(String answerStringReceive,
			String jumpStringReceive, ArrayList<Integer> selectedAnswers,
			Boolean inLoopReceive, String questionkindReceive) {
		// TODO: fix loop stuff
		inLoop = (inLoopReceive == null) ? false : inLoopReceive;

		if (questionkindReceive.equals("LP") && answerStringReceive != null) {
			surveyHelper.loopTotal = Integer.parseInt(answerStringReceive);
			if (!askingTripQuestions) {
				surveyHelper.answerCurrentQuestion(answerStringReceive,
						selectedAnswers);
			} else {
				surveyHelper.answerCurrentTrackerQuestion(answerStringReceive,
						selectedAnswers);
			}
		} else if ((answerStringReceive != null) && (inLoop == false)) {
			if (!askingTripQuestions) {
				surveyHelper.answerCurrentQuestion(answerStringReceive,
						selectedAnswers);
			} else {
				surveyHelper.answerCurrentTrackerQuestion(answerStringReceive,
						selectedAnswers);
			}
		} else if ((answerStringReceive != null) && (inLoop = true)) {
			if (!askingTripQuestions) {
				surveyHelper.answerCurrentLoopQuestion(answerStringReceive,
						selectedAnswers);
			} else {
				surveyHelper.answerCurrentTrackerLoopQuestion(
						answerStringReceive, selectedAnswers);
			}
		}

		if (jumpStringReceive != null) {
			surveyHelper.updateJumpString(jumpStringReceive);
		}
	}

	// public void LoopReceive(String Loopend) {
	// if (Loopend != null) {
	// inLoop = true;
	// surveyHelper.setLoopLimits(Loopend);
	// }
	// }

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
		case UPDATE_PAGE:
			showingHubPage = true;
			messageHandler.sendEmptyMessage(EVENT_TYPE.UPDATE_HUB_PAGE
					.ordinal());
			break;
		case TOGGLETRIP:
			if (isTripStarted) {
				// Update status page info
				stopTripDialog();
			} else {
				surveyHelper.resetTracker();
				askingTripQuestions = true;

				// Starting question fragment and passing json question
				// information.
				surveyHelper.updateTrackerPosition(0);
				showCurrentQuestion();
			}
			break;
		case NEWSURVEY:
			surveyHelper.updateSurveyPosition(0, 0);
			showCurrentQuestion();
			break;
		case STATISTICS:
			if (askingTripQuestions)
				surveyHelper
						.updateTrackerPosition(SurveyHelper.STATS_PAGE_QUESTION_POSITION);
			else {
				surveyHelper.updateSurveyPosition(
						SurveyHelper.STATS_PAGE_CHAPTER_POSITION,
						SurveyHelper.STATS_PAGE_QUESTION_POSITION);
			}
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
							saveSurvey();
							synchronized (surveySubmissionQueue) {
								while (savingSurveySubmission) {
									try {
										surveySubmissionQueue.wait();
									} catch (Exception e) {
										e.printStackTrace();
										return;
									}
								}

								if (!submittingSubmission) {
									spawnSubmission();
								}
							}
						}
					}).start();

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

	/*
	 * Location tracking helper
	 */

	public void startTracker() {
		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		Intent intentAlarm = new Intent(this, Tracker.class);
		PendingIntent pi = PendingIntent.getBroadcast(this, 1, intentAlarm,
				PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
				System.currentTimeMillis(), TRACKER_INTERVAL, pi);
		Log.d("Tracker", "Tracker working.");
	}

	public void cancelTracker() {
		Log.d("Tracker", "Cancelling tracker");

		Intent intentAlarm = new Intent(this, Tracker.class);
		PendingIntent sender = PendingIntent.getBroadcast(this, 1, intentAlarm,
				PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

		alarmManager.cancel(sender);
	}

	public void stopTripDialog() {
		Builder dialog;
		dialog = new AlertDialog.Builder(this);
		dialog.setMessage(this.getResources().getString(
				R.string.stop_tracker_message));
		dialog.setPositiveButton(this.getResources().getString(R.string.yes),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface paramDialogInterface,
							int paramInt) {
						stopTrip();
					}
				});
		dialog.setNegativeButton(this.getString(R.string.no),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface paramDialogInterface,
							int paramInt) {
						// Nothing happens.
					}
				});
		dialog.show();

	}

	public void stopTrip() {
		isTripStarted = false;
		mLocationClient.disconnect();
		tripID = "";
		startTripTime = null;
		cancelTracker();
		messageHandler.sendEmptyMessage(EVENT_TYPE.UPDATE_HUB_PAGE.ordinal());
		surveyHelper.resetTracker();
		ridesCompleted++;
		totalDistanceBefore += tripDistance;
		tripDistance = 0;
	}
}
