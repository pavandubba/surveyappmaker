package org.urbanlaunchpad.flocktracker.fragments;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.urbanlaunchpad.flocktracker.R;

import android.graphics.Typeface;
import android.graphics.drawable.StateListDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CheckBoxQuestionFragment extends QuestionFragment {
	private LinearLayout[] answers;

	private final int IMAGE_TAG = -2;
	private final int ANSWER_TAG = -3;
	private Integer lastAnswerId;

	public void setupLayout(boolean hasOther) throws JSONException {
		final int numAnswers = hasOther ? jquestion.getJSONArray("Answers")
				.length() : jquestion.getJSONArray("Answers").length() + 1;
		answers = new LinearLayout[numAnswers];

		JSONArray jsonAnswers = jquestion.getJSONArray("Answers");

		JSONObject aux;

		// Creating necessary variables.
		int totalanswers = jsonAnswers.length();
		answerlist = new String[totalanswers];
//		answers = new LinearLayout[totalanswers];
		cbanswer = new CheckBox[totalanswers];
		tvanswerlist = new TextView[totalanswers];

		// Filling them with info.
		for (int i = 0; i < jsonAnswers.length(); ++i) {

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
			answers[i] = new LinearLayout(rootView.getContext());
			answers[i].setOrientation(LinearLayout.HORIZONTAL);

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
			answers[i].addView(cbanswer[i]);
			answers[i].addView(tvanswerlist[i]);
			answers[i].setId(i);
			answers[i].setOnClickListener(QuestionFragment.this);
			answerlayout.addView(answers[i]);

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
}
