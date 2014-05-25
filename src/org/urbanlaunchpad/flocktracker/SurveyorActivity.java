package org.urbanlaunchpad.flocktracker;

import android.annotation.SuppressLint;
import android.app.*;
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
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import org.json.JSONObject;
import org.urbanlaunchpad.flocktracker.adapters.DrawerListViewAdapter;
import org.urbanlaunchpad.flocktracker.controllers.QuestionController;
import org.urbanlaunchpad.flocktracker.fragments.HubPageFragment;
import org.urbanlaunchpad.flocktracker.fragments.HubPageManager;
import org.urbanlaunchpad.flocktracker.fragments.QuestionFragment;
import org.urbanlaunchpad.flocktracker.fragments.StatusPageFragment;
import org.urbanlaunchpad.flocktracker.fragments.StatusPageFragment.StatusPageUpdate;
import org.urbanlaunchpad.flocktracker.helpers.*;
import org.urbanlaunchpad.flocktracker.helpers.SurveyHelper.Tuple;
import org.urbanlaunchpad.flocktracker.menu.RowItem;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

public class SurveyorActivity extends Activity implements
		QuestionFragment.AnswerSelected, StatusPageUpdate,
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener {

	public static final Integer INCOMPLETE_CHAPTER = R.drawable.complete_red;
	public static final Integer COMPLETE_CHAPTER = R.drawable.complete_green;
	public static final Integer HALF_COMPLETE_CHAPTER = R.drawable.complete_orange;
	public static final String TRACKER_TYPE = "Tracker";
	public static final String SURVEY_TYPE = "Survey";
	public static final String LOOP_TYPE = "Loop";
	public static final int UPDATE_INTERVAL_IN_SECONDS = 5;
	private static final int MILLISECONDS_PER_SECOND = 1000;
	private static final long UPDATE_INTERVAL = MILLISECONDS_PER_SECOND
			* UPDATE_INTERVAL_IN_SECONDS;
	private static final int FASTEST_INTERVAL_IN_SECONDS = 1;
	private static final long FASTEST_INTERVAL = MILLISECONDS_PER_SECOND
			* FASTEST_INTERVAL_IN_SECONDS;
	public static GoogleDriveHelper driveHelper;
	public static Boolean askingTripQuestions = false;
	public static boolean submittingSubmission = false;
	public static boolean savingSurveySubmission = false;
	public static boolean savingTrackerSubmission = false;

	// Stored queues of surveys to submit
	public static HashSet<String> surveySubmissionQueue;
	public static HashSet<String> trackerSubmissionQueue;
	private final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	public LocationClient mLocationClient;

	// Define an object that holds accuracy and frequency parameters
	LocationRequest mLocationRequest;

	// Drawer fields
	private DrawerLayout chapterDrawerLayout;
	private ListView fixedNavigationList;
	private ListView chapterDrawerList;
	private LinearLayout drawer;
	private ActionBarDrawerToggle chapterDrawerToggle;
	private CharSequence chapterDrawerTitle;
	private CharSequence title;
	private List<RowItem> rowItems;
	private QuestionFragment currentQuestionFragment;
	private SurveyHelper surveyHelper;
	private StatusPageHelper statusPageHelper;

	// Metadata
	private String username;
	private String surveyID;
	private String tripID;
	private Integer maleCount = 0;
	private Integer femaleCount = 0;
	private boolean isTripStarted = false;
	private boolean showingStatusPage = false;
	private boolean showingHubPage = false;

  private QuestionController questionController;

	@SuppressLint("HandlerLeak")
	private Handler messageHandler = new Handler() {

		@SuppressWarnings("deprecation")
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
				askingTripQuestions = false;

				if (isTripStarted) {
					ImageView gear = (ImageView) findViewById(R.id.start_trip_button);
					gear.setImageResource(R.drawable.ft_grn_st1);
				} else {
					ImageView gear = (ImageView) findViewById(R.id.start_trip_button);
					gear.setImageResource(R.drawable.ft_red_st);
				}
			} else if (msg.what == EVENT_TYPE.UPDATE_STATS_PAGE.ordinal()) {
				statusPageHelper.updateStatusPage();
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_surveyor);
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);


		username = ProjectConfig.get().getUsername();
		surveyHelper = new SurveyHelper(this);
    SubmissionHelper submissionHelper = new SubmissionHelper();
    questionController = new QuestionController(this, getFragmentManager(), submissionHelper);

		driveHelper = new GoogleDriveHelper(this);
		statusPageHelper = new StatusPageHelper(this);

		// Navigation drawer information.
		title = chapterDrawerTitle = getTitle();
		chapterDrawerLayout = (DrawerLayout) findViewById(R.id.chapter_drawer_layout);
		chapterDrawerList = (ListView) findViewById(R.id.chapter_drawer);
		fixedNavigationList = (ListView) findViewById(R.id.fixed_navigation);
		drawer = (LinearLayout) findViewById(R.id.drawer);
		rowItems = new ArrayList<RowItem>();
		for (String chapterTitle : surveyHelper.getChapterTitles()) {
			RowItem rowItem = new RowItem(INCOMPLETE_CHAPTER, chapterTitle);
			rowItems.add(rowItem);
		}

		// set a custom shadow that overlays the main content when the drawer
		// opens
		chapterDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
				GravityCompat.START);
		// set up the drawer's list view with items and click listener
		fixedNavigationList.setAdapter((new ArrayAdapter<String>(this,
				R.layout.old_chapter_list_item, new String[] {
						getString(R.string.hub_page_title),
						getString(R.string.statistics_page_title) })));
		fixedNavigationList
				.setOnItemClickListener(new FixedNavigationItemClickListener());
		chapterDrawerList.setAdapter(new DrawerListViewAdapter(this,
				R.layout.chapter_drawer_list_item, rowItems));
		chapterDrawerList.setOnItemClickListener(new DrawerItemClickListener());
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);
		chapterDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
		chapterDrawerLayout, /* DrawerLayout object */
		R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
		R.string.chapter_drawer_open, /* For accessibility */
		R.string.chapter_drawer_close /* For accessibility */) {
			public void onDrawerClosed(View view) {
				getActionBar().setTitle(title);
			}

			public void onDrawerOpened(View drawerView) {
				getActionBar().setTitle(chapterDrawerTitle);
			}
		};

		chapterDrawerLayout.setDrawerListener(chapterDrawerToggle);

		maleCount = 0;
		femaleCount = 0;

		if (savedInstanceState == null) {
			showHubPage();
		}

		mLocationClient = new LocationClient(this, this, this);

		mLocationRequest = LocationRequest.create();
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		mLocationRequest.setInterval(UPDATE_INTERVAL);
		mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

		// Creating a random survey ID

		surveyID = "S" + createID();

		// location tracking
