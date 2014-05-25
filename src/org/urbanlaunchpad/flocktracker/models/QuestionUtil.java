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

    if (type.equals("LP")) {
      return QuestionType.LOOP;
    }

    return null;
  }

  public static String getColumnTypeFromQuestionType(QuestionType type) {
    switch (type) {
      case MULTIPLE_CHOICE:
        return "STRING";
      case OPEN:
        return "NUMBER";
      case IMAGE:
        return "STRING";
      case CHECKBOX:
        return "STRING";
      case ORDERED:
        return "STRING";
      case LOOP:
        return "STRING";
    }
    return null;
  }
}
