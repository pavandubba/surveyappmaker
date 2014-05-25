package org.urbanlaunchpad.flocktracker.helpers;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.urbanlaunchpad.flocktracker.ProjectConfig;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.models.Submission;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.LinkedList;

public class SubmissionHelper {
  private LinkedList<Submission> surveySubmissionQueue;
  private LinkedList<Submission> trackerSubmissionQueue;
  private boolean savingTrackerSubmission = false;
  private boolean savingSurveySubmission = false;
  private boolean submittingSubmission = false;

  private SharedPreferences prefs;

  public SubmissionHelper() {
    prefs = ProjectConfig.get().getSharedPreferences();

    // Load saved submission queues.
    String surveySubmissionQueueGson = prefs.getString("surveySubmissionQueue", null);
    if (surveySubmissionQueueGson == null) {
      surveySubmissionQueue = new LinkedList<Submission>();
    } else {
      Type listType = new TypeToken<LinkedList<Submission>>() {
      }.getType();
      surveySubmissionQueue = new Gson().fromJson(surveySubmissionQueueGson, listType);
    }

    String trackerSubmissionQueueGson = prefs.getString("trackerSubmissionQueue", null);
    if (trackerSubmissionQueueGson == null) {
      trackerSubmissionQueue = new LinkedList<Submission>();
    } else {
      Type listType = new TypeToken<LinkedList<Submission>>() {
      }.getType();
      trackerSubmissionQueue = new Gson().fromJson(trackerSubmissionQueueGson, listType);
    }

    if (!surveySubmissionQueue.isEmpty() || !trackerSubmissionQueue.isEmpty()) {
      spawnSubmission();
    }
  }

  public void saveSubmission(Submission submission) {
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

    if (!submittingSubmission) {
      spawnSubmission();
    }
  }

  private void spawnSubmission() {
    new Thread(new Runnable() {
      public void run() {
        while (true) {
          synchronized (trackerSubmissionQueue) {
            if (trackerSubmissionQueue.isEmpty()) {
              submittingSubmission = false;
            } else {
              submittingSubmission = true;
            }
          }

          if (!submittingSubmission) {
            synchronized (surveySubmissionQueue) {
              if (surveySubmissionQueue.isEmpty()) {
              } else {
                submittingSubmission = true;
              }
            }
          }

          if (!submittingSubmission) {
            Log.d("Spawn submission queue", "Queue is empty");
            break;
          }

          // Iterate through queues to submit surveys
          for (Iterator<Submission> i = surveySubmissionQueue.iterator(); i.hasNext(); ) {
            Submission submission = i.next();
            if (submission.submit()) {
              i.remove();
              prefs.edit().putString("surveySubmissionQueue", new Gson().toJson(surveySubmissionQueue)).commit();
            } else { // Failed. Try again
              try {
                Thread.sleep(3000);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            }
          }

          // Iterate through queues to submit tracker updates
          for (Iterator<Submission> i = trackerSubmissionQueue.iterator(); i.hasNext(); ) {
            Submission submission = i.next();
            if (submission.submit()) {
              i.remove();
              prefs.edit().putString("trackerSubmissionQueue", new Gson().toJson(trackerSubmissionQueue)).commit();
            } else { // Failed. Try again
              try {
                Thread.sleep(3000);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            }
          }
        }
      }
    }).start();
  }
}
