package org.urbanlaunchpad.flocktracker.views;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.urbanlaunchpad.flocktracker.ProjectConfig;
import org.urbanlaunchpad.flocktracker.R;

public class IniconfigView extends LinearLayout implements IniconfigManager {
  private TextView usernameField;
  private TextView projectNameField;
  private RelativeLayout navBar;
  private ImageView continueButton;
  private AutoCompleteTextView input;
  private IniconfigListener listener;

  private AlertDialog alertDialog;

  public IniconfigView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    this.usernameField = (TextView) findViewById(R.id.usernameText);
    this.projectNameField = (TextView) findViewById(R.id.projectNameText);
    this.navBar = (RelativeLayout) findViewById(R.id.iniconfig_navbar);
    this.continueButton = (ImageView) findViewById(R.id.bcontinue);
    this.input = new AutoCompleteTextView(getContext());
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    View projectNameSelectRow = findViewById(R.id.projectNameRow);
    View usernameSelectRow = findViewById(R.id.usernameRow);

    usernameSelectRow.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        listener.displayUsernameSelection();
      }
    });

    projectNameSelectRow.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        displayInputName();
      }
    });

    continueButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (ProjectConfig.get().getOriginalJSONSurveyString() == null) {
          Toast toast = Toast.makeText(getContext(),
              R.string.invalid_user_project, Toast.LENGTH_SHORT);
          toast.show();
          return;
        }

        listener.onContinue();
      }
    });
  }

  @Override
  public void initialize(IniconfigListener listener, String lastProjectName) {
    this.listener = listener;

    // initialize dialog for inputting project name
    AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
    alert.setTitle(R.string.select_project);

    if (lastProjectName != null && !lastProjectName.isEmpty()) {
      // Create the adapter and set it to the AutoCompleteTextView
      ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1,
          new String[]{lastProjectName});
      input.setThreshold(1);
      input.setAdapter(adapter);
      input.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1,
            int arg2, long arg3) {
          onProjectNameInput();
        }
      });
    }

    alert.setView(input);

    alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        onProjectNameInput();
      }
    });

    alert.setNegativeButton(R.string.cancel,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            hideKeyboard();
            dialog.dismiss();
          }
        }
    );

    alertDialog = alert.create();
  }

  @Override
  public void setUsername(String username) {
    usernameField.setText(username);
  }

  public void setProjectName(String projectName) {
    projectNameField.setText(projectName);
  }

  @Override
  public void onSurveyParsedCorrectly() {
    navBar.removeViewAt(0);
    continueButton.setClickable(true);

    // got survey!
    Toast.makeText(getContext(), R.string.survey_parsed, Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onSurveyParsedIncorrectly() {
    navBar.removeViewAt(0);
    continueButton.setClickable(true);
    projectNameField.setText(getContext().getString(R.string.projectNameButtonDesc));

    // got bad/no survey!
    Toast.makeText(getContext(), R.string.no_survey_obtained, Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onParsingSurvey() {
    if (continueButton.isClickable()) {
      ProgressBar loading = new ProgressBar(getContext());
      RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
          RelativeLayout.LayoutParams.WRAP_CONTENT,
          RelativeLayout.LayoutParams.WRAP_CONTENT);
      params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
      loading.setLayoutParams(params);

      navBar.addView(loading, 0);
      continueButton.setClickable(false);
    }
  }

  private void onProjectNameInput() {
    String projectName = input.getText().toString().trim();
    listener.onProjectNameInput(projectName);
    setProjectName(projectName);
    hideKeyboard();
    alertDialog.dismiss();
  }

  private void requestFocusAndShowKeyboard() {
    input.requestFocus();
    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
  }

  private void hideKeyboard() {
    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
  }

  private void displayInputName() {
    String username = ProjectConfig.get().getUsername();
    if (username == null || username.isEmpty()) {
      Toast toast = Toast.makeText(getContext(),
          R.string.select_user_first, Toast.LENGTH_SHORT);
      toast.show();
      return;
    }

    input.setText(ProjectConfig.get().getProjectName());
    requestFocusAndShowKeyboard();
    alertDialog.show();
  }
}
