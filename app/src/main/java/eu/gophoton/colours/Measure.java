package eu.gophoton.colours;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View.OnClickListener;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

import eu.gophoton.colours.R;


public class Measure extends Fragment {
	
	private static final AtomicBoolean processing = new AtomicBoolean(false);
	private static SurfaceView preview = null;
	private static SurfaceHolder previewHolder = null;
	private static SurfaceView transparentView = null;
	private static SurfaceHolder holderTransparent = null;
	private static Camera camera = null;
	private boolean trigger = false; //This value of this determines start/stop camera
	private static ImageView tiviImageView = null;
	private static ImageView redImageView = null;
	private static ImageView greenImageView = null;
	private static ImageView blueImageView = null;
	private static XYPlot tiviHistoryPlot = null;
	private static SimpleXYSeries tiviHistorySeries = null;
	private static Canvas canvas = null;
	private static Paint paint = null;
	private static float tiviImageScaleMin = 0;
	private static float tiviImageScaleMax = 1;
	private static boolean scaleTiViImage = true;
    private int CameraPermissionRequestCode = 1;

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

    	View ios = inflater.inflate(R.layout.measure_frag, container, false);
    	
    	((TextView)ios.findViewById(R.id.textView3)).setText(R.string.Measure); 
        
        final Button start_stop_button = (Button) ios.findViewById(R.id.button1);  
        start_stop_button.setText(R.string.btn_start_text); //Initialize text on button
    	
    	start_stop_button.setOnClickListener(
    			new OnClickListener() {
    				@Override
    				public void onClick(View v){

                        if (ActivityCompat.checkSelfPermission(getContext(),
                                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            if (trigger == false) {
                                trigger = true;
                                startProcessing();
                                start_stop_button.setText(R.string.btn_stop_text); //Rename the text on button
                            } else {
                                trigger = false;
                                stopProcessing();
                                start_stop_button.setText(R.string.btn_start_text); //Rename the text on button
                            }
                        }
                        else
                        {
                            requestPermissions(new String[]{Manifest.permission.CAMERA}, CameraPermissionRequestCode);
                        }
    				} //End onClick(View v)	          					  
    			}
    	);
        
    	preview = (SurfaceView) ios.findViewById(R.id.full_colour);
    	previewHolder = preview.getHolder();
    	previewHolder.addCallback(surfaceCallback);
    	
    	// Create second surface with another holder (holderTransparent)
        transparentView = (SurfaceView)ios.findViewById(R.id.TransparentView);
        transparentView.setZOrderMediaOverlay(true);
        holderTransparent = transparentView.getHolder();
        holderTransparent.setFormat(PixelFormat.TRANSPARENT);
        holderTransparent.addCallback(surfaceCallback); 
    	
    	tiviImageView = (ImageView) ios.findViewById(R.id.tiviImageView);
    	
    	redImageView = (ImageView) ios.findViewById(R.id.redImageView);
    	greenImageView = (ImageView) ios.findViewById(R.id.greenImageView);
    	blueImageView = (ImageView) ios.findViewById(R.id.blueImageView); 	
    	
    	tiviHistoryPlot = (XYPlot) ios.findViewById(R.id.tiviHistoryPlot);
        tiviHistorySeries = new SimpleXYSeries("Mean Contrast Value");
        tiviHistorySeries.useImplicitXVals();
        tiviHistoryPlot.addSeries(tiviHistorySeries, new LineAndPointFormatter(Color.rgb(0, 0, 255), null, null, null));
       // tiviHistoryPlot.getGraphWidget().setDomainLabelPaint(null);//This gets rid of the x labels
       // tiviHistoryPlot.getGraphWidget().setRangeLabelPaint(null);//This gets rid of the y labels
        tiviHistoryPlot.getLayoutManager().remove(tiviHistoryPlot.getLegendWidget());
        tiviHistoryPlot.setTitle(getString(R.string.graph_title));
        tiviHistoryPlot.setRangeBoundaries(30, 200, BoundaryMode.AUTO);
        tiviHistoryPlot.getTitleWidget().setPaddingBottom((float) 0.0);
        tiviHistoryPlot.getGraphWidget().setPaddingTop((float) 10.0);
        tiviHistoryPlot.setDomainLabel(getString(R.string.x_label));  //Defines the x-axis
        tiviHistoryPlot.setRangeLabel(getString(R.string.y_label));   //Defines the y-axis   	
       
        return ios;
    }
    
