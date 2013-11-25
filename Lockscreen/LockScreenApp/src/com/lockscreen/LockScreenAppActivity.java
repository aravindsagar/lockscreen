package com.lockscreen;

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
import android.os.Bundle;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

public class LockScreenAppActivity extends Activity {

	/** Called when the activity is first created. */
	@SuppressWarnings("deprecation")
	KeyguardManager.KeyguardLock k1;
	boolean inDragMode;
	int selectedImageViewX;
	int selectedImageViewY;
	int windowwidth;
	int windowheight;
	ImageView droid, phone, home;
	// int phone_x,phone_y;
	int home_x, home_y;
	int[] droidpos;

	public static final String PREF_FILE = "com.lockscreen.prefs";
	public static final String LOCKED = "com.lockscreen.locked";
	public static final String LAUNCHER = "com.lockscreen.launcher";
	
	private boolean locked = false;
	
	private LayoutParams layoutParams;

	@Override
	public void onAttachedToWindow() {
		// TODO Auto-generated method stub
		this.getWindow().setType(
				WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
					//	| WindowManager.LayoutParams.FLAG_FULLSCREEN);

		super.onAttachedToWindow();
	}

	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
						| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
				//		| WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.main);
		droid = (ImageView) findViewById(R.id.droid);

		final SharedPreferences settings = getSharedPreferences(PREF_FILE,0);
		String launcher = settings.getString(LAUNCHER, "launcher");
		locked = settings.getBoolean(LOCKED, false);
		
		if (getIntent() != null && getIntent().hasExtra("kill")
				&& getIntent().getExtras().getInt("kill") == 1) {
			finish();
		}
		if (!locked) {
			PackageManager pm = getPackageManager();
			Intent i = new Intent("android.intent.action.MAIN");
			i.addCategory("android.intent.category.HOME");
			List<ResolveInfo> lst = pm.queryIntentActivities(i, 0);
			if (!lst.isEmpty()) {
				for (ResolveInfo l : lst) {
					if (l.activityInfo.packageName.contains(launcher)) {
						ActivityInfo activity=l.activityInfo;
						ComponentName name=new ComponentName(activity.applicationInfo.packageName,
						                                     activity.name);
						Intent intent=new Intent(Intent.ACTION_MAIN);
		
						intent.addCategory(Intent.CATEGORY_LAUNCHER);
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
						            Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
						intent.setComponent(name);
		
						startActivity(intent);
						finish();
					}
				}
			}
			finish();
		}

		try {
			// initialize receiver

			startService(new Intent(this, MyService.class));
			
			lock(this, true);
			
			StateListener phoneStateListener = new StateListener();
			TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
			telephonyManager.listen(phoneStateListener,
					PhoneStateListener.LISTEN_CALL_STATE);

			windowwidth = getWindowManager().getDefaultDisplay().getWidth();
			System.out.println("windowwidth" + windowwidth);
			windowheight = getWindowManager().getDefaultDisplay().getHeight();
			System.out.println("windowheight" + windowheight);

			MarginLayoutParams marginParams2 = new MarginLayoutParams(
					droid.getLayoutParams());

			marginParams2.setMargins((windowwidth / 24) * 10,
					((windowheight / 32) * 8), 0, 0);

			// marginParams2.setMargins(((windowwidth-droid.getWidth())/2),((windowheight/32)*8),0,0);
			RelativeLayout.LayoutParams layoutdroid = new RelativeLayout.LayoutParams(
					marginParams2);

			droid.setLayoutParams(layoutdroid);

			LinearLayout homelinear = (LinearLayout) findViewById(R.id.homelinearlayout);
			homelinear.setPadding(0, 0, 0, (windowheight / 32) * 3);
			home = (ImageView) findViewById(R.id.home);

			MarginLayoutParams marginParams1 = new MarginLayoutParams(
					home.getLayoutParams());

			marginParams1.setMargins((windowwidth / 24) * 10, 0,
					(windowheight / 32) * 8, 0);
			// marginParams1.setMargins(((windowwidth-home.getWidth())/2),0,(windowheight/32)*10,0);
			LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(
					marginParams1);

			home.setLayoutParams(layout);

			droid.setOnTouchListener(new View.OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					layoutParams = (LayoutParams) v.getLayoutParams();

					switch (event.getAction()) {

					case MotionEvent.ACTION_DOWN:
						int[] hompos = new int[2];
						// int[] phonepos=new int[2];
						droidpos = new int[2];
						// phone.getLocationOnScreen(phonepos);
						home.getLocationOnScreen(hompos);
						home_x = hompos[0];
						home_y = hompos[1];
						// phone_x=phonepos[0];
						// phone_y=phonepos[1];

						break;
					case MotionEvent.ACTION_MOVE:
						int x_cord = (int) event.getRawX();
						int y_cord = (int) event.getRawY();

						if (x_cord > windowwidth - (windowwidth / 24)) {
							x_cord = windowwidth - (windowwidth / 24) * 2;
						}
						if (y_cord > windowheight - (windowheight / 32)) {
							y_cord = windowheight - (windowheight / 32) * 2;
						}

						layoutParams.leftMargin = x_cord;
						layoutParams.topMargin = y_cord;

						droid.getLocationOnScreen(droidpos);
						v.setLayoutParams(layoutParams);

						if (((x_cord - home_x) <= (windowwidth / 24) * 5 && (home_x - x_cord) <= (windowwidth / 24) * 4)
								&& ((home_y - y_cord) <= (windowheight / 32) * 5)) {
							System.out.println("home overlapps");
							lock(v.getContext(), false);
							System.out.println("homeee" + home_x + "  "
									+ (int) event.getRawX() + "  " + x_cord
									+ " " + droidpos[0]);

							System.out.println("homeee" + home_y + "  "
									+ (int) event.getRawY() + "  " + y_cord
									+ " " + droidpos[1]);

							v.setVisibility(View.GONE);

							// startActivity(new Intent(Intent.ACTION_VIEW,
							// Uri.parse("content://contacts/people/")));
							finish();
						} else {
							System.out.println("homeee" + home_x + "  "
									+ (int) event.getRawX() + "  " + x_cord
									+ " " + droidpos[0]);

							System.out.println("homeee" + home_y + "  "
									+ (int) event.getRawY() + "  " + y_cord
									+ " " + droidpos[1]);

							System.out.println("home notttt overlapps");
						}

						break;
					case MotionEvent.ACTION_UP:

						int x_cord1 = (int) event.getRawX();
						int y_cord2 = (int) event.getRawY();

						if (((x_cord1 - home_x) <= (windowwidth / 24) * 5 && (home_x - x_cord1) <= (windowwidth / 24) * 4)
								&& ((home_y - y_cord2) <= (windowheight / 32) * 5)) {
							System.out.println("home overlapps");
							System.out.println("homeee" + home_x + "  "
									+ (int) event.getRawX() + "  " + x_cord1
									+ " " + droidpos[0]);

							System.out.println("homeee" + home_y + "  "
									+ (int) event.getRawY() + "  " + y_cord2
									+ " " + droidpos[1]);

							// startActivity(new Intent(Intent.ACTION_VIEW,
							// Uri.parse("content://contacts/people/")));
							// finish();
						} else {

							layoutParams.leftMargin = (windowwidth / 24) * 10;
							layoutParams.topMargin = (windowheight / 32) * 8;
							v.setLayoutParams(layoutParams);

						}

					}

					return true;
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

	public void onSlideTouch(View view, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			break;
		case MotionEvent.ACTION_MOVE:
			int x_cord = (int) event.getRawX();
			int y_cord = (int) event.getRawY();

			if (x_cord > windowwidth) {
				x_cord = windowwidth;
			}
			if (y_cord > windowheight) {
				y_cord = windowheight;
			}

			layoutParams.leftMargin = x_cord - 25;
			layoutParams.topMargin = y_cord - 75;

			view.setLayoutParams(layoutParams);
			break;
		default:
			break;

		}

	}

	@Override
	public void onBackPressed() {
		// Don't allow back to dismiss.
		return;
	}

	// only used in lockdown mode
	@Override
	protected void onPause() {
		super.onPause();

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
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

		Log.d("Focus debug", "Focus changed !");

		if (!hasFocus) {
			Log.d("Focus debug", "Lost focus !");

			ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
			am.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_WITH_HOME);
			sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
		}
	}
	*/
	
	@Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (!hasFocus) {
            windowCloseHandler.postDelayed(windowCloserRunnable, 250);
        }
    }

    private void toggleRecents() {
        Intent closeRecents = new Intent("com.android.systemui.recent.action.TOGGLE_RECENTS");
        closeRecents.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        ComponentName recents = new ComponentName("com.android.systemui", "com.android.systemui.recent.RecentsActivity");
        closeRecents.setComponent(recents);
        this.startActivity(closeRecents);
    }

    public static void lock(Context c, boolean set) {
		SharedPreferences settings = c.getSharedPreferences(PREF_FILE,0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(LOCKED,set);
		editor.commit();
    }
    
    private Handler windowCloseHandler = new Handler();
    private Runnable windowCloserRunnable = new Runnable() {
        @Override
        public void run() {
            ActivityManager am = (ActivityManager)getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
            ComponentName cn = am.getRunningTasks(1).get(0).topActivity;

            Log.d("com.lockscreen", String.format("component: '%s'",cn.getClassName()));
            if (cn != null && cn.getClassName().equals("com.android.systemui.recent.RecentsActivity")) {
            	am.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_WITH_HOME);
            }
        }
    };

	public void onDestroy() {

		super.onDestroy();
	}

}