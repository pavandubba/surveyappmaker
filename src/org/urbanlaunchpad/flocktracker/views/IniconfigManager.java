package org.urbanlaunchpad.flocktracker.views;

import org.urbanlaunchpad.flocktracker.IniconfigListener;

public interface IniconfigManager {

    void setUsername(String username);
    void onSurveyParsedCorrectly();
    void onSurveyParsedIncorrectly();
    void initialize(IniconfigListener listener, String lastProjectName);
    void onParsingSurvey();
}