    /**{@inheritDoc} */
    @Override
    public void onResume() {
        super.onResume();
        
        if(trigger==true)
        {
        	getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    /**{@inheritDoc} */
    @Override
    public void onPause() {
        super.onPause();
        
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        if(camera!=null)
        {
        	camera.setPreviewCallback(null);
        	camera.stopPreview();
        	camera.release();
        	camera = null;
        }
    }
    
    public void startProcessing()
    {
    	getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    	camera = Camera.open();
        Camera.Parameters parameters = camera.getParameters();
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        
    	Camera.Size size = getSmallestPreviewSize(parameters);
    	if (size != null) {
    		parameters.setPreviewSize(size.width, size.height);
    		Log.d(getClass().getSimpleName(), "Using width=" + size.width + " height=" + size.height);
    	}
        
        camera.setParameters(parameters);
        camera.setDisplayOrientation(90);
        
        try {
        	camera.setPreviewDisplay(previewHolder);
        }	catch (IOException e) {
        	e.printStackTrace();	
        }
        camera.setPreviewCallback(previewCallback);;
        camera.startPreview(); 
    }
    
    //Wrap up all the camera functions
    public void stopProcessing()
    {
    	getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    	
    	if(camera!=null)
    	{
    		camera.setPreviewCallback(null);
    		camera.stopPreview();
         	camera.release();
         	camera = null;
    	} 
    	
    }

    private static PreviewCallback previewCallback = new PreviewCallback() {

        /**{@inheritDoc}*/
        @SuppressLint("DefaultLocale")
    	@Override
        public void onPreviewFrame(byte[] data, Camera cam) {
        	
            if (data == null) throw new NullPointerException();
            
            Camera.Size size = cam.getParameters().getPreviewSize();
            
            if (size == null) throw new NullPointerException();

            // compareAndSet(boolean expect, boolean update)
            // Atomically sets the value to the given updated value if the current value == the expected value.
            // Returns true if successful. False return indicates that the actual value was not equal to the expected value.
            // So in the case above, if processing is false, then we set it to true. 
            // If this fails (returns false) it means that processing == true
            if (!processing.compareAndSet(false, true)) 
            {
            	// Reject frame because we are already processing one.
            	return;
            }

            int width = size.width;
            int height = size.height; 

            DrawFocusRect();
            
            new FrameProcessor(data, width, height, tiviImageView, redImageView, greenImageView, blueImageView).execute();
        }
    }; 
    
    //Loops through sizes and returns smallest. 
    private static Camera.Size getSmallestPreviewSize(Camera.Parameters parameters) {
    	
    	Camera.Size result = null;
    	int cWidth = Integer.MAX_VALUE;
    	int cHeight = Integer.MAX_VALUE;

    	for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
    		if (size.width <= cWidth && size.height <= cHeight) {
    			
    			if (result == null) {
    				result = size;
    			}   
    			else {
    				int resultArea = result.width * result.height;
    				int newArea = size.width * size.height;

    				if (newArea < resultArea) 
    				{
    					result = size;
    				}
    			}
    		}
    	}

    	return result;
    }

    private static SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

        /**{@inheritDoc} */
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            
        }

