package com.jackal.photoeditor.black.border;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {
	private static final int SELECT_PHOTO = 100;
	private static final int DOWN_SAMPLE_BOUNDARY = 1080;
	private static final String TAG = "Black_Border";
	private long gMaxBoundary = -1;

	private ArrayList<String> mRatioList = new ArrayList<String>();
	private int mIndexRatio = -1;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);
		
		// Get the max heap size to get the max boundary to avoid OutOfMemory excpetion.
		long maxMemory = Runtime.getRuntime().maxMemory();
		gMaxBoundary = (long) Math.pow((maxMemory /4), 0.5);
		Log.d(TAG, "The maxMemory: " + maxMemory + ", maxBoundary: " + gMaxBoundary);

		Button button = (Button) findViewById(R.id.startButton);
		button.setOnClickListener(this);
		
		Spinner mSpRatio = (Spinner)findViewById(R.id.spinnerratio);
		
		mRatioList.clear();
		mRatioList.add(getString(R.string.ratio_1_1));
		mRatioList.add(getString(R.string.ratio_3_2));
		
		ArrayAdapter<String> adapter = null;
    	adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mRatioList);
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	mSpRatio.setAdapter(adapter);
    	mSpRatio.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
    		public void onItemSelected(AdapterView adapterView, View view, int position, long id){
    				Log.d(TAG, "Click: " +position +", " +adapterView.getSelectedItem().toString());
    				mIndexRatio = position;
    			}
    		public void onNothingSelected(AdapterView arg0) {
    			
    			}
    	});
	}

	@Override
	public void onClick(View v) {
		Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
		photoPickerIntent.setType("image/*");
		startActivityForResult(photoPickerIntent, SELECT_PHOTO);
	}

	private class AsyncBitmapBuilder extends AsyncTask<Uri, Integer, Boolean> {

		@Override
		protected void onPostExecute(Boolean result) {
			if (result) {
				Toast.makeText(getApplicationContext(), R.string.toastDone, Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getApplicationContext(), R.string.toastFail, Toast.LENGTH_SHORT).show();
			}
			Button button = (Button) findViewById(R.id.startButton);
			button.setEnabled(true);

			super.onPostExecute(result);
		}

		@Override
		protected Boolean doInBackground(Uri... params) {
			String selectedImagePath = getPath(params[0]);

			if (selectedImagePath == null) { return false; }

			Log.d(TAG, "File Uri: " + params[0].toString() + ", Path: " + selectedImagePath);

			// Just decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(selectedImagePath, o);

			// Get the boundary
			int width_tmp = o.outWidth, height_tmp = o.outHeight;
			int boundary = width_tmp > height_tmp ? width_tmp : height_tmp;
			Log.d(TAG, "The boundary of the photo: " + boundary);

			if (boundary < 1) { return false; }

			// set inPurgeable to true hence they can be purged if the system needs to reclaim memory.
			o = new BitmapFactory.Options();
			o.inPurgeable = true;
			//o.inSampleSize = 1;
			if (boundary > DOWN_SAMPLE_BOUNDARY) {
				o.inSampleSize = 2;
				boundary /= 2;
			}

			// If the boundary is bigger than the max boundary, down sampling the source bitmap
			while (boundary > gMaxBoundary) {
				o.inSampleSize++;
				boundary /= 2;
			}

			if (o.inSampleSize > 2) {
				width_tmp /= (o.inSampleSize - 1) * 2;
				height_tmp /= (o.inSampleSize - 1) * 2;				
			} else {
				width_tmp /= o.inSampleSize;
				height_tmp /= o.inSampleSize;
			}
			Log.d(TAG, "width:" +width_tmp +" height:" +height_tmp);
			// reflection to set the hidden variable for allocating memory to native not heap memory.
			try { BitmapFactory.Options.class.getField("inNativeAlloc").setBoolean(o, true); }
			catch (Exception e) { e.printStackTrace(); }

			Bitmap srcBitmap = BitmapFactory.decodeFile(selectedImagePath, o);

			if (srcBitmap == null) {
				Log.d(TAG, "The src Bitmap is NULL");
				return false;
			}

			// Create a 1:1 size blank bitmap.
			/*
			Bitmap bitmap = Bitmap.createBitmap(boundary, boundary, Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			Paint paint = new Paint();
			// fill out the bitmap with black and put the source bitmap at the center 
			paint.setColor(Color.BLACK);
			canvas.drawRect(new Rect(0, 0, boundary, boundary), paint);
			canvas.drawBitmap(srcBitmap, ((boundary - width_tmp) / 2), ((boundary - height_tmp) / 2), paint);
			canvas.save(Canvas.ALL_SAVE_FLAG);
			canvas.restore();
			*/
			Bitmap bitmap = null;
			Canvas canvas = null;
			Paint paint = new Paint();
			switch(mIndexRatio){
				case 0: //ratio 1:1
					bitmap = Bitmap.createBitmap(boundary, boundary, Config.ARGB_8888);
					canvas = new Canvas(bitmap);
					// fill out the bitmap with black and put the source bitmap at the center
					paint.setColor(Color.BLACK);
					canvas.drawRect(new Rect(0, 0, boundary, boundary), paint);
					canvas.drawBitmap(srcBitmap, ((boundary - width_tmp) / 2), ((boundary - height_tmp) / 2), paint);
					canvas.save(Canvas.ALL_SAVE_FLAG);
					canvas.restore();
				break;
				case 1: //ratio 3:2
					if(width_tmp > height_tmp){
						bitmap = Bitmap.createBitmap(boundary, boundary *2/3, Config.ARGB_8888);
						canvas = new Canvas(bitmap);
						// fill out the bitmap with black and put the source bitmap at the center 
						paint.setColor(Color.BLACK);
						canvas.drawRect(new Rect(0, 0, boundary, boundary *2/3), paint);
						canvas.drawBitmap(srcBitmap, ((boundary - width_tmp) / 2), ((boundary*2/3 - height_tmp) / 2), paint);
					}
					else{
						bitmap = Bitmap.createBitmap(boundary *3/2, boundary, Config.ARGB_8888);
						canvas = new Canvas(bitmap);
						// fill out the bitmap with black and put the source bitmap at the center 
						paint.setColor(Color.BLACK);
						canvas.drawRect(new Rect(0, 0, boundary *3/2, boundary), paint);
						canvas.drawBitmap(srcBitmap, ((boundary *3/2 - width_tmp) / 2), ((boundary - height_tmp) / 2), paint);
					}
					canvas.save(Canvas.ALL_SAVE_FLAG);
					canvas.restore();
				break;
				default:
				break;	
			}
			if(bitmap == null || canvas == null)
				return false;
			
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
			String currentDateandTime = sdf.format(new Date());

			File myDrawFile = new File(Environment.getExternalStorageDirectory().getPath() + "/Black_Border_" + currentDateandTime + ".jpg");
			FileOutputStream fos = null;
			
			Log.d(TAG, "File will be saved at " + myDrawFile.getPath());

			try {
				fos = new FileOutputStream(myDrawFile);
				bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
				fos.flush();
				fos.close();
			} catch (Exception e) { e.printStackTrace(); }

			srcBitmap.recycle();
			bitmap.recycle();

			return true;
		}

		private String getPath(Uri uri) {
			String[] projection = { MediaStore.Images.Media.DATA };
			String resultPath = null;
			Cursor cursor = managedQuery(uri, projection, null, null, null);
			if (cursor != null) {
				int column_index = cursor
						.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
				cursor.moveToFirst();
				resultPath = cursor.getString(column_index);
			}
			return resultPath;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent imageReturnedIntent) {
		super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

		switch (requestCode) {
		case SELECT_PHOTO:
			if (resultCode == RESULT_OK) {
				Uri selectedImageUri = imageReturnedIntent.getData();

				if (selectedImageUri != null) {
					new AsyncBitmapBuilder().execute(selectedImageUri);
					Button button = (Button) findViewById(R.id.startButton);
					button.setEnabled(false);
				}
			}
		}
	}
}
