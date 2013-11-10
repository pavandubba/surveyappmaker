package org.urbanlaunchpad.flocktracker;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class Start_trip_fragment extends Fragment {
	private View rootView;

	// Passes Answer to activity.
	HubButtonCallback callback;

	// The container Activity must implement this interface so the fragment can
	// deliver messages
	public interface HubButtonCallback {
		public enum HubButtonType {
			NEWSURVEY, STATISTICS
		}
		/** Called by Fragment when an button is selected */
		public void HubButtonPressed(HubButtonType typeButton);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		rootView = inflater.inflate(R.layout.activity_start_trip, container,
				false);
		
		
		// start new survey button callback
		View startSurveyButton = (View) rootView.findViewById(R.id.startSurveyButton);

		startSurveyButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				callback.HubButtonPressed(HubButtonCallback.HubButtonType.NEWSURVEY);
			}
		});
		
		// statistics button callback
		View statisticsButton = (View) rootView.findViewById(R.id.statsButton);

		statisticsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				callback.HubButtonPressed(HubButtonCallback.HubButtonType.STATISTICS);
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
			callback = (HubButtonCallback) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement HubButtonPressed");
		}
	}
}
