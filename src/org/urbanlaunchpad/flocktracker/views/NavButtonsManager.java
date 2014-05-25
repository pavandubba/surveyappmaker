package org.urbanlaunchpad.flocktracker.views;

import org.urbanlaunchpad.flocktracker.fragments.QuestionManager.QuestionType;

public interface NavButtonsManager {

  /**
   * Used to initialize nav buttons view based on whether it's first, middle, last, or
   * the only question in a chapter.
   *
   * @param type
   */
  void setQuestionType(NavButtonsListener listener, QuestionType type);

  /**
   * Used to clear up listeners
   */
  void cleanUp();

  /**
   * Interface for listener to handle user actions
   */
  public interface NavButtonsListener {
    void onPrevQuestionButtonClicked();

    void onNextQuestionButtonClicked();

    void onSubmitButtonClicked();
  }
}
