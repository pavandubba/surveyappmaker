package org.urbanlaunchpad.flocktracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
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
	private JSONObject jsurv = null;
	private JSONArray jchapterlist;
	private JSONArray jtrackerquestions;

	// Hashmaps that store previously entered answers
	public static HashMap<Tuple<Integer>, ArrayList<Integer>> selectedAnswersMap = new HashMap<Tuple<Integer>, ArrayList<Integer>>();
	public static HashMap<Tuple<Integer>, Uri> prevImages = new HashMap<Tuple<Integer>, Uri>();

	private String[] ChapterTitles;
	private Integer[] chapterQuestionCounts;

	private Integer chapterPosition = null;
	private Integer questionPosition = null;
	
	// Backstack
	public Stack<Tuple<Integer>> prevPositions = new Stack<Tuple<Integer>>();
	public Stack<Integer> prevTrackingPositions = new Stack<Integer>();

	public Integer prevQuestionPosition = null;
	private Integer tripQuestionPosition = 0;
	private String jumpString = null;
	private Integer[] jumpPosition = null;
	private Integer[] loopEndPosition = null;
	private Integer[] loopStartPosition = null;

	public SurveyHelper(String username, String jsonSurvey, Context context) {
		this.username = username;
		this.jsonSurvey = jsonSurvey;
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
			String tripID) {
		String columnnamesString = getnames("id", "nq", "Survey");
		String answerfinalString = getnames("Answer", "wq", "Survey");
		String latlng = LocationHelper.getLatLngAlt(currentLocation);
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

		boolean success = false;
		try {
			Sql sql = Iniconfig.fusiontables.query().sql(query);
			sql.setKey(Iniconfig.API_KEY);
			sql.execute();
			success = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return success;
	}

	public void submitLocation(Location currentLocation, String tripID)
			throws ClientProtocolException, IOException {
		String columnnamesString = getnames("id", "nq", "Trip");
		String answerfinalString = getnames("Answer", "wq", "Trip");

		String latlng = LocationHelper.getLatLngAlt(currentLocation);
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
		Sql sql = Iniconfig.fusiontables.query().sql(query);
		sql.setKey(Iniconfig.API_KEY);
		sql.execute();
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
			columnlistNameString = getColumnListNames(TABLE_ID);
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
				requestColumnCreate(hardcolumnsStrings[i],
						hardcolumntypeStrings[i], TABLE_ID);
			}
		}

		// Checking for the existence of question columns on Fusion table.
		String[] questionIdlistString = getValues("id", type);
		String[] questionKindlistString = getValues("Kind", type);
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
				requestColumnCreate(questionIdlistString[i], auxkind, TABLE_ID);
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
						result[auxcount++] = jchapterlist.getJSONObject(i)
								.getJSONArray("Questions").getJSONObject(j)
								.getString(key);
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
					result[i] = jtrackerquestions.getJSONObject(i).getString(
							key);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			break;
		}

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
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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

	public void answerCurrentQuestion(String answer,
			ArrayList<Integer> selectedAnswers) {
		try {
			if (chapterPosition >= 0) {
				jsurv.getJSONObject("Survey").getJSONArray("Chapters")
						.getJSONObject(chapterPosition)
						.getJSONArray("Questions")
						.getJSONObject(questionPosition).put("Answer", answer);
				Tuple<Integer> key = new Tuple<Integer>(chapterPosition,
						questionPosition);
				selectedAnswersMap.put(key, selectedAnswers);
			}
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

	public void answerCurrentTrackerLoopQuestion(String answer) {

	}

	public void answerCurrentLoopQuestion(String answer) {

	}

	public void resetSurvey() {
		try {
			jsurv = new JSONObject(jsonSurvey);
			selectedAnswersMap = new HashMap<Tuple<Integer>, ArrayList<Integer>>();
			prevImages = new HashMap<Tuple<Integer>, Uri>();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void updateSurveyPosition(Integer chapterpositionreceive,
			Integer questionpositionreceive) {
		prevPositions.add(new Tuple<Integer>(chapterPosition, questionPosition));
		chapterPosition = chapterpositionreceive;
		questionPosition = questionpositionreceive;
	}
	
	public void updateSurveyPositionOnBack(Integer chapterpositionreceive,
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
		}
		
		jumpString = null;
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
			prevPositions.add(new Tuple<Integer>(chapterPosition, questionPosition));
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

	public static class Tuple<Integer> {

		public final Integer chapterPosition;
		public final Integer questionPosition;

		public Tuple(Integer chapterPosition, Integer questionPosition) {
			this.chapterPosition = chapterPosition;
			this.questionPosition = questionPosition;
		}

		@Override
		public int hashCode() {
			return chapterPosition.hashCode() ^ chapterPosition.hashCode();
		}

		@Override
		public boolean equals(Object o) {
			if (o == null)
				return false;
			if (!(o instanceof Tuple))
				return false;
			Tuple<Integer> tuple = (Tuple<Integer>) o;
			return this.chapterPosition.equals(tuple.chapterPosition)
					&& this.questionPosition.equals(tuple.questionPosition);
		}

	}
}
