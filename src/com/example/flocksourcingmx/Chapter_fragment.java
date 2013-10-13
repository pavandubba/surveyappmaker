package com.example.flocksourcingmx;

import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

	public class Chapter_fragment extends Fragment {
		public static final String ARG_CHAPTER_NUMBER = "chapter_number";
		public static final String ARG_CHAPTER_LIST = "chapter_list";
		public static final String ARG_JSON_CHAPTER = "Json_chapter";
		private Toast toast;
		public Chapter_fragment() {
			// Empty constructor required for fragment subclasses
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_chapter,
					container, false);
			int i = getArguments().getInt(ARG_CHAPTER_NUMBER);			
			String[] chapters = getArguments().getStringArray(ARG_CHAPTER_LIST);
			String jchapterstring = getArguments().getString(ARG_JSON_CHAPTER);
			try {
				JSONObject jchapter = new JSONObject(jchapterstring);
				toast = Toast.makeText(getActivity(), jchapterstring, Toast.LENGTH_SHORT);
				toast.show();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			
			String chapter = chapters[i];
			
			int imageId = getResources().getIdentifier(
					chapter.toLowerCase(Locale.getDefault()), "drawable",
					getActivity().getPackageName());
			((ImageView) rootView.findViewById(R.id.image))
					.setImageResource(imageId);
			getActivity().setTitle(chapter);
			return rootView;
		}
	}