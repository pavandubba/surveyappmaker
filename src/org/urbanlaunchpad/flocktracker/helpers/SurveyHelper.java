package org.urbanlaunchpad.flocktracker.helpers;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.inputmethodservice.Keyboard.Key;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.drive.internal.e;
import com.google.api.services.fusiontables.Fusiontables;
import com.google.api.services.fusiontables.Fusiontables.Column.Insert;
import com.google.api.services.fusiontables.Fusiontables.Query.Sql;
import com.google.api.services.fusiontables.model.Column;
import com.google.api.services.fusiontables.model.ColumnList;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.urbanlaunchpad.flocktracker.IniconfigActivity;
import org.urbanlaunchpad.flocktracker.ProjectConfig;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.SurveyorActivity;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class SurveyHelper {

	// Constants
	public static final Integer HUB_PAGE_CHAPTER_POSITION = -15;
	private Integer chapterPosition = HUB_PAGE_CHAPTER_POSITION;
	public static final Integer HUB_PAGE_QUESTION_POSITION = -15;
	private Integer questionPosition = HUB_PAGE_QUESTION_POSITION;
	public static final Integer STATS_PAGE_CHAPTER_POSITION = -16;
	public static final Integer STATS_PAGE_QUESTION_POSITION = -16;
	public static final Integer MAX_QUERY_LENGTH = 2000; // max length allowed
															// by fusion table
	// Hashmaps that store previously entered answers
	public static HashMap<ArrayList<Integer>, ArrayList<Integer>> selectedAnswersMap = new HashMap<ArrayList<Integer>, ArrayList<Integer>>(); // chapter
																																				// position,
																																				// question
																																				// position,
																																				// loop
																																				// iteration,
																																				// loop
																																				// position
	public static HashMap<ArrayList<Integer>, ArrayList<Integer>> selectedTrackingAnswersMap = new HashMap<ArrayList<Integer>, ArrayList<Integer>>(); // question
																																						// position,
																																						// loop
																																						// iteration,
																																						// loop
																																						// position
	public static HashMap<ArrayList<Integer>, Uri> prevImages = new HashMap<ArrayList<Integer>, Uri>();
	public static HashMap<ArrayList<Integer>, Uri> prevTrackerImages = new HashMap<ArrayList<Integer>, Uri>();
	public static String TRIP_TABLE_ID = "1Q2mr8ni5LTxtZRRi3PNSYxAYS8HWikWqlfoIUK4";
	public static String SURVEY_TABLE_ID = "11lGsm8B2SNNGmEsTmuGVrAy1gcJF9TQBo3G1Vw0";
	public JSONObject jsurv = null;
	public JSONObject jtracker = null;
	// Backstack
	public Stack<Tuple> prevPositions = new Stack<Tuple>();
	public Stack<Integer> prevTrackingPositions = new Stack<Integer>();
	public Integer prevQuestionPosition = null;
	public String jumpString = null;
	// Survey / Tracker State
	private String username;
	private Integer tripQuestionPosition = 0;
	private Integer[] jumpPosition = null;
	private Context context;
	private String jsonSurvey = null;
	private String jTrackerString = null;
	private JSONArray jChapterList;
	private JSONArray jTrackerQuestions;
	private String[] chapterTitles;
	private Integer[] chapterQuestionCounts;
	public static String questionKind;
	// Loop stuff
	public Integer loopTotal = null; // Number of times loop questions repeat.
	public Boolean inLoop = false; // Toggle that turns on if the survey gets
									// into a loop.
	public Integer loopPosition = -1; // Position in the questions array in the
										// loop the survey is in.
	public Integer loopIteration = -1; // Iteration step where the loop process
										// is.
	public Integer loopLimit = 0; // Total number of questions in the loop being
									// asked.

	public SurveyHelper(String username, String jsonSurvey, Context context) {
		this.username = username;
		this.jsonSurvey = jsonSurvey;
		this.context = context;

		// parse json survey
		try {
			this.jsurv = new JSONObject(jsonSurvey);
			this.jTrackerString = this.jsurv.getJSONObject(
					SurveyorActivity.TRACKER_TYPE).toString();
			this.jtracker = new JSONObject(this.jTrackerString);
		} catch (JSONException e) {
			Toast.makeText(context, R.string.json_format_error,
					Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}

		// get information out of survey
		getTableID();

		// Parse survey and tracking information.
		parseChapters();
		parseTrackingQuestions();
		parseChapterQuestionCount();

		// Checking existence of columns in the Fusion Tables.
		new Thread(new Runnable() {
			public void run() {
				columnCheck(SURVEY_TABLE_ID, SurveyType.SURVEY, jChapterList);
				columnCheck(TRIP_TABLE_ID, SurveyType.TRACKER,
						jTrackerQuestions);
			}
		}).start();

	}

	/*
	 * Initialization code
	 */

	public static void checkLocationConfig(final Context context) {
		LocationManager lm = null;
		Builder dialog;
		boolean gps_enabled = false;
		boolean network_enabled = false;
		if (lm == null) {
			lm = (LocationManager) context
					.getSystemService(Context.LOCATION_SERVICE);
		}

		try {
			gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
		} catch (Exception ex) {
		}

		try {
			network_enabled = lm
					.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		} catch (Exception ex) {
		}

		if (!gps_enabled && !network_enabled) {
			dialog = new AlertDialog.Builder(context);
			dialog.setMessage(context.getResources().getString(
					R.string.gps_network_not_enabled));
			dialog.setPositiveButton(
					context.getResources().getString(
							R.string.open_location_settings),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(
								DialogInterface paramDialogInterface,
								int paramInt) {
							Intent myIntent = new Intent(
									Settings.ACTION_LOCATION_SOURCE_SETTINGS);
							myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							context.startActivity(myIntent);
							// get gps
						}
					});

			dialog.setNegativeButton(
					context.getString(R.string.cancel_location_settings),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(
								DialogInterface paramDialogInterface,
								int paramInt) {

						}
					});
			dialog.show();
		}
	}

	// Get trip and survey table id's
	public void getTableID() {
		try {
			TRIP_TABLE_ID = jtracker.getString("TableID");
			SURVEY_TABLE_ID = jsurv.getJSONObject(SurveyorActivity.SURVEY_TYPE)
					.getString("TableID");
		} catch (JSONException e) {
			Toast.makeText(context, R.string.fusion_tables_id_problem,
					Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
	}

	// Get chapter titles
	public void parseChapters() {
		try {
			jChapterList = jsurv.getJSONObject(SurveyorActivity.SURVEY_TYPE)
					.getJSONArray("Chapters");
			chapterTitles = new String[jChapterList.length()];
			for (int i = 0; i < jChapterList.length(); ++i) {
				chapterTitles[i] = jChapterList.getJSONObject(i).getString(
						"Chapter");
			}
		} catch (JSONException e) {
			e.printStackTrace();
			Toast.makeText(context, R.string.chapters_not_parsed,
					Toast.LENGTH_SHORT).show();
		}
	}

	// Get tracking questions related to each trip
	public void parseTrackingQuestions() {
		try {
			jTrackerQuestions = jtracker.getJSONArray("Questions");
		} catch (JSONException e2) {
			Toast.makeText(context, R.string.no_tracker_questions,
					Toast.LENGTH_SHORT).show();
			e2.printStackTrace();
		}
	}

	/*
	 * Uploading Logic
	 */

	// Get counts of questions in each chapter
	public void parseChapterQuestionCount() {
		chapterQuestionCounts = new Integer[jChapterList.length()];
		for (int i = 0; i < jChapterList.length(); ++i) {
			try {
				chapterQuestionCounts[i] = jChapterList.getJSONObject(i)
						.getJSONArray("Questions").length();
			} catch (JSONException e) {
				chapterQuestionCounts[i] = 0;
			}
		}
	}

	public boolean submitSubmission(String jsurvString, String lat, String lng,
			String alt, String imagePaths, String surveyID, String tripID,
			String timestamp, String type, String maleCount,
			String femaleCount, String totalCount, String speed) {
		boolean success = false;

		try {
			JSONObject jsurvQueueObject = new JSONObject(jsurvString);
			JSONObject imageMap = new JSONObject(imagePaths);

			// Upload images and put in answers
			for (@SuppressWarnings("unchecked")
			Iterator<String> i = imageMap.keys(); i.hasNext();) {
				String keyString = i.next();
				String fileLink = SurveyorActivity.driveHelper
						.saveFileToDrive(imageMap.getString(keyString));

				if (fileLink != null) {
					if (type.equals(SurveyorActivity.TRACKER_TYPE)) {
						Integer key = Integer.parseInt(keyString);
						jsurvQueueObject.getJSONObject(type)
								.getJSONArray("Questions").getJSONObject(key)
								.put("Answer", fileLink);
					} else if (type.equals(SurveyorActivity.SURVEY_TYPE)) {
						Quadruple key = new Quadruple(keyString);
						jsurvQueueObject.getJSONObject(type)
								.getJSONArray("Chapters")
								.getJSONObject(key.chapterPosition)
								.getJSONArray("Questions")
								.getJSONObject(key.questionPosition)
								.put("Answer", fileLink);
					}
				}
			}

			// Create and submit query
			String columnnamesString = getNames("id", "nq", type,
					jsurvQueueObject);
			String answerfinalString = getNames("Answer", "wq", type,
					jsurvQueueObject);
			String lnglat = LocationHelper.getLngLatAlt(lng, lat, alt);
			String query = "";

			if (type.equals(SurveyorActivity.TRACKER_TYPE)) {
				query = "INSERT INTO "
						+ TRIP_TABLE_ID
						+ " ("
						+ columnnamesString
						+ ",Location,Lat,Lng,Alt,Date,TripID,Username,TotalCount,FemaleCount,MaleCount,Speed) VALUES ("
						+ answerfinalString + ",'<Point><coordinates>" + lnglat
						+ "</coordinates></Point>','" + lat + "','" + lng
						+ "','" + alt + "','" + timestamp + "','" + tripID
						+ "','" + username + "','" + totalCount + "','"
						+ femaleCount + "','" + maleCount + "','" + speed
						+ "');";
				Log.v("TrackerAlarm submit", query);
			} else if (type.equals(SurveyorActivity.SURVEY_TYPE)) {
				query = "INSERT INTO "
						+ SURVEY_TABLE_ID
						+ " ("
						+ columnnamesString
						+ ",Location,Lat,Lng,Alt,Date,SurveyID,TripID,Username,TotalCount,FemaleCount,MaleCount,Speed"
						+ ") VALUES (" + answerfinalString
						+ ",'<Point><coordinates>" + lnglat
						+ "</coordinates></Point>','" + lat + "','" + lng
						+ "','" + alt + "','" + timestamp + "','" + surveyID
						+ "','" + tripID + "','" + username + "','"
						+ totalCount + "','" + femaleCount + "','" + maleCount
						+ "','" + speed + "');";
				Log.v("Survey submit", query);
				// Toast.makeText(context,
				// "Survey submitted succesfully!",
				// Toast.LENGTH_SHORT).show();
			}

			if (query.length() >= MAX_QUERY_LENGTH) {
				ArrayList<String> columnNames = getNamesArray("id", type,
						jsurvQueueObject);
				ArrayList<String> answers = getNamesArray("Answer", type,
						jsurvQueueObject);
				int rowID;

				// Send initial insert and get row ID
				if (type.equals(SurveyorActivity.TRACKER_TYPE)) {
					String metaDataQuery = "INSERT INTO "
							+ TRIP_TABLE_ID
							+ " (Location,Lat,Lng,Alt,Date,TripID,TotalCount,FemaleCount,MaleCount,Speed)"
							+ " VALUES (" + "'<Point><coordinates>" + lnglat
							+ "</coordinates></Point>','" + lat + "','" + lng
							+ "','" + alt + "','" + timestamp + "','" + tripID
							+ "','" + totalCount + "','" + femaleCount + "','"
							+ maleCount + "','" + speed + "');";

					rowID = Integer
							.parseInt((String) IniconfigActivity.fusiontables
									.query().sql(metaDataQuery)
									.setKey(ProjectConfig.get().getApiKey())
									.execute().getRows().get(0).get(0));
				} else {
					String metaDataQuery = "INSERT INTO "
							+ SURVEY_TABLE_ID
							+ " (Location,Lat,Lng,Alt,Date,SurveyID,TripID,TotalCount,FemaleCount,"
							+ "MaleCount,Speed)" + " VALUES ("
							+ "'<Point><coordinates>" + lnglat
							+ "</coordinates></Point>','" + lat + "','" + lng
							+ "','" + alt + "','" + timestamp + "','"
							+ surveyID + "','" + tripID + "','" + totalCount
							+ "','" + femaleCount + "','" + maleCount + "','"
							+ speed + "');";

					rowID = Integer
							.parseInt((String) IniconfigActivity.fusiontables
									.query().sql(metaDataQuery)
									.setKey(ProjectConfig.get().getApiKey())
									.execute().getRows().get(0).get(0));
				}

				// Send rest of info one at a time
				sendUpdateQuery("Username", username, type, rowID);

				for (int i = 0; i < columnNames.size(); i++) {
					sendUpdateQuery(columnNames.get(i), answers.get(i), type,
							rowID);
				}
			} else {
				Sql sql = IniconfigActivity.fusiontables.query().sql(query);
				sql.setKey(ProjectConfig.get().getApiKey());
				sql.execute();
			}

			success = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return success;
	}

	public boolean sendUpdateQuery(String key, String value, String type,
			int rowID) {
		if (value.isEmpty()) {
			return true;
		}
		try {
			String query = "";

			if (type.equals(SurveyorActivity.TRACKER_TYPE)) {
				query = "UPDATE " + TRIP_TABLE_ID + " SET " + key + " = '"
						+ value + "' WHERE ROWID = '" + rowID + "'";
				Log.v("TrackerAlarm submit", query);
			} else if (type.equals(SurveyorActivity.SURVEY_TYPE)) {
				query = "UPDATE " + SURVEY_TABLE_ID + " SET " + key + " = '"
						+ value + "' WHERE ROWID = '" + rowID + "'";
				Log.v("Survey submit", query);
			}

			Sql sql = IniconfigActivity.fusiontables.query().sql(query);
			sql.setKey(ProjectConfig.get().getApiKey());
			sql.execute();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/*
	 * Column Check Code
	 */

	public String getNames(String nametoget, String syntaxtype,
			String triporsurvey, JSONObject survey) {
		ArrayList<String> names = getNamesArray(nametoget, triporsurvey, survey);

		String namesString = "";

		if (syntaxtype.equals("wq")) {
			namesString = "'" + names.get(0) + "'";
			for (int i = 1; i < names.size(); i++) {
				namesString += ",'" + names.get(i) + "'";
			}
		} else if (syntaxtype.equals("nq")) {
			namesString = names.get(0);
			for (int i = 1; i < names.size(); i++) {
				namesString += "," + names.get(i);
			}
		}

		Log.v("Names", triporsurvey + " " + namesString);
		return namesString;
	}

	public ArrayList<String> getNamesArray(String nametoget,
			String triporsurvey, JSONObject survey) {
		ArrayList<String> names = new ArrayList<String>();
		JSONArray questionsArray = null;
		Integer totalchapters = null;

		if (triporsurvey.equals(SurveyorActivity.SURVEY_TYPE)) {
			try {
				totalchapters = survey
						.getJSONObject(SurveyorActivity.SURVEY_TYPE)
						.getJSONArray("Chapters").length();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} else if (triporsurvey.equals(SurveyorActivity.TRACKER_TYPE)) {
			totalchapters = 1;
		}

		for (int i = 0; i < totalchapters; ++i) {
			if (triporsurvey.equals(SurveyorActivity.SURVEY_TYPE)) {
				try {
					questionsArray = survey
							.getJSONObject(SurveyorActivity.SURVEY_TYPE)
							.getJSONArray("Chapters").getJSONObject(i)
							.getJSONArray("Questions");
				} catch (JSONException e) {
					e.printStackTrace();
				}
			} else if (triporsurvey.equals(SurveyorActivity.TRACKER_TYPE)) {
				try {
					questionsArray = jtracker.getJSONArray("Questions");
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			for (int j = 0; j < questionsArray.length(); ++j) {
				try {
					names.add(questionsArray.getJSONObject(j).getString(
							nametoget));
				} catch (JSONException e) {
					names.add("");
				}
			}
		}
		return names;
	}

	@SuppressWarnings("unchecked")
	public void saveSubmission(Location currentLocation, String surveyID,
			String tripID, String jsurvString, JSONObject imagePaths,
			String type, String maleCount, String femaleCount,
			String totalCount, String speed) {
		String lat = "";
		String lng = "";
		String alt = "";
		if (currentLocation != null) {
			lat = "" + currentLocation.getLatitude();
			lng = "" + currentLocation.getLongitude();
			alt = "" + currentLocation.getAltitude();
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String timestamp = sdf.format(new Date());
		JSONObject submission = new JSONObject();

		// Serialize into shared preferences
		try {
			submission.put("type", type);
			submission.put("lat", lat);
			submission.put("lng", lng);
			submission.put("alt", alt);
			submission.put("timestamp", timestamp);
			submission.put("jsurv", jsurvString);
			submission.put("surveyID", surveyID);
			submission.put("tripID", tripID);
			submission.put("imagePaths", imagePaths.toString());
			submission.put("maleCount", maleCount);
			submission.put("femaleCount", femaleCount);
			submission.put("totalCount", totalCount);
			submission.put("speed", speed);
			if (type.equals(SurveyorActivity.TRACKER_TYPE)) {
				synchronized (SurveyorActivity.trackerSubmissionQueue) {
					SurveyorActivity.trackerSubmissionQueue.add(submission
							.toString());
					IniconfigActivity.prefs
							.edit()
							.putStringSet(
									"trackerSubmissionQueue",
									(Set<String>) SurveyorActivity.trackerSubmissionQueue
											.clone()).commit();
					SurveyorActivity.savingTrackerSubmission = false;
					SurveyorActivity.trackerSubmissionQueue.notify();
				}
			} else if (type.equals(SurveyorActivity.SURVEY_TYPE)) {
				synchronized (SurveyorActivity.surveySubmissionQueue) {
					SurveyorActivity.surveySubmissionQueue.add(submission
							.toString());
					IniconfigActivity.prefs
							.edit()
							.putStringSet(
									"surveySubmissionQueue",
									(Set<String>) SurveyorActivity.surveySubmissionQueue
											.clone()).commit();
					SurveyorActivity.savingSurveySubmission = false;
					SurveyorActivity.surveySubmissionQueue.notify();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void columnCheck(final String TABLE_ID, SurveyType type,
			JSONArray sourceJsonArray) {
		String[] hardcolumnsStrings = null; // Columns that are in all projects.
		String[] hardcolumntypeStrings = null; // Types for the columns that are
		// in all projects.
		String[] questionIdlistString = null;
		String[] questionKindlistString = null;
		int numberofhardcolumns = 0;
		int numberofquestions = 0;
		switch (type) {
		case SURVEY:
			hardcolumnsStrings = new String[] { "Location", "Date", "Lat",
					"Alt", "Lng", "SurveyID", "TripID", "TotalCount",
					"FemaleCount", "MaleCount", "Speed", "Username" };
			hardcolumntypeStrings = new String[] { "LOCATION", "DATETIME",
					"NUMBER", "NUMBER", "NUMBER", "STRING", "STRING", "NUMBER",
					"NUMBER", "NUMBER", "NUMBER", "STRING" };
			numberofhardcolumns = hardcolumnsStrings.length;
			break;
		case TRACKER:
			hardcolumnsStrings = new String[] { "Location", "Date", "Lat",
					"Alt", "Lng", "TripID", "TotalCount", "FemaleCount",
					"MaleCount", "Speed", "Username" };
			hardcolumntypeStrings = new String[] { "LOCATION", "DATETIME",
					"NUMBER", "NUMBER", "NUMBER", "STRING", "NUMBER", "NUMBER",
					"NUMBER", "NUMBER", "STRING" };
			numberofhardcolumns = hardcolumnsStrings.length;
			break;
		case LOOP:
			break;
		}

		String[] columnlistNameString = null;

		// Getting the types and names of the columns in the fusion table.
		try {
			columnlistNameString = getColumnListNames(TABLE_ID);
			// Checking for the existence of the hard columns on Fusion table.

			for (int i = 0; i < numberofhardcolumns; ++i) {
				if (arrayContainsString(columnlistNameString,
						hardcolumnsStrings[i])) {
					// TODO: check column type
				} else {
					requestColumnCreate(hardcolumnsStrings[i],
							hardcolumntypeStrings[i], TABLE_ID);
				}
			}

			// Checking for the existence of question columns on Fusion table.
			switch (type) {
			case SURVEY:
				for (int j = 0; j < sourceJsonArray.length(); ++j) {
					numberofquestions = numberofquestions
							+ chapterQuestionCounts[j];
				}
				questionIdlistString = new String[numberofquestions];
				questionKindlistString = new String[numberofquestions];
				String[] auxIDArray;
				String[] auxKindArray;
				JSONArray auxArray;
				int k = 0;
				for (int j = 0; j < sourceJsonArray.length(); ++j) {
					try {
						auxArray = sourceJsonArray.getJSONObject(j)
								.getJSONArray("Questions");
						auxIDArray = getValues("id", auxArray);
						auxKindArray = getValues("Kind", auxArray);
						for (int i = 0; i < auxIDArray.length; ++i) {
							questionIdlistString[k] = auxIDArray[i];
							questionKindlistString[k] = auxKindArray[i];

							// Handling the existence of Looped questions.
							if (questionKindlistString[k].equals("LP")) {
								Log.v("columnCheck", "Loop found!");
								final JSONArray forLoop = auxArray
										.getJSONObject(i).getJSONArray(
												"Questions");
								new Thread(new Runnable() {

									public void run() {
										columnCheck(TABLE_ID, SurveyType.LOOP,
												forLoop);
									}
								}).start();

							}
							k++;
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}

				break;
			case TRACKER:
				questionIdlistString = getValues("id", sourceJsonArray);
				questionKindlistString = getValues("Kind", sourceJsonArray);
				numberofquestions = questionIdlistString.length;
				for (int k1 = 0; k1 < numberofquestions; k1++) {
					if (questionKindlistString[k1].equals("LP")) {
						Log.v("columnCheck", "Loop found!");
						try {
							final JSONArray forLoop;
							forLoop = sourceJsonArray.getJSONObject(k1)
									.getJSONArray("Questions");
							new Thread(new Runnable() {

								public void run() {
									columnCheck(TABLE_ID, SurveyType.LOOP,
											forLoop);
								}
							}).start();
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}

				}

				break;
			case LOOP:
				questionIdlistString = getValues("id", sourceJsonArray);
				questionKindlistString = getValues("Kind", sourceJsonArray);
				numberofquestions = questionIdlistString.length;
				break;
			}

			String auxkind = null;
			for (int i = 0; i < numberofquestions; ++i) {
				if ((questionKindlistString[i].equals("MC") || questionKindlistString[i]
						.equals("CB"))
						|| questionKindlistString[i].equals("OT")) {
					auxkind = "STRING";
				} else if (questionKindlistString[i].equals("ON")) {
					auxkind = "NUMBER";

					// Check for the questions inside loop questions.
				} else if (questionKindlistString[i].equals("LP")) {
					auxkind = "STRING";

				} else {
					auxkind = "STRING";
				}

				if (arrayContainsString(columnlistNameString,
						questionIdlistString[i])) {
					// TODO: check column type
				} else {
					requestColumnCreate(questionIdlistString[i], auxkind,
							TABLE_ID);
				}

			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Get a list of values corresponding to the key in either tracker
	// or survey questions
	public String[] getValues(String key, JSONArray sourceJsonArray) {
		Integer numQuestions = 0;
		String[] result = null;

		numQuestions = sourceJsonArray.length();
		result = new String[numQuestions];

		for (int i = 0; i < numQuestions; ++i) {
			try {
				result[i] = sourceJsonArray.getJSONObject(i).getString(key);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		Log.v("Number of questions", numQuestions.toString() + " " + key);
		return result;
	}

	public String[] getColumnListNames(String tableID) throws IOException {
		// Returns the column list
		Fusiontables.Column.List columnRequest = IniconfigActivity.fusiontables
				.column().list(tableID);
		columnRequest.setKey(ProjectConfig.get().getApiKey());
		columnRequest.setMaxResults((long) 500);
		ColumnList columnList = columnRequest.execute();

		String[] output = new String[columnList.getItems().size()];

		// Get column list names or types
		for (int i = 0; i < output.length; i++) {
			output[i] = columnList.getItems().get(i).getName();
		}

		Log.v("Number of columns", "" + output.length);
		return output;
	}

	public void requestColumnCreate(String name, String type, String tableID) {
		Column newColumn = new Column();
		newColumn.setName(name);
		newColumn.setType(type);

		Insert columnRequest;
		try {
			columnRequest = IniconfigActivity.fusiontables.column().insert(
					tableID, newColumn);
			columnRequest.setKey(ProjectConfig.get().getApiKey());
			columnRequest.execute();
			Log.v("requestColumnCreate", "Column created!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// helper to search for string in array
	public boolean arrayContainsString(String[] array, String string) {
		if (array.length == 0) {
			return false;
		}

		for (int i = 0; i < array.length; ++i) {
			if (array[i].equals(string)) {
				return true;
			}
		}

		return false;
	}

	/*
	 * Survey Update Functions
	 */

	public void answerCurrentQuestion(String answer,
			ArrayList<Integer> selectedAnswers) {
		if (!inLoop) {
			if (chapterPosition >= 0) {
				// Saving the answer string in JSON
				try {
					jsurv.getJSONObject(SurveyorActivity.SURVEY_TYPE)
							.getJSONArray("Chapters")
							.getJSONObject(chapterPosition)
							.getJSONArray("Questions")
							.getJSONObject(questionPosition)
							.put("Answer", answer);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				// Saving the selected questions.
				ArrayList<Integer> key = new ArrayList<Integer>(Arrays.asList(
						chapterPosition, questionPosition, -1, -1));
				selectedAnswersMap.put(key, selectedAnswers);
			}
		} else {
			try {
				jsurv.getJSONObject(SurveyorActivity.SURVEY_TYPE)
						.getJSONArray("Chapters")
						.getJSONObject(chapterPosition)
						.getJSONArray("Questions")
						.getJSONObject(questionPosition)
						.getJSONArray("Questions").getJSONObject(loopPosition)
						.getJSONArray("LoopAnswers").put(loopIteration, answer);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			ArrayList<Integer> key = new ArrayList<Integer>(Arrays.asList(
					chapterPosition, questionPosition, loopIteration,
					loopPosition));
			selectedAnswersMap.put(key, selectedAnswers);
		}
	}

	public void answerCurrentTrackerQuestion(String answer,
			ArrayList<Integer> selectedAnswers) {
		if (!inLoop) {
			try {
				jtracker.getJSONArray("Questions")
						.getJSONObject(tripQuestionPosition)
						.put("Answer", answer);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			ArrayList<Integer> key = new ArrayList<Integer>(Arrays.asList(
					questionPosition, -1, -1));
			selectedTrackingAnswersMap.put(key, selectedAnswers);
		} else {
			try {
				jtracker.getJSONArray("Questions")
						.getJSONObject(questionPosition)
						.getJSONArray("Questions").getJSONObject(loopPosition)
						.getJSONArray("LoopAnswers").put(loopIteration, answer);

				// .getJSONObject(loopIteration).put("Answer", answer);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ArrayList<Integer> key = new ArrayList<Integer>(Arrays.asList(
					questionPosition, loopIteration, loopPosition));
			selectedAnswersMap.put(key, selectedAnswers);
		}
	}

	public void resetSurvey() {
		try {
			chapterPosition = 0;
			questionPosition = 0;
			jsurv = new JSONObject(jsonSurvey);
			jsurv.put(SurveyorActivity.TRACKER_TYPE, jtracker);
			prevPositions = new Stack<Tuple>();
			selectedAnswersMap = new HashMap<ArrayList<Integer>, ArrayList<Integer>>();
			prevImages = new HashMap<ArrayList<Integer>, Uri>();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void resetTracker() {
		tripQuestionPosition = 0;
		prevTrackingPositions = new Stack<Integer>();
		selectedTrackingAnswersMap = new HashMap<ArrayList<Integer>, ArrayList<Integer>>();
		prevTrackerImages = new HashMap<ArrayList<Integer>, Uri>();
		try {
			jtracker = new JSONObject(jTrackerString);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void updateSurveyPosition(Integer chapterpositionreceive,
			Integer questionpositionreceive) {
		prevPositions.add(new Tuple(chapterPosition, questionPosition));
		chapterPosition = chapterpositionreceive;
		questionPosition = questionpositionreceive;
	}

	public void updateSurveyPositionOnBack(Integer chapterpositionreceive,
			Integer questionpositionreceive) {
		chapterPosition = chapterpositionreceive;
		questionPosition = questionpositionreceive;
	}

	public void updateTrackerPosition(Integer questionpositionreceive) {
		prevTrackingPositions.add(questionpositionreceive);
		tripQuestionPosition = questionpositionreceive;
	}

	public void updateTrackerPositionOnBack(Integer questionpositionreceive) {
		tripQuestionPosition = questionpositionreceive;

	}

	public boolean wasJustAtHubPage(Tuple prevPosition) {
		return prevPosition.questionPosition == HUB_PAGE_QUESTION_POSITION;
	}

	public boolean wasJustAtStatsPage(Tuple prevPosition) {
		return prevPosition.questionPosition == STATS_PAGE_QUESTION_POSITION;
	}

	public void updateJumpString(String jumpStringReceive) {
		jumpString = jumpStringReceive;
	}

	public void onPrevQuestionPressed(Boolean askingTripQuestions) {
		jumpString = null;
	}

	// updates positions to get next question. returns true if end of survey
	// reached
	public NextQuestionResult onNextQuestionPressed(
	// TODO fix the backstack of the loop questions
			Boolean askingTripQuestions) {
		if (askingTripQuestions && inLoop) {
			if (loopIteration == -1) {
				loopIteration = 0;
			}
			loopPosition++;
			Log.v("Loop position", loopPosition.toString());
			if (loopPosition == loopLimit) {
				loopPosition = 0;
				loopIteration++;
				if (loopIteration == loopTotal) {
					tripQuestionPosition++;
					loopPosition = -1;
					loopIteration = -1;
					inLoop = false;
					if (tripQuestionPosition == jTrackerQuestions.length()) {
						return NextQuestionResult.END;
					}
				}
			}
		} else if (!askingTripQuestions && inLoop) {
			Integer looptemporalpositionInteger = loopPosition;
			Integer loopTemporaryIteration = loopIteration;
			if (loopIteration == -1) {
				loopIteration = 0;
			}
			loopPosition++;
			Log.v("Loop position", loopPosition.toString());
			if (loopPosition == loopLimit) {
				loopPosition = 0;
				loopIteration++;
				if (loopIteration == loopTotal) {
					questionPosition++;
					loopPosition = -1;
					loopIteration = -1;
					inLoop = false;
					if (questionPosition == chapterQuestionCounts[chapterPosition]) {
						if (chapterPosition == jChapterList.length() - 1) {
							loopPosition = looptemporalpositionInteger;
							loopIteration = loopTemporaryIteration;
							inLoop = true;
							questionPosition--;
							return NextQuestionResult.END;
						} else {
							chapterPosition++;
							questionPosition = 0;
							inLoop = false;
							return NextQuestionResult.CHAPTER_END;
						}
					}
				}
			}

		} else if ((askingTripQuestions && !inLoop)
				&& (!questionKind.equals("LP"))) {
			prevTrackingPositions.add(tripQuestionPosition);
			tripQuestionPosition++;
			if (tripQuestionPosition == jTrackerQuestions.length()) {
				return NextQuestionResult.END;
			}
		} else if ((!askingTripQuestions && !inLoop)
				&& (!questionKind.equals("LP"))) {
			prevPositions.add(new Tuple(chapterPosition, questionPosition));
			questionPosition++;
			if (questionPosition == chapterQuestionCounts[chapterPosition]) {
				if (chapterPosition == jChapterList.length() - 1) {
					questionPosition--;
					return NextQuestionResult.END;
				} else {
					chapterPosition++;
					questionPosition = 0;
					return NextQuestionResult.CHAPTER_END;
				}
			}
		} else if ((askingTripQuestions && !inLoop)
				&& (questionKind.equals("LP"))) {
			loopTotal = getCurrentLoopTotal();
			prevTrackingPositions.add(tripQuestionPosition);
			if (loopTotal == 0) {
				tripQuestionPosition++;
				if (tripQuestionPosition == jTrackerQuestions.length()) {
					return NextQuestionResult.END;
				}
			} else {
				loopIteration = 0;
				loopPosition = 0;
				inLoop = true;
			}
		} else if ((!askingTripQuestions && !inLoop)
				&& (questionKind.equals("LP"))) {
			loopTotal = getCurrentLoopTotal();
			prevPositions.add(new Tuple(chapterPosition, questionPosition));
			if (loopTotal == 0) {
				questionPosition++;
				if (questionPosition == chapterQuestionCounts[chapterPosition]) {
					if (chapterPosition == jChapterList.length() - 1) {
						questionPosition--;
						return NextQuestionResult.END;
					} else {
						chapterPosition++;
						questionPosition = 0;
						return NextQuestionResult.CHAPTER_END;
					}
				}
			} else {
				loopIteration = 0;
				loopPosition = 0;
				inLoop = true;
			}
		}

		if (jumpString != null) {
			findIDPosition(jumpString);
			jumpPosition = findIDPosition(jumpString);
			chapterPosition = jumpPosition[0];
			questionPosition = jumpPosition[1];
			jumpString = null;
			jumpPosition = null;
			return NextQuestionResult.JUMPSTRING;
		}

		return NextQuestionResult.NORMAL;
	}

	public Integer[] findIDPosition(String iDtoFind) {
		// Searches for a question with the same id as the jumpString value
		Integer position[] = new Integer[2];
		for (int i = 0; i < jChapterList.length(); ++i) {
			for (int j = 0; j < chapterQuestionCounts[i]; ++j) {
				try {
					String questionID = jsurv
							.getJSONObject(SurveyorActivity.SURVEY_TYPE)
							.getJSONArray("Chapters").getJSONObject(i)
							.getJSONArray("Questions").getJSONObject(j)
							.getString("id");
					if (iDtoFind.equals(questionID)) {
						position[0] = i;
						position[1] = j;
						return position;
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	public Integer getChapterPosition() {
		return chapterPosition;
	}

	/*
	 * Getters
	 */

	public Integer getQuestionPosition() {
		return questionPosition;
	}

	public Integer getTripQuestionPosition() {
		return tripQuestionPosition;
	}

	public Integer getLoopPosition() {
		return loopPosition;
	}

	public JSONObject getCurrentQuestion() throws JSONException {
		JSONObject currentQuestion = null;
		if (inLoop) {
			currentQuestion = jsurv.getJSONObject(SurveyorActivity.SURVEY_TYPE)
					.getJSONArray("Chapters").getJSONObject(chapterPosition)
					.getJSONArray("Questions").getJSONObject(questionPosition)
					.getJSONArray("Questions").getJSONObject(loopPosition);
			Log.v("Loop lenght", loopLimit.toString());
		} else {
			currentQuestion = jsurv.getJSONObject(SurveyorActivity.SURVEY_TYPE)
					.getJSONArray("Chapters").getJSONObject(chapterPosition)
					.getJSONArray("Questions").getJSONObject(questionPosition);
		}
		return currentQuestion;
	}

	public JSONObject getCurrentTripQuestion() throws JSONException {
		JSONObject currentQuestion = null;
		if (inLoop) {
			currentQuestion = jTrackerQuestions
					.getJSONObject(tripQuestionPosition)
					.getJSONArray("Questions").getJSONObject(loopPosition);
			loopLimit = jTrackerQuestions.getJSONObject(tripQuestionPosition)
					.getJSONArray("Questions").length();
			Log.v("Loop lenght", loopLimit.toString());
		} else {
			currentQuestion = jTrackerQuestions
					.getJSONObject(tripQuestionPosition);
		}

		return currentQuestion;
	}

	public int getTripQuestionCount() {
		return jTrackerQuestions.length();
	}

	public String[] getChapterTitles() {
		return chapterTitles;
	}

	private enum SurveyType {
		SURVEY, TRACKER, LOOP
	}

	public enum NextQuestionResult {
		NORMAL, CHAPTER_END, END, JUMPSTRING
	}

	public static class Tuple {
		// TODO Erase this class
		public final Integer chapterPosition;
		public final Integer questionPosition;

		public Tuple(Integer chapterPosition, Integer questionPosition) {
			this.chapterPosition = chapterPosition;
			this.questionPosition = questionPosition;
		}

		public Tuple(String tupleString) {
			String[] positions = tupleString.split(",");
			this.chapterPosition = Integer.parseInt(positions[0]);
			this.questionPosition = Integer.parseInt(positions[1]);
		}

		@Override
		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
			if (!(o instanceof Tuple)) {
				return false;
			}
			Tuple tuple = (Tuple) o;
			return this.chapterPosition.equals(tuple.chapterPosition)
					&& this.questionPosition.equals(tuple.questionPosition);
		}

		@Override
		public int hashCode() {
			return chapterPosition.hashCode() ^ chapterPosition.hashCode();
		}

		@Override
		public String toString() {
			return chapterPosition + "," + questionPosition;
		}

	}

	public static class Quadruple {

		public final Integer chapterPosition;
		public final Integer questionPosition;
		public final Integer loopPosition;
		public final Integer loopIteration;

		public Quadruple(Integer chapterPosition, Integer questionPosition,
				Integer loopIteration, Integer loopPosition) {
			this.chapterPosition = chapterPosition;
			this.questionPosition = questionPosition;
			this.loopIteration = loopIteration;
			this.loopPosition = loopPosition;
		}

		public Quadruple(String quadrupleString) {
			String tempString = quadrupleString.substring(1,
					quadrupleString.length() - 1);
			String[] positions = tempString.split(",");
			this.chapterPosition = Integer.parseInt(positions[0]);
			this.questionPosition = Integer.parseInt(positions[1].substring(1,
					positions[1].length()));
			this.loopIteration = Integer.parseInt(positions[2].substring(1,
					positions[2].length()));
			this.loopPosition = Integer.parseInt(positions[3].substring(1,
					positions[3].length()));

		}

		@Override
		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
			if (!(o instanceof Quadruple)) {
				return false;
			}
			Tuple tuple = (Tuple) o;
			return this.chapterPosition.equals(tuple.chapterPosition)
					&& this.questionPosition.equals(tuple.questionPosition);
		}

		@Override
		public String toString() {
			return "[" + chapterPosition + "," + questionPosition + ","
					+ loopIteration + "," + loopPosition + "]";
		}

	}

	public void updateLoopLimit() {
		loopLimit = getLoopLimit(chapterPosition, questionPosition,
				SurveyorActivity.askingTripQuestions);
	}

	private Integer getLoopLimit(Integer chapterPositionString,
			Integer questionPositionString, Boolean askingTripQuestionsBoolean) {
		if (!askingTripQuestionsBoolean) {
			try {
				loopLimit = jsurv.getJSONObject(SurveyorActivity.SURVEY_TYPE)
						.getJSONArray("Chapters")
						.getJSONObject(chapterPositionString)
						.getJSONArray("Questions")
						.getJSONObject(questionPositionString)
						.getJSONArray("Questions").length();
			} catch (JSONException e) {
				// e.printStackTrace();
				loopLimit = 0;
			}
		} else {
			try {
				loopLimit = jtracker.getJSONArray("Questions")
						.getJSONObject(questionPositionString)
						.getJSONArray("Questions").length();
			} catch (JSONException e) {
				// e.printStackTrace();
				loopLimit = 0;
			}
		}

		Log.v("Loop lenght", loopLimit.toString());
		return loopLimit;
	}

	public void initializeLoop() {
		// Clearing hashmap
		clearLoopAnswerHashMap(chapterPosition, questionPosition, SurveyorActivity.askingTripQuestions);
		// Clearing Loop answers arrays
		for (int i = 0; i < loopLimit; ++i) {
			if (!SurveyorActivity.askingTripQuestions) {
				try {
					jsurv.getJSONObject(SurveyorActivity.SURVEY_TYPE)
							.getJSONArray("Chapters")
							.getJSONObject(chapterPosition)
							.getJSONArray("Questions")
							.getJSONObject(questionPosition)
							.getJSONArray("Questions").getJSONObject(i)
							.remove("LoopAnswers");
				} catch (JSONException e) {
					// e.printStackTrace();
				}
			} else {
				try {
					jtracker.getJSONArray("Questions")
							.getJSONObject(questionPosition)
							.getJSONArray("Questions").getJSONObject(i)
							.remove("LoopAnswers");
				} catch (JSONException e) {
					// e.printStackTrace();
				}
			}
		}
		for (int i = 0; i < loopLimit; ++i) {
			// Creating an empty array
			JSONArray tempArray = new JSONArray();
			for (int j = 0; j < loopTotal; ++j) {
				try {
					tempArray.put(j, "");
				} catch (JSONException e) {
					// e.printStackTrace();
				}
			}
			// Putting it on the JSON structure
			if (!SurveyorActivity.askingTripQuestions) {
				try {
					jsurv.getJSONObject(SurveyorActivity.SURVEY_TYPE)
							.getJSONArray("Chapters")
							.getJSONObject(chapterPosition)
							.getJSONArray("Questions")
							.getJSONObject(questionPosition)
							.getJSONArray("Questions").getJSONObject(i)
							.put("LoopAnswers", tempArray);
				} catch (JSONException e) {
					// e.printStackTrace();
				}
			} else {
				try {
					jtracker.getJSONArray("Questions")
							.getJSONObject(questionPosition)
							.getJSONArray("Questions").getJSONObject(i)
							.put("LoopAnswers", tempArray);
				} catch (JSONException e) {
					// e.printStackTrace();
				}
			}
		}
		Log.v("Initialize loop array", "Loop initialized!");
	}

	public void clearLoopAnswerHashMap(Integer chapterPosition,
			Integer questionPosition, Boolean askingTripQuestions) {
		Integer loopTotalInteger = getLoopTotal(chapterPosition, questionPosition, askingTripQuestions);
		Integer loopLimitInteger = getLoopLimit(chapterPosition,
				questionPosition, askingTripQuestions);

		for (int i = 0; i < loopLimitInteger; ++i) {
			if (!SurveyorActivity.askingTripQuestions) {
				for (int j = 0; j < loopTotal; ++j) {
					ArrayList<Integer> key = new ArrayList<Integer>(
							Arrays.asList(chapterPosition, questionPosition, i,
									j));
					if (selectedAnswersMap.containsKey(key)) {
						selectedAnswersMap.remove(key);
					}
				}
			} else {
				for (int j = 0; j < loopTotalInteger; ++j) {
					ArrayList<Integer> key = new ArrayList<Integer>(
							Arrays.asList(questionPosition, i, j));
					if (selectedTrackingAnswersMap.containsKey(key)) {
						selectedTrackingAnswersMap.remove(key);
					}
				}
			}
		}
		Log.v("Clear Loop", "Loop cleared!");
	}

	public Integer getCurrentLoopTotal() {
		Integer currentLoopTotal = getLoopTotal(chapterPosition,
				questionPosition, SurveyorActivity.askingTripQuestions);
		return currentLoopTotal;
	}

	private Integer getLoopTotal(Integer chapterPositionInteger,
			Integer questionPositionInteger, Boolean askingTripQuestionsBoolean) {
		String answer = null;
		Integer localLoopTotal = null;
		if (!askingTripQuestionsBoolean) {
			try {
				answer = jsurv.getJSONObject(SurveyorActivity.SURVEY_TYPE)
						.getJSONArray("Chapters")
						.getJSONObject(chapterPositionInteger)
						.getJSONArray("Questions")
						.getJSONObject(questionPositionInteger)
						.getString("Answer");
			} catch (JSONException e) {
				// e.printStackTrace();
			}
		} else {
			try {
				answer = jtracker.getJSONArray("Questions")
						.getJSONObject(questionPositionInteger)
						.getString("Answer");
			} catch (JSONException e) {
				// e.printStackTrace();
			}
		}
		if (answer != null) {
			if (!answer.equals("")) {
				localLoopTotal = Integer.parseInt(answer);
			} else {
				localLoopTotal = 0;
			}
		} else {
			localLoopTotal = 0;
		}
		return localLoopTotal;
	}
}
