package org.urbanlaunchpad.flocktracker.controllers;

import org.urbanlaunchpad.flocktracker.fragments.HubPageManager;
import org.urbanlaunchpad.flocktracker.models.Metadata;

public class HubPageController implements HubPageManager.HubPageActionListener {
  private Metadata metadata;
  private QuestionController questionController;

  public HubPageController(Metadata metadata, QuestionController questionController) {
    this.metadata = metadata;
    this.questionController = questionController;
  }

  @Override
  public void onStartTrip() {
    metadata.setTripID("T" + createID());
    questionController.startTrip();
  }

  @Override
  public void onStartSurvey() {
    // TODO(adchia): implement
  }

  @Override
  public void onGetStatistics() {
    //TODO(adchia): implement
  }

  @Override
  public void onMaleCountChanged(int maleCount) {
    metadata.setMaleCount(maleCount);
  }

  @Override
  public void onFemaleCountChanged(int femaleCount) {
    metadata.setFemaleCount(femaleCount);
  }

  public String createID() {
    String ID = null;
    Integer randy;
    for (int i = 0; i < 7; ++i) {
      randy = (int) (Math.random() * ((9) + 1));
      if (i == 0) {
        ID = randy.toString();
      } else {
        ID = ID + randy.toString();
      }
    }

    return ID;
  }
}
