package org.urbanlaunchpad.flocktracker.helpers;

import android.content.Context;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.urbanlaunchpad.flocktracker.ProjectConfig;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.SurveyorActivity;
import org.urbanlaunchpad.flocktracker.models.Chapter;
import org.urbanlaunchpad.flocktracker.models.Question;
import org.urbanlaunchpad.flocktracker.models.QuestionUtil;

public class JSONUtil {
  /**
   * De-serialize JSON information into Chapter and Question objects
   */
  public static Chapter[] parseChapters(Context context, JSONObject jsonSurvey) {
    try {
      JSONArray jsonChapterList = jsonSurvey.getJSONObject("Survey").getJSONArray("Chapters");
      Chapter[] chapterList = new Chapter[jsonChapterList.length()];

      // Parse chapters
      for (int i = 0; i < jsonChapterList.length(); ++i) {
        Chapter chapter = new Chapter();
        chapter.setChapterNumber(i);
        chapter.setTitle(jsonChapterList.getJSONObject(i).getString("Chapter"));

        JSONArray jsonQuestionList = jsonChapterList.getJSONObject(i).getJSONArray("Questions");

        Question[] questions = new Question[jsonQuestionList.length()];

        // Parse questions
        for (int j = 0; j < jsonQuestionList.length(); j++) {
          JSONObject jsonQuestion = jsonQuestionList.getJSONObject(j);
          questions[j] = new Question();
          questions[j].setQuestionID(jsonQuestion.getString("id"));
          questions[j].setType(QuestionUtil.getQuestionTypeFromString(jsonQuestion.getString("Kind")));

          if (jsonQuestion.has("Answers")) {
            JSONArray jsonAnswers = jsonQuestion.getJSONArray("Answers");
            String[] answers = new String[jsonAnswers.length()];
            for (int k = 0; k < answers.length; k++) {
              answers[k] = jsonAnswers.getJSONObject(k).getString("Answer");
            }
            questions[j].setAnswers(answers);
          }
        }

        chapterList[i] = chapter;
      }

      ProjectConfig.get().setSurveyUploadTableID(jsonSurvey.getJSONObject("Survey").getString("TableID"));
      ProjectConfig.get().setTrackerTableID(jsonSurvey.getJSONObject("Tracker").getString("TableID"));

      return chapterList;
    } catch (JSONException e) {
      e.printStackTrace();
      Toast.makeText(context, R.string.chapters_not_parsed,
        Toast.LENGTH_SHORT).show();
    }

    return null;
  }

  /**
   * De-serialize tracker questions into Question objects
   */
  public static Question[] parseTrackingQuestions(Context context, JSONObject jsonSurvey) {
    try {
      JSONArray jTrackerQuestions = jsonSurvey.getJSONObject("Tracker").getJSONArray("Questions");
      Question[] trackingQuestions = new Question[jTrackerQuestions.length()];

      // Create Question objects
      for (int i = 0; i < trackingQuestions.length; i++) {
        JSONObject jsonQuestion = jTrackerQuestions.getJSONObject(i);

        trackingQuestions[i] = new Question();
        trackingQuestions[i].setQuestionID(jsonQuestion.getString("id"));
        trackingQuestions[i].setType(QuestionUtil.getQuestionTypeFromString(jsonQuestion.getString("Kind")));

        if (jsonQuestion.has("Answers")) {
          JSONArray jsonAnswers = jsonQuestion.getJSONArray("Answers");
          String[] answers = new String[jsonAnswers.length()];
          for (int j = 0; j < answers.length; j++) {
            answers[j] = jsonAnswers.getJSONObject(j).getString("Answer");
          }
          trackingQuestions[i].setAnswers(answers);
        }
      }

      // Update trip table ID if any
      ProjectConfig.get().setTrackerTableID(jsonSurvey.getJSONObject("Tracker").getString("TableID"));

      return trackingQuestions;
    } catch (JSONException e2) {
      Toast.makeText(context, R.string.no_tracker_questions,
        Toast.LENGTH_SHORT).show();
      e2.printStackTrace();
    }

    return null;
  }
}
