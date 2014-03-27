package org.urbanlaunchpad.flocktracker.helpers;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

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
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.SurveyorActivity;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class SurveyHelper {

	// Hashmaps that store previously entered answers
	public static HashMap<Tuple, ArrayList<Integer>> selectedAnswersMap = new HashMap<Tuple, ArrayList<Integer>>();
	public static HashMap<Integer, ArrayList<Integer>> selectedTrackingAnswersMap = new HashMap<Integer, ArrayList<Integer>>();

	public static HashMap<Tuple, Uri> prevImages = new HashMap<Tuple, Uri>();
	public static HashMap<Integer, Uri> prevTrackerImages = new HashMap<Integer, Uri>();
	public JSONObject jsurv = null;
	public JSONObject jtracker = null;

	// Backstack
	public Stack<Tuple> prevPositions = new Stack<Tuple>();
	public Stack<Integer> prevTrackingPositions = new Stack<Integer>();
	public Integer prevQuestionPosition = null;

    // Survey / Tracker State
    private String username;
    public static String TRIP_TABLE_ID = "1Q2mr8ni5LTxtZRRi3PNSYxAYS8HWikWqlfoIUK4";
    public static String SURVEY_TABLE_ID = "11lGsm8B2SNNGmEsTmuGVrAy1gcJF9TQBo3G1Vw0";
    private Integer chapterPosition = HUB_PAGE_CHAPTER_POSITION;
    private Integer questionPosition = HUB_PAGE_QUESTION_POSITION;
    private Integer tripQuestionPosition = 0;
    private Integer[] jumpPosition = null;
	public String jumpString = null;
	private Context context;
	private String jsonSurvey = null;
	private String jTrackerString = null;
	private JSONArray jChapterList;
	private JSONArray jTrackerQuestions;
	private String[] chapterTitles;
	private Integer[] chapterQuestionCounts;
    public Integer loopTotal = null; // Number of times loop questions repeat

    // Constants
	public static final Integer HUB_PAGE_CHAPTER_POSITION = -15;
	public static final Integer HUB_PAGE_QUESTION_POSITION = -15;
	public static final Integer STATS_PAGE_CHAPTER_POSITION = -16;
	public static final Integer STATS_PAGE_QUESTION_POSITION = -16;
    public static final Integer MAX_QUERY_LENGTH = 2000; // max length allowed by fusion table

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
			Toast.makeText(context,
					"Your survey json file is not formatted correctly",
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
		if (lm == null)
			lm = (LocationManager) context
					.getSystemService(Context.LOCATION_SERVICE);

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
			Toast.makeText(
					context,
					"Your project has messy Fusion Table IDs, uploading won't work.",
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
			Toast.makeText(context, "Chapters not parsed, check survey file.",
					Toast.LENGTH_SHORT).show();
		}
	}

	// Get tracking questions related to each trip
	public void parseTrackingQuestions() {
		try {
			jTrackerQuestions = jtracker.getJSONArray("Questions");
		} catch (JSONException e2) {
			Toast.makeText(
					context,
					"Project does not contain tracker questions or questions were created erroneusly.",
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
			String femaleCount, String totalCount) {
		boolean success = false;

		try {
			JSONObject jsurvQueueObject = new JSONObject(jsurvString);
			JSONObject imageMap = new JSONObject(imagePaths);

			// Upload images and put in answers
			for (@SuppressWarnings("unchecked")
			Iterator<String> i = imageMap.keys(); i.hasNext();) {
				String keyString = i.next();
				String fileLink = SurveyorActivity.driveHelper.saveFileToDrive(imageMap
						.getString(keyString));

				if (fileLink != null) {
					if (type.equals(SurveyorActivity.TRACKER_TYPE)) {
						Integer key = Integer.parseInt(keyString);
						jsurvQueueObject.getJSONObject(type)
								.getJSONArray("Questions").getJSONObject(key)
								.put("Answer", fileLink);
					} else if (type.equals(SurveyorActivity.SURVEY_TYPE)) {
						Tuple key = new Tuple(keyString);
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
						+ ",Location,Lat,Lng,Alt,Date,TripID,Username,TotalCount,FemaleCount,MaleCount) VALUES ("
						+ answerfinalString + ",'<Point><coordinates>" + lnglat
						+ "</coordinates></Point>','" + lat + "','" + lng
						+ "','" + alt + "','" + timestamp + "','" + tripID
						+ "','" + username + "','" + totalCount + "','"
						+ femaleCount + "','" + maleCount + "');";
				Log.v("TrackerAlarm submit", query);
			} else if (type.equals(SurveyorActivity.SURVEY_TYPE)) {
				query = "INSERT INTO "
						+ SURVEY_TABLE_ID
						+ " ("
						+ columnnamesString
						+ ",Location,Lat,Lng,Alt,Date,SurveyID,TripID,Username,TotalCount,FemaleCount,MaleCount) VALUES ("
						+ answerfinalString + ",'<Point><coordinates>" + lnglat
						+ "</coordinates></Point>','" + lat + "','" + lng
						+ "','" + alt + "','" + timestamp + "','" + surveyID
						+ "','" + tripID + "','" + username + "','"
						+ totalCount + "','" + femaleCount + "','" + maleCount
						+ "');";
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
							+ " (Location,Lat,Lng,Alt,Date,TripID,TotalCount,FemaleCount,MaleCount)"
							+ " VALUES (" + "'<Point><coordinates>" + lnglat
							+ "</coordinates></Point>','" + lat + "','" + lng
							+ "','" + alt + "','" + timestamp + "','" + tripID
							+ "','" + totalCount + "','" + femaleCount + "','"
							+ maleCount + "');";

					rowID = Integer.parseInt((String) IniconfigActivity.fusiontables
							.query().sql(metaDataQuery)
							.setKey(IniconfigActivity.API_KEY).execute().getRows()
							.get(0).get(0));
				} else {
					String metaDataQuery = "INSERT INTO "
							+ SURVEY_TABLE_ID
							+ " (Location,Lat,Lng,Alt,Date,SurveyID,TripID,TotalCount,FemaleCount,MaleCount)"
							+ " VALUES (" + "'<Point><coordinates>" + lnglat
							+ "</coordinates></Point>','" + lat + "','" + lng
							+ "','" + alt + "','" + timestamp + "','"
							+ surveyID + "','" + tripID + "','" + totalCount
							+ "','" + femaleCount + "','" + maleCount + "');";

					rowID = Integer.parseInt((String) IniconfigActivity.fusiontables
							.query().sql(metaDataQuery)
							.setKey(IniconfigActivity.API_KEY).execute().getRows()
							.get(0).get(0));
				}

				// Send rest of info one at a time
				sendUpdateQuery("Username", username, type, rowID);

				for (int i = 0; i < columnNames.size(); i++) {
					sendUpdateQuery(columnNames.get(i), answers.get(i), type,
							rowID);
				}
			} else {
				Sql sql = IniconfigActivity.fusiontables.query().sql(query);
				sql.setKey(IniconfigActivity.API_KEY);
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
		if (value.isEmpty())
			return true;
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
			sql.setKey(IniconfigActivity.API_KEY);
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
				totalchapters = survey.getJSONObject(SurveyorActivity.SURVEY_TYPE)
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
					questionsArray = survey.getJSONObject(SurveyorActivity.SURVEY_TYPE)
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
			String type, String maleCount, String femaleCount, String totalCount) {
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
			if (type.equals(SurveyorActivity.TRACKER_TYPE)) {
				synchronized (SurveyorActivity.trackerSubmissionQueue) {
					SurveyorActivity.trackerSubmissionQueue.add(submission.toString());
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
					SurveyorActivity.surveySubmissionQueue.add(submission.toString());
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
					"FemaleCount", "MaleCount", "Username" };
			hardcolumntypeStrings = new String[] { "LOCATION", "DATETIME",
					"NUMBER", "NUMBER", "NUMBER", "STRING", "STRING", "NUMBER",
					"NUMBER", "NUMBER", "STRING" };
			numberofhardcolumns = hardcolumnsStrings.length;
			break;
		case TRACKER:
			hardcolumnsStrings = new String[] { "Location", "Date", "Lat",
					"Alt", "Lng", "TripID", "TotalCount", "FemaleCount",
					"MaleCount", "Username" };
			hardcolumntypeStrings = new String[] { "LOCATION", "DATETIME",
					"NUMBER", "NUMBER", "NUMBER", "STRING", "NUMBER", "NUMBER",
					"NUMBER", "STRING" };
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
						// TODO Auto-generated catch block
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
							// TODO Auto-generated catch block
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

	public String[] getColumnListNames(String tableID)
			throws ClientProtocolException, IOException {
		// Returns the column list
		Fusiontables.Column.List columnRequest = IniconfigActivity.fusiontables
				.column().list(tableID);
		columnRequest.setKey(IniconfigActivity.API_KEY);
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
			columnRequest = IniconfigActivity.fusiontables.column().insert(tableID,
					newColumn);
			columnRequest.setKey(IniconfigActivity.API_KEY);
			columnRequest.execute();
			Log.v("requestColumnCreate", "Column created!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// helper to search for string in array
	public boolean arrayContainsString(String[] array, String string) {
		if (array.length == 0)
			return false;

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
		try {
			if (chapterPosition >= 0) {
				jsurv.getJSONObject(SurveyorActivity.SURVEY_TYPE)
						.getJSONArray("Chapters")
						.getJSONObject(chapterPosition)
						.getJSONArray("Questions")
						.getJSONObject(questionPosition).put("Answer", answer);
				Tuple key = new Tuple(chapterPosition, questionPosition);
				selectedAnswersMap.put(key, selectedAnswers);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void answerCurrentTrackerQuestion(String answer,
			ArrayList<Integer> selectedAnswers) {
		try {
			jtracker.getJSONArray("Questions")
					.getJSONObject(tripQuestionPosition).put("Answer", answer);
			selectedTrackingAnswersMap.put(tripQuestionPosition,
					selectedAnswers);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void answerCurrentTrackerLoopQuestion(String answer,
			ArrayList<Integer> selectedAnswers) {

	}

	public void answerCurrentLoopQuestion(String answer,
			ArrayList<Integer> selectedAnswers) {

	}

	public void resetSurvey() {
		try {
			chapterPosition = 0;
			questionPosition = 0;
			jsurv = new JSONObject(jsonSurvey);
			jsurv.put(SurveyorActivity.TRACKER_TYPE, jtracker);
			prevPositions = new Stack<Tuple>();
			selectedAnswersMap = new HashMap<Tuple, ArrayList<Integer>>();
			prevImages = new HashMap<Tuple, Uri>();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void resetTracker() {
		tripQuestionPosition = 0;
		prevTrackingPositions = new Stack<Integer>();
		selectedTrackingAnswersMap = new HashMap<Integer, ArrayList<Integer>>();
		prevTrackerImages = new HashMap<Integer, Uri>();
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
			Boolean askingTripQuestions, Boolean inLoop) {
		if (askingTripQuestions && inLoop) {

		} else if (!askingTripQuestions && inLoop) {

		} else if (askingTripQuestions && !inLoop) {
			prevTrackingPositions.add(tripQuestionPosition);
			tripQuestionPosition++;
			if (tripQuestionPosition == jTrackerQuestions.length()) {
				return NextQuestionResult.END;
			}
		} else if (!askingTripQuestions && !inLoop) {
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

	public JSONObject getCurrentQuestion() throws JSONException {
		return jsurv.getJSONObject(SurveyorActivity.SURVEY_TYPE)
				.getJSONArray("Chapters").getJSONObject(chapterPosition)
				.getJSONArray("Questions").getJSONObject(questionPosition);
	}

	public JSONObject getCurrentTripQuestion() throws JSONException {
		return jTrackerQuestions.getJSONObject(tripQuestionPosition);
	}

	public int getTripQuestionCount() {
		return jTrackerQuestions.length();
	}

	public String[] getChapterTitles() {
		return chapterTitles;
	}

	// public void setLoopLimits(String loopend) {
	// loopEndPosition = findIDPosition(loopend);
	// if (questionPosition + 1 == chapterQuestionCounts[chapterPosition]) {
	// if (chapterPosition == jChapterList.length() - 1) {
	// Toast.makeText(context,
	// "Loop at the end of a survey will not work",
	// Toast.LENGTH_SHORT).show();
	// } else {
	// loopStartPosition[0] = chapterPosition + 1;
	// loopStartPosition[1] = 0;
	// }
	// } else {
	// loopStartPosition[0] = chapterPosition;
	// loopStartPosition[1] = questionPosition + 1;
	//
	// }
	//
	// }

	private enum SurveyType {
		SURVEY, TRACKER, LOOP
	}

	public enum NextQuestionResult {
		NORMAL, CHAPTER_END, END, JUMPSTRING
	}

	public static class Tuple {

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
			if (o == null)
				return false;
			if (!(o instanceof Tuple))
				return false;
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
}