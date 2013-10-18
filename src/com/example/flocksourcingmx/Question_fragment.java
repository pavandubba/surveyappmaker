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

	public class Question_fragment extends Fragment implements View.OnClickListener{
		public static final String ARG_JSON_QUESTION = "Json_chapter";
		private static String[] answerlist = null;
		private Toast toast;
		private JSONObject jquestion = null;
		private JSONArray janswerlist = null;
		private String questionstring = "No questions on chapter";
		private Integer totalanswers;
		private TextView[] tvanswerlist = null;
		private Integer[] tvansweridlist = null;
		public Question_fragment() {
			// Empty constructor required for fragment subclasses
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_question,
					container, false);
			
		
			// Getting json question from parent activity.
			String jquestionstring = getArguments().getString(ARG_JSON_QUESTION);
			try {
				jquestion = new JSONObject(jquestionstring);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				toast = Toast.makeText(getActivity(), "Chapter not recieved from main activity.", Toast.LENGTH_SHORT);
				toast.show();
			}
			
			ViewGroup answerlayout = (ViewGroup) rootView.findViewById(R.id.answerlayout); // Layout for the dynamic content of the activity (mainly answers).

			
			
			try {
				questionstring = jquestion.getString("Question");
				janswerlist = jquestion.getJSONArray("Answers");
				totalanswers = janswerlist.length();
				answerlist = new String[totalanswers];
				tvanswerlist = new TextView[totalanswers];
				tvansweridlist = new Integer[totalanswers];
				for (int i = 0; i < totalanswers; ++i) {
					JSONObject aux = janswerlist.getJSONObject(i);
					answerlist[i] = aux.getString("Answer");
					tvanswerlist[i] = new TextView(rootView.getContext());
					tvanswerlist[i].setText(answerlist[i]);
					answerlayout.addView(tvanswerlist[i]);
					tvansweridlist[i] = i; // Not sure if this is going to have conflicts with other view ids...
					// tvansweridlist[i] = findId(); // This method was to generate valid View ids that were not used by any other View, but it's not working.
					tvanswerlist[i].setId(tvansweridlist[i]);					
					tvanswerlist[i].setOnClickListener(this);
			      }				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				toast = Toast.makeText(getActivity(), "Chapter  does not contain questions, a Question attribute or answers.", Toast.LENGTH_SHORT);
				toast.show();
			}
			
            TextView questionview = (TextView) rootView.findViewById(R.id.questionview);
            questionview.setText(questionstring); 
			
            return rootView;
	
			
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
		
		@Override
		public void onClick(View view) {
		    for (int i = 0; i < totalanswers; ++i) {
		    	if (view instanceof TextView) {
		    	    TextView textView = (TextView) tvanswerlist[i];
			    	if (view.getId() == textView.getId()){
			    		textView.setTextColor(getResources().getColor(R.color.answer_selected));		    	
				      }
			    	else{
			    		textView.setTextColor(getResources().getColor(R.color.text_color_dark));
			    	}
		    	}
		    

		    } 
			
		}
		
//		public int findId(){  
//		    Integer id = 1;
//			View v = getActivity().findViewById(id);  
//		    while (v != null){  
//		        v = getActivity().findViewById(++id);  
//		    }  
//		    return id++;  
//		}
		
		
		
		

	}