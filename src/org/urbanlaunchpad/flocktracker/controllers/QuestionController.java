package org.urbanlaunchpad.flocktracker.controllers;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import org.urbanlaunchpad.flocktracker.ProjectConfig;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.fragments.*;
import org.urbanlaunchpad.flocktracker.fragments.QuestionManager.QuestionAnswerListener;
import org.urbanlaunchpad.flocktracker.helpers.ColumnCheckHelper;
import org.urbanlaunchpad.flocktracker.helpers.JSONUtil;
import org.urbanlaunchpad.flocktracker.helpers.SubmissionHelper;
import org.urbanlaunchpad.flocktracker.models.Chapter;
import org.urbanlaunchpad.flocktracker.models.Metadata;
import org.urbanlaunchpad.flocktracker.models.Question;
import org.urbanlaunchpad.flocktracker.models.Submission;
import org.urbanlaunchpad.flocktracker.views.NavButtonsManager.NavButtonsListener;

public class QuestionController implements QuestionAnswerListener, NavButtonsListener {
  private Context context;
  private Metadata metadata;
  private FragmentManager fragmentManager;
  private SubmissionHelper submissionHelper;

  private int chapterPosition = 0;
  private int questionPosition = 0;
  private int trackerQuestionPosition = 0;
  private Chapter[] chapterList;
  private Question[] trackingQuestions;

  private boolean inLoop = false; // Toggle that turns on if the survey gets into a loop.
  private int loopPosition = -1; // Position in the questions array in the loop the survey is in.
  private int loopIteration = -1; // Iteration step where the loop process is.

  private boolean isAskingTripQuestions = false;

  public QuestionController(Context context, Metadata metadata, FragmentManager fragmentManager,
      SubmissionHelper submissionHelper) {
    this.context = context;
    this.metadata = metadata;
    this.fragmentManager = fragmentManager;
    this.submissionHelper = submissionHelper;
    resetSurvey();
    resetTrip();

    // Do column checks.
    new Thread(new Runnable() {
      @Override
      public void run() {
        new ColumnCheckHelper(chapterList, trackingQuestions).runChecks();
      }
    }).run();
  }

  public void startTrip() {
    trackerQuestionPosition = 0;
    isAskingTripQuestions = true;
    showCurrentQuestion();
  }

  public void showCurrentQuestion() {
    Question currentQuestion = getCurrentQuestion();
    QuestionFragment fragment = null;
    switch (currentQuestion.getType()) {
      case MULTIPLE_CHOICE:
        fragment = new MultipleChoiceQuestionFragment(currentQuestion);
        break;
      case OPEN:
        fragment = new OpenQuestionFragment(currentQuestion);
        break;
      case IMAGE:
        fragment = new ImageQuestionFragment(currentQuestion);
        break;
      case CHECKBOX:
        fragment = new CheckBoxQuestionFragment(currentQuestion);
        break;
      case ORDERED:
        fragment = new OrderedListQuestionFragment(currentQuestion);
        break;
      case LOOP:
        break;
    }

    FragmentTransaction transaction = fragmentManager.beginTransaction();
    transaction.replace(R.id.surveyor_frame, fragment);
    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
    transaction.addToBackStack(null);
    transaction.commit();
  }

  private Question getCurrentQuestion() {
    Question currentQuestion;
    if (isAskingTripQuestions) {
      if (inLoop) {
        currentQuestion = trackingQuestions[trackerQuestionPosition].getLoopQuestions()[loopPosition];
      } else {
        currentQuestion = trackingQuestions[trackerQuestionPosition];
      }
    } else {
      if (inLoop) {
        currentQuestion = chapterList[chapterPosition].getQuestions()[questionPosition].getLoopQuestions()[loopPosition];
      } else {
        currentQuestion = chapterList[chapterPosition].getQuestions()[questionPosition];
      }
    }
    return currentQuestion;
  }

  @Override
  public void onSelectedAnswer(String answer) {
    // TODO(adchia): implement
  }

  @Override
  public void onPrevQuestionButtonClicked() {
    if (isAskingTripQuestions) {
      trackerQuestionPosition--;
      showCurrentQuestion();
    } else {
      if (questionPosition == 0) {
        chapterPosition--;
        questionPosition = chapterList[chapterPosition].getQuestionCount() - 1;
        showCurrentQuestion();
      } else {
        questionPosition--;
        showCurrentQuestion();
      }
    }
  }

  @Override
  public void onNextQuestionButtonClicked() {
    if (isAskingTripQuestions) {
      if (trackerQuestionPosition == trackingQuestions.length - 1) {
        // show hub page and start tracking
      } else {
        trackerQuestionPosition++;
        showCurrentQuestion();
      }
    } else {
      if (questionPosition == chapterList[chapterPosition].getQuestionCount() - 1) {
        chapterPosition++;
        questionPosition = 0;
        showCurrentQuestion();
      } else {
        questionPosition++;
        showCurrentQuestion();
      }
    }
  }

  @Override
  public void onSubmitButtonClicked() {
    new Thread(new Runnable() {
      public void run() {
        Submission submission = new Submission();
        submission.setChapters(chapterList);
        submission.setType(Submission.Type.SURVEY);
        submission.setMetadata(metadata);
        submissionHelper.saveSubmission(submission);
      }
    }).start();
  }

  public void updateSurveyPosition(int chapterPosition, int questionPosition) {
    this.chapterPosition = chapterPosition;
    this.questionPosition = questionPosition;
    this.inLoop = false;
  }

  // TODO(adchia): move into a view
  public String[] getChapterTitles() {
    String[] chapterTitles = new String[chapterList.length];
    for (int i = 0; i < chapterTitles.length; i++) {
      chapterTitles[i] = chapterList[i].getTitle();
    }
    return chapterTitles;
  }

  private void resetSurvey() {
    JSONObject surveyJSONObject = null;

    // parse json survey
    try {
      surveyJSONObject = new JSONObject(ProjectConfig.get().getOriginalJSONSurveyString());
    } catch (JSONException e) {
      Toast.makeText(context, R.string.json_format_error, Toast.LENGTH_SHORT).show();
      e.printStackTrace();
    }

    // Parse survey information.
    chapterList = JSONUtil.parseChapters(context, surveyJSONObject);
    chapterPosition = 0;
    questionPosition = 0;
  }

  private void resetTrip() {
    JSONObject surveyJSONObject = null;

    // parse json survey
    try {
      surveyJSONObject = new JSONObject(ProjectConfig.get().getOriginalJSONSurveyString());
    } catch (JSONException e) {
      Toast.makeText(context, R.string.json_format_error, Toast.LENGTH_SHORT).show();
      e.printStackTrace();
    }

    // Tracking information.
    trackingQuestions = JSONUtil.parseTrackingQuestions(context, surveyJSONObject);
    trackerQuestionPosition = 0;
  }
}
