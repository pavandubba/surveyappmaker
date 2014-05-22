package org.urbanlaunchpad.flocktracker.helpers;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.urbanlaunchpad.flocktracker.IniconfigActivity;
import org.urbanlaunchpad.flocktracker.ProjectConfig;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.SurveyorActivity;
import org.urbanlaunchpad.flocktracker.models.Chapter;
import org.urbanlaunchpad.flocktracker.models.Question;
import org.urbanlaunchpad.flocktracker.models.Submission;
import org.urbanlaunchpad.flocktracker.models.Submission.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.Stack;

public class SurveyHelper {

  // Constants
  public static final Integer HUB_PAGE_CHAPTER_POSITION = -15;
  public static final Integer HUB_PAGE_QUESTION_POSITION = -15;
  public static final Integer STATS_PAGE_CHAPTER_POSITION = -16;
  public static final Integer STATS_PAGE_QUESTION_POSITION = -16;
  // Backstack
  public Stack<Tuple> prevPositions = new Stack<Tuple>();
  public Stack<Integer> prevTrackingPositions = new Stack<Integer>();
  public Integer prevQuestionPosition = null;
  public String jumpString = null;
  // Loop stuff
  public Integer loopTotal = null; // Number of times loop questions repeat.
  public Boolean inLoop = false; // Toggle that turns on if the survey gets into a loop.
  public Integer loopPosition = -1; // Position in the questions array in the loop the survey is in.
  public Integer loopIteration = -1; // Iteration step where the loop process is.
  public Integer loopLimit = 0; // Total number of questions in the loop being asked.
  private Integer chapterPosition = HUB_PAGE_CHAPTER_POSITION;
  private Integer questionPosition = HUB_PAGE_QUESTION_POSITION;
  private Integer trackerQuestionPosition = 0;
  private Integer[] jumpPosition = null;
  private Chapter[] chapterList;
  private Question[] trackingQuestions;

  private Context context;

  public SurveyHelper(Context context) {
    this.context = context;
    resetSurvey();

    // Checking existence of columns in the Fusion Tables.
    new Thread(new Runnable() {
      @Override
      public void run() {
        new ColumnCheckManager(chapterList, trackingQuestions).runChecks();
      }
    }).run();
  }

  public void saveSubmission(Submission submission) {
    if (submission.getType().equals(Type.TRACKER)) {
      synchronized (SurveyorActivity.trackerSubmissionQueue) {
        SurveyorActivity.trackerSubmissionQueue.add(new Gson().toJson(submission));
        IniconfigActivity.prefs.edit().putStringSet("trackerSubmissionQueue",
          (Set<String>) SurveyorActivity.trackerSubmissionQueue.clone()).commit();
        SurveyorActivity.savingTrackerSubmission = false;
        SurveyorActivity.trackerSubmissionQueue.notify();
      }
    } else if (submission.getType().equals(Type.SURVEY)) {
      synchronized (SurveyorActivity.surveySubmissionQueue) {
        SurveyorActivity.surveySubmissionQueue.add(new Gson().toJson(submission));
        IniconfigActivity.prefs.edit().putStringSet("surveySubmissionQueue",
          (Set<String>) SurveyorActivity.surveySubmissionQueue.clone()).commit();
        SurveyorActivity.savingSurveySubmission = false;
        SurveyorActivity.surveySubmissionQueue.notify();
      }
    }
  }

    /*
     * Submission Update Functions
     */

  public void answerCurrentQuestion(Set<String> selectedAnswers) {
    if (chapterPosition < 0) {
      return;
    }

    Question currentQuestion = chapterList[chapterPosition].getQuestions()[questionPosition];
    if (!inLoop) {
      if (chapterPosition >= 0) {
        currentQuestion.setSelectedAnswers(selectedAnswers);
      }
    } else {
      currentQuestion.getLoopQuestions()[loopPosition].setSelectedAnswers(selectedAnswers);
    }
  }

  public void answerCurrentTrackerQuestion(Set<String> selectedAnswers) {
    if (trackerQuestionPosition < 0) {
      return;
    }

    Question currentQuestion = trackingQuestions[trackerQuestionPosition];

    if (!inLoop) {
      currentQuestion.setSelectedAnswers(selectedAnswers);
    } else {
      currentQuestion.getLoopQuestions()[loopPosition].setSelectedAnswers(selectedAnswers);
    }
  }

  public void resetSurvey() {
    JSONObject surveyJSONObject = null;

    // parse json survey
    try {
      surveyJSONObject = new JSONObject(ProjectConfig.get().getOriginalJSONSurveyString());
    } catch (JSONException e) {
      Toast.makeText(context, R.string.json_format_error, Toast.LENGTH_SHORT).show();
      e.printStackTrace();
    }

    // Parse survey and tracking information.
    chapterList = JSONUtil.parseChapters(context, surveyJSONObject);
    chapterPosition = HUB_PAGE_CHAPTER_POSITION;
    questionPosition = HUB_PAGE_QUESTION_POSITION;
    prevPositions = new Stack<Tuple>();
  }

