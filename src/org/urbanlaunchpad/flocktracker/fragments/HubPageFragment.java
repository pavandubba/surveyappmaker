package org.urbanlaunchpad.flocktracker.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.urbanlaunchpad.flocktracker.R;

public class HubPageFragment extends Fragment {

    HubButtonCallback callback;
    private View startTripButton;
    private View startSurveyButton;
    private View statisticsButton;
    private View moreMenButton;
    private View fewerMenButton;
    private View moreWomenButton;
    private View fewerWomenButton;
    private TextView maleCountView;
    private TextView femaleCountView;
    private TextView totalCountView;

    private int maleCount = 0;
    private int femaleCount = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_hub_page, container, false);

        this.startTripButton = rootView.findViewById(R.id.start_trip_button);
        this.startSurveyButton = rootView.findViewById(R.id.startSurveyButton);
        this.statisticsButton = rootView.findViewById(R.id.statsButton);
        this.moreMenButton = rootView.findViewById(R.id.moreMenButton);
        this.fewerMenButton = rootView.findViewById(R.id.fewerMenButton);
        this.moreWomenButton = rootView.findViewById(R.id.moreWomenButton);
        this.fewerWomenButton = rootView.findViewById(R.id.fewerWomenButton);
        this.maleCountView = (TextView) rootView.findViewById(R.id.maleCount);
        this.femaleCountView = (TextView) rootView.findViewById(R.id.femaleCount);
        this.totalCountView = (TextView) rootView.findViewById(R.id.totalPersonCount);

        setupClickListeners();

        return rootView;
    }

    private void setupClickListeners() {
        startTripButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.HubButtonPressed(HubButtonCallback.HubButtonType.TOGGLETRIP);
            }
        });


        startSurveyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.HubButtonPressed(HubButtonCallback.HubButtonType.NEWSURVEY);
            }
        });


        statisticsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.HubButtonPressed(HubButtonCallback.HubButtonType.STATISTICS);
            }
        });

        moreMenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                maleCount++;
                maleCountView.setText(Integer.toString(maleCount));
                totalCountView.setText(Integer.toString(maleCount + femaleCount));
            }
        });

        fewerMenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                maleCount--;
                maleCountView.setText(Integer.toString(maleCount));
                totalCountView.setText(Integer.toString(maleCount + femaleCount));
            }
        });

        moreWomenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                femaleCount++;
                femaleCountView.setText(Integer.toString(femaleCount));
                totalCountView.setText(Integer.toString(maleCount + femaleCount));
            }
        });

        fewerWomenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                femaleCount--;
                femaleCountView.setText(Integer.toString(femaleCount));
                totalCountView.setText(Integer.toString(maleCount + femaleCount));
            }
        });
    }

    public void initializeHubPage() {

    }

    @Override
    public void onStart() {
        super.onStart();

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try {
            callback = (HubButtonCallback) getActivity();
            callback.HubButtonPressed(HubButtonCallback.HubButtonType.UPDATE_PAGE);
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                                         + " must implement HubButtonPressed");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try {
            callback = (HubButtonCallback) getActivity();
            callback.HubButtonPressed(HubButtonCallback.HubButtonType.UPDATE_PAGE);
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                                         + " must implement HubButtonPressed");
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try {
            callback = (HubButtonCallback) activity;
            callback.HubButtonPressed(HubButtonCallback.HubButtonType.UPDATE_PAGE);
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                                         + " must implement HubButtonPressed");
        }
    }

    // The container Activity must implement this interface so the fragment can
    // deliver messages
    public interface HubButtonCallback {

        /**
         * Called by Fragment when an button is selected
         */
        public void HubButtonPressed(HubButtonType typeButton);

        public enum HubButtonType {
            NEWSURVEY, STATISTICS, MOREMEN, FEWERMEN, MOREWOMEN, FEWERWOMEN, TOGGLETRIP, UPDATE_PAGE
        }
    }
}
