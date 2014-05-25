package org.urbanlaunchpad.flocktracker.controllers;

import com.google.android.gms.location.LocationClient;
import org.urbanlaunchpad.flocktracker.helpers.SubmissionHelper;
import org.urbanlaunchpad.flocktracker.models.Metadata;
import org.urbanlaunchpad.flocktracker.models.Submission;

public class TrackerController {
  private Metadata metadata;
  private SubmissionHelper submissionHelper;
  private LocationClient locationClient;

  public TrackerController(Metadata metadata, SubmissionHelper submissionHelper, LocationClient locationClient) {
    this.metadata = metadata;
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
      submission.setMetadata(metadata);
      submissionHelper.saveSubmission(submission);
    }
  }
}