  public void resetTracker() {
    JSONObject trackerJSONObject = null;
    JSONObject surveyJSONObject;

    // parse json survey
    try {
      surveyJSONObject = new JSONObject(ProjectConfig.get().getOriginalJSONSurveyString());
      trackerJSONObject = surveyJSONObject.getJSONObject(SurveyorActivity.TRACKER_TYPE);
    } catch (JSONException e) {
      Toast.makeText(context, R.string.json_format_error, Toast.LENGTH_SHORT).show();
      e.printStackTrace();
    }

    // Parse survey and tracking information.
    trackingQuestions = JSONUtil.parseTrackingQuestions(context, trackerJSONObject);
    trackerQuestionPosition = 0;
    prevTrackingPositions = new Stack<Integer>();
  }

  public void updateSurveyPosition(int chapterPosition, int questionPosition) {
    prevPositions.add(new Tuple(this.chapterPosition, this.questionPosition));
    updateSurveyPositionOnBack(chapterPosition, questionPosition);
  }

  public void updateSurveyPositionOnBack(int chapterPosition, int questionPosition) {
    this.chapterPosition = chapterPosition;
    this.questionPosition = questionPosition;
  }

  public void updateTrackerPosition(int trackerQuestionPosition) {
    prevTrackingPositions.add(trackerQuestionPosition);
    updateTrackerPositionOnBack(trackerQuestionPosition);
  }

  public void updateTrackerPositionOnBack(int trackerQuestionPosition) {
    this.trackerQuestionPosition = trackerQuestionPosition;
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

  public void onPrevQuestionPressed() {
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
          trackerQuestionPosition++;
          loopPosition = -1;
          loopIteration = -1;
          inLoop = false;
          if (trackerQuestionPosition == trackingQuestions.length) {
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
          if (questionPosition == chapterList[chapterPosition].getQuestionCount()) {
            if (chapterPosition == chapterList.length - 1) {
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
      prevTrackingPositions.add(trackerQuestionPosition);
      trackerQuestionPosition++;
      if (trackerQuestionPosition == jTrackerQuestions.length()) {
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
      prevTrackingPositions.add(trackerQuestionPosition);
      if (loopTotal == 0) {
        trackerQuestionPosition++;
        if (trackerQuestionPosition == jTrackerQuestions.length()) {
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
      jumpPosition = findIDPosition(jumpString);
      chapterPosition = jumpPosition[0];
      questionPosition = jumpPosition[1];
      jumpString = null;
      jumpPosition = null;
      return NextQuestionResult.JUMPSTRING;
    }

    return NextQuestionResult.NORMAL;
  }

  public Question findQuestionByID(String id) {
    // Searches for a question with the same id as the jumpString value
    for (Chapter chapter : chapterList) {
      for (Question question : chapter.getQuestions()) {
        if (question.getQuestionID().equals(id)) {
          return question;
        }
      }
    }
    return null;
  }

  public Integer getChapterPosition() {
    return chapterPosition;
  }

  public Integer getQuestionPosition() {
    return questionPosition;
  }

  public Integer getTrackerQuestionPosition() {
    return trackerQuestionPosition;
  }

  public Integer getLoopPosition() {
    return loopPosition;
  }

  public Question getCurrentQuestion() {
    Question currentQuestion;
    if (inLoop) {
      currentQuestion = chapterList[chapterPosition].getQuestions()[questionPosition].getLoopQuestions()[loopPosition];
      Log.v("Loop length", loopLimit.toString());
    } else {
      currentQuestion = chapterList[chapterPosition].getQuestions()[questionPosition];
    }
    return currentQuestion;
  }

  public Question getCurrentTripQuestion() {
    Question currentQuestion;
    if (inLoop) {
      currentQuestion = trackingQuestions[trackerQuestionPosition].getLoopQuestions()[loopPosition];
      loopLimit = trackingQuestions[trackerQuestionPosition].getLoopQuestions().length;
      Log.v("Loop length", loopLimit.toString());
    } else {
      currentQuestion = trackingQuestions[trackerQuestionPosition];
    }

    return currentQuestion;
  }

  public int getTripQuestionCount() {
    return trackingQuestions.length;
  }

  public String[] getChapterTitles() {
    String[] chapterTitles = new String[chapterList.length];
    for (int i = 0; i < chapterList.length; i++) {
      chapterTitles[i] = chapterList[i].getTitle();
    }
    return chapterTitles;
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
    clearLoopAnswerHashMap(chapterPosition, questionPosition,
      SurveyorActivity.askingTripQuestions);
    // Clearing Loop answers arrays
    for (int i = 0; i < loopLimit; ++i) {
      if (!SurveyorActivity.askingTripQuestions) {
        try {
          chapterList[chapterPosition].getQuestions()[questionPosition]
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
          chapterList[chapterPosition].getQuestions()[questionPosition]
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
    Integer loopTotalInteger = getLoopTotal(chapterPosition,
      questionPosition, askingTripQuestions);
    Integer loopLimitInteger = getLoopLimit(chapterPosition,
      questionPosition, askingTripQuestions);

    for (int i = 0; i < loopLimitInteger; ++i) {
      if (!SurveyorActivity.askingTripQuestions) {
        for (int j = 0; j < loopTotalInteger; ++j) {
          ArrayList<Integer> key = new ArrayList<Integer>(
            Arrays.asList(chapterPosition, questionPosition, i,
              j)
          );
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

  public enum NextQuestionResult {
    NORMAL, CHAPTER_END, END, JUMPSTRING
  }

  public static class Tuple {

    // TODO Change this class to accept loop variables (Quaduple class
    // already there)
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
}
