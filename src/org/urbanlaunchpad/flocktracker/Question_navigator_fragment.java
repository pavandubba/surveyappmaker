package org.urbanlaunchpad.flocktracker;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class Question_navigator_fragment extends Fragment{
	
	// Passes Answer to activity.
	NavButtonCallback callback;
	View rootView;

	// The container Activity must implement this interface so the fragment can
	// deliver messages
	public interface NavButtonCallback {
		public enum NavButtonType {
			NEXT, PREVIOUS, SUBMIT
		};
		
		/** Called by Fragment when an button is selected */
		public void NavButtonPressed(NavButtonType typeButton);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_question_buttons, container,
				false);

		// Next and previous question navigation.
		View nextquestionbutton = (View) rootView.findViewById(R.id.next_question_button);

		nextquestionbutton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				callback.NavButtonPressed(NavButtonCallback.NavButtonType.NEXT);
			}
		});

		View previousquestionbutton = (View) rootView.findViewById(R.id.previous_question_button);

		previousquestionbutton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				callback.NavButtonPressed(NavButtonCallback.NavButtonType.PREVIOUS);
			}
		});

		// Submit button behavior.
		View submitbutton = (View) rootView.findViewById(R.id.submit_survey_button);
		submitbutton.setOnClickListener(new View.OnClickListener() {
	
			@Override
			public void onClick(View v) {
				callback.NavButtonPressed(NavButtonCallback.NavButtonType.SUBMIT);
			}
		});
		
		return rootView;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// This makes sure that the container activity has implemented
		// the callback interface. If not, it throws an exception.
		try {
			callback = (NavButtonCallback) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement NavButtonPressed");
		}
	}
}
