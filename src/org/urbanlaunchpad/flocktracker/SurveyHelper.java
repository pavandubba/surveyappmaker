package org.urbanlaunchpad.flocktracker;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.R.integer;
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

public class SurveyHelper {
	private String username;
	private String TRIP_TABLE_ID = "1Q2mr8ni5LTxtZRRi3PNSYxAYS8HWikWqlfoIUK4";
	private String SURVEY_TABLE_ID = "11lGsm8B2SNNGmEsTmuGVrAy1gcJF9TQBo3G1Vw0";
	private Context context;
	private String jsonSurvey = null;
	private String jtrackerString = null;
	public JSONObject jsurv = null;
	private JSONArray jchapterlist;
	private JSONArray jtrackerquestions;
	public JSONObject jtracker = null;

	// Hashmaps that store previously entered answers
	public static HashMap<Tuple, ArrayList<Integer>> selectedAnswersMap = new HashMap<Tuple, ArrayList<Integer>>();
	public static HashMap<Integer, ArrayList<Integer>> selectedTrackingAnswersMap = new HashMap<Integer, ArrayList<Integer>>();
	public static HashMap<Tuple, Uri> prevImages = new HashMap<Tuple, Uri>();
	public static HashMap<Integer, Uri> prevTrackerImages = new HashMap<Integer, Uri>();

	private String[] ChapterTitles;
	private Integer[] chapterQuestionCounts;

	private Integer chapterPosition = null;
	private Integer questionPosition = null;

	// Backstack
	public Stack<Tuple> prevPositions = new Stack<Tuple>();
	public Stack<Integer> prevTrackingPositions = new Stack<Integer>();

	public Integer prevQuestionPosition = null;
	private Integer tripQuestionPosition = 0;
	public String jumpString = null;
	private Integer[] jumpPosition = null;
	private Integer[] loopEndPosition = null;
	private Integer[] loopStartPosition = null;

