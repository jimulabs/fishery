package com.jimulabs.shake_camera;

import android.app.Application;

public class ShakeCameraApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		GestureSensorService.bindAndSetSensorEnabled(this, true);
	}
	
}
