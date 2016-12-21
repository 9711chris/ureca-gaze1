package com.example.christantia.switchback;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Date;

import static android.R.attr.angle;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String DEBUG_TAG = "Gestures";
    private static final int MENU_GROUP_ID_SIZE = 2;
    int[] textSizes = {12, 14, 20, 24, 36, 48, 52, 64};
    //private float x = -1, y = -1;
    private float storedX = 0, storedY = 0;
    private int hitBottom = 0;
    private int[] bottomTouched = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
    private Date timeLastTouch = new Date();
    private Mat mBgr;
    private CameraBridgeViewBase mCameraView;
    private CascadeClassifier faceDetector;
    private static final String TAG = MainActivity.class.getSimpleName();
    //private int[] faceExist = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
    private int faceExist = 0;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(final int status) {
            switch(status){
                case LoaderCallbackInterface.SUCCESS:
                    Log.d(TAG, "OpenCV loaded successfully");

                    mBgr = new Mat();
                    try {
                        //load cascade file
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        File mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1){
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        faceDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        faceDetector.load(mCascadeFile.getAbsolutePath());

                        if (faceDetector.empty()){
                            Log.e(TAG, "Failed to load cascade classifier");
                            faceDetector = null;
                        }
                        else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
                        cascadeDir.delete();
                    }catch(IOException e){
                        e.printStackTrace();
                        Log.i(TAG, "Failed to load cascade. Exception thrown: " + e );
                    }
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            };
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraView = (CameraBridgeViewBase) findViewById(R.id.cameraLayout);
        //mCameraView = new JavaCameraView(this, CameraBridgeViewBase.CAMERA_ID_FRONT);
        mCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        mCameraView.enableView();

        mCameraView.setCvCameraViewListener(this);

        //CopyReadAssets();
        //*Don't* hardcode "/sdcard"
        //File sdcard = Environment.getExternalStorageDirectory();

        //Get the text file

        //Read text from file
        StringBuilder text = new StringBuilder();

        try {
            File file = CopyReadAssets();
            BufferedReader br = new BufferedReader(new FileReader(file.getAbsolutePath()));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        }
        catch (Exception e) {
            //You'll need to add proper error handling here
            text.append("error");
        }
        TextView tv = (TextView)findViewById(R.id.text_view);


        tv.setText(text.toString());



    }
    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(final Menu menu){
        //getMenuInflater().inflate(R.menu.activity_menu, menu);

        final SubMenu sizeSubMenu = menu.addSubMenu(R.string.menu_text_size);
        for (int i = 0; i < 8;i++) {
            sizeSubMenu.add(MENU_GROUP_ID_SIZE, i, Menu.NONE, Integer.toString(textSizes[i]));
        }
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getGroupId() == MENU_GROUP_ID_SIZE) {
            TextView tv = (TextView)findViewById(R.id.text_view);
            tv.setTextSize(textSizes[item.getItemId()]);
        }
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        Log.d(DEBUG_TAG,"Action!!");
        TextView tv1 = (TextView)findViewById(R.id.text_view);
        TextView tv2 = (TextView) findViewById(R.id.text_view2);
        float x = event.getX();
        float y = event.getY();
        int i;
        int sum = 0;
        if (event.getAction() == MotionEvent.ACTION_UP) {
            for (i = 0; i < 14; i++)
                bottomTouched[i] = bottomTouched[i + 1];
            if (y >= 2300 && y <= 2600)
                bottomTouched[i] = 1;
            else
                bottomTouched[i] = 0;
            for (i = 0; i < 15; i++)
                sum += bottomTouched[i];
        }
        /*if (faceExist == 1)
            Log.d(DEBUG_TAG, "faceExist = 1");
        else if (faceExist == 0)
            Log.d(DEBUG_TAG, "faceExist = 0");*/
        tv2.setText(Float.toString(x) + "  ,   " + Float.toString(y));
        TextView tv3 = (TextView) findViewById(R.id.text_view3);
        TextView tv4 = (TextView) findViewById(R.id.text_view4);

        RelativeLayout rl = (RelativeLayout) findViewById(R.id.activity_main);
        View shape = (View) findViewById(R.id.shape);
        //ImageView iv = new ImageView(this);

        /*RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(50, 50);
        params.leftMargin = Float.floatToIntBits(x);
        params.topMargin = Float.floatToIntBits(y);
        rl.addView(shape, params);*/

        //hitBottom = 0;
        InteractiveScrollView scrollView = (InteractiveScrollView) findViewById(R.id.ScrollViewID);
        Date timeNewTouch = new Date();
        Date temp = (Date) timeLastTouch.clone();
        temp.setSeconds(timeLastTouch.getSeconds() + 3);
        //tv4.setText(temp.toString());
        int[] coor = new int[2];
        tv1.getLocationOnScreen(coor);
        //if (faceExist == 1)
            if (timeNewTouch.compareTo(temp) >= 0) {
                //shape.setX(storedX-50);
                //shape.setY(storedY-400);
                shape.setX(storedX - coor[0]);
                shape.setY(storedY - coor[1]);
                //shape.setX(storedX-(storedX/2)); shape.setY(storedY-(storedY/2));
                //tv4.setText(Integer.toString(timeNewTouch.getSeconds()) + " " + Integer.toString(timeLastTouch.getSeconds()));
                tv4.setText(Integer.toString(faceExist));

            }
        timeLastTouch = timeNewTouch;
        //tv3.setText("Touch");

        //scrollView.pageScroll(View.FOCUS_DOWN);
        if (sum == 15){
           // scrollView.pageScroll(View.FOCUS_DOWN);
            scrollView.smoothScrollBy(0,200);
        }

        /*switch (event.getAction()) {
            *//*case MotionEvent.ACTION_MOVE:
                tv3.setText("hey");
                break;*//*
            case MotionEvent.ACTION_MOVE:
                Date timeNewTouch = new Date();
                Date temp = (Date) timeLastTouch.clone();
                temp.setSeconds(timeLastTouch.getSeconds() + 3);
                //tv4.setText(temp.toString());
                if (timeNewTouch.compareTo(temp) >= 0) {
                    shape.setX(storedX-50);
                    shape.setY(storedY-400);
                    //shape.setX(storedX-(storedX/2)); shape.setY(storedY-(storedY/2));
                    //tv4.setText(Integer.toString(timeNewTouch.getSeconds()) + " " + Integer.toString(timeLastTouch.getSeconds()));
                    tv4.setText(Float.toString(shape.getX()) + " , " + Float.toString(shape.getY()));
                }
                timeLastTouch = timeNewTouch;
                tv3.setText("Touch");
                *//*InteractiveScrollView scrollView = (InteractiveScrollView) findViewById(R.id.ScrollViewID);
                scrollView.setOnBottomReachedListener(
                        new InteractiveScrollView.OnBottomReachedListener() {
                            @Override
                            public void onBottomReached() {
                                hitBottom = 1;
                            }
                        }
                );
                if (hitBottom == 1) {
                    scrollView.fullScroll(ScrollView.FOCUS_UP);
                    hitBottom = 0;
                }*//*
                break;
            default:
                break;
        }*/
        storedX = x;
        storedY = y;
        return super.dispatchTouchEvent(event);
    }
    /*@Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(DEBUG_TAG,"Action!!");
        x = (int)event.getX();
        y = (int)event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
        }
        return false;
    }*/

    private File CopyReadAssets(){
        AssetManager assetManager = getAssets();

        InputStream in = null;
        OutputStream out = null;
        File file = new File(getFilesDir(), "testswitch.txt");
        try
        {
            in = assetManager.open("testswitch.txt");
            out = openFileOutput(file.getName(), Context.MODE_WORLD_READABLE);

            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
            return file;
        } catch (Exception e)
        {
            Log.e("tag", e.getMessage());
        }

        /*Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(
                Uri.parse("file://" + getFilesDir() + "/BTS_-_Run_Vioin_and_Viola_Duet.pdf"),
                "application/pdf");

        startActivity(intent);*/
        return file;
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException
    {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1)
        {
            out.write(buffer, 0, read);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(final CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        Log.d(DEBUG_TAG,"Camera Activated");
        Mat originalframe = inputFrame.rgba();

        Mat grayFrame = inputFrame.gray(); //to grayscale
        MatOfRect faces = new MatOfRect(); //storage for detected faces
        double scaleFactor = 1.1;
        int minNeighbors = 2;
        int flags = 2;
        Size minSize = new Size(50, 50); //min face size
        Size maxSize = new Size(); //max face size
        Mat reversedframe = new Mat();
        //Core.flip(originalframe, originalframe, 0);
        //Core.flip(originalframe, originalframe, 1);

        //Core.transpose(grayFrame, reversedframe);
       // Core.flip(reversedframe, reversedframe,0); //transpose+flip(1)=CW
        faceDetector.detectMultiScale(grayFrame,faces,scaleFactor,minNeighbors,flags,minSize,maxSize);
        /*for (i = 0; i < 14; i++)
            bottomTouched[i] = bottomTouched[i + 1];
        if (y >= 2300 && y <= 2600)
            bottomTouched[i] = 1;
        else
            bottomTouched[i] = 0;
        for (i = 0; i < 15; i++)
            sum += bottomTouched[i];*/

        Rect[] facesArray = faces.toArray();
        for (int i = 0; i<facesArray.length;i++)
        if (facesArray.length >= 1) {
            Imgproc.rectangle(originalframe, facesArray[0].tl(),facesArray[0].br(),new Scalar(141,116,42),3);
            faceExist = 1;
        }
        else
            faceExist = 0;
       /* String filename = "output222.png";
        System.out.println(String.format("Writing %s", filename));
        Imgcodecs.imwrite(filename, grayFrame.t());*/
        //takePhoto(grayFrame.t());
        return originalframe;
/*
        Mat reversedframe = new Mat();
    	Core.flip(originalframe, reversedframe, 1);
        Mat grayFrame2 = inputFrame.gray();

    	Point upcorner = randomupCorner();
    	Point downcorner = randomdownCorner(upcorner);
    	//int radius = randomRadius();
    	Scalar color = new Scalar(120,194,196);
    	int thickness = 5;

       b2 = (Button)findViewById(R.id.button2);
        b2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (choice == 0)
                    choice = 1;
                else choice = 0;
            }
        });
    	//Imgproc.rectangle(reversedframe, upcorner, downcorner, color);

        int radius = radiusnew;


    	if (choice == 0) {
            for (int i = 0; i<facesArray.length;i++)
                Imgproc.rectangle(reversedframe, facesArray[i].tl(),facesArray[i].br(),new Scalar(141,116,42),3);
            return reversedframe;
        }
        else {
            for (int i = 0; i<facesArray.length;i++)
                Imgproc.rectangle(originalframe, facesArray[i].tl(),facesArray[i].br(),new Scalar(141,116,42),3);
            return originalframe;
        }
        return originalframe;*/
    }
    /*private void takePhoto(final Mat rgba) {


        Uri mUri;
        String mDataPath;


        final long currentTimeMillis = System.currentTimeMillis();
        final String appName = getString(R.string.app_name);
        final String galleryPath =
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES).toString();
        final String albumPath = galleryPath + File.separator +
                appName;
        final String photoPath = albumPath + File.separator +
                currentTimeMillis + ".png";
        final ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, photoPath);
        values.put(MediaStore.Images.Media.MIME_TYPE,"image/png");
        values.put(MediaStore.Images.Media.TITLE, appName);
        values.put(MediaStore.Images.Media.DESCRIPTION, appName);
        values.put(MediaStore.Images.Media.DATE_TAKEN, currentTimeMillis);
// Ensure that the album directory exists.
        File album = new File(albumPath);
        if (!album.isDirectory() && !album.mkdirs()) {
            Log.e(TAG, "Failed to create album directory at " +
                    albumPath);
            //onTakePhotoFailed();
            return;
        }
// Try to create the photo.
        //Imgproc.cvtColor(rgba, mBgr, Imgproc.COLOR_RGBA2BGR, 3);
        if (!Imgcodecs.imwrite(photoPath, rgba)) {
            Log.e(TAG, "Failed to save photo to " + photoPath);
            //onTakePhotoFailed();
        }


        Log.d(TAG, "Photo saved successfully to " + photoPath);
// Try to insert the photo into the MediaStore.
        Uri uri;
        try {
            uri = getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (final Exception e) {
            Log.e(TAG, "Failed to insert photo into MediaStore");
            e.printStackTrace();
// Since the insertion failed, delete the photo.
            File photo = new File(photoPath);
            if (!photo.delete()) {
                Log.e(TAG, "Failed to delete non-inserted photo");
            }
            //onTakePhotoFailed();
            return;
        }
// Open the photo in LabActivity.
        *//*final Intent intent = new Intent(this, LabActivity.class);
        intent.putExtra(LabActivity.EXTRA_PHOTO_URI, uri);
        intent.putExtra(LabActivity.EXTRA_PHOTO_DATA_PATH,
                photoPath);
        startActivity(intent);*//*
    }*/
}
