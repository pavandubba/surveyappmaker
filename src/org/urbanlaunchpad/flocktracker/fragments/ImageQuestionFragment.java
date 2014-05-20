package org.urbanlaunchpad.flocktracker.fragments;

import org.json.JSONException;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.SurveyorActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

public class ImageQuestionFragment extends QuestionFragment {

	private OnClickListener cameraButtonOnClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			SurveyorActivity.driveHelper.startCameraIntent();
		}
	};

	public void setupLayout() throws JSONException {
		ImageView cameraButton = new ImageView(getActivity());
		cameraButton.setImageResource(R.drawable.camera);
		cameraButton.setOnClickListener(cameraButtonOnClickListener);
		// answerlayout.addView(cameraButton);
		addThumbnail();
	}

	public void addThumbnail() {
		Tuple key = new Tuple(chapterposition, questionposition);
		if (!SurveyorActivity.askingTripQuestions) {
			ArrayList<Integer> key = new ArrayList<Integer>(Arrays.asList(
					chapterposition, questionposition, -1, -1));
			if (SurveyHelper.prevImages.containsKey(key)) {
				Uri imagePath = SurveyHelper.prevImages.get(key);
				ImageView prevImage = new ImageView(rootView.getContext());
				try {
					Bitmap imageBitmap = ImageHelper
							.decodeSampledBitmapFromPath(imagePath.getPath(),
									512, 512);
					prevImage.setImageBitmap(imageBitmap);
					prevImage.setPadding(10, 30, 10, 10);

					answerlayout.addView(prevImage);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else if (SurveyorActivity.askingTripQuestions) {
			ArrayList<Integer> key = new ArrayList<Integer>(Arrays.asList(
					questionposition, -1, -1));
			if (SurveyHelper.prevTrackerImages.containsKey(key)) {
				Uri imagePath = SurveyHelper.prevTrackerImages
						.get(questionposition);
				ImageView prevImage = new ImageView(rootView.getContext());
				try {
					Bitmap imageBitmap = ThumbnailUtils.extractThumbnail(
							BitmapFactory.decodeFile(imagePath.getPath()), 512,
							512);
					prevImage.setImageBitmap(imageBitmap);
					prevImage.setPadding(10, 30, 10, 10);

					answerlayout.addView(prevImage);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendAnswer() {
		// TODO Auto-generated method stub

	}

	@Override
	public void prepopulateQuestion() {
		// TODO Auto-generated method stub

	}

}
