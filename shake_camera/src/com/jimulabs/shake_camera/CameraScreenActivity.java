package com.jimulabs.shake_camera;


import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;
import android.widget.Button;

public class CameraScreenActivity extends Activity {

	public static final String EXTRA_AUTO_SHOOT = "EXTRA_AUTO_SHOOT";

	private TextView mCameraView;

	private Button mShootButton;

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		// TODO below should probably be moved somewhere else
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.camera_screen_activity);
		findViews();
		initViews();
		unlockScreen();
	}

	private void initViews() {
		mShootButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				GestureSensorService.bindAndSetSensorEnabled(CameraScreenActivity.this, false);
			}
		});
	}

	private void unlockScreen() {
		Window wind = getWindow();
		wind.addFlags(LayoutParams.FLAG_DISMISS_KEYGUARD);
		wind.addFlags(LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		wind.addFlags(LayoutParams.FLAG_TURN_SCREEN_ON);
	}

	public static void launch(Context context, boolean auto_shoot) {
		Intent intent = createLaunchIntent(context, auto_shoot);
		context.startActivity(intent);
	}

	public static void launchNewTask(Context context, boolean auto_shoot) {
		Intent intent = createLaunchIntent(context, auto_shoot);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

	private static Intent createLaunchIntent(Context context, boolean auto_shoot) {
		Intent intent = new Intent(context, CameraScreenActivity.class);
		intent.putExtra(CameraScreenActivity.EXTRA_AUTO_SHOOT, auto_shoot);
		return intent;
	}
	
	private void findViews() {
		mCameraView = (TextView) findViewById(R.id.camera_view);
		mShootButton = (Button) findViewById(R.id.shoot_button);
	}
}
