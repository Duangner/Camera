package com.qianfeng.ryan.camerademo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.view.annotation.ContentView;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.lidroid.xutils.view.annotation.event.OnClick;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@ContentView(R.layout.activity_main)
public class MainActivity extends AppCompatActivity {

    @ViewInject(R.id.sfPreView)
    private SurfaceView surfaceView;

    private Camera camera;

    private int iFrontCameraId;//前置摄像头
    private int iBackCameraId;//后置摄像头
    private int iCameraCnt;//摄像头数量
    private boolean isBack;//是否后置摄像头
    //新的


    /**
     * 获取摄像头信息
     */
    protected void getCameraInfo(){
        //定义一个结构存储相机信息
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        iCameraCnt = Camera.getNumberOfCameras();//得到相机数量
        Log.d("getCameraInfo","iCameraCnt = "+iCameraCnt);
        for (int i = 0; i < iCameraCnt; i++) {
            Camera.getCameraInfo(i,cameraInfo);
            //判断是否前置摄像头
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
                iFrontCameraId = i;
                Log.d("getCameraInfo","iFrontCameraId="+iFrontCameraId);
            }
            //判断是否后置摄像头
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK){
                iBackCameraId = i;
                Log.d("getCameraInfo","iBackCameraId="+iBackCameraId);
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
        ViewUtils.inject(this);

        //设置回调
        surfaceView.getHolder().addCallback(new MyCallBack());

        getCameraInfo();
    }


    //点击拍照按钮调用
    @OnClick(R.id.begin_btn)
    public void onTakePicture(View view){
        camera.takePicture(null, null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                //通过数据流得到图片
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                // 保存图片
                saveBitmap(rotateBitmap(bitmap));

                camera.startPreview();

            }
        });
    }


    /**
     * 保存图片到本地
     * @param bmToSave
     */
    public void saveBitmap(Bitmap bmToSave){
        //指定图片名为当前时间
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddhhmmss");
        String stringDT = format.format(new Date());
        String strName = "/sdcard/duang"+stringDT+".jpg";
        File file = new File(strName);
        //使用带缓冲区的输出流保存图片

        try {
            BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(file));

            //第二个参数:图片质量
            bmToSave.compress(Bitmap.CompressFormat.JPEG,100,stream);

            //输出缓冲区的内容到本地
            stream.flush();
            stream.close();

        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    protected Bitmap rotateBitmap(Bitmap srcBm){

        Bitmap bmRet = null;

        Matrix matrix = new Matrix();

        matrix.setRotate(90);

        bmRet = Bitmap.createBitmap(srcBm,0,0,srcBm.getWidth(),srcBm.getHeight(),matrix,true);

        srcBm.recycle();

        return bmRet;

    }

    /**
     * 切换摄像头
     * @param view
     */
    public void onChange(View view) {
        //释放前面已经打开的摄像头
        if (camera != null){
            camera.release();
            camera = null;
        }
        //判断当前是哪个摄像头，然后打开另外一个摄像头
        if (isBack){
            camera = Camera.open(iFrontCameraId);
            isBack = false;

//            camera.setPreviewDisplay();
            try {
                camera.setPreviewDisplay(surfaceView.getHolder());
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else {
            camera = Camera.open(iBackCameraId);
            isBack = true;
//            setCamera();
            try {
                camera.setPreviewDisplay(surfaceView.getHolder());
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    /**
     * surfaceView 的回调类
     */
    class MyCallBack implements SurfaceHolder.Callback{
        void setCamera(){
            camera.setDisplayOrientation(90);

            Camera.Parameters parameters = camera.getParameters();

            //获取手机支持的图片尺寸列表
            List<Camera.Size> pictureSizes = parameters.getSupportedPictureSizes();

            parameters.setPictureSize(pictureSizes.get(11).width,pictureSizes.get(11).height);
            parameters.setPictureFormat(ImageFormat.JPEG);

            camera.setParameters(parameters);

        }
        //创建的时候调用
        @Override
        public void surfaceCreated(SurfaceHolder holder) {


            camera = Camera.open();

            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
                setCamera();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        //状态改变的时候调用
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            //自动对焦
            camera.autoFocus(null);
        }

        //销毁的时候调用
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {


            //重要:释放资源
            if (camera!=null){
                camera.release();
                camera = null;
            }

        }
    }
}
