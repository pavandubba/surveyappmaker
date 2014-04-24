package org.urbanlaunchpad.flocktracker.models;

public class Chapter {
    private int chapterNumber;
    private String title;
    private Question[] questions;

    public int getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(int chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Question[] getQuestions() {
        return questions;
    }

    public void setQuestions(Question[] questions) {
        this.questions = questions;
    }

    public int getQuestionCount() {
        return questions.length;
    }
}
