/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera;

import java.io.IOException;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.AutoFocusMoveCallback;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.OnZoomChangeListener;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.Build;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;

public class CameraManager {
    private static final String TAG = "CameraManager";
    private static CameraManager sCameraManager = new CameraManager();

    // Thread progress signals
    private ConditionVariable mSig = new ConditionVariable();

    private Parameters mParameters;
    private IOException mReconnectException;

    private static final int RELEASE = 1;
    private static final int RECONNECT = 2;
    private static final int UNLOCK = 3;
    private static final int LOCK = 4;
    private static final int SET_PREVIEW_TEXTURE_ASYNC = 5;
    private static final int START_PREVIEW_ASYNC = 6;
    private static final int STOP_PREVIEW = 7;
    private static final int SET_PREVIEW_CALLBACK_WITH_BUFFER = 8;
    private static final int ADD_CALLBACK_BUFFER = 9;
    private static final int AUTO_FOCUS = 10;
    private static final int CANCEL_AUTO_FOCUS = 11;
    private static final int SET_AUTO_FOCUS_MOVE_CALLBACK = 12;
    private static final int SET_DISPLAY_ORIENTATION = 13;
    private static final int SET_ZOOM_CHANGE_LISTENER = 14;
    private static final int SET_FACE_DETECTION_LISTENER = 15;
    private static final int START_FACE_DETECTION = 16;
    private static final int STOP_FACE_DETECTION = 17;
    private static final int SET_ERROR_CALLBACK = 18;
    private static final int SET_PARAMETERS = 19;
    private static final int GET_PARAMETERS = 20;
    private static final int SET_PARAMETERS_ASYNC = 21;
    private static final int WAIT_FOR_IDLE = 22;
    private static final int SET_PREVIEW_DISPLAY_ASYNC = 23;
    private static final int SET_PREVIEW_CALLBACK = 24;
    private static final int ENABLE_SHUTTER_SOUND = 25;

    private static final int OPEN = 26;

    private Handler mCameraHandler;
    private CameraProxy mCameraProxy;
    private android.hardware.Camera mCamera;
	private OpenCameraHandler mOpenCameraHandler;

    public static CameraManager instance() {
        return sCameraManager;
    }

    private CameraManager() {
        HandlerThread ht = new HandlerThread("Camera Handler Thread");
        ht.start();
        mCameraHandler = new CameraHandler(ht.getLooper());
        
        HandlerThread htOpen = new HandlerThread("Camera Open Handler Thread");
        htOpen.start();
        mOpenCameraHandler = new OpenCameraHandler(htOpen.getLooper());
    }

