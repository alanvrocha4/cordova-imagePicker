package com.synconset;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.synconset.FakeR;
import com.synconset.CameraPreview;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

public class CameraActivity extends Activity {
	

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final int PHOTO_WIDTH = 480;
    public static final int PHOTO_HEIGHT = 360;
    public static final int PHOTO_QUALITY = 50;
    private ProgressDialog dialog;

    ArrayList<String> imgsPath = new ArrayList<String>();
	protected static final String TAG = "TESTECAMERA";
    


	private boolean checkCameraHardware(Context context) {
	    if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
	        // this device has a camera
	        return true;
	    } else {
	        // no camera on this device
	        return false;
	    }
	}
	
	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance(){

	    Camera c = null;
	    try {
	        c = Camera.open(); // attempt to get a Camera instance
	        Camera.Parameters params = c.getParameters();

    		List<String> focusModes = params.getSupportedFocusModes();
    		if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
    			
                // set the focus mode
    			params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
    		}

            //set quality
            params.setJpegQuality(PHOTO_QUALITY);

            // set Camera parameters
            c.setParameters(params);
	    }
	    catch (Exception e){
	        // Camera is not available (in use or does not exist)
	    }
	    return c; // returns null if camera is unavailable
	}
	
	private Camera mCamera;
    private CameraPreview mPreview;
    private ImageView captureButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        FakeR fakeR = new FakeR(this);
        super.onCreate(savedInstanceState);
        setContentView(fakeR.getId("layout", "stopmotion_activity_camera"));

        // Create an instance of Camera
        mCamera = getCameraInstance();
        

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(fakeR.getId("id", "camera_preview"));
        preview.addView(mPreview);
        
        captureButton = (ImageView) findViewById(fakeR.getId("id", "button_capture"));
        captureButton.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                	Log.d(TAG, "CLICOU");
                    captureButton.setEnabled(false);
                    captureButton.setClickable(false);
                    // get an image from the camera
                    mCamera.takePicture(null, null, mPicture);
                    
                    
                    
                }
            }
        );
        
        ImageView doneButton = (ImageView) findViewById(fakeR.getId("id", "button_done"));
        doneButton.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                	Log.d(TAG, "CLICOU");
                	Intent data = new Intent();
                	Log.d(TAG, "==================TOTAL IMAGENS:"+ imgsPath.size() +" ======");
                	if(imgsPath.size() > 0){
                		Bundle res = new Bundle();
                        res.putStringArrayList("MULTIPLEFILENAMES", imgsPath);
                        data.putExtras(res);
                        setResult(RESULT_OK, data);
                	}else{
                		setResult(RESULT_CANCELED, data);
                	}
            		finish();
                }
            }
        );
    }
    
    private void resetCam() {
    	Log.d(TAG,"ENTROU NO RESET CAM");
		mCamera.startPreview();
		captureButton.setEnabled(true);
        captureButton.setClickable(true);
		mPreview = new CameraPreview(this, mCamera);
	}
    
    private PictureCallback mPicture = new PictureCallback() {
    	
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
        	byte[] resized = resizeImage(data);
        	resetCam();
            new SaveImageTask().execute(resized);
        }
    };
    
	private void refreshGallery(File file) {
		Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		mediaScanIntent.setData(Uri.fromFile(file));
		sendBroadcast(mediaScanIntent);
	}

    private byte[] resizeImage(byte[] input) {
        Bitmap original = BitmapFactory.decodeByteArray(input , 0, input.length);
        Bitmap resized = Bitmap.createScaledBitmap(original, PHOTO_WIDTH, PHOTO_HEIGHT, true);
             
        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG, 100, blob);
     
        return blob.toByteArray();
    }
    
	private class SaveImageTask extends AsyncTask<byte[], Void, Void> {

        // @Override
        // public void onPreExecute() {

        //     dialog = new ProgressDialog(CameraActivity.this);

        //     dialog.setMessage("Gravando...");
        //     dialog.show();

        // }

		@Override
		protected Void doInBackground(byte[]... data) {
			FileOutputStream outStream = null;

			// Write to SD Card
			try {
				File sdCard = Environment.getExternalStorageDirectory();
				File dir = new File (sdCard.getAbsolutePath() + "/GOGOMOTION");
				dir.mkdirs();				

				String fileName = String.format("%d.jpg", System.currentTimeMillis());
				File outFile = new File(dir, fileName);

				outStream = new FileOutputStream(outFile);
				outStream.write(data[0]);
				outStream.flush();
				outStream.close();

				Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to " + Uri.fromFile(outFile).toString());

				refreshGallery(outFile);
				imgsPath.add(Uri.fromFile(outFile).toString());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
			}
			return null;
		}

        // @Override
        // protected void onPostExecute(String result) {
        //     dialog.cancel();
        //     resetCam();
        // }

	}

    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
          return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                  Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
            "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
            "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }
        Log.d(TAG, mediaFile.toString());
        return mediaFile;
    }
    
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Image captured and saved to fileUri specified in the Intent
                Toast.makeText(this, "Image saved to:\n" +
                         data.getData(), Toast.LENGTH_LONG).show();
            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the image capture
            } else {
                // Image capture failed, advise user
            }
        }

        if (requestCode == CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Video captured and saved to fileUri specified in the Intent
                Toast.makeText(this, "Video saved to:\n" +
                         data.getData(), Toast.LENGTH_LONG).show();
            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the video capture
            } else {
                // Video capture failed, advise user
            }
        }
    }
	
	
}
