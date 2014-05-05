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
import android.view.View.OnClickListener;
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
	
	
	private OnClickListener onClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			
		}
	
	};
	

	public void setupLayout(boolean hasOther) throws JSONException {
		final int numAnswers = hasOther ? jquestion.getJSONArray("Answers")
				.length() : jquestion.getJSONArray("Answers").length() + 1;
		answers = new LinearLayout[numAnswers];

		JSONArray jsonAnswers = jquestion.getJSONArray("Answers");

		for (int i = 0; i < jsonAnswers.length(); ++i) {
			String answer = jsonAnswers.getString(i);
			
			// Custom Checkbox.
			CheckBox cbanswer = new CheckBox(getActivity());
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
			answers[i].setId(i); // TODO Fix the ID behavior
			answers[i].setOnClickListener(onClickListener);
//			answerlayout.addView(answers[i]);

		}
		if (hasOther) {
			// Custom checkbox
			otherCB = new CheckBox(rootView.getContext());
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
