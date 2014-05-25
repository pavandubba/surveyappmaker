package org.urbanlaunchpad.flocktracker.controllers;

import android.content.SharedPreferences;
import org.urbanlaunchpad.flocktracker.*;
import org.urbanlaunchpad.flocktracker.models.Statistics;

import java.util.Calendar;

public class StatisticsPageController {
  SurveyorActivity surveyorActivity;

  private SharedPreferences prefs;
  private Statistics statistics;

  public StatisticsPageController(SurveyorActivity surveyorActivity, Statistics statistics) {
    this.surveyorActivity = surveyorActivity;
    this.statistics = statistics;

    prefs = ProjectConfig.get().getSharedPreferences();
    // Load statistics from previous run-through
    statistics.setTotalDistanceBefore(prefs.getFloat("totalDistanceBefore", 0));
    statistics.setRidesCompleted(prefs.getInt("ridesCompleted", 0));
    statistics.setSurveysCompleted(prefs.getInt("surveysCompleted", 0));
  }

  public void startTrip() {
    statistics.setStartTripTime(Calendar.getInstance());
  }

  public void stopTrip() {
    statistics.setStartTripTime(null);
    statistics.setRidesCompleted(statistics.getRidesCompleted() + 1);
    statistics.setTotalDistanceBefore(statistics.getTotalDistanceBefore() + statistics.getTripDistance());
    statistics.setTripDistance(0);
  }

}
