package org.urbanlaunchpad.flocktracker.fragments;

import android.content.Context;
import android.graphics.Typeface;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import com.google.android.gms.drive.internal.i;
import com.google.android.gms.internal.ig;

public class MultipleChoiceQuestionFragment extends QuestionFragment {
	private LinearLayout[] answers;
	private final int IMAGE_TAG = -2;
	private final int ANSWER_TAG = -3;
	private Integer answerId;
	private String answerString;

	private OnClickListener onClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (answerId != null) {
				unCheckView(answers[answerId]);
			}

			checkView((LinearLayout) v);
			answerId = v.getId();
			sendAnswer();
		}
	};

	// TODO Handle other answer.
	private void unCheckView(LinearLayout view) {
		ImageView iconImageView = (ImageView) view.findViewById(IMAGE_TAG);
		iconImageView.setImageResource(R.drawable.ft_cir_gry);
				
		View answerText = (View) view.findViewById(ANSWER_TAG);
		if (answerText instanceof TextView){
			TextView answerTextView = (TextView) answerText;
			answerTextView.setTextColor(getResources()
					.getColor(R.color.text_color_light));
			answerText.requestFocus();
		} else if (answerText instanceof EditText){
			EditText answerEditText = (EditText) answerText;
			answerEditText.setTextColor(getResources()
					.getColor(R.color.text_color_light));
			answerEditText.requestFocus();
		}
	}

	private void checkView(LinearLayout view) {
		ImageView iconImageView = (ImageView) view.findViewById(IMAGE_TAG);
		iconImageView.setImageResource(R.drawable.ft_cir_grn);

		View answerText = (View) view.findViewById(ANSWER_TAG);
		if (answerText instanceof TextView){
			TextView answerTextView = (TextView) answerText;
			answerTextView.setTextColor(getResources()
					.getColor(R.color.answer_selected));
			answerText.requestFocus();
		} else if (answerText instanceof EditText){
			EditText answerEditText = (EditText) answerText;
			answerEditText.setTextColor(getResources()
					.getColor(R.color.answer_selected));
			answerEditText.requestFocus();
		}
	}

	public void setupLayout() throws JSONException {
		
		Boolean hasOther = jquestion.getBoolean("Other");
		
		final int numAnswers = hasOther ? jquestion.getJSONArray("Answers")
				.length() : jquestion.getJSONArray("Answers").length() + 1;
		answers = new LinearLayout[numAnswers];

		JSONArray jsonAnswers = jquestion.getJSONArray("Answers");
		for (int i = 0; i < jsonAnswers.length(); i++) {
			String answer = jsonAnswers.getString(i);

			// Image
			ImageView iconImageView = new ImageView(getActivity());
			iconImageView.setId(IMAGE_TAG);
			iconImageView.setImageResource(R.drawable.ft_cir_gry);
			iconImageView.setAdjustViewBounds(true);
			iconImageView.setPadding(0, 0, 20, 0);
			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, 60);
			layoutParams.gravity = Gravity.CENTER_VERTICAL;
			iconImageView.setLayoutParams(layoutParams);

			// Answer Text
			LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			TextView answerTextView = (TextView) inflater.inflate(getActivity(),R.layout.answer_edit_text, null);
			answerTextView.setId(ANSWER_TAG);
			answerTextView.setText(answer);
			LinearLayout.LayoutParams layoutParamsText = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			layoutParamsText.gravity = Gravity.CENTER_VERTICAL;
			answerTextView.setLayoutParams(layoutParamsText);

			// Add both to a linear layout
			answers[i] = new LinearLayout(getActivity());
			answers[i].setId(i);
			answers[i].setWeightSum(4);
			LinearLayout.LayoutParams layoutParamsParent = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			answers[i].setLayoutParams(layoutParamsParent);
			answers[i].setPadding(10, 10, 10, 10);
			answers[i].addView(iconImageView);
			answers[i].addView(answerTextView);

			// Set onClick
			answers[i].setOnClickListener(onClickListener);
		}

		if (hasOther) {
			final int i = numAnswers - 1;

			// Image
			ImageView otherImage = new ImageView(getActivity());
			otherImage.setId(IMAGE_TAG + i);
			otherImage.setImageResource(R.drawable.ft_cir_gry);
			otherImage.setAdjustViewBounds(true);
			otherImage.setPadding(0, 0, 30, 0);
			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, 60);
			layoutParams.gravity = Gravity.CENTER_VERTICAL;
			otherImage.setLayoutParams(layoutParams);

			// Answer text
			final EditText otherET = new EditText(getActivity());
			otherET.setOnFocusChangeListener(new View.OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					InputMethodManager imm = (InputMethodManager) getActivity()
							.getSystemService(Context.INPUT_METHOD_SERVICE);
					if (hasFocus) {
						imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
					} else {
						imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
					}
				}
			});
			otherET.setId(ANSWER_TAG + i);
			otherET.setHint(getResources().getString(R.string.other_hint));
			otherET.setImeOptions(EditorInfo.IME_ACTION_DONE);
			otherET.setSingleLine();
			otherET.setTextColor(getResources().getColor(
					R.color.text_color_light));
			otherET.setTextSize(20);
			otherET.setTypeface(Typeface.create("sans-serif-light",
					Typeface.NORMAL));
			LinearLayout.LayoutParams layoutParamsText = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			layoutParamsText.gravity = Gravity.CENTER_VERTICAL;
			otherET.setLayoutParams(layoutParamsText);
			otherET.setBackgroundResource(R.drawable.edit_text);

			// Add both to a linear layout
			answers[i] = new LinearLayout(getActivity());
			answers[i].setId(i);
			answers[i].setWeightSum(4);
			LinearLayout.LayoutParams layoutParamsParent = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			answers[i].setLayoutParams(layoutParamsParent);
			answers[i].setPadding(10, 10, 10, 10);
			answers[i].addView(otherImage);
			answers[i].addView(otherET);
			answers[i].setOnClickListener(onClickListener);

			// Used to override the touch mechanism for the EditText
			otherET.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (MotionEvent.ACTION_UP == event.getAction()) {
						onClickListener.onClick(answers[i]);
					}
					return false;
				}
			});
		}
		prepopulateQuestion();
		sendAnswer();
	}
	

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendAnswer() {
		for (int i = 0; i <= answers.length; i++){
			if (answers[i].getId() == answerId){
				View answerView = answers[i].findViewById(ANSWER_TAG);
				if (answerView instanceof TextView){
					TextView answerTextView = (TextView) answerView;
					answerString = (String) answerTextView.getText();
				} else if (answerView instanceof EditText){
					EditText answerEditText = (EditText) answerView;
					answerString = answerEditText.getText().toString();
				}
			}
		}

	}

	@Override
	public void prepopulateQuestion() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void orderedListSendAnswer() {
		// TODO Auto-generated method stub
		
	}
}
