package com.example.flocksourcingmx;

import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

public class Surveyor extends Activity {
	private DrawerLayout ChapterDrawerLayout;
	private ListView ChapterDrawerList;
	private ActionBarDrawerToggle ChapterDrawerToggle;

	private CharSequence ChapterDrawerTitle;
	private CharSequence Title;
	private static String[] ChapterTitles;

	private String jsonsurveystring;
	private JSONObject jsurv = null;
	private int totalchapters;
	private JSONArray jchapterlist = null;
	private JSONObject aux = null;
	private Toast toast;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_surveyor);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			jsonsurveystring = extras.getString("jsonsurvey");
			try {
				jsurv = new JSONObject(jsonsurveystring);
				toast = Toast.makeText(getApplicationContext(), "json recieved"
						+ jsonsurveystring, Toast.LENGTH_SHORT);
				toast.show();
			} catch (JSONException e) {
				Log.e("JSON Parser", "Error parsing data " + e.toString());
			}
		}

		try {
			jchapterlist = jsurv.getJSONArray("Survey");
			totalchapters = jchapterlist.length();
			ChapterTitles = new String[totalchapters];
			for (int i = 0; i < totalchapters; ++i) {
				aux = jchapterlist.getJSONObject(i);
				ChapterTitles[i] = aux.getString("Chapter");
//			    toast = Toast.makeText(getApplicationContext(), "Chapters " + totalchapters, Toast.LENGTH_SHORT);
//			    toast.show();
			}
			toast = Toast.makeText(getApplicationContext(), "Chapters parsed.",
					Toast.LENGTH_SHORT);
			toast.show();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Title = ChapterDrawerTitle = getTitle();
		// ChapterTitles = new String[] { "abu", "seee", "go" };
		ChapterDrawerLayout = (DrawerLayout) findViewById(R.id.chapter_drawer_layout);
		ChapterDrawerList = (ListView) findViewById(R.id.chapter_drawer);

		// set a custom shadow that overlays the main content when the drawer
		// opens
		ChapterDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
				GravityCompat.START);
		// set up the drawer's list view with items and click listener
		ChapterDrawerList.setAdapter(new ArrayAdapter<String>(this,
				R.layout.chapter_drawer_list_item, ChapterTitles));
		ChapterDrawerList.setOnItemClickListener(new DrawerItemClickListener());
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);
		ChapterDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
		ChapterDrawerLayout, /* DrawerLayout object */
		R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
		R.string.chapter_drawer_open, /* For accessibility */
		R.string.chapter_drawer_close /* For accessibility */
		) {
			public void onDrawerClosed(View view) {
				getActionBar().setTitle(Title);
			}

			public void onDrawerOpened(View drawerView) {
				getActionBar().setTitle(ChapterDrawerTitle);
			}
		};
		ChapterDrawerLayout.setDrawerListener(ChapterDrawerToggle);

		if (savedInstanceState == null) {
			selectChapter(0);
		}

	}

	public boolean onOptionsItemSelected(MenuItem item) {
		// To make the action bar home/up action should open or close the
		// drawer.
		if (ChapterDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		return true;
	}

	private class DrawerItemClickListener implements
			ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			selectChapter(position);
		}
	}

	private void selectChapter(int position) {
		// update the main content by replacing fragments
		JSONObject jchapter = null;
		try {
			jchapter = jchapterlist.getJSONObject(position);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Fragment fragment = new Chapter_fragment();
		Bundle args = new Bundle();
		args.putInt(Chapter_fragment.ARG_CHAPTER_NUMBER, position);
		args.putStringArray(Chapter_fragment.ARG_CHAPTER_LIST, ChapterTitles);
		args.putString(Chapter_fragment.ARG_JSON_CHAPTER, jchapter.toString());
		fragment.setArguments(args);
		
		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction transaction = fragmentManager.beginTransaction();
		transaction.replace(R.id.surveyor_frame, fragment);
		transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		transaction.addToBackStack(null);
		transaction.commit();
		
		// update selected item and title, then close the drawer.
		ChapterDrawerList.setItemChecked(position, true);
		setTitle(ChapterTitles[position]);
		ChapterDrawerLayout.closeDrawer(ChapterDrawerList);
	}

	@Override
	public void setTitle(CharSequence title) {
		Title = title;
		getActionBar().setTitle(Title);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		ChapterDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggles
		ChapterDrawerToggle.onConfigurationChanged(newConfig);
	}

//	public static class Chapter_fragment extends Fragment {
//		public static final String ARG_CHAPTER_NUMBER = "chapter_number";
//
//		public Chapter_fragment() {
//			// Empty constructor required for fragment subclasses
//		}
//
//		@Override
//		public View onCreateView(LayoutInflater inflater, ViewGroup container,
//				Bundle savedInstanceState) {
//			View rootView = inflater.inflate(R.layout.fragment_chapter,
//					container, false);
//			int i = getArguments().getInt(ARG_CHAPTER_NUMBER);
//			String chapter = ChapterTitles[i];
//
//			int imageId = getResources().getIdentifier(
//					chapter.toLowerCase(Locale.getDefault()), "drawable",
//					getActivity().getPackageName());
//			((ImageView) rootView.findViewById(R.id.image))
//					.setImageResource(imageId);
//			getActivity().setTitle(chapter);
//			return rootView;
//		}
//	}

}