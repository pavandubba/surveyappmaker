package org.urbanlaunchpad.flocktracker.helpers;

import android.content.SharedPreferences;
import com.google.gson.Gson;
import org.urbanlaunchpad.flocktracker.ProjectConfig;
import org.urbanlaunchpad.flocktracker.models.Submission;

import java.util.LinkedList;

public class SubmissionHelper {
  private LinkedList<Submission> surveySubmissionQueue = new LinkedList<Submission>();
  private LinkedList<Submission> trackerSubmissionQueue = new LinkedList<Submission>();
  private boolean savingTrackerSubmission = false;
  private boolean savingSurveySubmission = false;

  public void saveSubmission(Submission submission) {
    SharedPreferences prefs = ProjectConfig.get().getSharedPreferences();
    if (submission.getType().equals(Submission.Type.TRACKER)) {
      savingTrackerSubmission = true;
      synchronized (trackerSubmissionQueue) {
        trackerSubmissionQueue.add(submission);
        prefs.edit().putString("trackerSubmissionQueue", new Gson().toJson(trackerSubmissionQueue)).commit();
        savingTrackerSubmission = false;
        trackerSubmissionQueue.notify();
      }
    } else if (submission.getType().equals(Submission.Type.SURVEY)) {
      savingSurveySubmission = true;
      synchronized (surveySubmissionQueue) {
        surveySubmissionQueue.add(submission);
        prefs.edit().putString("surveySubmissionQueue", new Gson().toJson(surveySubmissionQueue)).commit();
        savingSurveySubmission = false;
        surveySubmissionQueue.notify();
      }
    }
  }
}
