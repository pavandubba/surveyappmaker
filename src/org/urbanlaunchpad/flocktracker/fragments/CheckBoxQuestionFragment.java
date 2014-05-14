package org.urbanlaunchpad.flocktracker.fragments;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.urbanlaunchpad.flocktracker.R;

import com.google.gson.InstanceCreator;

import android.graphics.Typeface;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CheckBoxQuestionFragment extends QuestionFragment {
	private LinearLayout[] answers;

	private final int CB_TAG = -2;
	private final int ANSWER_TAG = -3;
	private ArrayList<Integer> selectedAnswers;
	private int numAnswers;

	private OnClickListener onClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			toggleCheckBox((LinearLayout) v);
			sendAnswer();
		}
	};

	private void toggleCheckBox(LinearLayout v) {
		CheckBox cb = (CheckBox) v.findViewById(CB_TAG);
		TextView tv = (TextView) v.findViewById(ANSWER_TAG);
		if (cb.isChecked()) {
			selectedAnswers.remove((Integer) v.getId());
			cb.setChecked(false);
			tv.setTextColor(getResources().getColor(R.color.text_color_light));
		} else {
			cb.setChecked(true);
			selectedAnswers.add((Integer) v.getId());
			tv.setTextColor(getResources().getColor(R.color.answer_selected));
		}

	}

	public void setupLayout(boolean hasOther) throws JSONException {
		numAnswers = hasOther ? jquestion.getJSONArray("Answers")
				.length() : jquestion.getJSONArray("Answers").length() + 1;
		answers = new LinearLayout[numAnswers];

		JSONArray jsonAnswers = jquestion.getJSONArray("Answers");

		for (int i = 0; i < jsonAnswers.length(); ++i) {
			String answer = jsonAnswers.getString(i);

			// Custom Checkbox.
			CheckBox cbanswer = new CheckBox(getActivity());
			cbanswer.setId(CB_TAG);
			cbanswer.setBackgroundResource(R.drawable.custom_checkbox);
			cbanswer.setButtonDrawable(new StateListDrawable());
			cbanswer.setClickable(false);

			// Text for the answer
			TextView tvanswer = new TextView(rootView.getContext());
			tvanswer.setText(answer);

			// linearlayout for checkbox and answer.
			answers[i] = new LinearLayout(rootView.getContext());
			answers[i].setOrientation(LinearLayout.HORIZONTAL);

			// Text formating.
			tvanswer.setTextSize(20);
			tvanswer.setPadding(20, 20, 0, 20);
			tvanswer.setTextColor(getResources().getColor(
					R.color.text_color_light));

			// Checkbox formatting.
			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
					60, 60);
			layoutParams.gravity = Gravity.CENTER_VERTICAL;
			cbanswer.setLayoutParams(layoutParams);

			// Adding both to LinearLayout.
			answers[i].addView(cbanswer);
			answers[i].addView(tvanswer);
			answers[i].setId(ANSWER_TAG);
			answers[i].setOnClickListener(onClickListener);
			// answerlayout.addView(answers[i]);

		}
		if (hasOther) {
			final int i = numAnswers - 1;

			// Custom checkbox
			otherCB = new CheckBox(getActivity());
			otherCB.setId(CB_TAG);
			otherCB.setBackgroundResource(R.drawable.custom_checkbox);
			otherCB.setButtonDrawable(new StateListDrawable());
			otherCB.setClickable(false);

			// Answer text
			final EditText otherET = new EditText(getActivity());
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
						toggleCheckBox(answers[i]);
					}
					return false;
				}
			});

			// Add both to a linear layout
			answers[i] = new LinearLayout(getActivity());
			answers[i].setOrientation(LinearLayout.HORIZONTAL);
			answers[i].addView(otherCB);
			answers[i].addView(otherET);
			answers[i].setId(ANSWER_TAG);
			answers[i].setOnClickListener(this);

			// answerlayout.addView(answers[i]);

			otherET.setOnEditorActionListener(new TextView.OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView v, int actionId,
						KeyEvent event) {
					toggleCheckBox(answers[i]);
					return false;
				}
			});
		}
		sendAnswer();
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

	}

	@Override
	public void orderedListSendAnswer() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setupLayout() {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendAnswer() {
		// Sending the answer to the main activity.
		if (!selectedAnswers.isEmpty()) {
			String answerString = "";
			for (int j = 0; j <= numAnswers; ++j) {
				if (selectedAnswers.contains(j)) {
					View answerView = answers[j].findViewById(ANSWER_TAG);
					if (answerView instanceof TextView){
						TextView answerTextView = (TextView) answerView; 
						answerString += answerTextView.getText() + ",";
					} else if (answerView instanceof EditText){
						EditText answerEditText = (EditText) answerView; 
						answerString += answerEditText.getText() + ",";
					}
					

				}
			}
			answerString = answerString.substring(0, answerString.length() - 1);
			Callback.AnswerRecieve("(" + answerString + ")", null,
					selectedAnswers, inLoopBoolean, questionkind, key);
		} else {
			ArrayList<Integer> key = getkey();
			Callback.AnswerRecieve(null, null, selectedAnswers, inLoopBoolean,
					questionkind, key);
		}
	}

	@Override
	public void prepopulateQuestion() {
		// TODO Auto-generated method stub
		
	}
	
	private void CheckBoxOnClick(View view, Boolean editingtextBoolean) {
		if (!editingtextBoolean) {
			if (view instanceof LinearLayout) {
				int i = view.getId();
				// Getting answers from fields other than other.
				if (i < answers.length) {
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

	}
}
