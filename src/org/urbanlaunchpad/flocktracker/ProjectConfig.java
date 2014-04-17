package org.urbanlaunchpad.flocktracker;

import org.json.JSONObject;

public class ProjectConfig {

    private static ProjectConfig applicationConfig;

    private String username;
    private String projectName;
    private String originalJSONSurveyString;

    // Project specific URL’s
    private String surveyTableID;
    private String trackerTableID;
    private String imageDirectoryURL;
    private String apiKey;

    /**
     * Private constructor to ensure static instance.
     */
    private ProjectConfig() {};

    public static ProjectConfig get() {
        if (applicationConfig == null) {
            applicationConfig = new ProjectConfig();
        }
        return applicationConfig;
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

    public String getSurveyTableID() {
        return surveyTableID;
    }

    public void setSurveyTableID(String surveyTableID) {
        this.surveyTableID = surveyTableID;
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
