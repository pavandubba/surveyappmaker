package org.urbanlaunchpad.flocktracker;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.fusiontables.Fusiontables;
import com.google.api.services.fusiontables.Fusiontables.Query.Sql;
import com.google.api.services.fusiontables.model.Sqlresponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.urbanlaunchpad.flocktracker.views.IniconfigManager;
import org.urbanlaunchpad.flocktracker.views.IniconfigManager.IniconfigListener;

import java.io.IOException;
import java.util.Arrays;

public class IniconfigActivity extends Activity implements IniconfigListener {

  public static final int REQUEST_ACCOUNT_PICKER = 1;
  public static final int REQUEST_PERMISSIONS = 2;
  public static final String FUSION_TABLE_SCOPE = "https://www.googleapis.com/auth/fusiontables";
  private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
  private static final JsonFactory JSON_FACTORY = new JacksonFactory();

  public static GoogleAccountCredential credential;
  public static Fusiontables fusiontables;
  public static SharedPreferences prefs;
  String jsonSurveyString;
  private IniconfigManager iniconfigManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_iniconfig);

    prefs = this.getSharedPreferences("org.urbanlaunchpad.flocktracker", Context.MODE_PRIVATE);

    // get credential with scopes
    credential = GoogleAccountCredential.usingOAuth2(this,
      Arrays.asList(FUSION_TABLE_SCOPE, DriveScopes.DRIVE));

    iniconfigManager = (IniconfigManager) findViewById(R.id.iniconfig_view);
    iniconfigManager.initialize(this, prefs.getString("lastProject", null));
    ProjectConfig.get().setSurveyDownloadTableID("1isCCC51fe6nWx27aYWKfZWmk9w2Zj6a4yTyQ5c4");
    ProjectConfig.get().setSurveyUploadTableID("11lGsm8B2SNNGmEsTmuGVrAy1gcJF9TQBo3G1Vw0");
    ProjectConfig.get().setTrackerTableID("1Q2mr8ni5LTxtZRRi3PNSYxAYS8HWikWqlfoIUK4");
    ProjectConfig.get().setApiKey("AIzaSyB4Nn1k2sML-0aBN2Fk3qOXLF-4zlaNwmg");
  }

  @Override
  public void onProjectNameInput(String projectName) {
    if (!projectName.isEmpty()) {
      ProjectConfig.get().setProjectName(projectName);
      prefs.edit().putString("lastProject", projectName).commit();
      parseSurvey();
    }
  }

  @Override
  public void displayUsernameSelection() {
    // Google credentials
    startActivityForResult(credential.newChooseAccountIntent(),
      REQUEST_ACCOUNT_PICKER);
  }

  @Override
  public void onContinue() {
    // Go to survey
    startActivity(new Intent(getApplicationContext(), SurveyorActivity.class));
  }

    /*
     * Submission getting helper functions
     */

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    switch (requestCode) {
      case REQUEST_ACCOUNT_PICKER:
        if (resultCode == RESULT_OK && data != null
            && data.getExtras() != null) {
          String username = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
          ProjectConfig.get().setUsername(username);
          credential.setSelectedAccountName(username);

          fusiontables = new Fusiontables.Builder(HTTP_TRANSPORT,
            JSON_FACTORY, credential)
            .setApplicationName("UXMexico").build();

          // update our username field
          iniconfigManager.setUsername(username);
        }
        break;
      case REQUEST_PERMISSIONS:
        if (resultCode == RESULT_OK) {
          parseSurvey();
        } else {
          startActivityForResult(credential.newChooseAccountIntent(),
            REQUEST_ACCOUNT_PICKER);
        }
        break;
    }
  }

  public boolean getSurvey(String tableId) throws UserRecoverableAuthIOException, IOException {
    String MASTER_TABLE_ID = ProjectConfig.get().getSurveyDownloadTableID();
    Sql sql = fusiontables.query().sql(
      "SELECT survey_json FROM " + MASTER_TABLE_ID
      + " WHERE table_id = '" + tableId + "'"
    );
    sql.setKey(ProjectConfig.get().getApiKey());

    Sqlresponse response = sql.execute();
    if (response == null || response.getRows() == null) {
      return false;
    }

    jsonSurveyString = response.getRows().get(0).get(0).toString();

    // save this for offline use
    prefs.edit().putString("jsonSurveyString", jsonSurveyString).commit();
    Log.v("response", jsonSurveyString);
    return true;
  }

  public void parseSurvey() {
    iniconfigManager.onParsingSurvey();
    // get and parse survey
    new AsyncTask<Void, Void, Boolean>() {

      @Override
      protected Boolean doInBackground(Void... params) {
        try {
          getSurvey(ProjectConfig.get().getProjectName());
          return true;
        } catch (UserRecoverableAuthIOException e) {
          startActivityForResult(e.getIntent(), REQUEST_PERMISSIONS);
          return false;
        } catch (IOException e) {
          // If can't get updated version, use cached survey
          if (ProjectConfig.get().getProjectName().equals(prefs.getString("lastProject", ""))) {
            jsonSurveyString = prefs.getString("jsonSurveyString", "");
          } else {
            e.printStackTrace();
          }
          return true;
        }
      }

      @Override
      protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);
        if (success) {
          try {
            if (jsonSurveyString == null) {
              throw new JSONException("Could not parse empty string");
            }
            // Try to parse
            new JSONObject(jsonSurveyString);
            ProjectConfig.get().setOriginalJSONSurveyString(jsonSurveyString);
            iniconfigManager.onSurveyParsedCorrectly();
          } catch (JSONException e) {
            Log.e("JSON Parser",
              "Error parsing data " + e.toString());
            iniconfigManager.onSurveyParsedIncorrectly();
            ProjectConfig.get().setOriginalJSONSurveyString(null);
          }
        }
      }
    }.execute();
  }
}
