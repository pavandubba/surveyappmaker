package org.urbanlaunchpad.flocktracker.fragments;

public interface QuestionManager {

  /**
   * Enum to specify question type
   */
  enum QuestionType {
    FIRST, NORMAL, LAST, TRIP_FIRST, TRIP_NORMAL
  }

  /**
   * Listener for answer changes
   */
  interface QuestionActionListener {
    void onSelectedAnswer(String answer);

    void onPrevQuestionButtonClicked();

    void onNextQuestionButtonClicked();

    void onSubmitButtonClicked();
  }
}
