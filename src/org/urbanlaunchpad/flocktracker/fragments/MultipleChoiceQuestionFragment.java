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
import org.urbanlaunchpad.flocktracker.R;

public class MultipleChoiceQuestionFragment extends QuestionFragment {
	private LinearLayout[] answers;

	private final int IMAGE_TAG = -2;
	private final int ANSWER_TAG = -3;
	private Integer lastAnswerId;

	private OnClickListener onClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (lastAnswerId != null) {
				unCheckView(answers[lastAnswerId]);
			}

			checkView((LinearLayout) v);
			lastAnswerId = v.getId();
			sendAnswer();
		}
	};

	// TODO Handle other answer.
	private void unCheckView(LinearLayout view) {
		ImageView iconImageView = (ImageView) view.findViewById(IMAGE_TAG);
		iconImageView.setImageResource(R.drawable.ft_cir_gry);

		TextView answerText = (TextView) view.findViewById(ANSWER_TAG);
		answerText.setTextColor(getResources().getColor(
				R.color.text_color_light));
	}

	private void checkView(LinearLayout view) {
		ImageView iconImageView = (ImageView) view.findViewById(IMAGE_TAG);
		iconImageView.setImageResource(R.drawable.ft_cir_grn);

		TextView answerText = (TextView) view.findViewById(ANSWER_TAG);
		answerText.setTextColor(getResources()
				.getColor(R.color.answer_selected));
		answerText.requestFocus();
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
			TextView answerTextView = new TextView(getActivity());
			answerTextView.setId(ANSWER_TAG);
			answerTextView.setText(answer);
			answerTextView.setTextColor(getResources().getColor(
					R.color.text_color_light));
			answerTextView.setTextSize(20);
			answerTextView.setPadding(10, 10, 10, 10);
			answerTextView.setTypeface(Typeface.create("sans-serif-light",
					Typeface.NORMAL));
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
	

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendAnswer() {
		// TODO Auto-generated method stub

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
