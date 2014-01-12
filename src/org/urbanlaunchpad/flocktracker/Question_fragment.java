package org.urbanlaunchpad.flocktracker;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.urbanlaunchpad.flocktracker.SurveyHelper.Tuple;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class Question_fragment extends Fragment implements View.OnClickListener {
	public static final String ARG_JSON_QUESTION = "Json question";
	public static final String ARG_QUESTION_POSITION = "Question position";
	public static final String ARG_CHAPTER_POSITION = "Chapter position";
	public static final String ARG_POSITION = null;
	private static String[] answerlist = null;
	private Toast toast;
	private String jquestionstring;
	private JSONObject jquestion = null;
	private JSONArray janswerlist = null;
	private String questionstring = "No questions on chapter";
	private Integer totalanswers;
	private TextView[] tvanswerlist = null;
	private String answerString;
	private ArrayList<Integer> selectedAnswers = new ArrayList<Integer>();
	private String jumpString = null;
	private String answerjumpString = null;
	private ViewGroup answerlayout;
	private View rootView;
	private String questionkind = null;
	private Integer questionposition;
	private Integer chapterposition;
	private boolean other;
	private EditText otherET = null;
	private LinearLayout otherfield = null;
	private LinearLayout answerfield;
	private EditText openET;
	private TextView openanswer;
	private Integer opentotal = 0;
	private LinearLayout[] answerinsert;
	private CheckBox[] cbanswer;
	private String loopLimitString = null;
	private ImageView[] answerImages;
	private ImageView otherImage;
	private Spinner[] answerspinners;
	ArrayList<String>[] spinnerarray;
	ArrayList<String> originalspinnerarray;
	ArrayAdapter<String>[] spinnerarrayadapters;
	OnItemSelectedListener OrderedListListener;

	public Question_fragment() {
		// Empty constructor required for fragment subclasses
	}

	// Information interface with main activity.

	// Passes Answer and jump information to activity.
	AnswerSelected Callback;

	// The container Activity must implement this interface so the fragment can
	// deliver messages
	public interface AnswerSelected {
		/**
		 * Called by Fragment when an answer is selected
		 * 
		 * @param selectedAnswers
		 */
		public void AnswerRecieve(String answerString, String jumpString,
				ArrayList<Integer> selectedAnswers);
	}

	// Passes information about looped question.

	LoopPasser Loopback;

	public interface LoopPasser {
		/** Called by Fragment when a loop is about to be started */
		public void LoopReceive(String Loopend);
	}

	// Passes position information to activity. Will be called when fragment
	// resumes.
	PositionPasser Posback;

	public interface PositionPasser {
		/** Called by Fragment when an answer is selected */
		public void PositionRecieve(Integer chapterposition,
				Integer questionposition);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_question, container,
				false);

		// Getting json question and position in survey from parent activity.
		Bundle args = this.getArguments();
		if (args != null) {
			jquestionstring = args.getString(ARG_JSON_QUESTION);
			chapterposition = args.getInt(ARG_CHAPTER_POSITION);
			questionposition = args.getInt(ARG_QUESTION_POSITION);
		}

		if (jquestionstring != null) {
			try {
				jquestion = new JSONObject(jquestionstring);
			} catch (JSONException e) {
				e.printStackTrace();
				toast = Toast.makeText(getActivity(),
						"Question not recieved from main activity.",
						Toast.LENGTH_SHORT);
				toast.show();
			}

			// Setting up layout of fragment.

			answerlayout = (ViewGroup) rootView.findViewById(R.id.answerlayout);

			try {
				questionkind = jquestion.getString("Kind");
			} catch (JSONException e) {
				e.printStackTrace();
				questionkind = "no kind";
				toast = Toast.makeText(getActivity(),
						"No question kind in question.", Toast.LENGTH_SHORT);
				toast.show();
			}

			try {
				other = jquestion.getBoolean("Other");
			} catch (JSONException e1) {
				// e1.printStackTrace();
				other = false;
			}

			jumpString = getJump(jquestion);
			Callback.AnswerRecieve(answerString, jumpString, null);

			try {
				questionstring = jquestion.getString("Question");
			} catch (JSONException e) {
				// e.printStackTrace();
			}

			// Generating question kind specific layouts.
			if (questionkind.equals("MC")) {
				MultipleChoiceLayout();

				// Prepopulate question
				selectedAnswers = SurveyHelper.selectedAnswersMap
						.get(new Tuple<Integer>(chapterposition,
								questionposition));
				if (selectedAnswers != null) {
					for (Integer id : selectedAnswers) {
						if (id == -1) {
							try {
								otherET.setText(jquestion.getString("Answer"));
								otherET.setTextColor(getResources().getColor(
										R.color.answer_selected));
								answerString = (String) otherET.getText()
										.toString();
								selectedAnswers = new ArrayList<Integer>();
								selectedAnswers.add(-1);
								Callback.AnswerRecieve(answerString,
										jumpString, selectedAnswers);
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						} else {
							TextView textView = tvanswerlist[id];
							MultipleChoiceOnClick((LinearLayout) textView
									.getParent());
						}
					}
				}
			} else if (questionkind.equals("OT") || questionkind.equals("ON")) {
				OpenLayout();
			} else if (questionkind.equals("CB")) {
				CheckBoxLayout();
				// Prepopulate question
				selectedAnswers = SurveyHelper.selectedAnswersMap
						.get(new Tuple<Integer>(chapterposition,
								questionposition));
				if (selectedAnswers != null) {
					for (Integer id : selectedAnswers) {
						CheckBox checkbox = cbanswer[id];
						checkbox.setChecked(true);
						CheckBoxOnClick(checkbox);
					}
				}
			} else if (questionkind.equals("IM")) {
				ImageLayout();
			} else if (questionkind.equals("LP")) {
				OpenLayout();
				// Obtaining lmits of the Loop.
				loopLimitString = getLimit(jquestion);
			} else if (questionkind.equals("OL")) {
				OrderedListLayout();
			}

			TextView questionview = (TextView) rootView
					.findViewById(R.id.questionview);
			questionview.setText(questionstring);
			questionview.setTextSize(20);
		}
		return rootView;

	}

	@Override
	public void onResume() {
		// Communicates position of the question in the general suvey so the
		// position counter on the main activity can be updated accordingly.
		// Fixed a bug with the back stack where the main activity would get
		// lost in terms of its position on the survey.
		Posback.PositionRecieve(chapterposition, questionposition);
		super.onResume();
	}

	public void OrderedListLayout() {

		OrderedListListener = new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// // TODO Auto-generated method stub
				// ArrayList<String> newspinnerarray = new ArrayList<String>();
				// boolean[] alreadyselected = new boolean[totalanswers];
				// String aux = null;
				// Integer j;
				// for (int i = 0; i < totalanswers; ++i) {
				// aux = answerspinners[i].getSelectedItem().toString();
				// for (int k = 1; k < totalanswers + 1 ; ++k){
				// alreadyselected[i] = false;
				// if (originalspinnerarray.get(k).equals(aux) &&
				// (!aux.equals(""))){
				// alreadyselected[i] = true;
				// break;
				// }
				// }
				// }
				// // newspinnerarray.add("");
				// for (int i = 0; i < totalanswers ; ++i)
				// if (alreadyselected[i] = false) {
				// j = i +1;
				// newspinnerarray.add(j.toString());
				// }
				// ArrayList<String> auxarray = new ArrayList<String>();
				// ArrayAdapter<String> auxarrayadapter;
				// for (int i = 0; i < totalanswers + 1 ; ++i){
				// auxarray.add(aux);
				// auxarray.addAll(newspinnerarray);
				// spinnerarray[i] = new ArrayList<String>();
				// spinnerarray[i] = auxarray;
				// // auxarrayadapter = new ArrayAdapter(rootView.getContext(),
				// android.R.layout.simple_spinner_dropdown_item, auxarray);
				// // answerspinners[i] = new Spinner(rootView.getContext());
				// // spinnerarrayadapters[i] = auxarrayadapter;
				// spinnerarrayadapters[i].notifyDataSetChanged();
				// // answerspinners[i].setAdapter(auxarrayadapter);
				// }
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}

		};

		JSONObject aux;

		try {
			janswerlist = jquestion.getJSONArray("Answers");
			totalanswers = janswerlist.length();
			answerlist = new String[totalanswers];
			tvanswerlist = new TextView[totalanswers];
			answerinsert = new LinearLayout[totalanswers];
			answerspinners = new Spinner[totalanswers];
			spinnerarray = new ArrayList[totalanswers];
			spinnerarrayadapters = new ArrayAdapter[totalanswers];
			originalspinnerarray = new ArrayList<String>();

			// Creating spinner array of answers.

			originalspinnerarray.add("");
			Integer j = 0;
			for (int i = 0; i < totalanswers; ++i) {
				j = i + 1;
				originalspinnerarray.add(j.toString());
			}

			for (int i = 0; i < totalanswers; ++i) {
				aux = janswerlist.getJSONObject(i);
				answerlist[i] = aux.getString("Answer");
				tvanswerlist[i] = new TextView(rootView.getContext());

				LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
						LayoutParams.WRAP_CONTENT, 60);
				layoutParams.gravity = Gravity.CENTER_VERTICAL;

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

				// Spinner

				spinnerarray[i] = originalspinnerarray;
				spinnerarrayadapters[i] = new ArrayAdapter(
						rootView.getContext(),
						android.R.layout.simple_spinner_dropdown_item,
						spinnerarray[i]);
				answerspinners[i] = new Spinner(rootView.getContext());
				answerspinners[i].setAdapter(spinnerarrayadapters[i]);

				// Add both to a linear layout
				answerinsert[i] = new LinearLayout(rootView.getContext());
				answerinsert[i].setWeightSum(4);
				LinearLayout.LayoutParams layoutParamsParent = new LinearLayout.LayoutParams(
						LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				answerinsert[i].setLayoutParams(layoutParamsParent);
				answerinsert[i].setPadding(10, 10, 10, 10);
				answerinsert[i].addView(answerspinners[i]);
				answerinsert[i].addView(tvanswerlist[i]);

				// add this linear layout to the parent viewgroup
				answerlayout.addView(answerinsert[i]);
				tvanswerlist[i].setId(i);
				answerinsert[i].setId(i);
				answerspinners[i]
						.setOnItemSelectedListener(OrderedListListener);
			}
		} catch (JSONException e) {
			e.printStackTrace();
			totalanswers = 0;
			toast = Toast.makeText(getActivity(),
					"Poblems with question parsing, please check surve file.",
					Toast.LENGTH_SHORT);
			toast.show();
		}
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
					"Poblems with question parsing, please check surve file.",
					Toast.LENGTH_SHORT);
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
			janswerlist = jquestion.getJSONArray("Answers");
			totalanswers = janswerlist.length();
			answerlist = new String[totalanswers];
			answerinsert = new LinearLayout[totalanswers];
			cbanswer = new CheckBox[totalanswers];
			tvanswerlist = new TextView[totalanswers];
			for (int i = 0; i < totalanswers; ++i) {
				aux = janswerlist.getJSONObject(i);
				answerlist[i] = aux.getString("Answer");
				tvanswerlist[i] = new TextView(rootView.getContext());
				cbanswer[i] = new CheckBox(rootView.getContext());
				answerinsert[i] = new LinearLayout(rootView.getContext());
				answerinsert[i].setOrientation(LinearLayout.HORIZONTAL);
				tvanswerlist[i].setText(answerlist[i]);
				tvanswerlist[i].setTextColor(getResources().getColor(
						R.color.text_color_light));
				answerinsert[i].addView(cbanswer[i]);
				answerinsert[i].addView(tvanswerlist[i]);
				answerlayout.addView(answerinsert[i]);
				tvanswerlist[i].setId(i);
				cbanswer[i].setId(i + totalanswers);
				tvanswerlist[i].setOnClickListener(Question_fragment.this);
				cbanswer[i].setOnClickListener(Question_fragment.this);
				cbanswer[i].setButtonDrawable(R.drawable.custom_checkbox);
				cbanswer[i].setScaleX((float) 0.5);
				cbanswer[i].setScaleY((float) 0.5);
				// TODO Generate Scale factors dependent on screen size.
				// cbanswer[i].setWidth(30);
			}
		} catch (JSONException e) {
			e.printStackTrace();
			totalanswers = 0;
			toast = Toast.makeText(getActivity(),
					"Poblems with question parsing, please check surve file.",
					Toast.LENGTH_SHORT);
			toast.show();
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
					++opentotal;
					openanswer = new TextView(rootView.getContext());
					openanswer.setText(openET.getText());
					answerfield.addView(openanswer);
					openanswer.setId(opentotal - 1);
					openanswer.setOnClickListener(Question_fragment.this);
					OpenOnClick(openanswer);
					openET.setText("");
					return false; // If false hides the keyboard after pressing
									// Done.
				}
				return false;
			}
		});

	}

	public void ImageLayout() {
		answerlayout.removeAllViews();
		ImageView cameraButton = new ImageView(rootView.getContext());
		cameraButton.setImageResource(R.drawable.ft_ridercomp);
		cameraButton.setOnClickListener(this);
		answerlayout.addView(cameraButton);
		addThumbnail();
	}
	
	public void addThumbnail() {
		Tuple<Integer> key = new Tuple<Integer>(chapterposition,
				questionposition);
		if (SurveyHelper.prevImages.containsKey(key)) {
			Uri imagePath = SurveyHelper.prevImages.get(key);
			ImageView prevImage = new ImageView(rootView.getContext());
			try {
				Bitmap imageBitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(imagePath.getPath()), 512, 300);
				prevImage.setImageBitmap(imageBitmap);
				prevImage.setPadding(10, 30, 10, 10);

				answerlayout.addView(prevImage);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
			CheckBoxOnClick(view);
		} else if (questionkind.equals("IM")) {
			ImageOnClick(view);
		}

	}

	public void MultipleChoiceOnClick(View view) {
		if (view instanceof LinearLayout) {
			boolean foundAnswer = false;
			for (int i = 0; i < totalanswers; ++i) {
				TextView textView = (TextView) tvanswerlist[i];
				if (view.getId() == textView.getId()) {
					foundAnswer = true;

					otherET.setTextColor(getResources().getColor(
							R.color.text_color_light));

					textView.setTextColor(getResources().getColor(
							R.color.answer_selected));
					try {
						answerjumpString = getJump(janswerlist.getJSONObject(i));
						if (answerjumpString != null) {
							jumpString = answerjumpString;
						}
					} catch (JSONException e) {
						// e.printStackTrace();
					}
					answerString = answerlist[i].toString(); // Sets the answer
																// to be sent to
																// parent
																// activity.
					selectedAnswers = new ArrayList<Integer>();
					selectedAnswers.add(view.getId());
					Callback.AnswerRecieve(answerString, jumpString,
							selectedAnswers);
				} else {
					textView.setTextColor(getResources().getColor(
							R.color.text_color_light));
				}
			}

			if (!foundAnswer) {
				// focus ET
				otherET.requestFocusFromTouch();
				InputMethodManager lManager = (InputMethodManager) getActivity()
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				lManager.showSoftInput(otherET, 0);

				otherET.setTextColor(getResources().getColor(
						R.color.answer_selected));
				answerString = (String) otherET.getText().toString();
				selectedAnswers = new ArrayList<Integer>();
				selectedAnswers.add(-1);
				Callback.AnswerRecieve(answerString, jumpString,
						selectedAnswers);
			}
		}
	}

	private void CheckBoxOnClick(View view) {
		answerString = null;
		selectedAnswers = new ArrayList<Integer>();
		for (int i = 0; i < totalanswers; ++i) {
			TextView textView = tvanswerlist[i];
			CheckBox checkBox = cbanswer[i];
			if (checkBox.isChecked()) {
				selectedAnswers.add(i);
				textView.setTextColor(getResources().getColor(
						R.color.answer_selected));
				answerString = addanswer(answerString, answerlist[i].toString());
			} else if (!checkBox.isChecked()) {
				textView.setTextColor(getResources().getColor(
						R.color.text_color_light));
			}
		}
		if (!(answerString == null)) {
			Callback.AnswerRecieve("(" + answerString + ")", null,
					selectedAnswers);
		} else {
			Callback.AnswerRecieve(null, null, selectedAnswers);
		}
	}

	private String addanswer(String answerContainer, String answerAdded) {
		if (answerContainer == null) {
			answerContainer = answerAdded;
		} else {
			answerContainer = answerContainer + "," + answerAdded;
		}
		return answerContainer;
	}

	// TODO
	// We want to take the ids and also save it in the json
	// Extract it on the createview and click the views
	private void OpenOnClick(View view) {
		if (view instanceof TextView) {
			for (int i = 0; i < opentotal; ++i) {
				TextView textView = (TextView) rootView.findViewById(i);
				if (view.getId() == textView.getId()) {
					textView.setTextColor(getResources().getColor(
							R.color.answer_selected));
					answerString = (String) textView.getText().toString();
					Callback.AnswerRecieve(answerString, jumpString, null);
					if (questionkind.equals("LP")) {
						Loopback.LoopReceive(loopLimitString);
					}
				} else {
					textView.setTextColor(getResources().getColor(
							R.color.text_color_light));
				}
			}
		}
	}

	// TODO
	// We want to take the image and display it.
	// Ask kuan about where the image needs to go?
	private void ImageOnClick(View view) {
		Surveyor.driveHelper.startCameraIntent(jumpString);
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
					+ " must implement Question_fragment.AnswerSelected");
		}
		try {
			Posback = (PositionPasser) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement Question_fragment.PositionPasser");
		}
	}

	public String getLimit(JSONObject Obj) {
		String auxlimit;
		try {
			auxlimit = Obj.getString("Limit");
		} catch (JSONException e1) {
			auxlimit = null;
			toast = Toast.makeText(getActivity(),
					"No limit on looped quesiton.", Toast.LENGTH_SHORT);
			toast.show();
			// e1.printStackTrace();
		}
		return auxlimit;
	}

}