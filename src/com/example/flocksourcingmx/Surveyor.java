package com.example.flocksourcingmx;

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
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class Surveyor extends Activity implements
		Question_fragment.AnswerSelected, Question_fragment.PositionPasser {
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
	JSONArray jquestionlist = null;
	JSONObject jchapter;
	JSONObject jquestion = null;
	private JSONObject aux = null;
	private Toast toast;
	private View nextquestionbutton;
	private View previousquestionbutton;
	private Integer questionposition;
	private Integer chapterposition;
	private Integer[] totalquestionsArray;
	String jumpString = null;
	String answerString = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_surveyor);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			jsonsurveystring = extras.getString("jsonsurvey");
			try {
				jsurv = new JSONObject(jsonsurveystring);
				// toast = Toast.makeText(getApplicationContext(),
				// "json recieved"
				// + jsonsurveystring, Toast.LENGTH_SHORT);
				// toast.show();
			} catch (JSONException e) {
				Log.e("JSON Parser", "Error parsing data, check survey file."
						+ e.toString());
			}
		}

		// Obtaining information about survey.

		try {
			jchapterlist = jsurv.getJSONArray("Survey");
			totalchapters = jchapterlist.length();
			ChapterTitles = new String[totalchapters];
			for (int i = 0; i < totalchapters; ++i) {
				aux = jchapterlist.getJSONObject(i);
				ChapterTitles[i] = aux.getString("Chapter");
			}
			// toast = Toast.makeText(getApplicationContext(), "Chapters " +
			// totalchapters, Toast.LENGTH_SHORT);
			// toast.show();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			toast = Toast.makeText(getApplicationContext(),
					"Chapters not parsed, check survey file.",
					Toast.LENGTH_SHORT);
			toast.show();
		}

		// Filling number of questions per chapter.
		totalquestionsArray = new Integer[totalchapters];
		for (int i = 0; i < totalchapters; ++i) {
			try {
				aux = jchapterlist.getJSONObject(i);
				totalquestionsArray[i] = aux.getJSONArray("Questions").length();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				totalquestionsArray[i] = 0;
			}
			// toast = Toast.makeText(this, "No of questions on chapter " + i
			// +":"+totalquestionsArray[i], Toast.LENGTH_SHORT);
			// toast.show();
		}

		// Navigation drawer information.

		Title = ChapterDrawerTitle = getTitle();
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
			chapterposition = 0;
			questionposition = 0;
			selectChapter(chapterposition, questionposition);
		}

		// Next and previous question navigation.

		nextquestionbutton = (View) findViewById(R.id.next_question_button);

		nextquestionbutton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toast = Toast.makeText(Surveyor.this,
						"Antes de click chapterposition" + chapterposition
								+ "questionposition" + questionposition,
						Toast.LENGTH_SHORT);
				toast.show();
				if (jumpString != null) {
					jumpFinder(jumpString);
				} else if (questionposition + 1 < totalquestionsArray[chapterposition]) {
					++questionposition;
				} else if (questionposition + 1 >= totalquestionsArray[chapterposition]) {
					questionposition = 0;
					++chapterposition;
				}
				toast = Toast.makeText(Surveyor.this,
						"Despues de click chapterposition" + chapterposition
								+ "questionposition" + questionposition,
						Toast.LENGTH_SHORT);
				toast.show();

				selectChapter(chapterposition, questionposition);
			}
		});

		previousquestionbutton = (View) findViewById(R.id.previous_question_button);

		previousquestionbutton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

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
			chapterposition = position;
			questionposition = 0;
			selectChapter(chapterposition, questionposition);
		}
	}

	private void selectChapter(int position, int qposition) {
		// update the main content by replacing fragments
		jchapter = null;
		try {
			jchapter = jchapterlist.getJSONObject(position);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// update selected item and title, then close the drawer.
		ChapterDrawerList.setItemChecked(position, true);
		setTitle(ChapterTitles[position]);
		ChapterDrawerLayout.closeDrawer(ChapterDrawerList);

		// Obtaining the question desired to send to fragment
		getQuestion(qposition);

		// Starting question fragment and passing json question information.
		toast = Toast.makeText(this, "Antes de changequestion chapterposition"
				+ chapterposition + "questionposition" + questionposition,
				Toast.LENGTH_SHORT);
		toast.show();
		ChangeQuestion(jquestion, chapterposition, questionposition);

	}

	private void getQuestion(int position) {
		try {
			jquestion = jchapter.getJSONArray("Questions").getJSONObject(
					position);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			toast = Toast.makeText(this, "Question " + position
					+ " does not exist in chapter.", Toast.LENGTH_SHORT);
			toast.show();
			String nullquestionhelper = "{\"Question\":\"No questions on chapter\"}";
			try {
				jquestion = new JSONObject(nullquestionhelper);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}

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

	@Override
	public void onBackPressed() {
		FragmentManager fm = getFragmentManager();
		if (fm.getBackStackEntryCount() > 0) {
			Log.i("MainActivity", "popping backstack");
			fm.popBackStack();
		} else {
			Log.i("MainActivity", "nothing on backstack, calling super");
			super.onBackPressed();
		}
	}

	public void ChangeQuestion(JSONObject jquestion, Integer chapterposition,
			Integer questionposition) {
		// Starting question fragment and passing json question information.
		Fragment fragment = new Question_fragment();
		Bundle args = new Bundle();
		args.putString(Question_fragment.ARG_JSON_QUESTION,
				jquestion.toString());
		toast = Toast.makeText(this, "Antes de Fragment: chapterposition"
				+ chapterposition + "questionposition" + questionposition,
				Toast.LENGTH_SHORT);
		toast.show();
		args.putInt(Question_fragment.ARG_CHAPTER_POSITION, chapterposition);
		args.putInt(Question_fragment.ARG_QUESTION_POSITION, questionposition);
		fragment.setArguments(args);

		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction transaction = fragmentManager.beginTransaction();
		transaction.replace(R.id.surveyor_frame, fragment);
		transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		transaction.addToBackStack(null);
		transaction.commit();

	}

	public void AnswerRecieve(String answerStringRecieve,
			String jumpStringRecieve) {
		answerString = answerStringRecieve;
		jumpString = jumpStringRecieve;
		if (answerString != null) {
			try {
				jsurv.getJSONArray("Survey").getJSONObject(chapterposition)
						.getJSONArray("Questions")
						.getJSONObject(questionposition)
						.put("Answer", answerString);
				// toast = Toast.makeText(this, "Answer passed: " +
				// answerString,
				// Toast.LENGTH_SHORT);
				// toast.show();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// toast = Toast.makeText(this, "After: Jump: " + jumpString
		// + "Answer: " + answerString, Toast.LENGTH_SHORT);
		// toast.show();
	}

	public void PositionRecieve(Integer chapterpositionrecieve,
			Integer questionpositionrecieve) {
		questionposition = questionpositionrecieve;
		chapterposition = chapterpositionrecieve;
		toast = Toast.makeText(this, "DE FRAGMENT chapterposition"
				+ chapterposition + "questionposition" + questionposition,
				Toast.LENGTH_SHORT);
		toast.show();
	}

	public void jumpFinder(String jumpString) {
		// Searches for a question with the same id as the jumpString value
		for (int i = 0; i < totalchapters; ++i) {
			for (int j = 0; j < totalquestionsArray[i]; ++j) {
				String jumpAUX = null;
				try {
					jumpAUX = jsurv.getJSONArray("Survey").getJSONObject(i)
							.getJSONArray("Questions").getJSONObject(j)
							.getString("id");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (jumpString.equals(jumpAUX)) {
					chapterposition = i;
					questionposition = j;
					break;
				}
			}
		}
	}

}