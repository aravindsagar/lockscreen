package com.lockscreen;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

public class LockScreenAppActivity extends Activity {

	public static final String PREF_FILE = "com.lockscreen.prefs";
	public static final String LOCKED = "com.lockscreen.locked";
	public static final String LAUNCHER = "com.lockscreen.launcher";
	public static final String WHICH_GEST = "com.lockscreen.gesture";
	public static final String THRESHOLD = "com.lockscreen.threshold";

	private boolean locked = false;

	@Override
	public void onAttachedToWindow() {
		// TODO Auto-generated method stub
		this.getWindow().setType(
				WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
		// | WindowManager.LayoutParams.FLAG_FULLSCREEN);

		super.onAttachedToWindow();
	}

	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
						| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

		setContentView(R.layout.lock_layout);

		final SharedPreferences settings = getSharedPreferences(PREF_FILE, 0);
		String launcher = settings.getString(LAUNCHER, "launcher");
		locked = settings.getBoolean(LOCKED, false);

		if (getIntent() != null && getIntent().hasExtra("kill")
				&& getIntent().getExtras().getInt("kill") == 1) {
			finish();
		}
		if (!locked) {
			ActivityManager mngr = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

			List<ActivityManager.RunningTaskInfo> taskList = mngr
					.getRunningTasks(2);
			int sizeStack = mngr.getRunningTasks(1).size();
			for (int i = 0; i < sizeStack; i++) {

				ComponentName cn = mngr.getRunningTasks(2).get(i).topActivity;
				Log.d("com.lockscreen", cn.getClassName());
			}
			//Log.d("com.lockscreen", String.format("Num activities : %d",taskList.get(1).numActivities));
			if (taskList.size() > 1
					&& !taskList.get(1).topActivity.getClassName().contains(
							launcher)) {
				PackageManager pm = getPackageManager();
				Intent i = new Intent("android.intent.action.MAIN");
				i.addCategory("android.intent.category.HOME");
				List<ResolveInfo> lst = pm.queryIntentActivities(i, 0);
				if (!lst.isEmpty()) {
					for (ResolveInfo l : lst) {
						if (l.activityInfo.packageName.contains(launcher)) {
							ActivityInfo activity = l.activityInfo;
							ComponentName name = new ComponentName(
									activity.applicationInfo.packageName,
									activity.name);
							Intent intent = new Intent(Intent.ACTION_MAIN);

							intent.addCategory(Intent.CATEGORY_LAUNCHER);
							intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
									| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
							intent.setComponent(name);

							startActivity(intent);
							finish();
						}
					}
				}
			}
			finish();
		}

		try {
			// initialize receiver

			startService(new Intent(this, MyService.class));

			StateListener phoneStateListener = new StateListener();
			TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
			telephonyManager.listen(phoneStateListener,
					PhoneStateListener.LISTEN_CALL_STATE);

			final GestureLibrary mLibrary = GestureLibraries.fromRawResource(
					this, R.raw.gestures);
			if (!mLibrary.load()) {
				finish();
			}

			GestureOverlayView gestures = (GestureOverlayView) findViewById(R.id.gestures);
			gestures.setGestureColor(Color.TRANSPARENT);
			gestures.setUncertainGestureColor(Color.TRANSPARENT);
			gestures.addOnGesturePerformedListener(new OnGesturePerformedListener() {

				@Override
				public void onGesturePerformed(GestureOverlayView overlay,
						Gesture gesture) {
					File f = new File(overlay.getContext().getExternalFilesDir(
							null), "gestures");

					String which = settings.getString(WHICH_GEST, "Unlock");
					Log.d("com.lockscreen",
							String.format("does it equal '%s'", which));

					GestureLibrary lib = null;
					if (f.exists()) {
						lib = GestureLibraries.fromFile(f);
						lib.load();
					} else {
						Toast.makeText(overlay.getContext(), "Unlocked",
								Toast.LENGTH_LONG).show();
						Log.d("com.lockscreen",
								String.format("unlocked with no gesture"));
						lock(overlay.getContext(), false);
						finish();
						return;
					}

					Log.d("com.lockscreen", "checking gesture");
					Log.d("com.lockscreen", String.format("found %d gestures",
							lib.getGestureEntries().size()));

					ArrayList<Prediction> predictions = lib.recognize(gesture);
					Log.d("com.lockscreen",
							String.format("size:%d", predictions.size()));
					Log.d("com.lockscreen",
							String.format("threshold:%f",
									settings.getFloat(THRESHOLD, (float) 2.0)));
					if (predictions.size() > 0)
						Log.d("com.lockscreen", String.format("score:%f",
								predictions.get(0).score));
					if (predictions.size() > 0) {
						for (Prediction predict : predictions) {
							String result = predict.name;

							if (which.equalsIgnoreCase(result)
									&& predict.score > settings.getFloat(
											THRESHOLD, (float) 2.0)) {
								Toast.makeText(overlay.getContext(),
										"Unlocked", Toast.LENGTH_LONG).show();
								Log.d("com.lockscreen",
										String.format("unlocked"));
								lock(overlay.getContext(), false);
								finish();
							}
						}
					} else if (predictions.size() <= 0) {
						Toast.makeText(overlay.getContext(), "Unlocked",
								Toast.LENGTH_LONG).show();
						Log.d("com.lockscreen",
								String.format("unlocked with no gesture"));
						lock(overlay.getContext(), false);
						finish();
						return;

					}
				}

			});

		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	class StateListener extends PhoneStateListener {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {

			super.onCallStateChanged(state, incomingNumber);
			switch (state) {
			case TelephonyManager.CALL_STATE_RINGING:
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK:
				System.out.println("call Activity off hook");
				finish();

				break;
			case TelephonyManager.CALL_STATE_IDLE:
				break;
			}
		}
	};

	@Override
	public void onBackPressed() {
		// Don't allow back to dismiss.
		return;
	}

	// only used in lockdown mode
	@Override
	protected void onPause() {
		super.onPause();

		ActivityManager am = (ActivityManager) getApplicationContext()
				.getSystemService(Context.ACTIVITY_SERVICE);
		// am.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_WITH_HOME);
		am.moveTaskToFront(getTaskId(), 0);
		// Don't hang around.
		// finish();
	}

	@Override
	protected void onStop() {
		super.onStop();

		// Don't hang around.
		// finish();
	}

	@Override
	public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {

		if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
				|| (keyCode == KeyEvent.KEYCODE_POWER)
				|| (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
				|| (keyCode == KeyEvent.KEYCODE_CAMERA)) {
			// this is where I can do my stuff
			return true; // because I handled the event
		}
		if ((keyCode == KeyEvent.KEYCODE_HOME)) {

			return true;
		}

		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			return true;
		} else if ((keyCode == KeyEvent.KEYCODE_CALL)) {
			return true;
		}
		return true;
		// return super.onKeyDown(keyCode, event);

	}

	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_POWER
				|| (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN)
				|| (event.getKeyCode() == KeyEvent.KEYCODE_POWER)) {
			// Intent i = new Intent(this, NewActivity.class);
			// startActivity(i);
			return false;
		}
		if ((event.getKeyCode() == KeyEvent.KEYCODE_HOME)) {
			return true;
		}
		return false;
	}

	/*
	 * public void onWindowFocusChanged(boolean hasFocus) {
	 * super.onWindowFocusChanged(hasFocus);
	 * 
	 * Log.d("Focus debug", "Focus changed !");
	 * 
	 * if (!hasFocus) { Log.d("Focus debug", "Lost focus !");
	 * 
	 * ActivityManager am = (ActivityManager)
	 * getSystemService(Context.ACTIVITY_SERVICE);
	 * am.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_WITH_HOME);
	 * sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)); } }
	 */

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

		if (!hasFocus) {
			windowCloseHandler.postDelayed(windowCloserRunnable, 250);
		}
	}

	public static void lock(Context c, boolean set) {
		SharedPreferences settings = c.getSharedPreferences(PREF_FILE, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(LOCKED, set);
		editor.commit();
	}

	private Handler windowCloseHandler = new Handler();
	private Runnable windowCloserRunnable = new Runnable() {
		@Override
		public void run() {
			ActivityManager am = (ActivityManager) getApplicationContext()
					.getSystemService(Context.ACTIVITY_SERVICE);
			ComponentName cn = am.getRunningTasks(1).get(0).topActivity;

			Log.d("com.lockscreen",
					String.format("component: '%s'", cn.getClassName()));
			// "com.android.systemui.recent.RecentsActivity"
			if (cn != null
					&& !cn.getClassName().equals(
							"com.lockscreen.LockScreenAppActivity")) {
				am.moveTaskToFront(getTaskId(), 0);
			}
		}
	};

	public void onDestroy() {

		super.onDestroy();
	}

}