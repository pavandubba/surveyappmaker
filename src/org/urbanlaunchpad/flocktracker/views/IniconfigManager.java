package org.urbanlaunchpad.flocktracker.views;

public interface IniconfigManager {

  void setUsername(String username);

  void onSurveyParsedCorrectly();

  void onSurveyParsedIncorrectly();

  void initialize(IniconfigListener listener, String lastProjectName);

  void onParsingSurvey();

  /**
   * Interface for listener to handle user actions
   */
  public interface IniconfigListener {
    void onProjectNameInput(String projectName);

    void displayUsernameSelection();

    void onContinue();
  }
}
