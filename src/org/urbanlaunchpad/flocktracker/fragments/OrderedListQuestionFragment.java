package org.urbanlaunchpad.flocktracker.fragments;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.adapters.StableArrayAdapter;
import org.urbanlaunchpad.flocktracker.menu.DynamicListView;

import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;

public class OrderedListQuestionFragment extends QuestionFragment {

	Integer totalanswers = null;
	ArrayList<String> answerList = null;
	Button skipButton;
	
	private OnClickListener skipButtonOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			skipButton.setEnabled(false);
			skipButton.setText(R.string.question_skipped);			
		}
	
	};
	
	public void setupLayout() throws JSONException {
		
		ArrayList<String> originalAnswerList = new ArrayList<String>();
		JSONArray jsonAnswers = jquestion.getJSONArray("Answers");
		totalanswers = jsonAnswers.length();

		// Filling array adapter with the answers.
			String aux;
			for (int i = 0; i < totalanswers; ++i) {
				try {
					aux = jsonAnswers.getJSONObject(i).getString("Answer");
					originalAnswerList.add(aux);
				} catch (JSONException e) {
					e.printStackTrace();
					originalAnswerList.add("");
				}

			}

		answerList = new ArrayList<String>();
		answerList.addAll(originalAnswerList);


		ViewGroup questionLayoutView = (ViewGroup) rootView
				.findViewById(R.id.questionlayout);
		ScrollView answerScroll = (ScrollView) rootView
				.findViewById(R.id.answerScroll);
		questionLayoutView.removeView(answerScroll);
		StableArrayAdapter adapter = new StableArrayAdapter(
				rootView.getContext(), R.layout.ordered_answer, answerList);
		answerlistView = (DynamicListView) new DynamicListView(getActivity(),
				this);
		answerlistView.setCheeseList(answerList);
		answerlistView.setAdapter(adapter);
		answerlistView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

		skipButton = (Button) new Button(getActivity());
		skipButton.setEnabled(false);
		skipButton.setText(R.string.question_skipped);

		LinearLayout orderanswerlayout = (LinearLayout) rootView
				.findViewById(R.id.orderanswerlayout);
		orderanswerlayout.setOrientation(LinearLayout.VERTICAL);
		orderanswerlayout.setWeightSum(6f);
		LinearLayout.LayoutParams lParams1 = (LinearLayout.LayoutParams) new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, 0);
		LinearLayout.LayoutParams lParams2 = (LinearLayout.LayoutParams) new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, 0);
		lParams1.weight = 5f;
		lParams2.weight = 1f;

		orderanswerlayout.addView(answerlistView);
		orderanswerlayout.addView(skipButton);
		answerlistView.setLayoutParams(lParams1);
		skipButton.setLayoutParams(lParams2);
		skipButton.setOnClickListener(skipButtonOnClickListener);
	}
	
	void PrepopulateQuestion(){
		// TODO Fix prepopulation
//		getselectedAnswers();
		
		ArrayList<Integer> selectedAnswers = null;
		if (selectedAnswers != null) {
			ArrayList<String> answerTempList = new ArrayList<String>();
			for (int i = 0; i < totalanswers; ++i) {
				answerTempList.add(answerList.get(selectedAnswers.get(i)));
			}
			answerList.clear();
			answerList.addAll(answerTempList);
		}
	}
	
	
}
