package com.lockscreen;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.SharedPreferences;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class ConfirmGesture extends Activity {

	private static final float LENGTH_THRESHOLD = 120.0f;
	private Gesture mGesture;
	private Button confirmButton;
	private SeekBar sensitivity;
	private TextView txtSens;
	private File mStoreFile;
	private float threshold;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.confirm_gesture);

		confirmButton = (Button) findViewById(R.id.confirm_gesture);
		sensitivity = (SeekBar) findViewById(R.id.seekThreshold);
		txtSens = (TextView) findViewById(R.id.txtThreshold);
		threshold = (float) 1.0;
		txtSens.setText(String.format("Sensitivity (%.2f):", threshold));
		
		sensitivity.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar bar, int progress, boolean byUser) {
				threshold = (float) ((float)progress / 20.0);
				txtSens.setText(String.format("Sensitivity (%.2f):", threshold));
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub
				
			}
			
		});
		

		GestureOverlayView overlay = (GestureOverlayView) findViewById(R.id.gestures_recorder);
		overlay.setGestureColor(Color.TRANSPARENT);
		overlay.setUncertainGestureColor(Color.TRANSPARENT);
		overlay.addOnGestureListener(new GesturesProcessor());

		mStoreFile = new File(getExternalFilesDir(null), "gestures");
	}

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mGesture != null) {
			outState.putParcelable("gesture", mGesture);
		}
	}

	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		mGesture = savedInstanceState.getParcelable("gesture");
		if (mGesture != null) {
			final GestureOverlayView overlay = (GestureOverlayView) findViewById(R.id.gestures_recorder);
			overlay.post(new Runnable() {
				public void run() {
					overlay.setGesture(mGesture);
				}
			});
			confirmButton.setEnabled(true);
		}
	}

	public void confirm(View v) {
		if (mGesture != null) {

			final String password = "password";
			GestureLibrary store = GestureLibraries.fromFile(mStoreFile);
			store.load();
			store.removeEntry(password);
			store.addGesture(password, mGesture);
			store.save();

			Log.d("com.lockscreen", "has gesture");

			SharedPreferences settings = v.getContext().getSharedPreferences(
					LockScreenAppActivity.PREF_FILE, 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(LockScreenAppActivity.WHICH_GEST, password);
			editor.putFloat(LockScreenAppActivity.THRESHOLD, threshold);
			editor.commit();

			Log.d("com.lockscreen", "saved gesture");
			Toast.makeText(this, "Gesture Saved", Toast.LENGTH_LONG).show();
			finish();
		}
		Log.d("com.lockscreen", "no gesture");

	}

	private class GesturesProcessor implements
			GestureOverlayView.OnGestureListener {
		public void onGestureStarted(GestureOverlayView overlay,
				MotionEvent event) {
			confirmButton.setEnabled(false);
			mGesture = null;
		}

		public void onGesture(GestureOverlayView overlay, MotionEvent event) {
		}

		public void onGestureEnded(GestureOverlayView overlay, MotionEvent event) {
			mGesture = overlay.getGesture();
			Log.d("com.lockscreen", "Got gesture");
			if (mGesture.getLength() < LENGTH_THRESHOLD) {
				overlay.clear(false);
			}
			if (mGesture != null) {

				GestureLibrary store = GestureLibraries.fromFile(mStoreFile);
				store.load();


				ArrayList<Prediction> predictions = store.recognize(mGesture);
				Log.d("com.lockscreen",
						String.format("size:%d", predictions.size()));
				for(Prediction predict: predictions)
					if (predict.name.equals("newgest"))
						Toast.makeText(overlay.getContext(), String.format("Gesture recognized with threshold %f", predict.score), Toast.LENGTH_LONG).show();
			}
			
			confirmButton.setEnabled(true);
		}

		public void onGestureCancelled(GestureOverlayView overlay,
				MotionEvent event) {
		}
	}

}
