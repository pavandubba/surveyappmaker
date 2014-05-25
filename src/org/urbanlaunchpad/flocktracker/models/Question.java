package org.urbanlaunchpad.flocktracker.models;

import android.net.Uri;

import java.util.HashSet;
import java.util.Set;

public class Question {

  private QuestionType type;

  private Chapter chapter;
  private Question[] loopQuestions;

  private int questionNumber;
  private String questionText;
  private static String[] answers;
  private String questionID;
  private boolean otherEnabled;

  // Loop related variables
  private boolean inLoop;
  private int loopTotal;
  private int loopIteration;
  private int loopPosition;

  // Image
  private Uri image;

  // Selected Answers
  private Set<String> selectedAnswers = new HashSet<String>();

  public QuestionType getType() {
    return type;
  }

  public void setType(QuestionType type) {
    this.type = type;
  }

  public Chapter getChapter() {
    return chapter;
  }

  public void setChapter(Chapter chapter) {
    this.chapter = chapter;
  }

  public String getQuestionText() {
    return questionText;
  }

  public void setQuestionText(String questionText) {
    this.questionText = questionText;
  }

  public String[] getAnswers() {
    return answers;
  }

  public void setAnswers(String[] answers) {
    this.answers = answers;
  }

  public String getQuestionID() {
    return questionID;
  }

  public void setQuestionID(String questionID) {
    this.questionID = questionID;
  }

  public boolean isOtherEnabled() {
    return otherEnabled;
  }

  public void setOtherEnabled(boolean otherEnabled) {
    this.otherEnabled = otherEnabled;
  }

  public boolean isInLoop() {
    return inLoop;
  }

  public void setInLoop(boolean inLoop) {
    this.inLoop = inLoop;
  }

  public int getLoopTotal() {
    return loopTotal;
  }

  public void setLoopTotal(int loopTotal) {
    this.loopTotal = loopTotal;
  }

  public int getLoopIteration() {
    return loopIteration;
  }

  public void setLoopIteration(int loopIteration) {
    this.loopIteration = loopIteration;
  }

  public int getLoopPosition() {
    return loopPosition;
  }

  public void setLoopPosition(int loopPosition) {
    this.loopPosition = loopPosition;
  }

  public Set<String> getSelectedAnswers() {
    return selectedAnswers;
  }

  public void setSelectedAnswers(Set<String> selectedAnswers) {
    this.selectedAnswers = selectedAnswers;
  }

  public Uri getImage() {
    return image;
  }

  public void setImage(Uri image) {
    this.image = image;
  }

  public int getQuestionNumber() {
    return questionNumber;
  }

  public void setQuestionNumber(int questionNumber) {
    this.questionNumber = questionNumber;
  }

  public Question[] getLoopQuestions() {
    return loopQuestions;
  }

  public void setLoopQuestions(Question[] loopQuestions) {
    this.loopQuestions = loopQuestions;
  }

  public enum QuestionType {MULTIPLE_CHOICE, OPEN, IMAGE, CHECKBOX, ORDERED, LOOP}
}