//		TrackerAlarm.surveyorActivity = this;

		trackerSubmissionQueue = new HashSet<String>(
				IniconfigActivity.prefs.getStringSet("trackerSubmissionQueue",
						new HashSet<String>()));
		surveySubmissionQueue = new HashSet<String>(
				IniconfigActivity.prefs.getStringSet("surveySubmissionQueue",
						new HashSet<String>()));
		if (!surveySubmissionQueue.isEmpty()
				|| !trackerSubmissionQueue.isEmpty()) {
			spawnSubmission();
		}

		// Check for location services.
		LocationHelper.checkLocationConfig(this);

	}

	/*
	 * Activity Lifecycle Handlers
	 */

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
//					success = surveyHelper.submitSubmission(
//							submission.getString("jsurv"),
//							submission.getString("lat"),
//							submission.getString("lng"),
//							submission.getString("alt"),
//							submission.getString("imagePaths"), surveyIDString,
//							tripIDString, submission.getString("timestamp"),
//							submission.getString("type"),
//							submission.getString("maleCount"),
//							submission.getString("femaleCount"),
//							submission.getString("totalCount"),
//							submission.getString("speed"));
				} catch (Exception e) {
					e.printStackTrace();
				}

				// Finished submitting. Remove from queue and
				// commit
				if (success) {
					Log.d("Spawn submission queue", "Submission success.");
					i.remove();
					IniconfigActivity.prefs
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
		statusPageHelper.onPause();
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
		chapterDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggles
		chapterDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public void onBackPressed() {
		if (askingTripQuestions) {
			if (surveyHelper.prevTrackingPositions.empty()
					|| surveyHelper.getTrackerQuestionPosition() == 0) {
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

	public void startTrip() {
		isTripStarted = true;
		mLocationClient.connect();
		tripID = "T" + createID();
	}

	/*
	 * Starting and stopping trip logic
	 */

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

	public void saveSurvey() {
		savingSurveySubmission = true;
		// connect if not tracking
		if (!mLocationClient.isConnected()) {
			mLocationClient.connect();
		}
		if (mLocationClient.isConnected()) {
//			new Thread(new Runnable() {
//				public void run() {
//					String jsurvString = surveyHelper.jsurv.toString();
//					JSONObject imagePaths = new JSONObject();
//					for (ArrayList<Integer> key : SurveyHelper.prevImages
//							.keySet()) {
//						try {
//							imagePaths.put(key.toString(),
//									SurveyHelper.prevImages.get(key).getPath());
//						} catch (JSONException e) {
//							e.printStackTrace();
//						}
//					}
//					resetSurvey();
//					surveyHelper.jumpString = null;
//
//					// save location tagged survey
//					surveyHelper.saveSubmission(
//							mLocationClient.getLastLocation(), surveyID,
//							tripID, jsurvString, imagePaths, SURVEY_TYPE,
//							maleCount.toString(), femaleCount.toString(),
//							((Integer) (maleCount + femaleCount)).toString(),
//							statusPageHelper.getSpeed().toString());
//					// disconnect if not tracking or not currently
//					// submitting
//					// surveys
//					if (!isTripStarted && !submittingSubmission) {
//						mLocationClient.disconnect();
//					}
//
//					statusPageHelper.surveysCompleted++;
//				}
//			}).start();

			messageHandler.sendEmptyMessage(EVENT_TYPE.SUBMITTED_SURVEY
					.ordinal());
		}
	}

	/*
	 * Submitting survey and location logic
	 */

	public void saveLocation() {
		// connect if not tracking
		Log.d("Save Location", "" + mLocationClient.isConnected());
		if (!mLocationClient.isConnected()) {
			mLocationClient.connect();
		}
		if (mLocationClient.isConnected()) {
//			new Thread(new Runnable() {
//				public void run() {
//					try {
//						surveyHelper.jsurv.put("TrackerAlarm",
//								surveyHelper.jtracker);
//					} catch (JSONException e1) {
//						e1.printStackTrace();
//					}
//
//					String jsurvString = surveyHelper.jsurv.toString();
//					JSONObject imagePaths = new JSONObject();
//					for (ArrayList<Integer> key : SurveyHelper.prevTrackerImages
//							.keySet()) {
//						try {
//							imagePaths.put(key.toString(),
//									SurveyHelper.prevTrackerImages.get(key)
//											.getPath());
//						} catch (JSONException e) {
//							e.printStackTrace();
//						}
//					}
//
//					// save location tagged survey
//					surveyHelper.saveSubmission(statusPageHelper.startLocation,
//							surveyID, tripID, jsurvString, imagePaths,
//							TRACKER_TYPE, maleCount,
//							femaleCount,
//							maleCount + femaleCount,
//							statusPageHelper.getSpeed());
//				}
//			}).start();
		}
	}

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
					ArrayList<Integer> key = new ArrayList<Integer>(
							Arrays.asList(
									surveyHelper.getTrackerQuestionPosition(), -1,
									-1));
//					SurveyHelper.prevTrackerImages
//							.put(key, driveHelper.fileUri);
				} else {
					ArrayList<Integer> key = new ArrayList<Integer>(
							Arrays.asList(surveyHelper.getChapterPosition(),
									surveyHelper.getQuestionPosition(), -1, -1));
//					SurveyHelper.prevImages.put(key, driveHelper.fileUri);
				}
//				currentQuestionFragment.ImageLayout();
			} else {
				startActivityForResult(
						IniconfigActivity.credential.newChooseAccountIntent(),
						GoogleDriveHelper.REQUEST_ACCOUNT_PICKER);
			}
			break;
		case GoogleDriveHelper.CAPTURE_IMAGE:
			try {
				Bitmap imageBitmap = BitmapFactory.decodeFile(
						driveHelper.fileUri.getPath(), null);
				float rotation = ImageHelper.rotationForImage(Uri
						.fromFile(new File(driveHelper.fileUri.getPath())));
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
					ArrayList<Integer> key = new ArrayList<Integer>(
							Arrays.asList(
									surveyHelper.getTrackerQuestionPosition(), -1,
									-1));
//					SurveyHelper.prevTrackerImages
//							.put(key, driveHelper.fileUri);
				} else {
					ArrayList<Integer> key = new ArrayList<Integer>(
							Arrays.asList(surveyHelper.getChapterPosition(),
									surveyHelper.getQuestionPosition(), -1, -1));
//					SurveyHelper.prevImages.put(key, driveHelper.fileUri);
				}
//				currentQuestionFragment.ImageLayout();
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
			Log.e("SurveyorActivity activity", "unknown request code");
			break;
		}
	}

	/*
	 * Drawer Logic
	 */

	public boolean onOptionsItemSelected(MenuItem item) {
		// To make the action bar home/up action should open or close the
		// drawer.
		chapterDrawerToggle.onOptionsItemSelected(item);
		return true;
	}

	private void showHubPage() {
//		showingHubPage = true;
//		showingStatusPage = false;
//
//		// Update fragments
//		FragmentManager fragmentManager = getFragmentManager();
//		Fragment fragment = new HubPageFragment();
//
//		FragmentTransaction transaction = fragmentManager.beginTransaction();
//		transaction.replace(R.id.surveyor_frame, fragment);
//		transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
//		transaction.addToBackStack(null);
//		transaction.commit();
//
//		// update selected item and title, then close the drawer.
//		fixedNavigationList.setItemChecked(0, true);
//		chapterDrawerList.setItemChecked(-1, true);
//		setTitle(getString(R.string.hub_page_title));
//		chapterDrawerLayout.closeDrawer(drawer);
//
//		// update ui
//		messageHandler.sendEmptyMessage(EVENT_TYPE.UPDATE_HUB_PAGE.ordinal());
	}

	private void showStatusPage() {
		showingHubPage = false;
		showingStatusPage = true;

		FragmentManager fragmentManager = getFragmentManager();
		Fragment fragment = new StatusPageFragment();

		FragmentTransaction transaction = fragmentManager.beginTransaction();
		transaction.replace(R.id.surveyor_frame, fragment);
		transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		transaction.addToBackStack(null);
		transaction.commit();

		// update selected item and title, then close the drawer.
		fixedNavigationList.setItemChecked(1, true);
		chapterDrawerList.setItemChecked(-1, true);
		setTitle(getString(R.string.statistics_page_title));
		chapterDrawerLayout.closeDrawer(drawer);
	}

	private void showCurrentQuestion() {
		showingHubPage = false;
		showingStatusPage = false;
//
//		int chapterPosition;
//		int questionPosition;
//		int loopPosition;
//		Boolean inloop;
//		inloop = surveyHelper.inLoop;
//
//		JSONObject currentQuestionJsonObject = null;
//		String currentQuestion = null;
//
//		if (surveyHelper.inLoop) {
//			loopPosition = surveyHelper.getLoopPosition();
//		}
//
//		if (askingTripQuestions) {
//			chapterPosition = 0;
//			questionPosition = surveyHelper.getChapterPosition();
//
//			// get current trip question
//			try {
//				currentQuestionJsonObject = surveyHelper
//						.getCurrentTripQuestion();
//			} catch (JSONException e) {
//				e.printStackTrace();
//			}
//
//			// Hide submit
//			navButtons.getView().findViewById(R.id.submit_survey_button)
//					.setVisibility(View.INVISIBLE);
//		} else {
//			chapterPosition = surveyHelper.getChapterPosition();
//			questionPosition = surveyHelper.getQuestionPosition();
//
//			// update selected item and title, then close the drawer.
//			fixedNavigationList.setItemChecked(-1, true);
//			chapterDrawerList.setItemChecked(chapterPosition, true);
//			setTitle(surveyHelper.getChapterTitles()[chapterPosition]);
//			chapterDrawerLayout.closeDrawer(drawer);
//
//			// Get current question
//			try {
//				currentQuestionJsonObject = surveyHelper.getCurrentQuestion();
//			} catch (JSONException e) {
//				e.printStackTrace();
//			}
//		}
//
//		// If in loop, putting the answer of the current iteration on its place.
//		if (surveyHelper.inLoop) {
//			try {
//				currentQuestionJsonObject.put("Answer",
//						currentQuestionJsonObject.getJSONArray("LoopAnswers")
//								.get(surveyHelper.loopIteration).toString());
//			} catch (JSONException e) {
//				// e.printStackTrace();
//			}
//		}
//
//		currentQuestion = currentQuestionJsonObject.toString();
//
//		// Starting question fragment and passing json question information.
//		currentQuestionFragment = new QuestionFragment(surveyHelper.getCurrentQuestion());
//		Bundle args = new Bundle();
//		args.putString(QuestionFragment.ARG_JSON_QUESTION,
//				question.getQuestionText());
//		args.putInt(QuestionFragment.ARG_CHAPTER_POSITION, question.getChapter().getChapterNumber());
//		args.putInt(QuestionFragment.ARG_QUESTION_POSITION, question.getQuestionNumber());

		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction transaction = fragmentManager.beginTransaction();

		// show navigation buttons and add new question
//		if (!navButtons.isVisible()) {
//			messageHandler.sendEmptyMessage(EVENT_TYPE.SHOW_NAV_BUTTONS
//					.ordinal());
//		}
//
//		// selectively show previous question button
//		if ((askingTripQuestions && surveyHelper.getTrackerQuestionPosition() == 0)
//				|| (!askingTripQuestions && questionPosition == 0)) {
//			navButtons.getView().findViewById(R.id.previous_question_button)
//					.setVisibility(View.INVISIBLE);
//		} else {
//			navButtons.getView().findViewById(R.id.previous_question_button)
//					.setVisibility(View.VISIBLE);
//		}

		transaction.replace(R.id.surveyor_frame, currentQuestionFragment);
		transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		if (!askingTripQuestions) {
			transaction.addToBackStack(null);
		}
		transaction.commit();
	}

	/*
	 * Displaying different pages
	 */

	@Override
	public void setTitle(CharSequence title) {
		this.title = title;
		getActionBar().setTitle(this.title);
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

		mLocationClient.requestLocationUpdates(mLocationRequest,
				new LocationListener() {
					@Override
					public void onLocationChanged(final Location location) {
						statusPageHelper.onLocationChanged(isTripStarted,
								location);
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
			statusPageHelper.startLocation = mLocationClient.getLastLocation();
			Toast.makeText(this, R.string.tracking_on, Toast.LENGTH_SHORT)
					.show();
		}
	}

	@Override
	public void onDisconnected() {
		// Display the connection status
		Toast.makeText(this, R.string.disconnected, Toast.LENGTH_SHORT).show();
	}
//
//	/*
//	 * Callback Handlers for Connecting to Google Play (Authentication)
//	 */
//
//	public void NavButtonPressed(NavButtonType type) {
//		switch (type) {
//
//		case PREVIOUS:
//			currentQuestionFragment.saveState();
//			surveyHelper.onPrevQuestionPressed(askingTripQuestions);
//			onBackPressed();
//			break;
//		case NEXT:
//			currentQuestionFragment.saveState();
//			NextQuestionResult result = surveyHelper
//					.onNextQuestionPressed(askingTripQuestions);
//
//			if (askingTripQuestions) {
//				if (result == NextQuestionResult.END) {
//					askingTripQuestions = false;
//					surveyHelper.updateSurveyPosition(
//							SurveyHelper.HUB_PAGE_CHAPTER_POSITION,
//							SurveyHelper.HUB_PAGE_QUESTION_POSITION);
//					surveyHelper.prevTrackingPositions = new Stack<Integer>();
//					showHubPage();
//					startTrip();
//					statusPageHelper.startTrip();
//					startTracker();
//					break;
//				}
//			} else {
//				if (result == NextQuestionResult.CHAPTER_END) {
//					rowItems.get(surveyHelper.getChapterPosition() - 1)
//							.setImageId(COMPLETE_CHAPTER);
//				} else if (result == NextQuestionResult.END) {
//					Toast.makeText(this, R.string.end_of_survey,
//							Toast.LENGTH_SHORT).show();
//					submitSurveyInterface();
//					break;
//				}
//
//				RowItem rowItem = rowItems.get(surveyHelper
//						.getChapterPosition());
//				if (rowItem.getImageId() != COMPLETE_CHAPTER) {
//					rowItem.setImageId(HALF_COMPLETE_CHAPTER);
//				}
//			}
//			showCurrentQuestion();
//			break;
//		case SUBMIT:
//			currentQuestionFragment.saveState();
//			submitSurveyInterface();
//			break;
//		}
//	}

	public void AnswerRecieve(String answerStringReceive,
			String jumpStringReceive, ArrayList<Integer> selectedAnswers,
			Boolean inLoopReceive, String questionkindReceive,
			ArrayList<Integer> questionkey) {
		// TODO: fix loop stuff

//		if (questionkindReceive.equals("LP") && (answerStringReceive != null)) {
//			Integer receivedLoopTotal = null;
//			Integer currentLoopTotal = surveyHelper.getCurrentLoopTotal();
//			if (!answerStringReceive.equals("")) {
//				receivedLoopTotal = Integer.parseInt(answerStringReceive);
//				if ((currentLoopTotal != receivedLoopTotal)
//						|| (receivedLoopTotal == 0)) {
//					surveyHelper.clearLoopAnswerHashMap(questionkey.get(0),questionkey.get(1), askingTripQuestions);
//					surveyHelper.loopTotal = receivedLoopTotal;
//					surveyHelper.updateLoopLimit();
//					surveyHelper.initializeLoop();
//					if (!askingTripQuestions) {
//						surveyHelper.answerCurrentQuestion(answerStringReceive,
//								selectedAnswers);
//					} else {
//						surveyHelper.answerCurrentTrackerQuestion(
//								answerStringReceive, selectedAnswers);
//					}
//				}
//			}
//
//		} else if ((answerStringReceive != null)) {
//			if (!askingTripQuestions) {
//				surveyHelper.answerCurrentQuestion(answerStringReceive,
//						selectedAnswers);
//			} else {
//				surveyHelper.answerCurrentTrackerQuestion(answerStringReceive,
//						selectedAnswers);
//			}
//		}
//
//		if (jumpStringReceive != null) {
//			surveyHelper.updateJumpString(jumpStringReceive);
//		}
	}

	@Override
	public void updateStatusPage() {
		// hide navigation buttons
		FragmentManager fragmentManager = getFragmentManager();

//		FragmentTransaction transactionHide = fragmentManager
//				.beginTransaction();
//		transactionHide.hide(navButtons);
//		transactionHide.commit();

		messageHandler.sendEmptyMessage(EVENT_TYPE.UPDATE_STATS_PAGE.ordinal());
	}

	/*
	 * Question Navigation Event Handlers
	 */

	@Override
	public void leftStatusPage() {
		showingStatusPage = false;
	}

	/*
	 * Question Event Handlers
	 */

//	@Override
//	public void HubButtonPressed(HubButtonType type) {
//		switch (type) {
//		case UPDATE_PAGE:
//			showingHubPage = true;
//			messageHandler.sendEmptyMessage(EVENT_TYPE.UPDATE_HUB_PAGE
//					.ordinal());
//			break;
//		case TOGGLETRIP:
//			if (isTripStarted) {
//				// Update status page info
//				stopTripDialog();
//			} else {
//				surveyHelper.resetTracker();
//				askingTripQuestions = true;
//
//				// Starting question fragment and passing json question
//				// information.
//				surveyHelper.updateTrackerPosition(0);
//				showCurrentQuestion();
//			}
//			break;
//		case NEWSURVEY:
//			surveyHelper.updateSurveyPosition(0, 0);
//			surveyHelper.inLoop = false;
//			surveyHelper.loopIteration = -1;
//			surveyHelper.loopPosition = -1;
//			showCurrentQuestion();
//			break;
//		case STATISTICS:
//			if (askingTripQuestions) {
//				surveyHelper
//						.updateTrackerPosition(SurveyHelper.STATS_PAGE_QUESTION_POSITION);
//			} else {
//				surveyHelper.updateSurveyPosition(
//						SurveyHelper.STATS_PAGE_CHAPTER_POSITION,
//						SurveyHelper.STATS_PAGE_QUESTION_POSITION);
//			}
//			showStatusPage();
//			break;
//		case FEWERMEN:
//			if (maleCount > 0) {
//				maleCount--;
//				updateCount("male");
//			}
//			break;
//		case FEWERWOMEN:
//			if (femaleCount > 0) {
//				femaleCount--;
//				updateCount("female");
//			}
//			break;
//		case MOREMEN:
//			maleCount++;
//			updateCount("male");
//			break;
//		case MOREWOMEN:
//			femaleCount++;
//			updateCount("female");
//			break;
//		default:
//			break;
//		}
//	}

	/*
	 * Status Page Event Handlers
	 */

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
	 * Hub Page Event Handlers
	 */

	public void startTracker() {
		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		Intent intentAlarm = new Intent(this, TrackerAlarm.class);
		PendingIntent pi = PendingIntent.getBroadcast(this, 1, intentAlarm,
				PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
				System.currentTimeMillis(), TrackerAlarm.TRACKER_INTERVAL, pi);
		Log.d("TrackerAlarm", "TrackerAlarm working.");
	}

	public void cancelTracker() {
		Log.d("TrackerAlarm", "Cancelling tracker");

		Intent intentAlarm = new Intent(this, TrackerAlarm.class);
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

	/*
	 * Location tracking helper
	 */

	public void stopTrip() {
		isTripStarted = false;
		mLocationClient.disconnect();
		tripID = "";
		cancelTracker();
		messageHandler.sendEmptyMessage(EVENT_TYPE.UPDATE_HUB_PAGE.ordinal());
		surveyHelper.resetTracker();
		statusPageHelper.stopTrip();
	}

	private enum EVENT_TYPE {
		MALE_UPDATE, FEMALE_UPDATE, UPDATE_STATS_PAGE, UPDATE_HUB_PAGE, SHOW_NAV_BUTTONS, SUBMITTED_SURVEY, SUBMIT_FAILED
	}

	private class FixedNavigationItemClickListener implements
			ListView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Log.d("Clicked on fixed position", position + "");
			if (askingTripQuestions) {
				resetTrackerSurvey();
			}

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
			if (askingTripQuestions) {
				resetTrackerSurvey();
			}
			surveyHelper.updateSurveyPosition(position, 0);
			surveyHelper.jumpString = null;
			showingHubPage = false;
			showingStatusPage = false;
			surveyHelper.inLoop = false;
			surveyHelper.loopTotal = 0;
			showCurrentQuestion();
		}
	}
}
