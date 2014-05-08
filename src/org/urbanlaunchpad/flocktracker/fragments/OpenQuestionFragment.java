package org.urbanlaunchpad.flocktracker.fragments;

import org.json.JSONException;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.fragments.QuestionFragment.questionInterface;

import android.graphics.Typeface;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OpenQuestionFragment extends QuestionFragment  implements questionInterface  {
	EditText openET;
	Boolean askingNumbers;
	LinearLayout answerLayout;
	
	private OnClickListener onClickListener = new OnClickListener()  {

		@Override
		public void onClick(View v) {
		// TODO Handling of changes in the answer.
		}
	};
	
	public void setupLayout() throws JSONException {		
		String questionkind = jquestion.getString("kind");
		if ((questionkind.equals("ON"))||questionkind.equals("LP")){
			askingNumbers = true;
		};
		
		openET = new EditText(getActivity());
		openET.setHint(getResources().getString(R.string.answer_hint));
		openET.setImeOptions(EditorInfo.IME_ACTION_DONE);
		if (askingNumbers) {
			openET.setInputType(InputType.TYPE_CLASS_NUMBER
					| InputType.TYPE_NUMBER_FLAG_DECIMAL);
		}
		openET.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
		openET.setSingleLine();
		openET.setTextSize(20);
		openET.setTextColor(getResources().getColor(R.color.text_color_light));
		openET.setBackgroundResource(R.drawable.edit_text);
		answerLayout.addView(openET);
		openET.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					onClick(openET);
					return false; // If false hides the keyboard after
				}
				return false;
			}
		});
		openET.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (MotionEvent.ACTION_UP == event.getAction()) {
					onClick(openET);
				}
				return false;
			}
		});
		
		
		
		
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
	public void sendAnswer() {
		// TODO Auto-generated method stub
		
	}
	
	
	

}