    private class CameraHandler extends Handler {
        CameraHandler(Looper looper) {
            super(looper);
        }

        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        private void startFaceDetection() {
            mCamera.startFaceDetection();
        }

        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        private void stopFaceDetection() {
            mCamera.stopFaceDetection();
        }

        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        private void setFaceDetectionListener(FaceDetectionListener listener) {
            mCamera.setFaceDetectionListener(listener);
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        private void setPreviewTexture(Object surfaceTexture) {
            try {
                mCamera.setPreviewTexture((SurfaceTexture) surfaceTexture);
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        private void enableShutterSound(boolean enable) {
            mCamera.enableShutterSound(enable);
        }

        /*
         * This method does not deal with the build version check.  Everyone should
         * check first before sending message to this handler.
         */
        @Override
        public void handleMessage(final Message msg) {
            try {
                switch (msg.what) {
                    case RELEASE:
                        mCamera.release();
                        mCamera = null;
                        mCameraProxy = null;                        
                        break;
                    case RECONNECT:
                        mReconnectException = null;
                        try {
                            mCamera.reconnect();
                        } catch (IOException ex) {
                            mReconnectException = ex;
                        }
                        break;

                    case UNLOCK:
                        mCamera.unlock();
                        break;

                    case LOCK:
                        mCamera.lock();
                        break;

                    case SET_PREVIEW_TEXTURE_ASYNC:
                        setPreviewTexture(msg.obj);
                        return;  // no need to call mSig.open()

                    case SET_PREVIEW_DISPLAY_ASYNC:
                        try {
                            mCamera.setPreviewDisplay((SurfaceHolder) msg.obj);
                        } catch(IOException e) {
                            throw new RuntimeException(e);
                        }
                        return;  // no need to call mSig.open()

                    case START_PREVIEW_ASYNC:
                        mCamera.startPreview();
                        return;  // no need to call mSig.open()

                    case STOP_PREVIEW:
                        mCamera.stopPreview();
                        break;

                    case SET_PREVIEW_CALLBACK_WITH_BUFFER:
                        mCamera.setPreviewCallbackWithBuffer(
                            (PreviewCallback) msg.obj);
                        break;

                    case ADD_CALLBACK_BUFFER:
                        mCamera.addCallbackBuffer((byte[]) msg.obj);
                        break;

                    case AUTO_FOCUS:
                        mCamera.autoFocus((AutoFocusCallback) msg.obj);
                        break;

                    case CANCEL_AUTO_FOCUS:
                        mCamera.cancelAutoFocus();
                        break;

                    case SET_AUTO_FOCUS_MOVE_CALLBACK:
                        setAutoFocusMoveCallback(mCamera, msg.obj);
                        break;

                    case SET_DISPLAY_ORIENTATION:
                        mCamera.setDisplayOrientation(msg.arg1);
                        break;

                    case SET_ZOOM_CHANGE_LISTENER:
                        mCamera.setZoomChangeListener(
                            (OnZoomChangeListener) msg.obj);
                        break;

                    case SET_FACE_DETECTION_LISTENER:
                        setFaceDetectionListener((FaceDetectionListener) msg.obj);
                        break;

                    case START_FACE_DETECTION:
                        startFaceDetection();
                        break;

                    case STOP_FACE_DETECTION:
                        stopFaceDetection();
                        break;

                    case SET_ERROR_CALLBACK:
                        mCamera.setErrorCallback((ErrorCallback) msg.obj);
                        break;

                    case SET_PARAMETERS:
                        mCamera.setParameters((Parameters) msg.obj);
                        break;

                    case GET_PARAMETERS:
                        mParameters = mCamera.getParameters();
                        break;

                    case SET_PARAMETERS_ASYNC:
                        mCamera.setParameters((Parameters) msg.obj);
                        return;  // no need to call mSig.open()

                    case SET_PREVIEW_CALLBACK:
                        mCamera.setPreviewCallback((PreviewCallback) msg.obj);
                        break;

                    case ENABLE_SHUTTER_SOUND:
                        enableShutterSound((msg.arg1 == 1) ? true : false);
                        break;

                    case WAIT_FOR_IDLE:
                        // do nothing
                        break;

                    default:
                        throw new RuntimeException("Invalid CameraProxy message=" + msg.what);
                }
            } catch (RuntimeException e) {
                if (msg.what != RELEASE && mCamera != null) {
                    try {
                        mCamera.release();
                    } catch (Exception ex) {
                        Log.e(TAG, "Fail to release the camera.");
                    }
                    mCamera = null;
                    mCameraProxy = null;
                }
                throw e;
            }
            mSig.open();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setAutoFocusMoveCallback(android.hardware.Camera camera,
            Object cb) {
        camera.setAutoFocusMoveCallback((AutoFocusMoveCallback) cb);
    }
    
    class OpenCameraHandler extends Handler {
    	OpenCameraHandler(Looper looper) {
    		super(looper);
    	}
    	@Override
    	public void handleMessage(Message msg) {
    		switch (msg.what) {
			case OPEN:
				CameraProxy camera;
				try {
					camera = cameraOpen(msg.arg1);
				} catch (Exception e) {
					camera = null;
					Log.e(TAG, "failed to open camera", e);
				}
				if (msg.obj instanceof OpenCameraCallback) {
					((OpenCameraCallback) msg.obj).afterOpen(camera);
				}
    		}
    	}
    }

    // Open camera synchronously. This method is invoked in the context of a
    // background thread.
    CameraProxy cameraOpen(int cameraId) {
    	if (mCameraProxy!=null) {
    		return mCameraProxy;
    	}
    	// Cannot open camera in mCameraHandler, otherwise all camera events
    	// will be routed to mCameraHandler looper, which in turn will call
    	// event handler like Camera.onFaceDetection, which in turn will modify
    	// UI and cause exception like this:
    	// CalledFromWrongThreadException: Only the original thread that created
    	// a view hierarchy can touch its views.
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			mCamera = openCameraGingerbread(cameraId);
		} else {
			mCamera = android.hardware.Camera.open();
		}    	
    	if (mCamera != null) {
    		mCameraProxy = new CameraProxy();
    		return mCameraProxy;
    	} else {
    		return null;
    	}
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
	private Camera openCameraGingerbread(int cameraId) {
    	return android.hardware.Camera.open(cameraId);
	}

	public class CameraProxy {
        private CameraProxy() {
            assert(mCamera != null);
        }

        public android.hardware.Camera getCamera() {
            return mCamera;
        }

        public void release() {
            mSig.close();
            mCameraHandler.sendEmptyMessage(RELEASE);
            mSig.block();
        }

        public void reconnect() throws IOException {
            mSig.close();
            mCameraHandler.sendEmptyMessage(RECONNECT);
            mSig.block();
            if (mReconnectException != null) {
                throw mReconnectException;
            }
        }

        public void unlock() {
            mSig.close();
            mCameraHandler.sendEmptyMessage(UNLOCK);
            mSig.block();
        }

        public void lock() {
            mSig.close();
            mCameraHandler.sendEmptyMessage(LOCK);
            mSig.block();
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        public void setPreviewTextureAsync(final SurfaceTexture surfaceTexture) {
            mCameraHandler.obtainMessage(SET_PREVIEW_TEXTURE_ASYNC, surfaceTexture).sendToTarget();
        }

        public void setPreviewDisplayAsync(final SurfaceHolder surfaceHolder) {
            mCameraHandler.obtainMessage(SET_PREVIEW_DISPLAY_ASYNC, surfaceHolder).sendToTarget();
        }

        public void startPreviewAsync() {
            mCameraHandler.sendEmptyMessage(START_PREVIEW_ASYNC);
        }

        public void stopPreview() {
            mSig.close();
            mCameraHandler.sendEmptyMessage(STOP_PREVIEW);
            mSig.block();
        }

        public void setPreviewCallback(final PreviewCallback cb) {
            mSig.close();
            mCameraHandler.obtainMessage(SET_PREVIEW_CALLBACK, cb).sendToTarget();
            mSig.block();
        }

        public void setPreviewCallbackWithBuffer(final PreviewCallback cb) {
            mSig.close();
            mCameraHandler.obtainMessage(SET_PREVIEW_CALLBACK_WITH_BUFFER, cb).sendToTarget();
            mSig.block();
        }

        public void addCallbackBuffer(byte[] callbackBuffer) {
            mSig.close();
            mCameraHandler.obtainMessage(ADD_CALLBACK_BUFFER, callbackBuffer).sendToTarget();
            mSig.block();
        }

        public void autoFocus(AutoFocusCallback cb) {
            mSig.close();
            mCameraHandler.obtainMessage(AUTO_FOCUS, cb).sendToTarget();
            mSig.block();
        }

        public void cancelAutoFocus() {
            mSig.close();
            mCameraHandler.sendEmptyMessage(CANCEL_AUTO_FOCUS);
            mSig.block();
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        public void setAutoFocusMoveCallback(AutoFocusMoveCallback cb) {
            mSig.close();
            mCameraHandler.obtainMessage(SET_AUTO_FOCUS_MOVE_CALLBACK, cb).sendToTarget();
            mSig.block();
        }

        public void takePicture(final ShutterCallback shutter, final PictureCallback raw,
                final PictureCallback postview, final PictureCallback jpeg) {
            mSig.close();
            // Too many parameters, so use post for simplicity
            mCameraHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCamera.takePicture(shutter, raw, postview, jpeg);
                    mSig.open();
                }
            });
            mSig.block();
        }

        public void takePicture2(final ShutterCallback shutter, final PictureCallback raw,
                final PictureCallback postview, final PictureCallback jpeg,
                final int cameraState, final int focusState) {
            mSig.close();
            // Too many parameters, so use post for simplicity
            mCameraHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        mCamera.takePicture(shutter, raw, postview, jpeg);
                    } catch (RuntimeException e) {
                        Log.w(TAG, "take picture failed; cameraState:" + cameraState
                            + ", focusState:" + focusState);
                        throw e;
                    }
                    mSig.open();
                }
            });
            mSig.block();
        }

