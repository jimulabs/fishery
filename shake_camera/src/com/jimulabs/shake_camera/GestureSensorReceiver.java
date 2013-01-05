package com.jimulabs.shake_camera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class GestureSensorReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (Intent.ACTION_SCREEN_ON.equals(action)) {
			GestureSensorService.bindAndSetSensorEnabled(context,true);
//			CameraScreenActivity.launchNewTask(context, true);
//			ExpActivity.launchNewTask(context);
		} else if (Intent.ACTION_SCREEN_OFF.equals(action)
				|| Intent.ACTION_BOOT_COMPLETED.equals(action)) {
			GestureSensorService.bindAndSetSensorEnabled(context,false);
		}
	}

}
