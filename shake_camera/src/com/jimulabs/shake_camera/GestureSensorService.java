package com.jimulabs.shake_camera;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;

import com.jimulabs.shake_camera.SensorGestureDetector.SensorGestureListener;

public class GestureSensorService extends Service implements SensorGestureListener {

	private static final int NOTIFICATION_ID = 92353253;

	class GestureSensorBinder extends Binder {
		public void setSensorEnabled(boolean sensorEnabled) {
			setGestureSensorEnabled(sensorEnabled);
		}
	}

	private GestureSensorBinder mBinder = new GestureSensorBinder();
	private GestureSensorReceiver mReceiver;
	private SensorGestureDetector mSensorGestureDetector;

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		startForeground(NOTIFICATION_ID, createNotification(true));

		mReceiver = new GestureSensorReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		registerReceiver(mReceiver, filter);
		mSensorGestureDetector = new SensorGestureDetector(this, this);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mReceiver);
	}
	
	protected void setGestureSensorEnabled(boolean enabled) {
		updateNotification(enabled);
		mSensorGestureDetector.setEnabled(enabled);
		if (enabled) {
			disableSensorAfterTimeout(10);
		}
	}

	private void disableSensorAfterTimeout(int seconds) {
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				setGestureSensorEnabled(false);
			}
		}, seconds*1000);
	}

	private void updateNotification(boolean enabled) {
		Notification notification = createNotification(enabled);
		NotificationManager nm =
			    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(NOTIFICATION_ID, notification);
	}

	private Notification createNotification(boolean enableSensor) {
		int icon = enableSensor? R.drawable.ic_launcher : android.R.drawable.ic_btn_speak_now;
		CharSequence ticker = enableSensor ? "shake detection enabled" : "shake detection disabled";
		CharSequence contentText = enableSensor ? "Shake me" : "Sensor disabled to save battery.";
		Intent intent = new Intent(this, WelcomeActivity.class);
		PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
		Builder builder = new NotificationCompat.Builder(this)
			.setSmallIcon(icon)
			.setContentTitle(ticker)
			.setContentText(contentText)
			.setContentIntent(pi)
			;
		if (enableSensor) {
//			builder.setTicker(ticker);
		}
		Notification notif = builder.build();
		return notif;
	}

	public static void bindAndSetSensorEnabled(Context context,
			final boolean sensorEnabled) {
		Intent service = new Intent(context, GestureSensorService.class);
		ServiceConnection conn = new ServiceConnection() {
			@Override
			public void onServiceDisconnected(ComponentName name) {
			}

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				GestureSensorBinder binder = ((GestureSensorBinder) service);
				binder.setSensorEnabled(sensorEnabled);
			}
		};
		int flags = Service.BIND_AUTO_CREATE;
		context.bindService(service, conn, flags);
	}

	@Override
	public void onShaken(int times) {
		Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		vibrator.vibrate(100);

		CameraScreenActivity.launchNewTask(this, true);
	}

}
