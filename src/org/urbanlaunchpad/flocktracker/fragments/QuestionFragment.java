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

	QuestionAnswerListener listener;
  NavButtonsManager.NavButtonsListener navButtonsListener;

	public static final String ARG_JSON_QUESTION = "Json question";
	public static final String ARG_QUESTION_POSITION = "Question position";
	public static final String ARG_CHAPTER_POSITION = "Chapter position";
	public static final String ARG_POSITION = "Position";
	public static final String ARG_IN_LOOP = "In loop";
	public static final String ARG_LOOP_ITERATION = "Loop iteration";
	public static final String ARG_LOOP_TOTAL = "Loop total";
	public static final String ARG_LOOP_POSITION = "Loop position";
	private static String[] answerlist = null;
	private static Activity mainActivity;
	LinearLayout orderanswerlayout;
	CheckBox otherCB;
	DynamicListView answerlistView;
	// Passes Answer and jump information to activity.
	AnswerSelected Callback;
	private Toast toast;
	private String jquestionstring;
	protected JSONObject jquestion = null;
	private JSONArray janswerlist = null;
	private String questionstring = "No questions on chapter";
	private Integer totalanswers;
	private TextView[] tvanswerlist = null;
	private String answerString;
	private ArrayList<Integer> selectedAnswers;
	private String jumpString = null;
	private String answerjumpString = null;
	private ViewGroup answerlayout;
	protected View rootView;
	private String questionkind = null;
	protected Integer questionposition;
  protected Integer chapterposition;
	private boolean other;
	private EditText otherET = null;
	private LinearLayout otherfield = null;
	private LinearLayout answerfield;
	private EditText openET;
	private LinearLayout[] answerinsert;
	private CheckBox[] cbanswer;
	private ImageView[] answerImages;
	private ImageView otherImage;
	private ArrayList<String> answerList;
	private ArrayList<String> originalAnswerList;

	// Information interface with main activity.
	private Button skipButton;
	private NavButtonsManager navButtonsManager;

  private Question question;

	public QuestionFragment(Question question) {
    this.question = question;
	}

	// Passes information about looped question.

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_question, container,
				false);

		setupNavButtons();

			try {
				setupLayout();
				prepopulateQuestion();
			} catch (JSONException e) {
			}

			return rootView;


	}

	public abstract void setupLayout() throws JSONException;

	public abstract void sendAnswer();

	public abstract void prepopulateQuestion() throws JSONException;

	private void setupNavButtons() {
		navButtonsManager = (NavButtonsManager) rootView
				.findViewById(R.id.questionButtons);
		navButtonsManager.setQuestionType(navButtonsListener, QuestionType.FIRST);
	}

	public void saveState() {
		if (selectedAnswers != null && selectedAnswers.contains(-1)) {
			if (otherfield != null) {
//				MultipleChoiceOnClick(otherfield);
				InputMethodManager lManager = (InputMethodManager) mainActivity
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				if (mainActivity.getCurrentFocus() != null) {
					lManager.hideSoftInputFromWindow(mainActivity
							.getCurrentFocus().getWindowToken(), 0);
				}
			} else if (openET != null) {
//				OpenOnClick(openET);
				InputMethodManager lManager = (InputMethodManager) mainActivity
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				if (mainActivity.getCurrentFocus() != null) {
					lManager.hideSoftInputFromWindow(mainActivity
							.getCurrentFocus().getWindowToken(), 0);
				}
			}
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// This makes sure that the container activity has implemented
		// the callback interface. If not, it throws an exception.
		try {
			Callback = (AnswerSelected) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement QuestionFragment.AnswerSelected");
		}
	}

	// The container Activity must implement this interface so the fragment can
	// deliver messages
	public interface AnswerSelected {

		/**
		 * Called by Fragment when an answer is selected
		 * 
		 * @param selectedAnswers
		 */
		public void AnswerRecieve(String answerString, String jumpString,
				ArrayList<Integer> selectedAnswers, Boolean inLoop,
				String questionkindRecieve, ArrayList<Integer> questionkey);
	}

	protected Set<String> getSelectedAnswers() {
    return question.getSelectedAnswers();
  }

  public Question getQuestion() {
    return question;
  }
}