package org.urbanlaunchpad.flocktracker.controllers;

import android.content.Context;
import android.util.Log;
import com.google.android.gms.location.LocationClient;
import org.urbanlaunchpad.flocktracker.ProjectConfig;
import org.urbanlaunchpad.flocktracker.TrackerAlarm;
import org.urbanlaunchpad.flocktracker.helpers.SubmissionHelper;
import org.urbanlaunchpad.flocktracker.models.Submission;

/**
 * Created by adchia on 5/25/14.
 */
public class TrackerController {
  private Context context;
  private SubmissionHelper submissionHelper;
  private LocationClient locationClient;

  public TrackerController(Context context, SubmissionHelper submissionHelper, LocationClient locationClient) {
    this.submissionHelper = submissionHelper;
  }

  public void saveLocation() {
    // connect if not tracking
    Log.d("Save Location", "" + locationClient.isConnected());
    if (!locationClient.isConnected()) {
      locationClient.connect();
    }

    if (locationClient.isConnected()) {
      Submission submission = new Submission();
      submission.setType(Submission.Type.TRACKER);


      submission.setMetadata(ProjectConfig.get().getMetadata());
//			new Thread(new Runnable() {
//				public void run() {
//					try {
//						surveyHelper.jsurv.put("TrackerAlarm",
//								surveyHelper.jtracker);
//					} catch (JSONException e1) {
//						e1.printStackTrace();
//					}
//
//					String jsurvString = surveyHelper.jsurv.toString();
//					JSONObject imagePaths = new JSONObject();
//					for (ArrayList<Integer> key : SurveyHelper.prevTrackerImages
//							.keySet()) {
//						try {
//							imagePaths.put(key.toString(),
//									SurveyHelper.prevTrackerImages.get(key)
//											.getPath());
//						} catch (JSONException e) {
//							e.printStackTrace();
//						}
//					}
//
//					// save location tagged survey
//					surveyHelper.saveSubmission(statusPageHelper.startLocation,
//							surveyID, tripID, jsurvString, imagePaths,
//							TRACKER_TYPE, maleCount,
//							femaleCount,
//							maleCount + femaleCount,
//							statusPageHelper.getSpeed());
//				}
//			}).start();
    }
  }
}