        public void setDisplayOrientation(int degrees) {
            mSig.close();
            mCameraHandler.obtainMessage(SET_DISPLAY_ORIENTATION, degrees, 0)
                    .sendToTarget();
            mSig.block();
        }

        public void setZoomChangeListener(OnZoomChangeListener listener) {
            mSig.close();
            mCameraHandler.obtainMessage(SET_ZOOM_CHANGE_LISTENER, listener).sendToTarget();
            mSig.block();
        }

        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        public void setFaceDetectionListener(FaceDetectionListener listener) {
            mSig.close();
            mCameraHandler.obtainMessage(SET_FACE_DETECTION_LISTENER, listener).sendToTarget();
            mSig.block();
        }

        public void startFaceDetection() {
            mSig.close();
            mCameraHandler.sendEmptyMessage(START_FACE_DETECTION);
            mSig.block();
        }

        public void stopFaceDetection() {
            mSig.close();
            mCameraHandler.sendEmptyMessage(STOP_FACE_DETECTION);
            mSig.block();
        }

        public void setErrorCallback(ErrorCallback cb) {
            mSig.close();
            mCameraHandler.obtainMessage(SET_ERROR_CALLBACK, cb).sendToTarget();
            mSig.block();
        }

        public void setParameters(Parameters params) {
            mSig.close();
            mCameraHandler.obtainMessage(SET_PARAMETERS, params).sendToTarget();
            mSig.block();
        }

        public void setParametersAsync(Parameters params) {
            mCameraHandler.removeMessages(SET_PARAMETERS_ASYNC);
            mCameraHandler.obtainMessage(SET_PARAMETERS_ASYNC, params).sendToTarget();
        }

        public Parameters getParameters() {
            mSig.close();
            mCameraHandler.sendEmptyMessage(GET_PARAMETERS);
            mSig.block();
            Parameters parameters = mParameters;
            mParameters = null;
            return parameters;
        }

        public void enableShutterSound(boolean enable) {
            mSig.close();
            mCameraHandler.obtainMessage(
                    ENABLE_SHUTTER_SOUND, (enable ? 1 : 0), 0).sendToTarget();
            mSig.block();
        }

        public void waitForIdle() {
            mSig.close();
            mCameraHandler.sendEmptyMessage(WAIT_FOR_IDLE);
            mSig.block();
        }
    }

    public static interface OpenCameraCallback {
    	void afterOpen(CameraProxy camera);
    }
    
	public void cameraOpenAsync(int id, OpenCameraCallback callback) {
		Message msg = mOpenCameraHandler.obtainMessage(OPEN, id, 0);
		msg.obj = callback;
		msg.sendToTarget();
	}
}
