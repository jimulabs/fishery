package com.jimulabs.shake_camera;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class CameraPreviewView extends RelativeLayout implements Callback {

	private static final String LOG_TAG = CameraPreviewView.class.getSimpleName();
	private SurfaceView mSurfaceView;
	private SurfaceHolder mHolder;
	private Camera mCamera;
	private List<Size> mSupportedPreviewSizes;
	private Size mPreviewSize;
	private ProgressBar mProgress;

	public CameraPreviewView(Context context, AttributeSet attrs) {
		super(context, attrs);
        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView);
        mProgress = new ProgressBar(context);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);
		addView(mProgress, lp);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	public void acquireCamera() {
		safeCameraOpen(0);
		initCamera();
		
//		new AsyncTask<Void, Void, Camera>() {
//			@Override
//			protected Camera doInBackground(Void... params) {
//				return safeCameraOpen(0);
//			}
//			
//			protected void onPreExecute() {
//				mProgress.setVisibility(View.VISIBLE);
//			};
//
//			@Override
//			protected void onPostExecute(Camera result) {
//				mProgress.setVisibility(View.GONE);
//				if (mCamera != null) {
//					initCamera();
//				} else {
//					Toast.makeText(getContext(), "Failed to open camera.",
//							Toast.LENGTH_SHORT).show();
//				}
//			}
//
//		}.execute();
	}
	
	private void initCamera() {
	    if (mCamera != null) {
	        List<Size> localSizes = mCamera.getParameters().getSupportedPreviewSizes();
	        mSupportedPreviewSizes = localSizes;
	        mPreviewSize = localSizes.get(0);
	        requestLayout();
	      
	        try {
	            mCamera.setPreviewDisplay(mHolder);
		        /*
		          Important: Call startPreview() to start updating the preview surface. Preview must 
		          be started before you can take a picture.
		          */
		        mCamera.startPreview();
//		        takePicture();
	        } catch (IOException e) {
	            Log.e(LOG_TAG, "failed to setPreviewDisplay", e);
	        }
	      
	    }
	}

	public void takePicture() {
		mCamera.autoFocus(new AutoFocusCallback() {
			@Override
			public void onAutoFocus(boolean success, Camera camera) {
				if (success) {
					PictureCallback jpeg = new PictureCallback() {
						@Override
						public void onPictureTaken(byte[] data,
								Camera camera) {
							Toast.makeText(getContext(), "picture taken!",
									Toast.LENGTH_SHORT).show();
						}
					};
					camera.takePicture(null, null, jpeg);
				}
			}
		});
	}
	
	private Camera safeCameraOpen(int id) {
	    try {
	        releaseCameraAndPreview();
	        mCamera = Camera.open(id);
	        return mCamera;
	    } catch (Exception e) {
	        Log.e(LOG_TAG, "failed to open Camera",e);
	        return null;
	    }
	}

	public synchronized void releaseCameraAndPreview() {
	    if (mCamera != null) {
	    	mCamera.stopPreview();
	        mCamera.release();
	        mCamera = null;
	    }
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		if (mCamera != null) {
//			// Now that the size is known, set up the camera parameters and
//			// begin
//			// the preview.
//			Camera.Parameters parameters = mCamera.getParameters();
//			parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
//			requestLayout();
//			mCamera.setParameters(parameters);
//
//			/*
//			 * Important: Call startPreview() to start updating the preview
//			 * surface. Preview must be started before you can take a picture.
//			 */
//			mCamera.startPreview();
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	    // Surface will be destroyed when we return, so stop the preview.
	    if (mCamera != null) {
	        /*
	          Call stopPreview() to stop updating the preview surface.
	        */
	        mCamera.stopPreview();
	    }
	}

}
