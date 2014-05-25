package org.urbanlaunchpad.flocktracker.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.fragments.QuestionManager.QuestionType;

public class NavButtonsView extends LinearLayout implements NavButtonsManager {

  private View previousQuestionButton;
  private View nextQuestionButton;
  private View submitSurveyButton;

  private NavButtonsListener listener;

  public NavButtonsView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    this.previousQuestionButton = findViewById(R.id.previous_question_button);
    this.nextQuestionButton = findViewById(R.id.next_question_button);
    this.submitSurveyButton = findViewById(R.id.submit_survey_button);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    previousQuestionButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (listener != null) {
          listener.onPrevQuestionButtonClicked();
        }
      }
    });
    nextQuestionButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (listener != null) {
          listener.onNextQuestionButtonClicked();
        }
      }
    });
    submitSurveyButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (listener != null) {
          listener.onSubmitButtonClicked();
        }
      }
    });
  }

  @Override
  public void setQuestionType(NavButtonsListener listener, QuestionType type) {
    this.listener = listener;

    switch (type) {
      case FIRST:
        previousQuestionButton.setVisibility(GONE);
        nextQuestionButton.setVisibility(VISIBLE);
      case NORMAL:
        previousQuestionButton.setVisibility(VISIBLE);
        nextQuestionButton.setVisibility(VISIBLE);
      case LAST:
        previousQuestionButton.setVisibility(VISIBLE);
        nextQuestionButton.setVisibility(GONE);
      case IS_WHOLE_CHAPTER:
        previousQuestionButton.setVisibility(GONE);
        nextQuestionButton.setVisibility(GONE);
    }
  }

  @Override
  public void cleanUp() {
    this.listener = null;
  }
}