	public SurveyHelper(String username, String jsonSurvey, Context context) {
		this.username = username;
		this.jsonSurvey = jsonSurvey;
		// parse json survey
		try {
			this.jsurv = new JSONObject(jsonSurvey);
			this.jtrackerString = this.jsurv.getJSONObject("Tracker")
					.toString();
			this.jtracker = new JSONObject(this.jtrackerString);
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
				columnCheck(SURVEY_TABLE_ID, SurveyType.SURVEY, jchapterlist);
				columnCheck(TRIP_TABLE_ID, SurveyType.TRACKER, jtrackerquestions);
			}
		}).start();

	}

	/*
	 * Initialization code
	 */

	// Get trip and survey table id's
	public void getTableID() {
		try {
			TRIP_TABLE_ID = jtracker.getString("TableID");
			SURVEY_TABLE_ID = jsurv.getJSONObject("Survey")
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
			jchapterlist = jsurv.getJSONObject("Survey").getJSONArray(
					"Chapters");
			ChapterTitles = new String[jchapterlist.length()];
			for (int i = 0; i < jchapterlist.length(); ++i) {
				ChapterTitles[i] = jchapterlist.getJSONObject(i).getString(
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
			jtrackerquestions = jtracker.getJSONArray("Questions");
		} catch (JSONException e2) {
			Toast.makeText(
					context,
					"Project does not contain tracker questions or questions were created erroneusly.",
					Toast.LENGTH_SHORT).show();
			e2.printStackTrace();
		}
	}

	// Get counts of questions in each chapter
	public void parseChapterQuestionCount() {
		chapterQuestionCounts = new Integer[jchapterlist.length()];
		for (int i = 0; i < jchapterlist.length(); ++i) {
			try {
				chapterQuestionCounts[i] = jchapterlist.getJSONObject(i)
						.getJSONArray("Questions").length();
			} catch (JSONException e) {
				chapterQuestionCounts[i] = 0;
			}
		}
	}

	/*
	 * Uploading Logic
	 */

	public boolean submitSubmission(String jsurvString, String lat, String lng,
			String alt, String imagePaths, String surveyID, String tripID,
			String timestamp, String type) {
		boolean success = false;

		try {
			JSONObject jsurvQueueObject = new JSONObject(jsurvString);
			JSONObject imageMap = new JSONObject(imagePaths);

			// Upload images and put in answers
			for (@SuppressWarnings("unchecked")
			Iterator<String> i = imageMap.keys(); i.hasNext();) {
				String keyString = i.next();
				String fileLink = Surveyor.driveHelper.saveFileToDrive(imageMap
						.getString(keyString));

				if (fileLink != null) {
					if (type.equals("Tracker")) {
						Integer key = Integer.parseInt(keyString);
						jsurvQueueObject.getJSONObject(type)
								.getJSONArray("Questions").getJSONObject(key)
								.put("Answer", fileLink);
					} else if (type.equals("Survey")) {
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
			String columnnamesString = getnames("id", "nq", type,
					jsurvQueueObject);
			String answerfinalString = getnames("Answer", "wq", type,
					jsurvQueueObject);
			String lnglat = LocationHelper.getLngLatAlt(lng, lat, alt);
			String query = "";

			if (type.equals("Tracker")) {
				query = "INSERT INTO "
						+ TRIP_TABLE_ID
						+ " ("
						+ columnnamesString
						+ ",Location,Lat,Lng,Alt,Date,TripID,Username) VALUES ("
						+ answerfinalString + ",'<Point><coordinates>" + lnglat
						+ "</coordinates></Point>','" + lat + "','" + lng
						+ "','" + alt + "','" + timestamp + "','" + tripID
						+ "','" + username + "');";
				Log.v("Tracker submit", query);
			} else if (type.equals("Survey")) {
				query = "INSERT INTO "
						+ SURVEY_TABLE_ID
						+ " ("
						+ columnnamesString
						+ ",Location,Lat,Lng,Alt,Date,SurveyID,TripID,Username) VALUES ("
						+ answerfinalString + ",'<Point><coordinates>" + lnglat
						+ "</coordinates></Point>','" + lat + "','" + lng
						+ "','" + alt + "','" + timestamp + "','" + surveyID
						+ "','" + tripID + "','" + username + "');";
				Log.v("Survey submit", query);
//				Toast.makeText(context,
//						"Survey submitted succesfully!",
//						Toast.LENGTH_SHORT).show();
			}
			Sql sql = Iniconfig.fusiontables.query().sql(query);
			sql.setKey(Iniconfig.API_KEY);
			sql.execute();
			success = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return success;
	}

	@SuppressWarnings("unchecked")
	public void saveSubmission(Location currentLocation, String surveyID,
			String tripID, String jsurvString, JSONObject imagePaths,
			String type) {
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
			synchronized (Surveyor.submissionQueue) {
				Surveyor.submissionQueue.add(submission.toString());
				Iniconfig.prefs
						.edit()
						.putStringSet("submissionQueue",
								(Set<String>) Surveyor.submissionQueue.clone())
						.commit();
				Surveyor.savingSubmission = false;
				Surveyor.submissionQueue.notify();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * Column Check Code
	 */

	private enum SurveyType {
		SURVEY, TRACKER, LOOP
	}

	public void columnCheck(final String TABLE_ID, SurveyType type, JSONArray sourceJsonArray) {
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
					"Alt", "Lng", "SurveyID", "TripID", "Username" };
			hardcolumntypeStrings = new String[] { "LOCATION", "DATETIME",
					"NUMBER", "NUMBER", "NUMBER", "STRING", "STRING", "STRING" };
			numberofhardcolumns = hardcolumnsStrings.length;
			break;
		case TRACKER:
			hardcolumnsStrings = new String[] { "Location", "Date", "Lat",
					"Alt", "Lng", "TripID", "Username" };
			hardcolumntypeStrings = new String[] { "LOCATION", "DATETIME",
					"NUMBER", "NUMBER", "NUMBER", "STRING", "STRING" };
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
				for (int j = 0; j < sourceJsonArray.length(); ++j){
					numberofquestions = numberofquestions + chapterQuestionCounts[j];
				}
				questionIdlistString = new String[numberofquestions];
				questionKindlistString = new String[numberofquestions];
				String[] auxIDArray;
				String[] auxKindArray;
				JSONArray auxArray;
				int k = 0;
				for (int j = 0; j < sourceJsonArray.length(); ++j){
					try {
						auxArray = sourceJsonArray.getJSONObject(j).getJSONArray("Questions");
						auxIDArray = getValues("id", auxArray);
						auxKindArray = getValues("Kind", auxArray);
						for (int i = 0; i < auxIDArray.length; ++i){
							questionIdlistString[k] = auxIDArray[i];
							questionKindlistString[k] = auxKindArray[i];
							
							// Handling the existence of Looped questions.
							if (questionKindlistString[k].equals("LP")){
								Log.v("columnCheck", "Loop found!");
								final JSONArray forLoop = auxArray.getJSONObject(i).getJSONArray("Questions");
								new Thread(new Runnable() {
									
									public void run() {
										columnCheck(TABLE_ID, SurveyType.LOOP, forLoop);
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
				break;
			case LOOP:
				questionIdlistString = getValues("id", sourceJsonArray);
				questionKindlistString = getValues("Kind", sourceJsonArray);
				numberofquestions = questionIdlistString.length;
				break;
			}
			
			
			
//			String[] questionIdlistString = getValues("id", type);
//			String[] questionKindlistString = getValues("Kind", type);
//			int numberofquestions = questionIdlistString.length;
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
				result[i] = sourceJsonArray.getJSONObject(i).getString(
						key);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

//		switch (type) {
//		case SURVEY:
//			// get number of questions
//			for (int i = 0; i < jchapterlist.length(); ++i) {
//				for (int j = 0; j < chapterQuestionCounts[i]; ++j) {
//					++numQuestions;
//				}
//			}
//
//			result = new String[numQuestions];
//
//			int auxcount = 0;
//			for (int i = 0; i < jchapterlist.length(); ++i) {
//				for (int j = 0; j < chapterQuestionCounts[i]; ++j) {
//					try {
//						result[auxcount++] = jchapterlist.getJSONObject(i)
//								.getJSONArray("Questions").getJSONObject(j)
//								.getString(key);
//					} catch (JSONException e) {
//						result[auxcount++] = "";
//					}
//				}
//			}
//			break;
//		case TRACKER:
//			numQuestions = jtrackerquestions.length();
//			result = new String[numQuestions];
//
//			for (int i = 0; i < numQuestions; ++i) {
//				try {
//					result[i] = jtrackerquestions.getJSONObject(i).getString(
//							key);
//				} catch (JSONException e) {
//					e.printStackTrace();
//				}
//			}
//			break;		
//			
//		}

		Log.v("Number of questions", numQuestions.toString() + " " + key);
		return result;
	}

	public String[] getColumnListNames(String tableID)
			throws ClientProtocolException, IOException {
		// Returns the column list
		Fusiontables.Column.List columnRequest = Iniconfig.fusiontables
				.column().list(tableID);
		columnRequest.setKey(Iniconfig.API_KEY);
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
			columnRequest = Iniconfig.fusiontables.column().insert(tableID,
					newColumn);
			columnRequest.setKey(Iniconfig.API_KEY);
			columnRequest.execute();
			Log.v("requestColumnCreate", "Column created!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getnames(String nametoget, String syntaxtype,
			String triporsurvey, JSONObject survey) {
		// If syntaxtype equals wq , quotes are aded, if it's nq , no quotes are
		// added.
		String addString = null;
		String namesString = null;
		JSONArray questionsArray = null;
		Integer totalchapters = null;
		Integer totalquestions = null;

		if (triporsurvey.equals("Survey")) {
			try {
				totalchapters = survey.getJSONObject("Survey")
						.getJSONArray("Chapters").length();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} else if (triporsurvey.equals("Tracker")) {
			totalchapters = 1;
		}
		for (int i = 0; i < totalchapters; ++i) {
			if (triporsurvey.equals("Survey")) {
				try {
					questionsArray = survey.getJSONObject("Survey")
							.getJSONArray("Chapters").getJSONObject(i)
							.getJSONArray("Questions");
					totalquestions = questionsArray.length();
				} catch (JSONException e) {
					e.printStackTrace();
				}
			} else if (triporsurvey.equals("Tracker")) {
				try {
					questionsArray = jtracker.getJSONArray("Questions");
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
		return namesString;
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
				jsurv.getJSONObject("Survey").getJSONArray("Chapters")
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

	public void answerCurrentTrackerLoopQuestion(String answer) {

	}

	public void answerCurrentLoopQuestion(String answer) {

	}

	public void resetSurvey() {
		try {
			chapterPosition = 0;
			questionPosition = 0;
			jsurv = new JSONObject(jsonSurvey);
			jsurv.put("Tracker", jtracker);
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
			jtracker = new JSONObject(jtrackerString);
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

	public void updateJumpString(String jumpStringReceive) {
		jumpString = jumpStringReceive;
	}

	public void onPrevQuestionPressed(Boolean askingTripQuestions) {
		jumpString = null;
	}

	public enum NextQuestionResult {
		NORMAL, END, JUMPSTRING
	}

	// updates positions to get next question. returns true if end of survey
	// reached
	public NextQuestionResult onNextQuestionPressed(Boolean askingTripQuestions) {
		if (askingTripQuestions) {
			prevTrackingPositions.add(tripQuestionPosition);
			tripQuestionPosition++;
			if (tripQuestionPosition == jtrackerquestions.length()) {
				return NextQuestionResult.END;
			}
		} else {
			prevPositions.add(new Tuple(chapterPosition, questionPosition));
			questionPosition++;
			if (questionPosition == chapterQuestionCounts[chapterPosition]) {
				if (chapterPosition == jchapterlist.length() - 1) {
					questionPosition--;
					return NextQuestionResult.END;
				} else {
					chapterPosition++;
					questionPosition = 0;
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
		for (int i = 0; i < jchapterlist.length(); ++i) {
			for (int j = 0; j < chapterQuestionCounts[i]; ++j) {
				try {
					String questionID = jsurv.getJSONObject("Survey")
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

	/*
	 * Getters
	 */

	public Integer getChapterPosition() {
		return chapterPosition;
	}

	public Integer getQuestionPosition() {
		return questionPosition;
	}

	public Integer getTripQuestionPosition() {
		return tripQuestionPosition;
	}

	public JSONObject getCurrentQuestion() throws JSONException {
		return jsurv.getJSONObject("Survey").getJSONArray("Chapters")
				.getJSONObject(chapterPosition).getJSONArray("Questions")
				.getJSONObject(questionPosition);
	}

	public JSONObject getCurrentTripQuestion() throws JSONException {
		return jtrackerquestions.getJSONObject(tripQuestionPosition);
	}

	public int getTripQuestionCount() {
		return jtrackerquestions.length();
	}

	public String[] getChapterTitles() {
		return ChapterTitles;
	}

	public void setLoopLimits(String loopend) {
		loopEndPosition = findIDPosition(loopend);
		if (questionPosition + 1 == chapterQuestionCounts[chapterPosition]) {
			if (chapterPosition == jchapterlist.length() - 1) {
				Toast.makeText(context,
						"Loop at the end of a survey will not work",
						Toast.LENGTH_SHORT).show();
			} else {
				loopStartPosition[0] = chapterPosition + 1;
				loopStartPosition[1] = 0;
			}
		} else {
			loopStartPosition[0] = chapterPosition;
			loopStartPosition[1] = questionPosition + 1;

		}

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
		public int hashCode() {
			return chapterPosition.hashCode() ^ chapterPosition.hashCode();
		}

		@Override
		public String toString() {
			return chapterPosition + "," + questionPosition;
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

	}
	
	static void checkLocationConfig(final Context context){
		LocationManager lm = null;
		Builder dialog;
	     boolean gps_enabled = false;
	     boolean network_enabled = false;
	        if(lm==null)
	            lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
	        try{
	        gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
	        }catch(Exception ex){}
	        try{
	        network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
	        }catch(Exception ex){}

	       if(!gps_enabled && !network_enabled){
	            dialog = new AlertDialog.Builder(context);
	            dialog.setMessage(context.getResources().getString(R.string.gps_network_not_enabled));
	            dialog.setPositiveButton(context.getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {

	                @Override
	                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
	                    Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS );
	                    myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	                    context.startActivity(myIntent);
	                    //get gps
	                }
	            });
	            dialog.setNegativeButton(context.getString(R.string.cancel_location_settings), new DialogInterface.OnClickListener() {

	                @Override
	                public void onClick(DialogInterface paramDialogInterface, int paramInt) {

	                }
	            });
	            dialog.show();
	        }
	}
}
