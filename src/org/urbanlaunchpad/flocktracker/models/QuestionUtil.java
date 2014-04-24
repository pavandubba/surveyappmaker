package org.urbanlaunchpad.flocktracker.models;
import org.urbanlaunchpad.flocktracker.models.Question.QuestionType;

public class QuestionUtil {

    public static QuestionType getQuestionTypeFromString(String type) {
        if (type.equals("MC")) {
            return QuestionType.MULTIPLE_CHOICE;
        }

        if (type.equals("ON")) {
            return QuestionType.OPEN;
        }

        if (type.equals("IM")) {
            return QuestionType.IMAGE;
        }

        if (type.equals("CB")) {
            return QuestionType.CHECKBOX;
        }

        return null;
    }
}
