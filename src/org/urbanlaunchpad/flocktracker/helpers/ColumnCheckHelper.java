package org.urbanlaunchpad.flocktracker.helpers;

import android.util.Log;
import com.google.api.services.fusiontables.Fusiontables;
import com.google.api.services.fusiontables.model.Column;
import com.google.api.services.fusiontables.model.ColumnList;
import org.urbanlaunchpad.flocktracker.IniconfigActivity;
import org.urbanlaunchpad.flocktracker.ProjectConfig;
import org.urbanlaunchpad.flocktracker.models.Chapter;
import org.urbanlaunchpad.flocktracker.models.Question;
import org.urbanlaunchpad.flocktracker.util.QuestionUtil;

import java.io.IOException;
import java.util.HashSet;

/**
 * ColumnCheckHelper checks to make sure the tables online have the necessary columns.
 *
 */
public class ColumnCheckHelper {
  private Chapter[] surveyChapters;
  private Question[] trackingQuestions;

  public ColumnCheckHelper(Chapter[] surveyChapters, Question[] trackingQuestions) {
    this.surveyChapters = surveyChapters;
    this.trackingQuestions = trackingQuestions;
  }

  public void runChecks() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        checkSurveyColumns();
        checkTrackerColumns();
      }
    }).start();
  }

  private void checkSurveyColumns() {
    String[] metadataColumnNames = new String[]{"Location", "Date", "Lat",
      "Alt", "Lng", "SurveyID", "TripID", "TotalCount",
      "FemaleCount", "MaleCount", "Speed", "Username"};
    String[] metadataColumnTypes = new String[]{"LOCATION", "DATETIME",
      "NUMBER", "NUMBER", "NUMBER", "STRING", "STRING", "NUMBER",
      "NUMBER", "NUMBER", "NUMBER", "STRING"};

    String surveyTableID = ProjectConfig.get().getSurveyUploadTableID();

    // Getting the types and names of the columns in the fusion table.
    try {
      HashSet<String> columnNames = getColumnListNames(surveyTableID);

      // Checking for the existence of the hard columns on Fusion table.
      for (int i = 0; i < metadataColumnNames.length; ++i) {
        if (columnNames.contains(metadataColumnNames[i])) {
          // TODO: check column type
        } else {
          requestColumnCreate(metadataColumnNames[i], metadataColumnTypes[i], surveyTableID);
        }
      }

      // Checking for the existence of question columns on Fusion table.
      for (Chapter chapter : surveyChapters) {
        for (Question question : chapter.getQuestions()) {
          checkQuestionColumn(columnNames, question, surveyTableID);

          // If it is a looped question, check its looped question id's as well.
          if (question.getType() == Question.QuestionType.LOOP) {
            for (Question loopQuestion : question.getLoopQuestions()) {
              checkQuestionColumn(columnNames, loopQuestion, surveyTableID);
            }
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void checkTrackerColumns() {
    String[] metadataColumnNames = new String[] { "Location", "Date", "Lat",
      "Alt", "Lng", "TripID", "TotalCount", "FemaleCount",
      "MaleCount", "Speed", "Username" };
    String[] metadataColumnTypes = new String[] { "LOCATION", "DATETIME",
      "NUMBER", "NUMBER", "NUMBER", "STRING", "NUMBER", "NUMBER",
      "NUMBER", "NUMBER", "STRING" };
    String trackerTableID = ProjectConfig.get().getTrackerTableID();

    // Getting the types and names of the columns in the fusion table.
    try {
      HashSet<String> columnNames = getColumnListNames(trackerTableID);

      // Checking for the existence of the hard columns on Fusion table.
      for (int i = 0; i < metadataColumnNames.length; ++i) {
        if (columnNames.contains(metadataColumnNames[i])) {
          // TODO: check column type
        } else {
          requestColumnCreate(metadataColumnNames[i], metadataColumnTypes[i],
            trackerTableID);
        }
      }

      // Checking for the existence of question columns on Fusion table.
      for (Question question : trackingQuestions) {
        checkQuestionColumn(columnNames, question, trackerTableID);

        // If it is a looped question, check its looped question id's as well.
        if (question.getType() == Question.QuestionType.LOOP) {
          for (Question loopQuestion : question.getLoopQuestions()) {
            checkQuestionColumn(columnNames, loopQuestion, trackerTableID);
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private HashSet<String> getColumnListNames(String tableID) throws IOException {
    // Returns the column list
    Fusiontables.Column.List columnRequest = IniconfigActivity.fusiontables
      .column().list(tableID);
    columnRequest.setKey(ProjectConfig.get().getApiKey());
    columnRequest.setMaxResults((long) 500);
    ColumnList columnList = columnRequest.execute();

    HashSet<String> output = new HashSet<String>();

    // Get column list names or types
    for (int i = 0; i < columnList.getItems().size(); i++) {
      output.add(columnList.getItems().get(i).getName());
    }

    Log.v("Number of columns", "" + output.size());
    return output;
  }

  private void checkQuestionColumn(HashSet<String> columnNames, Question question, String tableID) {
    // If it doesn't already exist, make the column.
    if (!columnNames.contains(question.getQuestionID())) {
      requestColumnCreate(question.getQuestionID(),
        QuestionUtil.getColumnTypeFromQuestionType(question.getType()), tableID);
    }
  }

  private void requestColumnCreate(String name, String type, String tableID) {
    Column newColumn = new Column();
    newColumn.setName(name);
    newColumn.setType(type);

    Fusiontables.Column.Insert columnRequest;
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

}
