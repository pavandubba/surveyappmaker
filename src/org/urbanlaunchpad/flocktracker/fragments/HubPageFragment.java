package org.urbanlaunchpad.flocktracker.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.urbanlaunchpad.flocktracker.fragments.HubPageManager.HubPageActionListener;
import org.urbanlaunchpad.flocktracker.R;

public class HubPageFragment extends Fragment {

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

  private HubPageActionListener listener;
  private int maleCount = 0;
  private int femaleCount = 0;

  public HubPageFragment(HubPageActionListener listener, int maleCount, int femaleCount) {
    super();
    this.listener = listener;
    this.maleCount = maleCount;
    this.femaleCount = femaleCount;
  }

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
        listener.onStartTrip();
      }
    });

    startSurveyButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        listener.onStartTrip();
      }
    });

    statisticsButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        listener.onGetStatistics();
      }
    });

    moreMenButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        maleCount++;
        maleCountView.setText(Integer.toString(maleCount));
        totalCountView.setText(Integer.toString(maleCount + femaleCount));
        listener.onMaleCountChanged(maleCount);
        fewerMenButton.setEnabled(maleCount > 0);
      }
    });

    fewerMenButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        maleCount--;
        maleCountView.setText(Integer.toString(maleCount));
        totalCountView.setText(Integer.toString(maleCount + femaleCount));
        listener.onMaleCountChanged(maleCount);
        fewerMenButton.setEnabled(maleCount > 0);
      }
    });

    moreWomenButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        femaleCount++;
        femaleCountView.setText(Integer.toString(femaleCount));
        totalCountView.setText(Integer.toString(maleCount + femaleCount));
        listener.onFemaleCountChanged(femaleCount);
        fewerWomenButton.setEnabled(femaleCount > 0);
      }
    });

    fewerWomenButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        femaleCount--;
        femaleCountView.setText(Integer.toString(femaleCount));
        totalCountView.setText(Integer.toString(maleCount + femaleCount));
        listener.onFemaleCountChanged(femaleCount);
        fewerWomenButton.setEnabled(femaleCount > 0);
      }
    });
  }

  @Override
  public void onStart() {
    super.onStart();
    maleCountView.setText(Integer.toString(maleCount));
    femaleCountView.setText(Integer.toString(femaleCount));
    totalCountView.setText(Integer.toString(maleCount + femaleCount));
    fewerMenButton.setEnabled(maleCount > 0);
    fewerWomenButton.setEnabled(femaleCount > 0);
  }
}
