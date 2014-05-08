package org.urbanlaunchpad.flocktracker;

public class ProjectConfig {

  private static ProjectConfig applicationConfig;

  private String username;
  private String projectName;
  private String originalJSONSurveyString;

  // Project specific URL’s
  private String surveyDownloadTableID;
  private String surveyUploadTableID;
  private String trackerTableID;
  private String imageDirectoryURL;
  private String apiKey;

  // Survey Table Columns

  /**
   * Private constructor to ensure static instance.
   */
  private ProjectConfig() {
  }

  public static ProjectConfig get() {
    if (applicationConfig == null) {
      applicationConfig = new ProjectConfig();
    }
    return applicationConfig;
  }

  public String getSurveyUploadTableID() {
    return surveyUploadTableID;
  }

  public void setSurveyUploadTableID(String surveyUploadTableID) {
    this.surveyUploadTableID = surveyUploadTableID;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getSurveyDownloadTableID() {
    return surveyDownloadTableID;
  }

  public void setSurveyDownloadTableID(String surveyDownloadTableID) {
    this.surveyDownloadTableID = surveyDownloadTableID;
  }

  public String getTrackerTableID() {
    return trackerTableID;
  }

  public void setTrackerTableID(String trackerTableID) {
    this.trackerTableID = trackerTableID;
  }

  public String getImageDirectoryURL() {
    return imageDirectoryURL;
  }

  public void setImageDirectoryURL(String imageDirectoryURL) {
    this.imageDirectoryURL = imageDirectoryURL;
  }

  public String getOriginalJSONSurveyString() {
    return originalJSONSurveyString;
  }

  public void setOriginalJSONSurveyString(String originalJSONSurveyString) {
    this.originalJSONSurveyString = originalJSONSurveyString;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }
}
