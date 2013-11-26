package com.lockscreen;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

public class RecordGesture extends Activity {

	private static final float LENGTH_THRESHOLD = 120.0f;
	private Gesture mGesture;
	private Button confirmButton;
	private File mStoreFile;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.record_gesture);

		confirmButton = (Button) findViewById(R.id.confirm_gesture);

		GestureOverlayView overlay = (GestureOverlayView) findViewById(R.id.gestures_recorder);
		overlay.setGestureColor(Color.CYAN);
		overlay.setUncertainGestureColor(Color.YELLOW);
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

	public void confirm(final View v) {
		if (mGesture != null) {

			final String password = "newgest";
			GestureLibrary store = GestureLibraries.fromFile(mStoreFile);
			store.load();
			store.removeEntry(password);
			store.addGesture(password, mGesture);
			store.save();

			Log.d("com.lockscreen", "has gesture");
			AlertDialog alertDialog = new AlertDialog.Builder(this).create();
			alertDialog.setTitle("Test Gesture");
			alertDialog.setMessage("WARNING: You are about to set the sensitivity of your gesture. A message shows the score of your gestures. Use this as a basis to set your sensitivity.");
			alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent intent = new Intent(v.getContext(), ConfirmGesture.class);
					startActivity(intent);
					
				}
				
			});
			alertDialog.show();
		}
		Log.d("com.lockscreen", "no gesture");

	}

	public void clear(View v) {
		GestureOverlayView overlay = (GestureOverlayView) findViewById(R.id.gestures_recorder);
		overlay.cancelClearAnimation();
		overlay.clear(true);
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
			confirmButton.setEnabled(true);
		}

		public void onGestureCancelled(GestureOverlayView overlay,
				MotionEvent event) {
		}
	}

}
