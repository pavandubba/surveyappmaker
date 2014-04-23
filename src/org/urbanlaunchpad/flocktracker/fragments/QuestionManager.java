package org.urbanlaunchpad.flocktracker.fragments;

public interface QuestionManager {

    /**
     * Enum to specify question type
     */
    enum QuestionType { FIRST, NORMAL, LAST, IS_WHOLE_CHAPTER, TRIP_FIRST, TRIP_NORMAL, TRIP_LAST};

    /**
     * Listener for answer changes
     */
    interface QuestionAnswerListener {
        void onSelectedAnswer(String answer);
    }
}
