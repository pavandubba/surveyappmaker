package org.urbanlaunchpad.flocktracker.views;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.fragments.QuestionManager;
import org.urbanlaunchpad.flocktracker.fragments.QuestionManager.*;

public class NavButtonsView extends LinearLayout implements NavButtonsManager {

  private View previousQuestionButton;
  private View nextQuestionButton;
  private View submitSurveyButton;

  private QuestionManager.QuestionActionListener listener;

  private DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
    @Override
    public void onClick(DialogInterface dialog, int which) {
      switch (which) {
        case DialogInterface.BUTTON_POSITIVE:
          // Yes button clicked
          Toast.makeText(getContext(), getResources().getString(R.string.submitting_survey), Toast.LENGTH_SHORT).show();
          listener.onSubmitButtonClicked();
          break;
        case DialogInterface.BUTTON_NEGATIVE:
          // No button clicked
          break;
      }
    }
  };

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

          // Show submitting dialog.
          AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
          builder.setMessage(getResources().getString(R.string.submit_survey_question))
              .setPositiveButton(getResources().getString(R.string.yes), dialogClickListener)
              .setNegativeButton(getResources().getString(R.string.no), dialogClickListener)
              .show();
        }
      }
    });
  }

  @Override
  public void setQuestionType(QuestionActionListener listener, QuestionType type) {
    this.listener = listener;

    switch (type) {
      case TRIP_FIRST:
        submitSurveyButton.setVisibility(GONE);
      case FIRST:
        previousQuestionButton.setVisibility(GONE);
        nextQuestionButton.setVisibility(VISIBLE);
        break;
      case TRIP_NORMAL:
        submitSurveyButton.setVisibility(GONE);
      case NORMAL:
        previousQuestionButton.setVisibility(VISIBLE);
        nextQuestionButton.setVisibility(VISIBLE);
        break;
      case LAST:
        previousQuestionButton.setVisibility(VISIBLE);
        nextQuestionButton.setVisibility(GONE);
        break;
    }
  }

  @Override
  public void cleanUp() {
    this.listener = null;
  }
}
