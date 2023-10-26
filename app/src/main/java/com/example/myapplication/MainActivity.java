package com.example.myapplication;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.View;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity implements SensorEventListener{
    private SensorManager sensorManager;
    private Sensor gyroscope, accelerometer, magneticField, rVector;
    private TextView textViewGyroX, textViewGyroY, textViewGyroZ;
    private TextView textView;
    private PreviewView previewView;
    Button btn0, camBtn;
    Integer[] degrees=new Integer[3];
    int sheets=0, num=1;
    String info="", foldername="test"+num;;
    String azimuth="", pitch="", roll="";
    public static final int CAMERA_PERM_CODE = 101;
    public static final int CAMERA_REQUEST_CODE = 102;
    private static final int REQUEST_IMAGE_CAPTURE = 103;
    private  static final int PERMISSION_REQUEST_CODE = 7;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //gyroscope
//        textViewGyroX=(TextView)findViewById(R.id.gyro_x);
//        textViewGyroY=(TextView)findViewById(R.id.gyro_y);
//        textViewGyroZ=(TextView)findViewById(R.id.gyro_z);

        sensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);
        //gyroscope=sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelerometer=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticField=sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        //rVector=sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

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

        btn0.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view) {
                sheets=0;
                info="";
                //imgInfo.setText("");
                num++;
            }
        });

        camBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                askCameraPermissions();

                foldername="test"+num;
                if (ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                    createDirectory(foldername);
                }
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
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector,preview, imageCapture);
    }

    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    /* create image file */
    private void askCameraPermissions() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.CAMERA}, CAMERA_PERM_CODE);
        }
    }

    File photoFile;
    private void capturePhoto() throws IOException {
        //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        //String imageFileName = "JPEG_" + timeStamp + "_";
        photoFile = null;
        String imageFileName = "img" + sheets;
        //getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        storageDir=new File(storageDir, foldername);
        File image = new File(storageDir.getAbsolutePath()+"/"+imageFileName+".jpg");
        //ImageCapture.OutputFileOptions outputFileOptions =

        imageCapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(image).build(),
                getExecutor(),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                        azimuth=String.format(String.valueOf(degrees[0]));
                        pitch=String.format(String.valueOf(degrees[1]));
                        roll=String.format(String.valueOf(degrees[2]));

                        sheets++;
                        saveText();

//                        info+="Image"+sheets+" - azimuth(Z): " + azimuth + ", pitch(X): " + pitch + ", roll(Y): " + roll + "\n";
//                        imgInfo.setText(info);

                        Toast.makeText(MainActivity.this,"photo has been saved ",Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onError(ImageCaptureException error) {
                        Toast.makeText(MainActivity.this,"error saving photo ",Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /* create text file */
    private void askDirPermission() {
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},PERMISSION_REQUEST_CODE);
    }

    private void saveText(){
        foldername="test"+num;
        askDirPermission();
        writeTextFile(foldername);
    }

    private void writeTextFile(String foldername){
        String filename = "Orientation.txt";
        // 目前日期
        String dateformat = "yyyyMMdd";
        SimpleDateFormat df = new SimpleDateFormat(dateformat);
        Log.d(TAG, "filename == " + filename);
        // 存放檔案位置在 內部空間/Download/
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(path, foldername+"/"+filename);
        try
        {
            // 第二個參數為是否 append
            // 若為 true，則新加入的文字會接續寫在文字檔的最後
            FileOutputStream Output = new FileOutputStream(file, true);

            dateformat = "yyyyMMdd kk:mm:ss";
            df.applyPattern(dateformat);
            //String string = "Hello world! " + df.format(new Date()) + "\n";
            String string=azimuth+" "+pitch+" "+roll+"\n";
            Output.write(string.getBytes());
            Output.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void createDirectory(String foldername){
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(path, foldername);
        if(!file.exists()){
            file.mkdir();
            Toast.makeText(MainActivity.this,"create folder: "+foldername,Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(MainActivity.this, foldername, Toast.LENGTH_SHORT).show();
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
        sensorManager.unregisterListener((SensorEventListener)this);
    }

    float[] accelerometer_values, magnitude_values;
    // real time values
    @Override
    public void onSensorChanged(SensorEvent event) {

       switch (event.sensor.getType()) {
           case Sensor.TYPE_ACCELEROMETER:
               //取得加速度感應器的數值
               accelerometer_values = (float[]) event.values.clone();
               break;
           case Sensor.TYPE_MAGNETIC_FIELD:
               //取得磁場感應器的數值
               magnitude_values = (float[]) event.values.clone();
               break;
           default:
               break;
       }

       if (magnitude_values != null && accelerometer_values != null) {
           float[] R = new float[9]; //rotationMatrix
           float[] values = new float[3];
           SensorManager.getRotationMatrix(R, null, accelerometer_values, magnitude_values);
           SensorManager.getOrientation(R, values);

           //value[0]: azimuth
           //value[1]: pitch
           //value[2]: roll
           for(int i=0;i<values.length;i++)
           {
               degrees[i]= (int) Math.round(values[i] * 180 / Math.PI);
           }

           String str = "azimuth(Z): " + String.format(String.valueOf(degrees[0])) + "\n"
                   + "pitch(X): " + String.format(String.valueOf(degrees[1])) + "\n"
                   + "roll(Y): " + String.format(String.valueOf(degrees[2])) + "\n";

           textView.setText(str);
       }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}