package org.urbanlaunchpad.flocktracker.fragments;

public interface HubPageManager {
  public interface HubPageActionListener {
    void onStartTrip();
    void onStartSurvey();
    void onGetStatistics();
    void onClickedMoreMen();
    void onClickedMoreWomen();
    void onClickedFewerMen();
    void onClickedFewerWomen();
  }
}
