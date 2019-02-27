package com.daylight.arccarema;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKMatching;
import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.guo.android_extend.java.AbsLoop;
import com.guo.android_extend.java.ExtByteArrayOutputStream;
import com.guo.android_extend.tools.CameraHelper;
import com.guo.android_extend.widget.CameraFrameData;
import com.guo.android_extend.widget.CameraGLSurfaceView;
import com.guo.android_extend.widget.CameraSurfaceView;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.crud.DataSupport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import pl.com.salsoft.sqlitestudioremote.SQLiteStudioService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;


public class MainActivity extends AppCompatActivity implements CameraSurfaceView.OnCameraListener,View.OnTouchListener,Camera.AutoFocusCallback{
    public static String appid = "4HWArkFf3q48Trk9B5F5Gti8Cs1estFWpcdDGtraMVhu";
    public static String ft_key = "3sAedYdce18nBKaQxcujZmGaFJotr4nY14JKpmzQ6qzY";
    public static String fr_key = "3sAedYdce18nBKaQxcujZmGpa7LCj6XP5drwNVtMQUvU";

    private int mWidth, mHeight, mFormat;
    private CameraGLSurfaceView mGLSurfaceView;
    private Camera mCamera;
    private TextView textView,textView1;
    private ImageView imageView;
    private Handler mHandler;
    private boolean playSound;

    private AFT_FSDKEngine engine = new AFT_FSDKEngine();
    private List<AFT_FSDKFace> result = new ArrayList<>();
    private List<FaceRegister> faceRegisterList;
    private Uri ringtoneUri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

    int mCameraID;
    int mCameraRotate;
    boolean mCameraMirror;
    byte[] mImageNV21 =null;
    FRAbsLoop mFRAbsLoop = null;
    List<AFT_FSDKFace> mAFT_FSDKFace = new ArrayList<>();
    Retrofit retrofit;
    HttpRequest request;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
        mCameraRotate = 0;
        mCameraMirror = false;
        playSound=true;

        mWidth = 1920;
        mHeight = 1080;
        mFormat = ImageFormat.NV21;
        retrofit=new Retrofit.Builder()
                .addConverterFactory(ScalarsConverterFactory.create())
                .baseUrl(SharedPreferencesUtil.getIpAddress(this))
                .build();
        request=retrofit.create(HttpRequest.class);
        Log.e("ipAddress",SharedPreferencesUtil.getIpAddress(this));

        SQLiteStudioService.instance().start(this);

        List<Face> faces = DataSupport.findAll(Face.class,true);

        faceRegisterList=new ArrayList<>();

        for (Face face: faces){
            FaceRegister faceRegister=new FaceRegister(face.getName());
            List<AFR_FSDKFace> afr_fsdkFaces=new ArrayList<>();
            for (Feature feature:face.getFeatureList())
                afr_fsdkFaces.add(new AFR_FSDKFace(feature.getData()));
            faceRegister.setFaceList(afr_fsdkFaces);
            faceRegisterList.add(faceRegister);
        }

        setContentView(R.layout.activity_main);
        textView=findViewById(R.id.textView);
        textView1=findViewById(R.id.textView1);
        imageView=findViewById(R.id.imageView);
        mGLSurfaceView =  findViewById(R.id.glsurfaceView);
        mGLSurfaceView.setOnTouchListener(this);
        CameraSurfaceView mSurfaceView =  findViewById(R.id.surfaceView);
        mSurfaceView.setOnCameraListener(this);
        mSurfaceView.setupGLSurafceView(mGLSurfaceView, true, mCameraMirror, mCameraRotate);
        mSurfaceView.debug_print_fps(false, false);

        mHandler=new Handler();

