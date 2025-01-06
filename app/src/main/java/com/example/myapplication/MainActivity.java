package com.example.myapplication;


import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import android.util.Size;

import com.chaquo.python.Python;
import com.chaquo.python.PyObject;
import com.chaquo.python.android.AndroidPlatform;

public class MainActivity extends AppCompatActivity implements SensorEventListener{
    private SensorManager sensorManager;
    private Sensor gyroscope, accelerometer, magneticField, gravitySensor;
    private TextView textView;
    private PreviewView previewView;
    Button btn0, camBtn;

    //number of photos, number of folder
    int sheets=0, num=1;
    String info="", foldername="test"+num;;
    //String azimuth="", pitch="", roll="";
    double px=0,pitch=0; //px:horizontal position
    String phi ="";
    String theta ="";
    private float[] gravity_values = new float[3];
    public static final int CAMERA_PERM_CODE = 101;
    private  static final int WRITE_PERM_CODE = 7;
    private  static final int  READ_PERM_CODE=8;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    long startTime;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        String modelName="model_1002500_medium_400.pt";
//        String modelPath = getFilesDir().getAbsolutePath() + "/"+modelName;
//        copyAssetToStorage(modelName, modelPath);

        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        //gyroscope
//        textViewGyroX=(TextView)findViewById(R.id.gyro_x);
//        textViewGyroY=(TextView)findViewById(R.id.gyro_y);
//        textViewGyroZ=(TextView)findViewById(R.id.gyro_z);

        //sensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        gyroscope=sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelerometer=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticField=sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        //rVector=sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        startTime=System.currentTimeMillis();

        textView=(TextView)findViewById(R.id.textView);
        btn0=(Button) findViewById(R.id.btn0);
        camBtn=(Button)findViewById(R.id.camBtn);
        previewView= findViewById(R.id.previewView);


        cameraProviderFuture= ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                startCameraX(cameraProvider);
            }catch (ExecutionException e){
                e.printStackTrace();
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        },getExecutor());

        askCameraPermissions();

        Python py = Python.getInstance();

        btn0.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view) {
                askReadingPermissions();

//                File downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

                /* /storage/emulated/0/Download/coordCamera/test_/ */
//                String photoPath = storageDir.getAbsolutePath();

                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//                String photoPath = new File(path, "coordCamera/inp_offset5.jpg").getAbsolutePath();
                String photoPath = new File(path, "coordCamera/"+foldername).getAbsolutePath();


//                File imageFile = new File(photoPath);
//                if (!imageFile.exists()) {
//                    Log.e("FileError", "image not exist" + photoPath);
//                }


                PyObject pyObject = py.getModule("test");
                PyObject result = pyObject.callAttr("get_result", photoPath);
//                Toast.makeText(MainActivity.this,"Restart",Toast.LENGTH_SHORT).show();


                boolean hasEmptyArea=result.asList().get(0).toInt() != -1;
                Log.d("PythonResult", result.asList().get(0).toString());

                if(!hasEmptyArea){
                    sheets=0;
                    num++;
                    Toast.makeText(MainActivity.this,"Restart",Toast.LENGTH_SHORT).show();
                } else {
                    String msg="請在 phi: "+result.asList().get(0).toString()+", theta: "+result.asList().get(1).toString()+" 附近再拍一張照片";
//                    String msg="hello";
                    Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_INDEFINITE);
                    snackbar.setAnchorView(R.id.btn0);
                    snackbar.setDuration(8000);
                    snackbar.show();
                }
            }
        });

        camBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                askDirPermission();
                foldername="test"+num;
                createDirectory(foldername);
//                if (ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
//
//                }
                //
