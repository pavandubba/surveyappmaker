package org.urbanlaunchpad.flocktracker.fragments;

import java.util.ArrayList;

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
import android.widget.Toast;

public class OrderedListQuestionFragment extends QuestionFragment {

	
	
	public void setupLayout() throws JSONException {
		
		originalAnswerList = new ArrayList<String>();

		janswerlist = jquestion.getJSONArray("Answers");
		totalanswers = janswerlist.length();

		// Filling array adapter with the answers.
			String aux;
			for (int i = 0; i < totalanswers; ++i) {
				try {
					aux = janswerlist.getJSONObject(i).getString("Answer");
					originalAnswerList.add(aux);
				} catch (JSONException e) {
					e.printStackTrace();
					originalAnswerList.add("");
				}

			}

		answerList = new ArrayList<String>();
		answerList.addAll(originalAnswerList);

		// Prepopulate question
		getselectedAnswers();

		if (selectedAnswers != null) {
			ArrayList<String> answerTempList = new ArrayList<String>();
			for (int i = 0; i < totalanswers; ++i) {
				answerTempList.add(answerList.get(selectedAnswers.get(i)));
			}
			answerList.clear();
			answerList.addAll(answerTempList);
		}

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
		answerString = "";
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
		skipButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				answerString = "";
				ArrayList<Integer> key = getkey();
				Callback.AnswerRecieve(answerString, null, null, inLoopBoolean,
						questionkind, key);
				skipButton.setEnabled(false);
				skipButton.setText(R.string.question_skipped);
			}
		});
		if (selectedAnswers != null) {
			orderedListSendAnswer();
		}		
	}
	
	
}
