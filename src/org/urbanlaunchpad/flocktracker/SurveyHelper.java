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

import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

public class SurveyHelper {
	private String username;
	private String token;
	private String API_KEY = "AIzaSyB4Nn1k2sML-0aBN2Fk3qOXLF-4zlaNwmg";
	private String TRIP_TABLE_ID = "1Q2mr8ni5LTxtZRRi3PNSYxAYS8HWikWqlfoIUK4";
	private String SURVEY_TABLE_ID = "11lGsm8B2SNNGmEsTmuGVrAy1gcJF9TQBo3G1Vw0";
	private Context context;
	private JSONObject jsurv = null;
	private JSONArray jchapterlist;
	private JSONArray jtrackerquestions;
	private String[] ChapterTitles;
	private Integer[] chapterQuestionCounts;

	private Integer chapterPosition = null;
	private Integer questionPosition = null;
	private Integer tripQuestionPosition = 0;
	private String jumpString = null;

	public SurveyHelper(String username, String token, String jsonSurvey,
			Context context) {
		this.username = username;
		this.token = token;

		// parse json survey
		try {
			this.jsurv = new JSONObject(jsonSurvey);
		} catch (JSONException e) {
			Toast.makeText(context,
					"Your survey json file is not formatted correctly",
					Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}

		// get information out of survey
		getTableID();

		// Checking existence of columns in the Fusion Tables.
		new Thread(new Runnable() {
			public void run() {
				columnCheck(SURVEY_TABLE_ID, SurveyType.SURVEY);
				columnCheck(TRIP_TABLE_ID, SurveyType.TRACKER);
			}
		}).start();

		parseChapters();
		parseTrackingQuestions();
		parseChapterQuestionCount();

	}

	/*
	 * Initialization code
	 */
	
	// Get trip and survey table id's
	public void getTableID() {
		try {
			TRIP_TABLE_ID = jsurv.getJSONObject("Tracker").getString("TableID");
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
			ChapterTitles = new String[1 + jchapterlist.length()];
			ChapterTitles[0] = "Hub Page";
			for (int i = 1; i <= jchapterlist.length(); ++i) {
				ChapterTitles[i] = jchapterlist.getJSONObject(i - 1).getString(
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
			jtrackerquestions = jsurv.getJSONObject("Tracker").getJSONArray(
					"Questions");
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
	
	public boolean submitSurvey(Location currentLocation, String surveyID,
			String tripID) throws ClientProtocolException, IOException {
		String columnnamesString = getnames("id", "nq", "Survey");
		String answerfinalString = getnames("Answer", "wq", "Survey");

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

		Log.v("Submit survey response code", response.getStatusLine()
				.getStatusCode()
				+ " "
				+ response.getStatusLine().getReasonPhrase());

		return response.getStatusLine().getStatusCode() == 200;
	}

	public void submitLocation(Location currentLocation, String tripID)
			throws ClientProtocolException, IOException {
		String columnnamesString = getnames("id", "nq", "Trip");
		String answerfinalString = getnames("Answer", "wq", "Trip");

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
	
	
	/*
	 * Column Check Code
	 */

	private enum SurveyType {
		SURVEY, TRACKER
	}
	
	public void columnCheck(String TABLE_ID, SurveyType type) {
		String[] hardcolumnsStrings = null; // Columns that are in all projects.
		String[] hardcolumntypeStrings = null; // Types for the columns that are
												// in all projects.
		switch (type) {
		case SURVEY:
			hardcolumnsStrings = new String[] { "Location", "Date", "Lat",
					"Alt", "Lng", "SurveyID", "TripID", "Username" };
			hardcolumntypeStrings = new String[] { "LOCATION", "DATETIME",
					"NUMBER", "NUMBER", "NUMBER", "STRING", "STRING", "STRING" };
			break;
		case TRACKER:
			hardcolumnsStrings = new String[] { "Location", "Date", "Lat",
					"Alt", "Lng", "TripID", "Username" };
			hardcolumntypeStrings = new String[] { "LOCATION", "DATETIME",
					"NUMBER", "NUMBER", "NUMBER", "STRING", "STRING" };
			break;
		}

		String[] columnlistNameString = null;

		// Getting the types and names of the columns in the fusion table.
		try {
			columnlistNameString = getColumnList(TABLE_ID, "name");
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Checking for the existence of the hard columns on Fusion table.
		int numberofhardcolumns = hardcolumnsStrings.length;
		for (int i = 0; i < numberofhardcolumns; ++i) {
			if (arrayContainsString(columnlistNameString, hardcolumnsStrings[i])) {
				// TODO: check column type
			} else {
				try {
					requestColumnCreate(hardcolumnsStrings[i],
							hardcolumntypeStrings[i], TABLE_ID);
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		// Checking for the existence of question columns on Fusion table.
		String[] questionIdlistString = getValues(
				"id", type);
		String[] questionKindlistString = getValues(
				"Kind", type);
		int numberofquestions = questionIdlistString.length;
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

			if (arrayContainsString(columnlistNameString,
					questionIdlistString[i])) {
				// TODO: check column type
			} else {
				try {
					requestColumnCreate(questionIdlistString[i], auxkind,
							TABLE_ID);
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}

	}
	
	// Get a list of values corresponding to the key in either tracker
	// or survey questions
	public String[] getValues(String key, SurveyType type) {
		Integer numQuestions = 0;
		String[] result = null;
		
		switch (type) {
		case SURVEY:
			// get number of questions
			for (int i = 0; i < jchapterlist.length(); ++i) {
				for (int j = 0; j < chapterQuestionCounts[i]; ++j) {
					++numQuestions;
				}
			}
			
			result = new String[numQuestions];

			int auxcount = 0;
			for (int i = 0; i < jchapterlist.length(); ++i) {
				for (int j = 0; j < chapterQuestionCounts[i]; ++j) {
					try {
						result[auxcount++] = jchapterlist
								.getJSONObject(i).getJSONArray("Questions")
								.getJSONObject(j).getString(key);
					} catch (JSONException e) {
						result[auxcount++] = "";
					}
				}
			}
			break;
		case TRACKER:
			numQuestions = jtrackerquestions.length();
			result = new String[numQuestions];

			for (int i = 0; i < numQuestions; ++i) {
				try {
					result[i] = jtrackerquestions.getJSONObject(
							i).getString(key);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			break;
		}
		
		Log.v("Number of questions", numQuestions.toString() + " "
				+ key);
		return result;
	}

	public String[] getColumnList(String tableID, String whatotogetString)
			throws ClientProtocolException, IOException {
		// Returns the column list (of maximum MAX items) of a given fusion
		// tables table as a JSON string.
		String url = "https://www.googleapis.com/fusiontables/v1/tables/"
				+ tableID + "/columns?key=" + API_KEY + "&maxResults=500";

		// query for column list
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
		
		JSONObject jcolumns = null;
		String[] columnList = null;

		Integer total = 0;
		try {
			jcolumns = new JSONObject(columnlistJSONString);
			total = jcolumns.getInt("totalItems");
			columnList = new String[total];
		} catch (JSONException e1) {
			e1.printStackTrace();
		}

		// Get column list names or types
		if (total > 0) {
			for (int i = 0; i < total; ++i) {
				try {
					columnList[i] = jcolumns.getJSONArray("items")
							.getJSONObject(i).getString(whatotogetString);
				} catch (JSONException e) {
					columnList[i] = "";
				}
			}
		}
		
		Log.v("Number of columns", total.toString() + " " + whatotogetString);
		return columnList;
	}

	public void requestColumnCreate(String name, String type, String TABLE_ID)
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
			totalchapters = jchapterlist.length();
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
	
	public void answerCurrentQuestion(String answer) {
		try {
			jsurv.getJSONObject("Survey").getJSONArray("Chapters")
					.getJSONObject(chapterPosition - 1)
					.getJSONArray("Questions").getJSONObject(questionPosition)
					.put("Answer", answer);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void answerCurrentTrackerQuestion(String answer) {
		try {
			jsurv.getJSONObject("Tracker").getJSONArray("Questions")
					.getJSONObject(tripQuestionPosition).put("Answer", answer);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void resetSurvey() {
		for (int i = 0; i < jchapterlist.length(); ++i) {
			for (int j = 0; j < chapterQuestionCounts[i]; j++) {
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

	public void updateSurveyPosition(Integer chapterpositionreceive,
			Integer questionpositionreceive) {
		chapterPosition = chapterpositionreceive;
		questionPosition = questionpositionreceive;
	}

	public void updateJumpString(String jumpStringReceive) {
		jumpString = jumpStringReceive;
	}
	
	public void onPrevQuestionPressed(Boolean askingTripQuestions) {
		if (askingTripQuestions) {
			tripQuestionPosition--;
		} else {
			questionPosition--;
		}
	}
	
	public enum NextQuestionResult {
		NORMAL, END, JUMPSTRING
	}
	
	// updates positions to get next question. returns true if end of survey
	// reached
	public NextQuestionResult onNextQuestionPressed(Boolean askingTripQuestions) {
		if (askingTripQuestions) {
			tripQuestionPosition++;
			if (tripQuestionPosition == jtrackerquestions.length()) {
				tripQuestionPosition = 0;
				return NextQuestionResult.END;
			}
		} else {
			questionPosition++;
			if (questionPosition == chapterQuestionCounts[chapterPosition - 1]) {
				chapterPosition++;
				questionPosition = 0;

				if (chapterPosition > jchapterlist.length())
					return NextQuestionResult.END;
			}
		}

		if (jumpString == null) {
			return NextQuestionResult.NORMAL;
		} else {
			jumpTo(jumpString);
			jumpString = null;
			return NextQuestionResult.JUMPSTRING;
		}
	}

	public void jumpTo(String jumpString) {
		// Searches for a question with the same id as the jumpString value
		for (int i = 0; i < jchapterlist.length(); ++i) {
			for (int j = 0; j < chapterQuestionCounts[i]; ++j) {
				try {
					String questionID = jsurv.getJSONObject("Survey")
							.getJSONArray("Chapters").getJSONObject(i)
							.getJSONArray("Questions").getJSONObject(j)
							.getString("id");
					if (jumpString.equals(questionID)) {
						chapterPosition = i + 1;
						questionPosition = j;
						return;
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
	}

	
	/*
	 * Getters
	 */
	
	public int getChapterPosition() {
		return chapterPosition;
	}

	public int getQuestionPosition() {
		return questionPosition;
	}

	public int getTripQuestionPosition() {
		return tripQuestionPosition;
	}

	public JSONObject getCurrentQuestion() throws JSONException {
		return jsurv.getJSONObject("Survey").getJSONArray("Chapters")
				.getJSONObject(chapterPosition - 1).getJSONArray("Questions")
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
}