        /**{@inheritDoc}*/
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        	// This method is required
        }

        /**{@inheritDoc} */
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
        	// This method is required
        }
        

    };
    
    private static void DrawFocusRect()
    {
    	int hWidth = transparentView.getWidth();
    	int hHeight = transparentView.getHeight();
         
		float f = (float) 0.5;
		float rLeft = (float) Math.floor(((1-f)/2)*hWidth);
		float rRight = (float) Math.floor((((1-f)/2)+f)*hWidth);
		float rTop = (float) Math.floor(((1-f)/2)*hHeight);
		float rBottom = (float) Math.floor((((1-f)/2)+f)*hHeight);

        canvas = holderTransparent.lockCanvas();
        canvas.drawColor(0, Mode.CLEAR);
        //border's properties
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(3);
        canvas.drawRect(rLeft, rTop, rRight, rBottom, paint);

        holderTransparent.unlockCanvasAndPost(canvas);
    }
    
    private static class FrameProcessor extends AsyncTask<Void, Void, Float>
    {
    	byte[] frame;
    	int width;
    	int height;
    		
        
        ColorMap tiviMap = ColorMap.getColorMap(ColorMap.TYPE_TIVI);
        
        ColorMap redMap = ColorMap.getColorMap(ColorMap.TYPE_RED);
        ColorMap greenMap = ColorMap.getColorMap(ColorMap.TYPE_GREEN); 
        ColorMap blueMap = ColorMap.getColorMap(ColorMap.TYPE_BLUE);
        
        int[] rgb = null;
        
        int[] redImage = null;
        int[] greenImage = null;
        int[] blueImage = null;
        
        Bitmap currentTiViFrame;
        
        Bitmap currentRedFrame;
        Bitmap currentGreenFrame;
        Bitmap currentBlueFrame;
        
        private final WeakReference<ImageView> tiviImageViewReference;
        
        private final WeakReference<ImageView> redImageViewReference;
        private final WeakReference<ImageView> greenImageViewReference;
        private final WeakReference<ImageView> blueImageViewReference;
        
        private float x1;
        private float x2;
        private float y1;
        private float y2;
        
        int numTiViDistBins = 100;  
        float scaleWidth = (float) 0.2;
        
        int[] tiviDistribution = null;
        float binWidth = (float) 1/numTiViDistBins;

	//	Log.d("i = " + binWidth);
		
    	FrameProcessor(byte[] frm, int w, int h, ImageView tiviImageView,
    			ImageView redImageView, ImageView greenImageView, ImageView blueImageView) { 
            this.frame = frm;
            this.width = w;
            this.height = h;
            
            rgb = new int[frm.length];
            
            redImage = new int[frm.length];
            greenImage = new int[frm.length];
            blueImage = new int[frm.length];
            
            currentTiViFrame = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            
            currentRedFrame = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            currentGreenFrame = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            currentBlueFrame = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            
            // Use a WeakReference to ensure the ImageView can be garbage collected
            tiviImageViewReference = new WeakReference<ImageView>(tiviImageView);
            
            redImageViewReference = new WeakReference<ImageView>(redImageView);
            greenImageViewReference = new WeakReference<ImageView>(greenImageView);
            blueImageViewReference = new WeakReference<ImageView>(blueImageView);
            
            float f = (float) 0.25;
    		x1 = (float) Math.floor(((1-f)/2)*width);
    		x2 = (float) Math.floor((((1-f)/2)+f)*width);
    		y1 = (float) Math.floor(((1-f)/2)*height);
    		y2 = (float) Math.floor((((1-f)/2)+f)*height);
    		
    		tiviDistribution = new int[numTiViDistBins];
    		
    		initializeTiViDistribution();
        }
    	
    	@Override
    	protected Float doInBackground(Void... params) {
    		
    		// Return result of my mean TiVi Calculation here
    		return decodeYUV420SP();
    		
    	}
    	
    	@Override
        protected void onPostExecute(Float avg) {
    		
    		currentTiViFrame.setPixels(rgb, 0, width, 0, 0, width, height);
    		
    		currentRedFrame.setPixels(redImage, 0, width, 0, 0, width, height);
    		currentGreenFrame.setPixels(greenImage, 0, width, 0, 0, width, height);
    		currentBlueFrame.setPixels(blueImage, 0, width, 0, 0, width, height);

    		if (tiviImageViewReference != null && currentTiViFrame != null) {
                final ImageView imageView = tiviImageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(currentTiViFrame);
                    imageView.setRotation(90);
                }
            }
    		
    		rgb = null;
    		currentTiViFrame = null;
    		
    		if (redImageViewReference != null && currentRedFrame != null) {
                final ImageView redImageView = redImageViewReference.get();
                if (redImageView != null) {
                    redImageView.setImageBitmap(currentRedFrame);
                    redImageView.setRotation(90);
                }
            }
    		
    		redImage = null;
    		currentRedFrame = null;
    		
    		if (greenImageViewReference != null && currentGreenFrame != null) {
                final ImageView greenImageView = greenImageViewReference.get();
                if (greenImageView != null) {
                    greenImageView.setImageBitmap(currentGreenFrame);
                    greenImageView.setRotation(90);
                }
            }
    		
    		greenImage = null;
    		currentGreenFrame = null;
    		
    		if (blueImageViewReference != null && currentBlueFrame != null) {
                final ImageView blueImageView = blueImageViewReference.get();
                if (blueImageView != null) {
                    blueImageView.setImageBitmap(currentBlueFrame);
                    blueImageView.setRotation(90);
                }
            }
    		
    		blueImage = null;
    		currentBlueFrame = null;
    		
    		tiviHistorySeries.addLast(null, avg);
    		
    		tiviHistoryPlot.redraw();

    		processing.set(false);
        }
    	
        // Byte decoder :
        //
        // ---------------------------------------------------------------------
        float decodeYUV420SP() {
                // Pulled directly from:
                //
                // http://ketai.googlecode.com/svn/trunk/ketai/src/edu/uic/ketai/inputService/KetaiCamera.java
                final int frameSize = width * height;

                float val = 0;
                
                float tiviSum = 0;

                int j, yp, uvp, i, y, y1192, r, g, b, u, v;
                
                int pixelsROICnt = 0;
                
                for (j = 0, yp = 0; j < height; j++) {
                        uvp = frameSize + (j >> 1) * width;
                        u = 0;
                        v = 0;
                        for (i = 0; i < width; i++, yp++) {
                                y = (0xff & ((int) frame[yp])) - 16;
                                if (y < 0)
                                        y = 0;
                                if ((i & 1) == 0) {
                                        v = (0xff & frame[uvp++]) - 128;
                                        u = (0xff & frame[uvp++]) - 128;
                                }

                                y1192 = 1192 * y;
                                r = (y1192 + 1634 * v);
                                g = (y1192 - 833 * v - 400 * u);
                                b = (y1192 + 2066 * u);

                                if (r < 0)
                                        r = 0;
                                else if (r > 262143)
                                        r = 262143;
                                if (g < 0)
                                        g = 0;
                                else if (g > 262143)
                                        g = 262143;
                                if (b < 0)
                                        b = 0;
                                else if (b > 262143)
                                        b = 262143;

                                // Convert to RGB
                                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);

                                /*
                                 * Tivi Processing
                                 */
                                r = Color.red(rgb[yp]);
                                g = Color.green(rgb[yp]);
                                b = Color.blue(rgb[yp]);
                                if (g == 0) {
                                        g = 1;
                                }
                                
                                float tiviVal;
                                if((r+g) == 0)
                                {
                                	// Prevent div by 0
                                	tiviVal = (float) ((r - g) / 0.00000001);
                                }
                                else {
                                	tiviVal = ((r - g) / ((float) r + g));
                                }
                                
                                // Convert to range 0 to 1
                                //val = (tiviVal - minVal) / (maxVal - minVal); //Obsolete if minVal=0
                                val = tiviVal;
                                if (val < 0) {
                                        val = 0;
                                }
                                if (val > 1) {
                                        val = 1;  //Originally 0
                                }
                                
                                double yt = Math.floor((yp-1)/width)+1;
                                double xt = yp-((yt-1)*width); 
                                
                
                                if((xt >= x1) && (xt <= x2) && (yt >= y1) && yt <= y2)
                                {
                                	tiviSum += val;
                                	pixelsROICnt++;
                                }
                                
                                if (scaleTiViImage)
                                {	
                                	binTiViValue(val);
                                
	                                val = displayRangeConvertor(val);
                                }
                                
                                rgb[yp] = tiviMap.getColor(val);
                                redImage[yp] = redMap.getColor(r);
                                greenImage[yp] = greenMap.getColor(g);
                                blueImage[yp] = blueMap.getColor(b);
                                
                        }
                }
                
                if (scaleTiViImage)
                {
                	setMaxAndMin();
                }
                
                float tiviAverage = 0;
                if (pixelsROICnt != 0)
                {
                	tiviAverage = tiviSum/pixelsROICnt;
                }
                
        	    return tiviAverage;
        }
        
        float displayRangeConvertor(float val)
        {
        	if(val < tiviImageScaleMin)
        	{
        		val = tiviImageScaleMin;
        	}
        	if(val > tiviImageScaleMax)
        	{
        		val = tiviImageScaleMax;
        	}
        	float newRange = tiviImageScaleMax - tiviImageScaleMin;
        	
        	float newDisplayValue = (val - tiviImageScaleMin)/newRange;
        	
        	return newDisplayValue;
        }
        
        void initializeTiViDistribution()
        {        	
        	for(int i = 0; i < numTiViDistBins; i++)
        	{
        		tiviDistribution[i] = 0;
        	}
        }

        void binTiViValue(float valToBin)
        {
        	float div = valToBin/binWidth;
        	//Log.e("Paul's tag","valToBin = " + valToBin);
        	int bin = (int) Math.floor(div);
        	//tiviDistribution[bin] = tiviDistribution[bin]++;
        	int currentCount = tiviDistribution[bin];
        	currentCount = currentCount+1;
        	tiviDistribution[bin] = currentCount;
        	//Log.e("Paul's tag","Bin = " + bin);
        	//Log.e("Paul's tag","tiviDistribution = " + tiviDistribution[bin]);  //WHY IS THIS NOT FILLING UP?
        	//Log.e("Paul's tag","valToBin = " + valToBin);
        	
        }
        
        void setMaxAndMin()
        {
        	int maxCount = 0;
        	int maxIndex = 0;
        	
        	for(int i = 5; i < numTiViDistBins-1; i++) //NB ignore first and last bins
        	{
        		int cnt = tiviDistribution[i];
        		//Log.e("Paul's tag","i = " + i);
        		//Log.e("Paul's tag","Distribution[i] = " + tiviDistribution[i]);
        		
        		if(cnt > maxCount)  //If a bin with a larger value is found
        		{
        			maxCount = cnt;  //Update the count
        			maxIndex = i;   //Update the bin number
        			//Log.e("In Loop","maxIndex = " + maxIndex);
        		}    		
        	}
        	
        	float medianTiViVal = maxIndex*binWidth;
        	float newMin = medianTiViVal - scaleWidth;
        	float newMax = medianTiViVal + scaleWidth;
        	

        	//Catch for if the scaleWidth is outside the limits of 0 and 1
        	if(newMin < 0)
        	{
        		newMin = 0;
        	}
        	if(newMax > 1)
        	{
        		newMax = 1;
        	}
        	tiviImageScaleMin = newMin;
        	tiviImageScaleMax = newMax;
        	//Log.e("Paul's tag","maxIndex = " + maxIndex);
        	//Log.e("Paul's tag","medianTiViVal = " + medianTiViVal);
        	//Log.e("Paul's tag","tiviImageScaleMin = " + newMin);
        	//Log.e("Paul's tag","tiviImageScaleMax = " + newMax);
        	
        }
    	
    }
	
}