//                imageCapture = new ImageCapture.Builder()
//                                .setTargetRotation(view.getDisplay().getRotation())
//                                .build();
                //
                try {
                    capturePhoto();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }


    private void startCameraX(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        //Preview
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture=new ImageCapture.Builder()
                .setTargetResolution(new Size(240, 320))
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector,preview, imageCapture);
    }

    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    File storageDir;
    private void createDirectory(String foldername){
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        storageDir = new File(path, "coordCamera");
        if(!storageDir.exists()){
            storageDir.mkdir();
            //Toast.makeText(MainActivity.this,"create folder: "+foldername,Toast.LENGTH_SHORT).show();
        }
        storageDir = new File(storageDir, foldername);
        if(!storageDir.exists()){
            storageDir.mkdir();
            //Toast.makeText(MainActivity.this,"create folder: "+foldername,Toast.LENGTH_SHORT).show();
        }

    }

    /* create image file */
    private void capturePhoto() throws IOException {
        //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        //String imageFileName = "JPEG_" + timeStamp + "_";

        String imageFileName = "img" + sheets + ".jpg";
        File image = new File(storageDir, imageFileName);

        imageCapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(image).build(),
                ContextCompat.getMainExecutor(this),//getExecutor(),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                        sheets++;
                        writeTextFile();

                        String text= phi +", "+ theta;
                        Toast.makeText(MainActivity.this,"photo ("+text+")has been saved ",Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onError(ImageCaptureException error) {
                        Toast.makeText(MainActivity.this,"error saving photo ",Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /* create text file */

    private void writeTextFile(){
        // 目前日期
//        String dateformat = "yyyyMMdd";
//        SimpleDateFormat df = new SimpleDateFormat(dateformat);

//        File p = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File txt_file = new File(storageDir, "Orientation.txt");

        FileOutputStream Output = null;
        try
        {
            // 第二個參數為是否 append
            // 若為 true，則新加入的文字會接續寫在文字檔的最後
            Output = new FileOutputStream(txt_file, true);

//            dateformat = "yyyyMMdd kk:mm:ss";
//            df.applyPattern(dateformat);
            //String string = "Hello world! " + df.format(new Date()) + "\n";
            String str= phi +" "+ theta +"\n";
            //String string=azimuth+" "+pitch+" "+roll+"\n";
            Output.write(str.getBytes());
            Output.close();
            Log.d("FileWrite", "File written successfully: " + txt_file.getAbsolutePath());
        }
        catch (IOException e)
        {
            e.printStackTrace();
            Log.e("FileWriteError", "Error writing file", e);
        }
    }



    public void copyAssetToStorage(String assetName, String outputPath) {
        InputStream in = null;
        FileOutputStream out = null;
        try {
            in = getAssets().open(assetName);
            File outFile = new File(outputPath);
            out = new FileOutputStream(outFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        //sensorManager.registerListener((SensorEventListener)this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        //sensorManager.registerListener((SensorEventListener)this, magneticField, SensorManager.SENSOR_DELAY_NORMAL);
        //sensorManager.registerListener((SensorEventListener)this, rVector, SensorManager.SENSOR_DELAY_NORMAL);
        //sensorManager.registerListener((SensorEventListener) this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }

        if (magneticField != null) {
            sensorManager.registerListener(this, magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        if (gravitySensor != null) {
            sensorManager.registerListener(this, gravitySensor,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        /*
        if(!(sensorManager.registerListener(listener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                 SensorManager.SENSOR_DELAY_UI) &&
                sensorManager.registerListener(listener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                 SensorManager.SENSOR_DELAY_UI))){
            sensorManager.unregisterListener(listener);
             }

         */
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    //float[] accelerometer_values, magnitude_values;
    private final float[] accelerometer_values = new float[3];
    private final float[] magnitude_values = new float[3];
    private float[] gyroscope_values = new float[3];
    double omegaY, omegaZ, deltaRad;

    // real time values
    @Override
    public void onSensorChanged(SensorEvent event) {

        //textView.setText(String.format(String.valueOf((System.currentTimeMillis()-startTime))));
        //startTime=System.currentTimeMillis();
        String turn="";

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                //取得加速度感應器的數值
                //accelerometer_values = (float[]) event.values.clone();
                System.arraycopy(event.values, 0, accelerometer_values, 0, accelerometer_values.length);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                //取得磁場感應器的數值
                //magnitude_values = (float[]) event.values.clone();
                System.arraycopy(event.values, 0, magnitude_values, 0, magnitude_values.length);
                break;
            case Sensor.TYPE_GRAVITY:
                gravity_values = (float[]) event.values.clone();
                break;
            case Sensor.TYPE_GYROSCOPE:
                long deltaT=(System.currentTimeMillis()-startTime);
                startTime=System.currentTimeMillis();
                //gyroscope_values = (float[]) event.values.clone();
                System.arraycopy(event.values, 0, gyroscope_values, 0, gyroscope_values.length);

                //deltaRadY =event.values[1]*0.04;
                //deltaRadZ =event.values[2]*0.04;
                omegaY=event.values[1];
                omegaZ=event.values[2];
                if(omegaY>0){ //left:+, right:-
                    turn="left";
                } else if (omegaY<0) {
                    turn="right";
                }

                deltaRad=((double)deltaT/1000)*Math.sqrt(omegaY*omegaY + omegaZ*omegaZ);
                //endTime=System.currentTimeMillis();
                break;
            default:
                break;
        }

        if (gyroscope_values != null) {
            if(sheets==0){
                px=0;
            }
            else{
                if(turn.equals("left")){
                    px+=-deltaRad;
                }else if(turn.equals("right")){
                    px+=deltaRad;
                }

            }
        }

        if (magnitude_values != null && accelerometer_values != null) {
            float[] R = new float[9]; //rotationMatrix
            float[] values = new float[3];
            SensorManager.getRotationMatrix(R, null, accelerometer_values, magnitude_values);
            SensorManager.getOrientation(R, values);

            //value[0]: azimuth
            //value[1]: pitch
            //value[2]: roll
//            for(int i=0;i<values.length;i++)
//            {
//                //orientation[i]= (int) Math.round(values[i] * 180 / Math.PI);
//                orientation[i]= (int)Math.round(Math.toDegrees(values[i]));
//            }
            pitch=values[1];

            //Correction theta
            phi =String.format(String.valueOf(Math.round((Math.toDegrees(px)+360)%360)));
            //Correction phi
            pitch= Math.toDegrees(pitch);

            if(gravity_values[2]>=0){ //螢幕朝上
                pitch+=90;
            }else{ //螢幕朝下
                pitch=-(pitch+90);
            }
            theta =String.format(String.valueOf(Math.round(pitch)));


            info = "phi(positionX): " + phi + "\n"
                    + "theta(pitch): " + theta + "\n";

//            info = "azimuth(Z): " + String.format(String.valueOf(orientation[0])) + "\n"
//                    + "pitch(X): " + String.format(String.valueOf(orientation[1])) + "\n"
//                    + "roll(Y): " + String.format(String.valueOf(orientation[2])) + "\n";

            //str+=gravity_values[0]+", "+gravity_values[1]+", "+gravity_values[2];
            textView.setText(info);
        }
    }

    private void askCameraPermissions() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.CAMERA}, CAMERA_PERM_CODE);
        }
    }
    private void askReadingPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_PERM_CODE);
        }
    }
    private void askDirPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_PERM_CODE);
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}