package org.urbanlaunchpad.flocktracker.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.StateListDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.SurveyorActivity;
import org.urbanlaunchpad.flocktracker.adapters.StableArrayAdapter;
import org.urbanlaunchpad.flocktracker.helpers.ImageHelper;
import org.urbanlaunchpad.flocktracker.helpers.SurveyHelper;
import org.urbanlaunchpad.flocktracker.menu.DynamicListView;
import org.urbanlaunchpad.flocktracker.models.Question;
import org.urbanlaunchpad.flocktracker.views.NavButtonsManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

public abstract class QuestionFragment extends Fragment implements
		QuestionManager {

	private QuestionActionListener listener;
	private NavButtonsManager navButtonsManager;

  private Question question;
  private QuestionType questionType;

	public QuestionFragment(QuestionActionListener listener, Question question, QuestionType questionType) {
    this.listener = listener;
    this.question = question;
    this.questionType = questionType;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_question, container, false);

    navButtonsManager = (NavButtonsManager) rootView.findViewById(R.id.questionButtons);
    navButtonsManager.setQuestionType(listener, questionType);

    setupLayout();
    prepopulateQuestion();

		return rootView;
	}

	abstract void setupLayout();
	abstract void prepopulateQuestion();

  protected Question getQuestion() {
    return question;
  }

  protected QuestionType getQuestionType() {
    return questionType;
  }

  protected QuestionActionListener getListener() {
    return listener;
  }

  protected LayoutInflater getInflater() {
    return (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  }
}