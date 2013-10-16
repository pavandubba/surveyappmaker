package com.example.flocksourcingmx;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

	public class Chapter_fragment extends Fragment {
		public static final String ARG_JSON_CHAPTER = "Json_chapter";
		private static String[] answerlist = null;
		private Toast toast;
		private JSONObject jchapter = null;
		private JSONArray jquestionlist = null;
		private JSONObject jquestion = null;
		private JSONArray janswerlist = null;
		private String questionstring = "No questions on chapter";
		private String chapter = null;
		private Integer totalanswers;
		private TextView[] tvanswerlist = null;
		public Chapter_fragment() {
			// Empty constructor required for fragment subclasses
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_chapter,
					container, false);
			
		
			// Getting json chapter from parent activity.
			String jchapterstring = getArguments().getString(ARG_JSON_CHAPTER);
			try {
				jchapter = new JSONObject(jchapterstring);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				toast = Toast.makeText(getActivity(), "Chapter not recieved from main activity.", Toast.LENGTH_SHORT);
				toast.show();
			}

			try {
				chapter = jchapter.getString("Chapter");
				
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				toast = Toast.makeText(getActivity(), "Chapter does not contain a chapter name.", Toast.LENGTH_SHORT);
				toast.show();
			}
			
			ViewGroup questionlayout = (ViewGroup) rootView.findViewById(R.id.questionlayout); // Layout for the dynamic content of the activity (mainly answers).

			
			
			try {
				jquestionlist = jchapter.getJSONArray("Questions");
				jquestion = jquestionlist.getJSONObject(0);  // Its getting only the first question of the chapter.
				questionstring = jquestion.getString("Question");
	            TextView questionview = new TextView(rootView.getContext());
	            questionview.setText(questionstring);            
	            questionlayout.addView(questionview);
				janswerlist = jquestion.getJSONArray("Answers");
				totalanswers = janswerlist.length();
				answerlist = new String[totalanswers];
				tvanswerlist = new TextView[totalanswers];
				for (int i = 0; i < totalanswers; ++i) {
					JSONObject aux = janswerlist.getJSONObject(i);
					answerlist[i] = aux.getString("Answer");
					tvanswerlist[i] = new TextView(rootView.getContext());
					tvanswerlist[i].setText(answerlist[i]);
					questionlayout.addView(tvanswerlist[i]);
			      }				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				toast = Toast.makeText(getActivity(), "Chapter " + chapter + " does not contain questions, a Question attribute or answers.", Toast.LENGTH_SHORT);
				toast.show();
			}
			
			
			
            return rootView;
            // The problem is about the addView part of the code and the returning of the rootView layout.
            // return questionview;
	
			
			// Changing background image, for debugging purpose.
//			int imageId = getResources().getIdentifier(
//					chapter.toLowerCase(Locale.getDefault()), "drawable",
//					getActivity().getPackageName());
//			((ImageView) rootView.findViewById(R.id.image))
//					.setImageResource(imageId);
//			
//			// Setting activity titles as the chapter.
//			getActivity().setTitle(chapter);
//			return rootView;
		}
		
		

	}