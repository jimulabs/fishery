package com.jimulabs.shake_camera;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager.LayoutParams;

public class ExpActivity extends Activity {

	private static final String LOG_TAG = "Exp";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.exp_activity);
		unlockScreen();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.exp_activity, menu);
		return true;
	}

	private void unlockScreen() {
		Window wind = getWindow();
		wind.addFlags(LayoutParams.FLAG_DISMISS_KEYGUARD);
		wind.addFlags(LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		wind.addFlags(LayoutParams.FLAG_TURN_SCREEN_ON);
	}
	
	public static void launchNewTask(Context context) {
		Intent intent = new Intent(context, ExpActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(intent);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.d(LOG_TAG, "onResume:"+this);
	}
	@Override
	protected void onPause() {
		super.onPause();
		Log.d(LOG_TAG, "onPause:"+this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(LOG_TAG, "onStop:"+this);
	}
	@Override
	protected void onStart() {
		super.onStart();
		Log.d(LOG_TAG, "onStart:"+this);
	}

}
