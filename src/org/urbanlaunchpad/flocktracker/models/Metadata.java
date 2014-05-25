package org.urbanlaunchpad.flocktracker.models;

public class Metadata {
  private String timeStamp;
  private String surveyID;
  private String tripID;
  private String imagePaths;
  private double latitude;
  private double longitude;
  private double altitude;
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

  public double getLatitude() {
    return latitude;
  }

  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  public double getAltitude() {
    return altitude;
  }

  public void setAltitude(double altitude) {
    this.altitude = altitude;
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
