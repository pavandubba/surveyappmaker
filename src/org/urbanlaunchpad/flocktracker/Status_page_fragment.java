package org.urbanlaunchpad.flocktracker;

import java.util.Timer;
import java.util.TimerTask;

import org.urbanlaunchpad.flocktracker.Start_trip_fragment.HubButtonCallback;
import org.urbanlaunchpad.flocktracker.Start_trip_fragment.HubButtonCallback.HubButtonType;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class Status_page_fragment extends Fragment {
	private View rootView;
	private Timer timer = new Timer();

	// Passes Answer to activity.
	StatusPageUpdate updateHandler;

	// The container Activity must implement this interface so the fragment can
	// deliver messages
	public interface StatusPageUpdate {
		/** Called by Fragment when an button is selected */
		public void updateStatusPage();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		rootView = inflater.inflate(R.layout.fragment_status, container, false);

		return rootView;
	}

	@Override
	public void onStart() {
		super.onStart();

		// This makes sure that the container activity has implemented
		// the callback interface. If not, it throws an exception.
		try {
			updateHandler = (StatusPageUpdate) getActivity();
			int period = 1000; // repeat every sec.

			timer.scheduleAtFixedRate(new TimerTask() {
				public void run() {
					updateHandler.updateStatusPage();
				}
			}, 0, period);
		} catch (ClassCastException e) {
			throw new ClassCastException(getActivity().toString()
					+ " must implement StatusPageUpdate");
		}
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// This makes sure that the container activity has implemented
		// the callback interface. If not, it throws an exception.
		try {
			updateHandler = (StatusPageUpdate) activity;
			int period = 1000; // repeat every sec.

			timer.scheduleAtFixedRate(new TimerTask() {
				public void run() {
					updateHandler.updateStatusPage();
				}
			}, 0, period);
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement StatusPageUpdate");
		}
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		timer.cancel();
	}
}
