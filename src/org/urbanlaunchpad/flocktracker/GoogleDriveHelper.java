package org.urbanlaunchpad.flocktracker;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import android.accounts.AccountManager;
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
import com.google.api.services.drive.model.ParentReference;

public class GoogleDriveHelper {
	static final int REQUEST_ACCOUNT_PICKER = 1;
	static final int REQUEST_AUTHORIZATION = 2;
	static final int CAPTURE_IMAGE = 3;
	static final String PHOTO_FOLDER_ID = "0BzQnDGTR4fYbQUdLeUUwcXFVOUE";

	public Uri fileUri;
	public static Drive service;
	private Surveyor activity;
	private String jumpString = null;

	public GoogleDriveHelper(Surveyor mainActivity) {
		this.activity = mainActivity;
		service = getDriveService(Iniconfig.credential);
	}

	public void startCameraIntent(String jumpString) {
		String mediaStorageDir = Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_PICTURES).getPath();
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
				.format(new Date());
		fileUri = Uri.fromFile(new java.io.File(mediaStorageDir
				+ java.io.File.separator + "IMG_" + timeStamp + ".jpg"));

		Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

		this.jumpString = jumpString;

		activity.startActivityForResult(cameraIntent, CAPTURE_IMAGE);
	}

	// If we just wish to start the intent without changing jump string
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

	public String saveFileToDrive(final String imagePath) throws IOException {
		try {
			// File's binary content
			java.io.File fileContent = new java.io.File(imagePath);
			FileContent mediaContent = new FileContent("image/jpeg",
					fileContent);

			// File's metadata.
			File body = new File();
			body.setTitle(fileContent.getName());
			body.setMimeType("image/jpeg");
			body.setParents(Arrays.asList(new ParentReference()
					.setId(PHOTO_FOLDER_ID)));

			File file = service.files().insert(body, mediaContent).execute();

			// Notify that we have captured an image
			jumpString = null;

			if (file != null) {
				showToast("Photo uploaded: " + file.getTitle());
			}
			return file.getWebContentLink();
		} catch (UserRecoverableAuthIOException e) {
			activity.startActivityForResult(e.getIntent(),
					REQUEST_AUTHORIZATION);
		}
		return null;
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