        engine.AFT_FSDK_InitialFaceEngine(appid, ft_key, AFT_FSDKEngine.AFT_OPF_0_HIGHER_EXT, 16, 5);
        mFRAbsLoop = new FRAbsLoop();
        mFRAbsLoop.start();
    }

    @Override
    protected void onDestroy() {
        mFRAbsLoop.shutdown();
        engine.AFT_FSDK_UninitialFaceEngine();
        SQLiteStudioService.instance().stop();
        super.onDestroy();
    }

    @Override
    public Camera setupCamera() {
        mCamera = Camera.open(mCameraID);
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(mWidth, mHeight);
            parameters.setPreviewFormat(mFormat);

            mCamera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mCamera != null) {
            mWidth = mCamera.getParameters().getPreviewSize().width;
            mHeight = mCamera.getParameters().getPreviewSize().height;
        }
        return mCamera;
    }

    @Override
    public void setupChanged(int format, int width, int height) {

    }

    @Override
    public boolean startPreviewLater() {
        return false;
    }

    @Override
    public Object onPreview(byte[] data, int width, int height, int format, long timestamp) {
        engine.AFT_FSDK_FaceFeatureDetect(data, width, height, AFT_FSDKEngine.CP_PAF_NV21, result);
        if (mImageNV21 == null) {
            if (!result.isEmpty()) {
                for (AFT_FSDKFace face : result) {
                    mAFT_FSDKFace.add(face.clone());
                }
                mImageNV21 = data.clone();
            }else
                mHandler.postDelayed(hide,2000);
        }

        //copy rects
        Rect[] rects = new Rect[result.size()];
        for (int i = 0; i < result.size(); i++) {
            rects[i] = new Rect(result.get(i).getRect());
        }
        //clear result.
        result.clear();
        //return the rects for render.
        return rects;
    }

    @Override
    public void onBeforeRender(CameraFrameData data) {

    }

    @Override
    public void onAfterRender(CameraFrameData data) {
        mGLSurfaceView.getGLES2Render().draw_rect((Rect[])data.getParams(), Color.GREEN, 2);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        CameraHelper.touchFocus(mCamera, event, v, this);
        return false;
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {

    }

    private Runnable hide = new Runnable() {
        @Override
        public void run() {
            textView.setAlpha(0.0f);
            textView1.setAlpha(0.0f);
            imageView.setImageAlpha(0);
        }
    };

    class FRAbsLoop extends AbsLoop {

        AFR_FSDKEngine engine = new AFR_FSDKEngine();
        AFR_FSDKFace result =new AFR_FSDKFace();
        @Override
        public void setup() {
            engine.AFR_FSDK_InitialEngine(appid, fr_key);
        }

        @Override
        public void loop() {
            if (mImageNV21 != null) {
                for (AFT_FSDKFace face:mAFT_FSDKFace) {
                    engine.AFR_FSDK_ExtractFRFeature(mImageNV21, mWidth, mHeight, AFR_FSDKEngine.CP_PAF_NV21, face.getRect(), face.getDegree(), result);

                    AFR_FSDKMatching score = new AFR_FSDKMatching();
                    float max = 0.0f;
                    String strName = null;
                    for (FaceRegister fr : faceRegisterList) {
                        for (AFR_FSDKFace face1 : fr.getFaceList()) {
                            engine.AFR_FSDK_FacePairMatching(result, face1, score);
                            if (max < score.getScore()) {
                                max = score.getScore();
                                strName = fr.getName();
                            }
                        }
                    }

                    byte[] data = mImageNV21;
                    YuvImage yuv = new YuvImage(data, ImageFormat.NV21, mWidth, mHeight, null);
                    ExtByteArrayOutputStream ops = new ExtByteArrayOutputStream();
                    yuv.compressToJpeg(face.getRect(), 80, ops);
                    final Bitmap bmp = BitmapFactory.decodeByteArray(ops.getByteArray(), 0, ops.getByteArray().length);
                    try {
                        ops.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    DetectedFace detectedFace=new DetectedFace();
                    detectedFace.setFace(Bitmap2Bytes(bmp));
                    detectedFace.setFeature(result.getFeatureData());
                    detectedFace.save();

                    RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), Bitmap2Bytes(bmp));
                    MultipartBody.Part body = MultipartBody.Part.createFormData("feature", "", requestFile);
                    RequestBody id = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(1));
                    Call<String> call=request.uploadFeature(body,id,id);
                    call.enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                            if (response.isSuccessful()) {
                                try {
                                    JSONObject jsonObject = new JSONObject(response.body());
                                    if (jsonObject.getBoolean("flag")) {
                                        final String succeed = "已授权";
                                        mHandler.removeCallbacks(hide);
                                        mHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                textView.setAlpha(1.0f);
                                                textView.setText(succeed);
                                                textView.setTextColor(Color.RED);
                                                textView1.setAlpha(1.0f);
                                                textView1.setText("");
                                                textView1.setTextColor(Color.RED);
                                                imageView.setRotation(mCameraRotate);
                                                if (mCameraMirror) {
                                                    imageView.setScaleY(-1);
                                                }
                                                imageView.setImageAlpha(255);
                                                imageView.setImageBitmap(bmp);
                                            }
                                        });
                                        if (playSound) {
                                            playSound(R.raw.door_open);
                                            playSound=false;
                                        }
                                    } else {
                                        mHandler.removeCallbacks(hide);
                                        mHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                textView.setAlpha(1.0f);
                                                textView.setText("未授权");
                                                textView.setTextColor(Color.RED);
                                                textView1.setAlpha(1.0f);
                                                textView1.setText("");
                                                imageView.setRotation(mCameraRotate);
                                                if (mCameraMirror) {
                                                    imageView.setScaleY(-1);
                                                }
                                                imageView.setImageAlpha(255);
                                                imageView.setImageBitmap(bmp);
                                            }
                                        });
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }else{
                                mHandler.removeCallbacks(hide);
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        textView.setAlpha(1.0f);
                                        textView.setText("未授权");
                                        textView.setTextColor(Color.RED);
                                        textView1.setAlpha(1.0f);
                                        textView1.setText("");
                                        imageView.setRotation(mCameraRotate);
                                        if (mCameraMirror) {
                                            imageView.setScaleY(-1);
                                        }
                                        imageView.setImageAlpha(255);
                                        imageView.setImageBitmap(bmp);
                                    }
                                });
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {

                        }
                    });

