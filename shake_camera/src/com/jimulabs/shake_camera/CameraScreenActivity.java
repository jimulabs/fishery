package com.jimulabs.shake_camera;


import com.jimulabs.shake_camera.CameraPreviewView.CameraPreviewCallback;
import com.jimulabs.shake_camera.SensorGestureDetector.SensorGestureListener;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
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

	private CameraPreviewView mCameraView;

	private Button mShootButton;

	private TextView mCountdown;

	private SensorGestureDetector mGestureDetector;

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		unlockScreen();
		// TODO below should probably be moved somewhere else
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.camera_screen_activity);
		findViews();
		initViews();
		mGestureDetector = new SensorGestureDetector(this, new SensorGestureListener() {
			@Override
			public void onShaken(int times) {
				Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
				vibrator.vibrate(100);
				shootInSeconds();
			}
		});
	}

	private void shootInSeconds() {
		int seconds = 5;
		int interval = 1;
		mGestureDetector.setEnabled(false);
		new CountDownTimer(seconds*1000, interval*1000) {
			@Override
			public void onTick(long millisUntilFinished) {
				int count = (int) (millisUntilFinished / 1000) - 1;
				mCountdown.setText(count+"");
				if (mCountdown.getVisibility()==View.GONE) {
					mCountdown.setVisibility(View.VISIBLE);
				}
			}			
			@Override
			public void onFinish() {
				mCameraView.takePicture();
				mCountdown.setVisibility(View.GONE);
				mGestureDetector.setEnabled(true);
			}
		}.start();
	}

	private void initViews() {
		mShootButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mCameraView.takePicture();
			}
		});
		mCameraView.setCallback(new CameraPreviewCallback() {
			@Override
			public void onCameraReady() {
				shootInSeconds();
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mCameraView.acquireCamera();
		mGestureDetector.setEnabled(true);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mCameraView.releaseCamera();
		mGestureDetector.setEnabled(false);
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
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(intent);
	}

	private static Intent createLaunchIntent(Context context, boolean auto_shoot) {
		Intent intent = new Intent(context, CameraScreenActivity.class);
		intent.putExtra(CameraScreenActivity.EXTRA_AUTO_SHOOT, auto_shoot);
		return intent;
	}
	
	private void findViews() {
		mCameraView = (CameraPreviewView) findViewById(R.id.camera_view);
		mShootButton = (Button) findViewById(R.id.shoot_button);
		mCountdown = (TextView) findViewById(R.id.countdown);
	}
}
