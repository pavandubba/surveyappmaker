package org.urbanlaunchpad.flocktracker.models;

import com.google.api.services.fusiontables.Fusiontables;
import org.urbanlaunchpad.flocktracker.IniconfigActivity;
import org.urbanlaunchpad.flocktracker.ProjectConfig;
import org.urbanlaunchpad.flocktracker.SurveyorActivity;
import org.urbanlaunchpad.flocktracker.helpers.LocationHelper;

import java.io.IOException;
import java.util.List;

public class Submission {
  public static final Integer MAX_QUERY_LENGTH = 2000; // max length allowed by fusion table

  private Chapter[] chapters;
  private Metadata metadata;
  private Type type;

  public Metadata getMetadata() {
    return metadata;
  }

  public void setMetadata(Metadata metadata) {
    this.metadata = metadata;
  }

  public Chapter[] getChapters() {
    return chapters;
  }

  public void setChapters(Chapter[] chapters) {
    this.chapters = chapters;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }


  /**
   * Submits this submission to the appropriate table.
   * @return true if submission succeeded.
   */
  public boolean submit() {
    boolean success = false;

    try {
      submitImages();
      String query = getQueryForSubmission();

      if (query.length() >= MAX_QUERY_LENGTH) {
        // Send initial insert and get row ID
        query = getMetadataInsertQuery();
        int rowID = Integer
          .parseInt((String) IniconfigActivity.fusiontables
            .query().sql(query)
            .setKey(ProjectConfig.get().getApiKey())
            .execute().getRows().get(0).get(0));

        // Send rest of info one at a time
        sendQuery(getUpdateQueryGivenRow(rowID, "Username", ProjectConfig.get().getUsername()));

        for (Chapter chapter : chapters) {
          for (Question question : chapter.getQuestions()) {
            sendQuery(getUpdateQueryGivenRow(rowID, question.getQuestionID(),
              question.getSelectedAnswers().toString()));
          }
        }
      } else {
        sendQuery(query);
      }
      success = true;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return success;
  }

  /**
   * Helper method to upload images associated with this submission.
   *
   * @return
   */
  private boolean submitImages() {
    try {
      for (Chapter chapter : chapters) {
        for (Question question : chapter.getQuestions()) {
          // Upload images
          String fileLink = question.getImage().toString();
          SurveyorActivity.driveHelper.saveFileToDrive(fileLink);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  /**
   * Helper method to get the full string query to submit the entire submission.
   *
   * @return
   */
  private String getQueryForSubmission() {
    // Create and submit query
    StringBuilder questionIDString = new StringBuilder();
    StringBuilder answerString = new StringBuilder();

    for (Chapter chapter : chapters) {
      for (Question question : chapter.getQuestions()) {
        // Get question ID's and answers
        questionIDString.append(question.getQuestionID() + ",");
        answerString.append("'" + question.getSelectedAnswers().toString() + "','");
      }
    }

    String locationString = LocationHelper.getLngLatAlt(
      metadata.getLongitude(),
      metadata.getLatitude(),
      metadata.getAltitude());
    String query = "";

    switch (type) {
      case TRACKER:
        query = "INSERT INTO "
                + ProjectConfig.get().getTrackerTableID()
                + " ("
                + questionIDString
                + "Location,Lat,Lng,Alt,Date,TripID,Username,TotalCount,FemaleCount,MaleCount,Speed) VALUES ("
                + answerString + "<Point><coordinates>" + locationString
                + "</coordinates></Point>','" + metadata.getLatitude() + "','" + metadata.getLongitude()
                + "','" + metadata.getAltitude() + "','" + metadata.getTimeStamp() + "','" + metadata.getTripID()
                + "','" + ProjectConfig.get().getUsername() + "','"
                + (metadata.getMaleCount() + metadata.getFemaleCount()) + "','"
                + metadata.getFemaleCount() + "','" + metadata.getMaleCount() + "','" + metadata.getSpeed() + "');";
        break;
      case SURVEY:
        query = "INSERT INTO "
                + ProjectConfig.get().getSurveyUploadTableID()
                + " ("
                + questionIDString
                + "Location,Lat,Lng,Alt,Date,SurveyID,TripID,Username,TotalCount,FemaleCount,MaleCount,Speed"
                + ") VALUES (" + answerString
                + "<Point><coordinates>" + locationString
                + "</coordinates></Point>','" + metadata.getLatitude() + "','" + metadata.getLongitude()
                + "','" + metadata.getAltitude() + "','" + metadata.getTimeStamp() + "','" + metadata.getSurveyID()
                + "','" + metadata.getTripID() + "','" + ProjectConfig.get().getUsername() + "','"
                + (metadata.getMaleCount() + metadata.getFemaleCount()) + "','" + metadata.getFemaleCount()
                + "','" + metadata.getMaleCount() + "','" + metadata.getSpeed() + "');";
        break;
    }

    return query;
  }

  /**
   * Helper method to get the string query needed to insert only the metadata.
   * @return
   */
  private String getMetadataInsertQuery() {
    String query = null;

    String locationString = LocationHelper.getLngLatAlt(
      metadata.getLongitude(),
      metadata.getLatitude(),
      metadata.getAltitude());

    switch (type) {
      case TRACKER:
        query = "INSERT INTO "
                + ProjectConfig.get().getTrackerTableID()
                + " (Location,Lat,Lng,Alt,Date,TripID,TotalCount,FemaleCount,MaleCount,Speed)"
                + " VALUES (" + "'<Point><coordinates>" + locationString
                + "</coordinates></Point>','" + metadata.getLatitude() + "','"
                + metadata.getLongitude() + "','" + metadata.getAltitude() + "','"
                + metadata.getTimeStamp() + "','" + metadata.getTripID()
                + "','" + (metadata.getMaleCount() + metadata.getFemaleCount()) + "','"
                + metadata.getFemaleCount() + "','" + metadata.getMaleCount() + "','"
                + metadata.getSpeed() + "');";
        break;
      case SURVEY:
        query = "INSERT INTO "
                + ProjectConfig.get().getSurveyUploadTableID()
                + " (Location,Lat,Lng,Alt,Date,SurveyID,TripID,TotalCount,FemaleCount,"
                + "MaleCount,Speed)" + " VALUES ("
                + "'<Point><coordinates>" + locationString
                + "</coordinates></Point>','" + metadata.getLatitude() + "','"
                + metadata.getLongitude() + "','" + metadata.getAltitude() + "','"
                + metadata.getTimeStamp() + "','" + metadata.getSurveyID() + "','"
                + metadata.getTripID() + "','" + (metadata.getMaleCount() + metadata.getFemaleCount())
                + "','" + metadata.getFemaleCount() + "','" + metadata.getMaleCount() + "','"
                + metadata.getSpeed() + "');";
        break;
    }

    return query;
  }

  /**
   * Helper method to get the string query to update a key-value pair given the rowID of the existing table entry.
   * @param rowID
   * @param key
   * @param value
   * @return
   */
  private String getUpdateQueryGivenRow(int rowID, String key, String value) {
    String query = null;
    if (value.isEmpty()) {
      return query;
    }

    switch (type) {
      case TRACKER:
        query = "UPDATE " + ProjectConfig.get().getTrackerTableID() + " SET " + key + " = '"
                + value + "' WHERE ROWID = '" + rowID + "'";
        break;
      case SURVEY:
        query = "UPDATE " + ProjectConfig.get().getSurveyUploadTableID() + " SET " + key + " = '"
                + value + "' WHERE ROWID = '" + rowID + "'";
        break;
    }

    return query;
  }

  /**
   * Helper method to send a fusion table query.
   * @param query
   * @throws IOException
   */
  private void sendQuery(String query) throws IOException {
    Fusiontables.Query.Sql sql = IniconfigActivity.fusiontables.query().sql(query);
    sql.setKey(ProjectConfig.get().getApiKey());
    sql.execute();
  }

  public enum Type {SURVEY, TRACKER}
}
