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
			NEWSURVEY, STATISTICS, MOREMEN, FEWERMEN, MOREWOMEN, FEWERWOMEN, TOGGLETRIP
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
		View startTripButton = (View) rootView
				.findViewById(R.id.start_trip_button);

		startTripButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				callback.HubButtonPressed(HubButtonCallback.HubButtonType.TOGGLETRIP);
			}
		});

		// start new survey button callback
		View startSurveyButton = (View) rootView
				.findViewById(R.id.startSurveyButton);

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

		// more men button callback
		View moreMenButton = (View) rootView.findViewById(R.id.moreMenButton);

		moreMenButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				callback.HubButtonPressed(HubButtonCallback.HubButtonType.MOREMEN);
			}
		});

		// fewer men button callback
		View fewerMenButton = (View) rootView.findViewById(R.id.fewerMenButton);

		fewerMenButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				callback.HubButtonPressed(HubButtonCallback.HubButtonType.FEWERMEN);
			}
		});

		// more women button callback
		View moreWomenButton = (View) rootView
				.findViewById(R.id.moreWomenButton);

		moreWomenButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				callback.HubButtonPressed(HubButtonCallback.HubButtonType.MOREWOMEN);
			}
		});

		// fewer men button callback
		View fewerWomenButton = (View) rootView
				.findViewById(R.id.fewerWomenButton);

		fewerWomenButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				callback.HubButtonPressed(HubButtonCallback.HubButtonType.FEWERWOMEN);
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
