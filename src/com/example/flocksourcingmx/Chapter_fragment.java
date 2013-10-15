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
		private Toast toast;
		private JSONObject jchapter;
		private JSONArray jquestionlist = null;
		private JSONObject jquestion;
		private String questionstring = "No questions on chapter";
		private String chapter = null;
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
			
			try {
				jquestionlist = jchapter.getJSONArray("Questions");
				jquestion = jquestionlist.getJSONObject(0);
				questionstring = jquestion.getString("Question");
				toast = Toast.makeText(getActivity(), jquestionlist.toString(), Toast.LENGTH_SHORT);
				toast.show();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				toast = Toast.makeText(getActivity(), "Chapter " + chapter + " does not contain questions or question does not have a Question attribute.", Toast.LENGTH_SHORT);
				toast.show();
			}
			
		
            TextView v = new TextView(getActivity());
            v.setText(questionstring);
            return v;
	
			
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