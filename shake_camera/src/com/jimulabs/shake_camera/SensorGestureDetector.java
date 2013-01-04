package com.jimulabs.shake_camera;

import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class SensorGestureDetector implements SensorEventListener {
	
	public static interface SensorGestureListener {
		void onShaken(int times);
	}

	private static final String LOG_TAG = SensorGestureDetector.class.getSimpleName();

	private Context mContext;
	private SensorManager mSensorManager;
	
	private SensorGestureListener mListener;

	public SensorGestureDetector(Context context, SensorGestureListener listener) {
		mContext = context;
		mListener = listener;
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
	}
	
	private void registerSensorListener() {
		List<Sensor> sensors = mSensorManager
				.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if (sensors.size() > 0) {
			mSensorManager.registerListener(this, sensors.get(0),
					SensorManager.SENSOR_DELAY_GAME);
		} else {
			Toast.makeText(mContext,
					"Unable to detect gestures: No accelerometer available on this device.",
					Toast.LENGTH_LONG).show();
		}
	}
	
	private void unregisterSensorListener() {
		mSensorManager.unregisterListener(this);
	}

	public void setEnabled(boolean enabled) {
		if (enabled) {
			initVars();
			registerSensorListener();
		} else {
			unregisterSensorListener();
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	/*
	 * TODO below needs a lot of work to make it detect shakes accurately.
	 */
	float mAccelLast, mAccelCurrent, mAccel;
	int mShakes;
	private void initVars() {
		mAccel = 0;
		mAccelCurrent = 0;
		mAccelLast = 0;
		mShakes = -5;
	}
	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			float x = event.values[0];
			float y = event.values[1];
			float z = event.values[2];
			mAccelLast = mAccelCurrent;
			mAccelCurrent = (float) Math.sqrt((double) (x * x + y * y + z * z));
			float delta = mAccelCurrent - mAccelLast;
			mAccel = mAccel * 0.9f + delta; // perform low-cut filter

			int thresholdAcc = 6;
			int thresholdShakes = 4;

			 Log.d(LOG_TAG, "acceel="+mAccel+" accelLast="+mAccelLast+" accelCurrent="+mAccelCurrent+" mShakes="+mShakes);
			if (mAccel > thresholdAcc) {
				mShakes++;
				if (mShakes >= thresholdShakes) {
					if (mListener!=null) {
						mListener.onShaken(1);
					}
					mShakes = 0;
				}
				new Handler().postDelayed(new Runnable() {
					public void run() {
						mShakes = 0;
					}
				}, 1000);
			}
		}
	}
}
