package org.urbanlaunchpad.flocktracker.models;

import android.location.Location;

public class Metadata {
  private String timeStamp;
  private String surveyID;
  private String tripID;
  private String imagePaths;
  private Location location;
  private int maleCount;
  private int femaleCount;
  private double speed;

  public String getTimeStamp() {
    return timeStamp;
  }

  public void setTimeStamp(String timeStamp) {
    this.timeStamp = timeStamp;
  }

  public String getSurveyID() {
    return surveyID;
  }

  public void setSurveyID(String surveyID) {
    this.surveyID = surveyID;
  }

  public String getTripID() {
    return tripID;
  }

  public void setTripID(String tripID) {
    this.tripID = tripID;
  }

  public String getImagePaths() {
    return imagePaths;
  }

  public void setImagePaths(String imagePaths) {
    this.imagePaths = imagePaths;
  }

  public Location getCurrentLocation() {
    return location;
  }

  public void setCurrentLocation(Location location) {
    this.location = location;
  }

  public int getMaleCount() {
    return maleCount;
  }

  public void setMaleCount(int maleCount) {
    this.maleCount = maleCount;
  }

  public int getFemaleCount() {
    return femaleCount;
  }

  public void setFemaleCount(int femaleCount) {
    this.femaleCount = femaleCount;
  }

  public double getSpeed() {
    return speed;
  }

  public void setSpeed(double speed) {
    this.speed = speed;
  }
}