//                    Log.e("match score",String.valueOf(max));
//
//                    if (max>0.6f) {
//                        final String succeed = "识别成功";
//                        final String name = strName.equals("临时访客")?strName:("姓名:" + strName);
//
//                        if (max<0.65f) {
//                            Feature feature = new Feature();
//                            feature.setData(result.getFeatureData());
//                            feature.save();
//                            Face face1 = DataSupport.where("name=?", strName).findFirst(Face.class);
//                            face1.getFeatureList().add(feature);
//                            face1.save();
//                        }
//                        mHandler.removeCallbacks(hide);
//                        mHandler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                textView.setAlpha(1.0f);
//                                textView.setText(succeed);
//                                textView.setTextColor(Color.RED);
//                                textView1.setAlpha(1.0f);
//                                textView1.setText(name);
//                                textView1.setTextColor(Color.RED);
//                                imageView.setRotation(mCameraRotate);
//                                if (mCameraMirror) {
//                                    imageView.setScaleY(-1);
//                                }
//                                imageView.setImageAlpha(255);
//                                imageView.setImageBitmap(bmp);
//                            }
//                        });
//                    }else{
//                        mHandler.removeCallbacks(hide);
//                        mHandler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                textView.setAlpha(1.0f);
//                                textView.setText("识别失败");
//                                textView.setTextColor(Color.RED);
//                                textView1.setAlpha(1.0f);
//                                textView1.setText("");
//                                imageView.setRotation(mCameraRotate);
//                                if (mCameraMirror) {
//                                    imageView.setScaleY(-1);
//                                }
//                                imageView.setImageAlpha(255);
//                                imageView.setImageBitmap(bmp);
//                            }
//                        });
//                    }
                }
                mAFT_FSDKFace.clear();
            }
            mImageNV21=null;
        }

        @Override
        public void over() {
             engine.AFR_FSDK_UninitialEngine();
        }
    }

    private interface HttpRequest {
        @POST("flow/requestAccess")
        @Multipart
        Call<String> uploadFeature(@Part MultipartBody.Part feature, @Part("bid") RequestBody bid, @Part("cid") RequestBody cid);
    }

    private byte[] Bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    public void playSound(int rawId) {
        SoundPool soundPool;
        if (Build.VERSION.SDK_INT >= 21) {
            SoundPool.Builder builder = new SoundPool.Builder();
            //传入音频的数量
            builder.setMaxStreams(1);
            //AudioAttributes是一个封装音频各种属性的类
            AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
            //设置音频流的合适属性
            attrBuilder.setLegacyStreamType(AudioManager.STREAM_MUSIC);
            builder.setAudioAttributes(attrBuilder.build());
            soundPool = builder.build();
        } else {
            //第一个参数是可以支持的声音数量，第二个是声音类型，第三个是声音品质
            soundPool = new SoundPool(1, AudioManager.STREAM_SYSTEM, 5);
        }
        //第一个参数Context,第二个参数资源Id，第三个参数优先级
        soundPool.load(this, rawId, 1);
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                soundPool.play(1, 1, 1, 0, 0, 1);
            }
        });
        //第一个参数id，即传入池中的顺序，第二个和第三个参数为左右声道，第四个参数为优先级，第五个是否循环播放，0不循环，-1循环
        //最后一个参数播放比率，范围0.5到2，通常为1表示正常播放
//        soundPool.play(1, 1, 1, 0, 0, 1);
        //回收Pool中的资源
        //soundPool.release();

    }
}
