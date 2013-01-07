package com.jimulabs.shake_camera;

import java.io.IOException;
import java.util.List;

import com.android.camera.CameraManager;
import com.android.camera.CameraManager.CameraProxy;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Media;
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

public class CameraPreviewView extends ViewGroup implements Callback {

	private static final String LOG_TAG = CameraPreviewView.class.getSimpleName();
	private SurfaceView mSurfaceView;
	private SurfaceHolder mHolder;
	private CameraProxy mCamera;
	private List<Size> mSupportedPreviewSizes;
	private Size mPreviewSize;

	public CameraPreviewView(Context context, AttributeSet attrs) {
		super(context, attrs);
		createSurface();
	}

//    public void setCamera(Camera camera) {
//        mCamera = camera;
//        initLockedCamera();
//    }


//    public void switchCamera(Camera camera) {
//       setCamera(camera);
//       try {
//           camera.setPreviewDisplay(mHolder);
//       } catch (IOException exception) {
//           Log.e(LOG_TAG, "IOException caused by setPreviewDisplay()", exception);
//       }
//       Camera.Parameters parameters = camera.getParameters();
//       parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
//       requestLayout();
//
//       camera.setParameters(parameters);
//    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);

            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null) {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
            }

            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
    	if (mCamera!=null) {
    		startPreview();
    	}
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
    	if (mCamera!=null && mPreviewSize!=null) {
    		Camera.Parameters parameters = mCamera.getParameters();
    		parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
    		requestLayout(); 
    		
    		mCamera.setParameters(parameters);
    		startPreview();
    	}
    }
    
    public static interface CameraPreviewCallback {
    	void onCameraReady();
    }
	private CameraPreviewCallback mCallback;

	public void setCallback(CameraPreviewCallback callback) {
		mCallback = callback;
	}
	
	public void acquireCamera() {
		Log.d(LOG_TAG, "Trying to acquire camera...");
		final Handler h = new Handler(Looper.getMainLooper());
		CameraManager.OpenCameraCallback callback = new CameraManager.OpenCameraCallback() {
			@Override
			public void afterOpen(final CameraProxy camera) {
				mCamera = camera;
				Log.d(LOG_TAG, "Camera acquired:" + mCamera);
				h.post(new Runnable() {

					@Override
					public void run() {
						Activity activity = (Activity)getContext();
						if (mCamera != null && !activity.isFinishing()) {
							initLockedCamera();
							if (mCallback!=null) {
								mCallback.onCameraReady();
							}
						} else {
							Toast.makeText(getContext(),
									"Failed to open camera.",
									Toast.LENGTH_SHORT).show();
						}
					}
				});
			}
		};
		CameraManager.instance().cameraOpenAsync(0, callback);
	}
	
	private void initLockedCamera() {
		if (mCamera != null) {
			Parameters parameters = mCamera.getParameters();
			mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
			requestLayout();
			if (!mHolder.isCreating()) {
				startPreview();
			}
		}
	}


	private void createSurface() {
		Context context = getContext();
		mSurfaceView = new SurfaceView(context);
		addView(mSurfaceView);

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = mSurfaceView.getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}


	private void startPreview() {
		mCamera.stopPreview();
		mCamera.setPreviewDisplayAsync(mHolder);
		/*
		          Important: Call startPreview() to start updating the preview surface. Preview must 
		          be started before you can take a picture.
		 */
		mCamera.startPreviewAsync();
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
//							Toast.makeText(getContext(), "picture taken!",
//									Toast.LENGTH_SHORT).show();
							ContentResolver contentResolver = getContext().getContentResolver();
							Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
							String title = "ShakenPicture";
							String description = "Picture taken by shaking";
							String uri = MediaStore.Images.Media.insertImage(contentResolver, bm, title, description);
							if (uri==null) {
								Toast.makeText(getContext(), "Failed to save picture.", Toast.LENGTH_SHORT).show();
							} else {
								Toast.makeText(getContext(), "Picture saved! Shake again to take another one.", Toast.LENGTH_SHORT).show();
							}
							mCamera.startPreviewAsync();
						}
					};
					camera.takePicture(null, null, jpeg);
				}
			}
		});
	}
	
	public void releaseCamera() {
	    if (mCamera != null) {
	    	mCamera.stopPreview();
	        mCamera.release();
	        Log.d(LOG_TAG, "Camera released:"+mCamera);
	        mCamera = null;
	    }
	}



}
