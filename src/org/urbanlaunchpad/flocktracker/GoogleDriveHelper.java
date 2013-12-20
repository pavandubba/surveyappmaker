package org.urbanlaunchpad.flocktracker;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

public class GoogleDriveHelper {
	static final int REQUEST_ACCOUNT_PICKER = 1;
	static final int REQUEST_AUTHORIZATION = 2;
	static final int CAPTURE_IMAGE = 3;

	private static Uri fileUri;
	public static Drive service;
	private Activity activity;

	public GoogleDriveHelper(Activity mainActivity) {
		this.activity = mainActivity;
	}

	public void startCameraIntent() {
		String mediaStorageDir = Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_PICTURES).getPath();
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
				.format(new Date());
		fileUri = Uri.fromFile(new java.io.File(mediaStorageDir
				+ java.io.File.separator + "IMG_" + timeStamp + ".jpg"));

		Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
		activity.startActivityForResult(cameraIntent, CAPTURE_IMAGE);
	}

	public void saveFileToDrive() {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					// File's binary content
					java.io.File fileContent = new java.io.File(
							fileUri.getPath());
					FileContent mediaContent = new FileContent("image/jpeg",
							fileContent);

					// File's metadata.
					File body = new File();
					body.setTitle(fileContent.getName());
					body.setMimeType("image/jpeg");

					File file = service.files().insert(body, mediaContent)
							.execute();
					if (file != null) {
						showToast("Photo uploaded: " + file.getTitle());
						startCameraIntent();
					}
				} catch (UserRecoverableAuthIOException e) {
					activity.startActivityForResult(e.getIntent(),
							REQUEST_AUTHORIZATION);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		t.start();
	}

	public void showToast(final String toast) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(activity.getApplicationContext(), toast,
						Toast.LENGTH_SHORT).show();
			}
		});
	}

	public Drive getDriveService(GoogleAccountCredential credential) {
		return new Drive.Builder(AndroidHttp.newCompatibleTransport(),
				new GsonFactory(), credential).build();
	}

	public void requestAccountPicker(Intent intent) {
		String accountName = intent
				.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
		if (accountName != null) {
			Iniconfig.credential.setSelectedAccountName(accountName);
			service = getDriveService(Iniconfig.credential);
			startCameraIntent();
		}
	}

}
