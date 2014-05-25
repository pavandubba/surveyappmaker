package org.urbanlaunchpad.flocktracker.controllers;

import com.google.android.gms.location.LocationClient;
import org.urbanlaunchpad.flocktracker.ProjectConfig;
import org.urbanlaunchpad.flocktracker.helpers.SubmissionHelper;
import org.urbanlaunchpad.flocktracker.models.Submission;

public class TrackerController {
  private SubmissionHelper submissionHelper;
  private LocationClient locationClient;

  public TrackerController(SubmissionHelper submissionHelper, LocationClient locationClient) {
    this.submissionHelper = submissionHelper;
    this.locationClient = locationClient;
  }

  public void saveLocation() {
    if (!locationClient.isConnected()) {
      locationClient.connect();
    }

    if (locationClient.isConnected()) {
      Submission submission = new Submission();
      submission.setType(Submission.Type.TRACKER);
      submission.setMetadata(ProjectConfig.get().getMetadata());
      submissionHelper.saveSubmission(submission);
    }
  }
}
