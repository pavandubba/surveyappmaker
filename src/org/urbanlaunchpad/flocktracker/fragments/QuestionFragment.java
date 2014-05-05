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
import org.urbanlaunchpad.flocktracker.views.NavButtonsManager;

import java.util.ArrayList;
import java.util.Arrays;

public class QuestionFragment extends Fragment implements QuestionManager, View.OnClickListener,
		DynamicListView.SwappingEnded {

    QuestionAnswerListener listener;


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
	LoopPasser Loopback;
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
	private Integer questionposition;
	private Integer chapterposition;
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

	// Loop stuff
	Boolean inLoopBoolean;
	Integer loopTotalInteger;
	Integer loopIterationInteger;
	Integer loopPositionInteger;

	public QuestionFragment() {
		// Empty constructor required for fragment subclasses
	}

	// Passes information about looped question.

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_question, container,
				false);
		mainActivity = getActivity();

		// Getting json question and position in survey from parent activity.
		Bundle args = this.getArguments();
		if (args != null) {
			jquestionstring = args.getString(ARG_JSON_QUESTION);
			chapterposition = args.getInt(ARG_CHAPTER_POSITION);
			questionposition = args.getInt(ARG_QUESTION_POSITION);
			inLoopBoolean = args.getBoolean(ARG_IN_LOOP);
			Log.v("In loop on fragment", inLoopBoolean.toString());
			if (inLoopBoolean) {
				loopTotalInteger = args.getInt(ARG_LOOP_TOTAL);
				loopIterationInteger = args.getInt(ARG_LOOP_ITERATION);
				loopPositionInteger = args.getInt(ARG_LOOP_POSITION);
			}
		}

        setupNavButtons();


        if (jquestionstring != null) {
			try {
				jquestion = new JSONObject(jquestionstring);
			} catch (JSONException e) {
				e.printStackTrace();
				toast = Toast.makeText(getActivity(),
						R.string.question_not_received, Toast.LENGTH_SHORT);
				toast.show();
			}

			// Setting up layout of fragment.

			answerlayout = (ViewGroup) rootView.findViewById(R.id.answerlayout);

			try {
				questionkind = jquestion.getString("Kind");
				SurveyHelper.questionKind = questionkind;
			} catch (JSONException e) {
				e.printStackTrace();
				questionkind = "no kind";
				toast = Toast.makeText(getActivity(),
						R.string.no_question_kind, Toast.LENGTH_SHORT);
				toast.show();
			}

			try {
				other = jquestion.getBoolean("Other");
			} catch (JSONException e1) {
				// e1.printStackTrace();
				other = false;
			}

			selectedAnswers = new ArrayList<Integer>();

			jumpString = getJump(jquestion);
			ArrayList<Integer> key = getkey();
			Callback.AnswerRecieve(answerString, jumpString, null, null,
					questionkind, key);

			try {
				questionstring = jquestion.getString("Question");
			} catch (JSONException e) {
				// e.printStackTrace();
			}

			// Generating question kind specific layouts.
			if (questionkind.equals("MC")) {
				MultipleChoiceLayout();

				// Prepopulate question
				getselectedAnswers();
				if (selectedAnswers != null) {
					for (Integer id : selectedAnswers) {
						if (id == -1) {
							try {
								otherET.setText(jquestion.getString("Answer"));
								otherET.setTextColor(getResources().getColor(
										R.color.answer_selected));
								otherImage
										.setImageResource(R.drawable.ft_cir_grn);
							} catch (JSONException e) {
								e.printStackTrace();
							}
						} else {
							TextView textView = tvanswerlist[id];
							MultipleChoiceOnClick((LinearLayout) textView
									.getParent());
						}
					}
				}
			} else if (questionkind.equals("OT") || (questionkind.equals("ON")
					|| questionkind.equals("LP"))) {
				OpenLayout();

				// Prepopulate question
				getselectedAnswers();

				if (selectedAnswers != null && selectedAnswers.get(0) == -1) {
					try {
						openET.setText(jquestion.getString("Answer"));
						openET.setTextColor(getResources().getColor(
								R.color.answer_selected));
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}

			} else if (questionkind.equals("CB")) {
				CheckBoxLayout();
				// Prepopulate question
				getselectedAnswers();

				if (selectedAnswers != null) {
					for (Integer id : selectedAnswers) {
						if (id == totalanswers) {
							otherCB.setChecked(false);
							CheckBoxPrePopulate(otherfield);
						} else {
							cbanswer[id].setChecked(false);
							CheckBoxPrePopulate(answerinsert[id]);
						}
					}
				} else {
					selectedAnswers = new ArrayList<Integer>();
				}
			} else if (questionkind.equals("IM")) {
				ImageLayout();
			} else if (questionkind.equals("OL")) {
				// Prepopulation occurs in the Layout creation.
				OrderedListLayout();
			}

			// Adding the loop element information to the question if inside a
			// loop.
			TextView loopElementTextView = (TextView) rootView
					.findViewById(R.id.loopelement);
			if (inLoopBoolean) {
				Integer iterationToShowInteger = loopIterationInteger + 1;
				loopElementTextView.setText("Element " + iterationToShowInteger
						+ " of " + loopTotalInteger);
				loopElementTextView.setTextSize(20);
			} else {
				loopElementTextView.setVisibility(View.GONE);
			}

			// Adding the actual question text to the view
			TextView questionview = (TextView) rootView
					.findViewById(R.id.questionview);
			questionview.setText(questionstring);
			questionview.setTextSize(20);
		}
		return rootView;

	}

    private void initializeQuestion()

    private void setupNavButtons() {
        navButtonsManager = (NavButtonsManager) rootView.findViewById(R.id.questionButtons);
        navButtonsManager.setQuestionType(listener);
    }
	public void OrderedListLayout() {
		originalAnswerList = new ArrayList<String>();
		try {
			janswerlist = jquestion.getJSONArray("Answers");
			totalanswers = janswerlist.length();
		} catch (JSONException e) {
			e.printStackTrace();
			totalanswers = 0;
			toast = Toast.makeText(getActivity(),
					R.string.question_parsing_problem, Toast.LENGTH_SHORT);
			toast.show();
		}
		// Filling array adapter with the answers.
		if (totalanswers == 0) {
			originalAnswerList.add("");
		} else {
			String aux;
			for (int i = 0; i < totalanswers; ++i) {
				try {
					aux = janswerlist.getJSONObject(i).getString("Answer");
					originalAnswerList.add(aux);
				} catch (JSONException e) {
					e.printStackTrace();
					originalAnswerList.add("");
				}

			}
		}

		answerList = new ArrayList<String>();
		answerList.addAll(originalAnswerList);

		// Prepopulate question
		getselectedAnswers();

		if (selectedAnswers != null) {
			ArrayList<String> answerTempList = new ArrayList<String>();
			for (int i = 0; i < totalanswers; ++i) {
				answerTempList.add(answerList.get(selectedAnswers.get(i)));
			}
			answerList.clear();
			answerList.addAll(answerTempList);
		}

		ViewGroup questionLayoutView = (ViewGroup) rootView
				.findViewById(R.id.questionlayout);
		ScrollView answerScroll = (ScrollView) rootView
				.findViewById(R.id.answerScroll);
		questionLayoutView.removeView(answerScroll);
		StableArrayAdapter adapter = new StableArrayAdapter(
				rootView.getContext(), R.layout.ordered_answer, answerList);
		answerlistView = (DynamicListView) new DynamicListView(getActivity(),
				this);
		answerlistView.setCheeseList(answerList);
		answerlistView.setAdapter(adapter);
		answerlistView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

		skipButton = (Button) new Button(getActivity());
		answerString = "";
		skipButton.setEnabled(false);
		skipButton.setText(R.string.question_skipped);

		LinearLayout orderanswerlayout = (LinearLayout) rootView
				.findViewById(R.id.orderanswerlayout);
		orderanswerlayout.setOrientation(LinearLayout.VERTICAL);
		orderanswerlayout.setWeightSum(6f);
		LinearLayout.LayoutParams lParams1 = (LinearLayout.LayoutParams) new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, 0);
		LinearLayout.LayoutParams lParams2 = (LinearLayout.LayoutParams) new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, 0);
		lParams1.weight = 5f;
		lParams2.weight = 1f;

		orderanswerlayout.addView(answerlistView);
		orderanswerlayout.addView(skipButton);
		answerlistView.setLayoutParams(lParams1);
		skipButton.setLayoutParams(lParams2);
		skipButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				answerString = "";
				ArrayList<Integer> key = getkey();
				Callback.AnswerRecieve(answerString, null, null, inLoopBoolean,
						questionkind, key);
				skipButton.setEnabled(false);
				skipButton.setText(R.string.question_skipped);
			}
		});
		if (selectedAnswers != null) {
			orderedListSendAnswer();
		}

	}

	public void orderedListSendAnswer() {
		if (skipButton != null) {
			skipButton.setEnabled(true);
			skipButton.setText(R.string.skip_question);
		}
		answerString = getorderedAnswers();
		selectedAnswers = new ArrayList<Integer>();

		for (int i = 0; i < totalanswers; ++i) {
			for (int j = 0; j < totalanswers; ++j) {
				if (originalAnswerList.get(i).equals(answerList.get(j))) {
					selectedAnswers.add(j);
					// toast = Toast.makeText(getActivity(), answerList.get(j)
					// + " " + selectedAnswers.get(j+1), Toast.LENGTH_SHORT);
					// toast.show();
					break;
				}
			}
		}
		ArrayList<Integer> key = getkey();
		Callback.AnswerRecieve(answerString, null, selectedAnswers,
				inLoopBoolean, questionkind, key);
	}

	private String getorderedAnswers() {
		String answer = null;
		StableArrayAdapter List = (StableArrayAdapter) answerlistView
				.getAdapter();
		for (int i = 0; i < totalanswers; ++i) {
			if (i == 0) {
				answer = "(";
			} else {
				answer = answer + ",";
			}
			answer = answer + List.getItem(i);
			if (i == (totalanswers - 1)) {
				answer = answer + ")";
			}
		}
		return answer;
	}

	public void MultipleChoiceLayout() {
		JSONObject aux;
		try {
			janswerlist = jquestion.getJSONArray("Answers");
			totalanswers = janswerlist.length();
			answerlist = new String[totalanswers];
			tvanswerlist = new TextView[totalanswers];
			answerinsert = new LinearLayout[totalanswers];
			answerImages = new ImageView[totalanswers];
			for (int i = 0; i < totalanswers; ++i) {
				aux = janswerlist.getJSONObject(i);
				answerlist[i] = aux.getString("Answer");
				tvanswerlist[i] = new TextView(rootView.getContext());

				// Image
				answerImages[i] = new ImageView(rootView.getContext());
				answerImages[i].setImageResource(R.drawable.ft_cir_gry);
				answerImages[i].setAdjustViewBounds(true);
				answerImages[i].setPadding(0, 0, 20, 0);

				LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
						LayoutParams.WRAP_CONTENT, 60);
				layoutParams.gravity = Gravity.CENTER_VERTICAL;
				answerImages[i].setLayoutParams(layoutParams);

				// Answer text
				tvanswerlist[i].setText(answerlist[i]);
				tvanswerlist[i].setTextColor(getResources().getColor(
						R.color.text_color_light));
				tvanswerlist[i].setTextSize(20);
				tvanswerlist[i].setPadding(10, 10, 10, 10);
				tvanswerlist[i].setTypeface(Typeface.create("sans-serif-light",
						Typeface.NORMAL));
				LinearLayout.LayoutParams layoutParamsText = new LinearLayout.LayoutParams(
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				layoutParamsText.gravity = Gravity.CENTER_VERTICAL;
				tvanswerlist[i].setLayoutParams(layoutParamsText);

				// Add both to a linear layout
				answerinsert[i] = new LinearLayout(rootView.getContext());
				answerinsert[i].setWeightSum(4);
				LinearLayout.LayoutParams layoutParamsParent = new LinearLayout.LayoutParams(
						LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				answerinsert[i].setLayoutParams(layoutParamsParent);
				answerinsert[i].setPadding(10, 10, 10, 10);
				answerinsert[i].addView(answerImages[i]);
				answerinsert[i].addView(tvanswerlist[i]);

				// add this linear layout to the parent viewgroup
				answerlayout.addView(answerinsert[i]);
				tvanswerlist[i].setId(i);
				answerinsert[i].setId(i);
				answerinsert[i].setOnClickListener(this);
			}
		} catch (JSONException e) {
			e.printStackTrace();
			totalanswers = 0;
			toast = Toast.makeText(getActivity(),
					R.string.question_parsing_problem, Toast.LENGTH_SHORT);
			toast.show();
		}
		if (other == Boolean.TRUE) {
			// Image
			otherImage = new ImageView(rootView.getContext());
			otherImage.setImageResource(R.drawable.ft_cir_gry);
			otherImage.setAdjustViewBounds(true);
			otherImage.setPadding(0, 0, 30, 0);

			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, 60);
			layoutParams.gravity = Gravity.CENTER_VERTICAL;
			otherImage.setLayoutParams(layoutParams);

			// Answer text
			otherET = new EditText(rootView.getContext());
			otherET.setHint(getResources().getString(R.string.other_hint));
			otherET.setImeOptions(EditorInfo.IME_ACTION_DONE);
			otherET.setSingleLine();
			otherET.setTextColor(getResources().getColor(
					R.color.text_color_light));
			otherET.setTextSize(20);
			otherET.setTypeface(Typeface.create("sans-serif-light",
					Typeface.NORMAL));
			LinearLayout.LayoutParams layoutParamsText = new LinearLayout.LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			layoutParamsText.gravity = Gravity.CENTER_VERTICAL;
			otherET.setLayoutParams(layoutParamsText);
			otherET.setBackgroundResource(R.drawable.edit_text);
			otherET.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (MotionEvent.ACTION_UP == event.getAction()) {
						MultipleChoiceOnClick(otherET);
					}
					return false;
				}
			});

			// Add both to a linear layout
			otherfield = new LinearLayout(rootView.getContext());
			otherfield.setWeightSum(4);
			LinearLayout.LayoutParams layoutParamsParent = new LinearLayout.LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			otherfield.setLayoutParams(layoutParamsParent);
			otherfield.setPadding(10, 10, 10, 10);
			otherfield.addView(otherImage);
			otherfield.addView(otherET);
			answerlayout.addView(otherfield);

			otherfield.setOnClickListener(this);
			otherET.setOnEditorActionListener(new TextView.OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView v, int actionId,
						KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_DONE) {
						MultipleChoiceOnClick(otherfield);
						return false; // If false hides the keyboard after
						// pressing Done.
					}
					return false;
				}
			});
		}

	}

	private void CheckBoxLayout() {
		JSONObject aux;
		try {

			// Creating necessary variables.
			janswerlist = jquestion.getJSONArray("Answers");
			totalanswers = janswerlist.length();
			answerlist = new String[totalanswers];
			answerinsert = new LinearLayout[totalanswers];
			cbanswer = new CheckBox[totalanswers];
			tvanswerlist = new TextView[totalanswers];

			// Filling them with info.
			for (int i = 0; i < totalanswers; ++i) {

				// Custom Checkbox.
				cbanswer[i] = new CheckBox(rootView.getContext());
				cbanswer[i].setBackgroundResource(R.drawable.custom_checkbox);
				cbanswer[i].setButtonDrawable(new StateListDrawable());
				cbanswer[i].setClickable(false);

				// Text for the answer
				tvanswerlist[i] = new TextView(rootView.getContext());
				aux = janswerlist.getJSONObject(i);
				answerlist[i] = aux.getString("Answer");
				tvanswerlist[i].setText(answerlist[i]);

				// linearlayout for checkbox and answer.
				answerinsert[i] = new LinearLayout(rootView.getContext());
				answerinsert[i].setOrientation(LinearLayout.HORIZONTAL);

				// Text formating.
				tvanswerlist[i].setTextSize(20);
				tvanswerlist[i].setPadding(20, 20, 0, 20);
				tvanswerlist[i].setTextColor(getResources().getColor(
						R.color.text_color_light));

				// Checkbox formatting.
				LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
						60, 60);
				layoutParams.gravity = Gravity.CENTER_VERTICAL;
				cbanswer[i].setLayoutParams(layoutParams);

				// Adding both to LinearLayout.
				answerinsert[i].addView(cbanswer[i]);
				answerinsert[i].addView(tvanswerlist[i]);
				answerinsert[i].setId(i);
				answerinsert[i].setOnClickListener(QuestionFragment.this);
				answerlayout.addView(answerinsert[i]);

			}
		} catch (JSONException e) {
			e.printStackTrace();
			totalanswers = 0;
			toast = Toast.makeText(getActivity(),
					R.string.question_parsing_problem, Toast.LENGTH_SHORT);
			toast.show();
		}
		if (other == Boolean.TRUE) {
			// Custom checkbox
			otherCB = new CheckBox(rootView.getContext());
			otherCB.setBackgroundResource(R.drawable.custom_checkbox);
			otherCB.setButtonDrawable(new StateListDrawable());
			otherCB.setClickable(false);

			// Answer text
			otherET = new EditText(rootView.getContext());
			otherET.setHint(getResources().getString(R.string.other_hint));
			otherET.setImeOptions(EditorInfo.IME_ACTION_DONE);
			otherET.setTextColor(getResources().getColor(
					R.color.text_color_light));

			// Checkbox formatting
			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
					60, 60);
			layoutParams.gravity = Gravity.CENTER_VERTICAL;
			otherCB.setLayoutParams(layoutParams);

			// Text formatting.

			LinearLayout.LayoutParams layoutParamsText = new LinearLayout.LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			layoutParamsText.gravity = Gravity.CENTER_VERTICAL;
			layoutParamsText.setMargins(20, 20, 0, 20);
			otherET.setLayoutParams(layoutParamsText);
			otherET.setBackgroundResource(R.drawable.edit_text);
			otherET.setSingleLine();
			otherET.setTextColor(getResources().getColor(
					R.color.text_color_light));
			otherET.setTextSize(20);
			otherET.setTypeface(Typeface.create("sans-serif-light",
					Typeface.NORMAL));

			otherET.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (MotionEvent.ACTION_UP == event.getAction()) {
						CheckBoxOnClick(otherfield, false);
					}
					return false;
				}
			});

			// Add both to a linear layout
			otherfield = new LinearLayout(rootView.getContext());
			otherfield.setOrientation(LinearLayout.HORIZONTAL);
			otherfield.addView(otherCB);
			otherfield.addView(otherET);
			answerlayout.addView(otherfield);
			otherfield.setId(totalanswers);
			otherfield.setOnClickListener(this);

			// Listen for changes on the content of Edit text.
			otherET.addTextChangedListener(new TextWatcher() {

				@Override
				public void onTextChanged(CharSequence s, int start,
						int before, int count) {

				}

				@Override
				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {

				}

				@Override
				public void afterTextChanged(Editable s) {
					CheckBoxOnClick(otherfield, true);
				}
			});

			otherET.setOnEditorActionListener(new TextView.OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView v, int actionId,
						KeyEvent event) {
					// if (actionId == EditorInfo.IME_ACTION_DONE) {
					CheckBoxOnClick(otherfield, false);
					// return false; // If false hides the keyboard after
					// // pressing Done.
					// }
					return false;
				}
			});
		}

	}

	public void saveState() {
		if (selectedAnswers != null && selectedAnswers.contains(-1)) {
			if (otherfield != null) {
				MultipleChoiceOnClick(otherfield);
				InputMethodManager lManager = (InputMethodManager) mainActivity
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				if (mainActivity.getCurrentFocus() != null) {
					lManager.hideSoftInputFromWindow(mainActivity
							.getCurrentFocus().getWindowToken(), 0);
				}
			} else if (openET != null) {
				OpenOnClick(openET);
				InputMethodManager lManager = (InputMethodManager) mainActivity
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				if (mainActivity.getCurrentFocus() != null) {
					lManager.hideSoftInputFromWindow(mainActivity
							.getCurrentFocus().getWindowToken(), 0);
				}
			}
		}
	}

	private void OpenLayout() {
		answerfield = new LinearLayout(rootView.getContext());
		answerfield.setOrientation(LinearLayout.VERTICAL);
		answerlayout.addView(answerfield);
		openET = new EditText(rootView.getContext());
		openET.setHint(getResources().getString(R.string.answer_hint));
		openET.setImeOptions(EditorInfo.IME_ACTION_DONE);
		if ((questionkind.equals("ON")) || (questionkind.equals("LP"))) {
			openET.setInputType(InputType.TYPE_CLASS_NUMBER
					| InputType.TYPE_NUMBER_FLAG_DECIMAL);
		}
		openET.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
		openET.setSingleLine();
		openET.setTextSize(20);
		openET.setTextColor(getResources().getColor(R.color.text_color_light));
		openET.setBackgroundResource(R.drawable.edit_text);
		answerlayout.addView(openET);
		openET.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					OpenOnClick(openET);
					return false; // If false hides the keyboard after
				}
				return false;
			}
		});
		openET.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (MotionEvent.ACTION_UP == event.getAction()) {
					OpenOnClick(openET);
				}
				return false;
			}
		});
	}

	public void ImageLayout() {
		answerlayout.removeAllViews();
		ImageView cameraButton = new ImageView(rootView.getContext());
		cameraButton.setImageResource(R.drawable.camera);
		cameraButton.setOnClickListener(this);
		answerlayout.addView(cameraButton);
		addThumbnail();
	}

	public void addThumbnail() {
		// Tuple key = new Tuple(chapterposition, questionposition);
		if (!SurveyorActivity.askingTripQuestions) {
			ArrayList<Integer> key = new ArrayList<Integer>(Arrays.asList(
					chapterposition, questionposition, -1, -1));
			if (SurveyHelper.prevImages.containsKey(key)) {
				Uri imagePath = SurveyHelper.prevImages.get(key);
				ImageView prevImage = new ImageView(rootView.getContext());
				try {
					Bitmap imageBitmap = ImageHelper
							.decodeSampledBitmapFromPath(imagePath.getPath(),
									512, 512);
					prevImage.setImageBitmap(imageBitmap);
					prevImage.setPadding(10, 30, 10, 10);

					answerlayout.addView(prevImage);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else if (SurveyorActivity.askingTripQuestions) {
			ArrayList<Integer> key = new ArrayList<Integer>(Arrays.asList(
					questionposition, -1, -1));
			if (SurveyHelper.prevTrackerImages.containsKey(key)) {
				Uri imagePath = SurveyHelper.prevTrackerImages
						.get(questionposition);
				ImageView prevImage = new ImageView(rootView.getContext());
				try {
					Bitmap imageBitmap = ThumbnailUtils.extractThumbnail(
							BitmapFactory.decodeFile(imagePath.getPath()), 512,
							512);
					prevImage.setImageBitmap(imageBitmap);
					prevImage.setPadding(10, 30, 10, 10);

					answerlayout.addView(prevImage);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public String getJump(JSONObject Obj) {
		String auxjump;
		try {
			auxjump = Obj.getString("Jump");
		} catch (JSONException e1) {
			auxjump = null;
			// e1.printStackTrace();
		}
		// toast = Toast.makeText(getActivity(), "Jump: " + auxjump,
		// Toast.LENGTH_SHORT);
		// toast.show();
		return auxjump;
	}

	@Override
	public void onClick(View view) {
		if (questionkind.equals("MC")) {
			MultipleChoiceOnClick(view);
		} else if (questionkind.equals("ON") || questionkind.equals("OT")) {
			OpenOnClick(view);
		} else if (questionkind.equals("CB")) {
			CheckBoxOnClick(view, false);
		} else if (questionkind.equals("IM")) {
			ImageOnClick(view);
		}

	}

	public void MultipleChoiceOnClick(View view) {
		boolean foundAnswer = false;

		if (view instanceof LinearLayout) {
			if (view.getId() >= 0) {
				for (int i = 0; i < totalanswers; ++i) {
					TextView textView = (TextView) tvanswerlist[i];
					ImageView imageView = (ImageView) answerImages[i];
					if (view.getId() == textView.getId()) {
						foundAnswer = true;
						if (otherET != null) {
							otherET.setTextColor(getResources().getColor(
									R.color.text_color_light));
							otherImage.setImageResource(R.drawable.ft_cir_gry);
						}
						textView.setTextColor(getResources().getColor(
								R.color.answer_selected));
						imageView.setImageResource(R.drawable.ft_cir_grn);
						try {
							answerjumpString = getJump(janswerlist
									.getJSONObject(i));
							if (answerjumpString != null) {
								jumpString = answerjumpString;
							}
						} catch (JSONException e) {
						}
						answerString = answerlist[i];
						selectedAnswers = new ArrayList<Integer>();
						selectedAnswers.add(view.getId());
						ArrayList<Integer> key = getkey();
						Callback.AnswerRecieve(answerString, jumpString,
								selectedAnswers, inLoopBoolean, questionkind,
								key);
					} else {
						textView.setTextColor(getResources().getColor(
								R.color.text_color_light));
						imageView.setImageResource(R.drawable.ft_cir_gry);
					}
				}
			}
		}

		if (view instanceof EditText || !foundAnswer) {
			for (int i = 0; i < totalanswers; ++i) {
				TextView textView = (TextView) tvanswerlist[i];
				ImageView imageView = (ImageView) answerImages[i];
				textView.setTextColor(getResources().getColor(
						R.color.text_color_light));
				imageView.setImageResource(R.drawable.ft_cir_gry);
			}

			// focus ET
			otherET.requestFocusFromTouch();
			InputMethodManager lManager = (InputMethodManager) getActivity()
					.getSystemService(Context.INPUT_METHOD_SERVICE);
			lManager.showSoftInput(otherET, 0);

			otherET.setTextColor(getResources().getColor(
					R.color.answer_selected));
			otherImage.setImageResource(R.drawable.ft_cir_grn);
			answerString = (String) otherET.getText().toString();
			selectedAnswers = new ArrayList<Integer>();
			selectedAnswers.add(-1);
			ArrayList<Integer> key = getkey();
			Callback.AnswerRecieve(answerString, jumpString, selectedAnswers,
					inLoopBoolean, questionkind, key);
		}
	}

	private void CheckBoxOnClick(View view, Boolean editingtextBoolean) {
		if (!editingtextBoolean) {
			if (view instanceof LinearLayout) {
				int i = view.getId();

				// Getting answers from fields other than other.
				if (i < tvanswerlist.length) {
					TextView textView = tvanswerlist[i];
					CheckBox checkBox = cbanswer[i];
					if (checkBox.isChecked()) {
						selectedAnswers.remove((Integer) i);
						textView.setTextColor(getResources().getColor(
								R.color.text_color_light));
						checkBox.setChecked(false);
					} else if (!checkBox.isChecked()) {
						selectedAnswers.add(i);
						textView.setTextColor(getResources().getColor(
								R.color.answer_selected));
						checkBox.setChecked(true);
					}
				}

				// Getting answer from other field.
				else {
					if (otherCB.isChecked()) {
						otherET.setTextColor(getResources().getColor(
								R.color.text_color_light));
						selectedAnswers.remove((Integer) i);
						otherCB.setChecked(false);
					} else if (!otherCB.isChecked()) {
						otherET.setTextColor(getResources().getColor(
								R.color.answer_selected));
						selectedAnswers.add(i);
						otherCB.setChecked(true);
					}

				}
			}
		}
		// Sending the answer to the main activity.
		if (!selectedAnswers.isEmpty()) {
			answerString = "";
			for (int j = 0; j <= totalanswers; ++j) {
				if (selectedAnswers.contains(j)) {
					if (j < tvanswerlist.length && selectedAnswers.contains(j)) {
						answerString += tvanswerlist[j].getText() + ",";
					} else {
						answerString += otherET.getText() + ",";
					}
				}
			}
			answerString = answerString.substring(0, answerString.length() - 1);

			ArrayList<Integer> key = getkey();
			Callback.AnswerRecieve("(" + answerString + ")", null,
					selectedAnswers, inLoopBoolean, questionkind, key);
		} else {
			ArrayList<Integer> key = getkey();
			Callback.AnswerRecieve(null, null, selectedAnswers, inLoopBoolean,
					questionkind, key);
		}
		Log.e("CheckBoxOnClick", answerString);

	}

	private void CheckBoxPrePopulate(View view) {
		CheckBox checkBox;
		int i = view.getId();
		if (view instanceof LinearLayout) {
			if (i == totalanswers) {
				EditText editText = otherET;
				checkBox = otherCB;
				editText.setTextColor(getResources().getColor(
						R.color.answer_selected));
				String otheranswerString = null;
				String aux = null;
				try {
					otheranswerString = jquestion.getString("Answer");
					otheranswerString = otheranswerString.substring(1,
							otheranswerString.length() - 1);
					// Log.e("CheckBoxPrePopulate", otheranswerString);
					for (int j = 0; j < totalanswers; ++j) {
						if (selectedAnswers.contains(j)) {
							aux = (String) tvanswerlist[j].getText();
							otheranswerString = otheranswerString.substring(
									aux.length() + 1,
									otheranswerString.length());
						}
					}

				} catch (JSONException e) {
					e.printStackTrace();
				}

				editText.setText(otheranswerString);

			} else {

				TextView textView = tvanswerlist[i];
				checkBox = cbanswer[i];
				textView.setTextColor(getResources().getColor(
						R.color.answer_selected));
			}
			checkBox.setChecked(true);
		}
	}

	private void OpenOnClick(View view) {
		if (view instanceof EditText) {
			openET.setTextColor(getResources()
					.getColor(R.color.answer_selected));
			answerString = (String) openET.getText().toString();
			selectedAnswers = new ArrayList<Integer>();
			selectedAnswers.add(-1);
//			if (questionkind.equals("LP")) {
//				inLoopBoolean = true;
//			}
			ArrayList<Integer> key = getkey();
			Callback.AnswerRecieve(answerString, jumpString, selectedAnswers,
					inLoopBoolean, questionkind, key);
		}
	}

	private void ImageOnClick(View view) {
		SurveyorActivity.driveHelper.startCameraIntent(jumpString);
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

	public interface LoopPasser {

		/**
		 * Called by Fragment when a loop is about to be started
		 */
		public void LoopReceive(String Loopend);
	}

	private void getselectedAnswers() {
		if (inLoopBoolean) {
			if (SurveyorActivity.askingTripQuestions) {
				selectedAnswers = SurveyHelper.selectedTrackingAnswersMap
						.get(new ArrayList<Integer>(Arrays.asList(
								questionposition, loopIterationInteger,
								loopPositionInteger)));
			} else {
				selectedAnswers = SurveyHelper.selectedAnswersMap
						.get(new ArrayList<Integer>(Arrays.asList(
								chapterposition, questionposition,
								loopIterationInteger, loopPositionInteger)));
			}
		} else {
			if (SurveyorActivity.askingTripQuestions) {
				selectedAnswers = SurveyHelper.selectedTrackingAnswersMap
						.get(new ArrayList<Integer>(Arrays.asList(
								questionposition, -1, -1)));
			} else {
				selectedAnswers = SurveyHelper.selectedAnswersMap
						.get(new ArrayList<Integer>(Arrays.asList(
								chapterposition, questionposition, -1, -1)));
			}
		}
	}

	private ArrayList<Integer> getkey() {
		ArrayList<Integer> keyArrayList = null;
		if (inLoopBoolean) {
			if (SurveyorActivity.askingTripQuestions) {
				keyArrayList = new ArrayList<Integer>(Arrays.asList(
						questionposition, loopIterationInteger,
						loopPositionInteger));
			} else {
				keyArrayList = new ArrayList<Integer>(Arrays.asList(
						chapterposition, questionposition,
						loopIterationInteger, loopPositionInteger));
			}
		} else {
			if (SurveyorActivity.askingTripQuestions) {
				keyArrayList = new ArrayList<Integer>(Arrays.asList(
						questionposition, -1, -1));
			} else {
				keyArrayList = new ArrayList<Integer>(Arrays.asList(
						chapterposition, questionposition, -1, -1));
			}
		}
		return keyArrayList;
	}

}