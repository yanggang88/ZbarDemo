package ygang.com.zbardemo;

import android.Manifest;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.PermissionListener;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.RationaleListener;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import ygang.com.zbardemo.zbar.camera.CameraManager;
import ygang.com.zbardemo.zbar.decode.MainHandler;
import ygang.com.zbardemo.zbar.utils.BeepManager;
import ygang.com.zbardemo.zbar.utils.ZbarResult;

public class MainActivity extends AppCompatActivity implements ZbarResult, SurfaceHolder.Callback {
    private boolean needCheckPermission = false;
    /*private static final int MY_PERMISSIONS_REQUEST_CAMERA = 26;
    public static final String EXTRA_STRING = "extra_string";
    public static final int TYPE_BOOK_COVER = 0x101;
    public static final int TYPE_BOOK_CHAPTER = 0x102;*/
    private static final String TAG = "CaptureActivity";
    private MainHandler mainHandler;
    private SurfaceHolder mHolder;

    private CameraManager mCameraManager;
    private BeepManager beepManager;

    private SurfaceView scanPreview;
    private RelativeLayout scanContainer;
    private RelativeLayout scanCropView;
    private ImageView scanLine;
    private Rect mCropRect = null;

    private boolean isHasSurface = false;
    private boolean isOpenCamera = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        requestPermission();
//        checkPermissionCamera();
    }


    //region 检查权限
   /* private void checkPermissionCamera() {
        int checkPermission = 0;
        if (Build.VERSION.SDK_INT >= 23) {
            checkPermission = PermissionChecker.checkSelfPermission(this, Manifest.permission.CAMERA);
            if (checkPermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_CAMERA);
            } else {
                isOpenCamera = true;
            }

        } else {
            checkPermission = checkPermission(26);
            if (checkPermission == AppOpsManager.MODE_ALLOWED) {
                isOpenCamera = true;
            } else if (checkPermission == AppOpsManager.MODE_IGNORED) {
                isOpenCamera = false;
                displayFrameworkBugMessageAndExit();
            }
        }
    }*/

    /**
     * 反射调用系统权限,判断权限是否打开
     * <p>
     * permissionCode 相应的权限所对应的code
     *
     * @see {@link AppOpsManager }
     */
    /*private int checkPermission(int permissionCode) {
        int checkPermission = 0;
        if (Build.VERSION.SDK_INT >= 19) {
            AppOpsManager _manager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            try {
                Class<?>[] types = new Class[]{int.class, int.class, String.class};
                Object[] args = new Object[]{permissionCode, Binder.getCallingUid(), getPackageName()};
                Method method = _manager.getClass().getDeclaredMethod("noteOp", types);
                method.setAccessible(true);
                Object _o = method.invoke(_manager, args);
                if ((_o instanceof Integer)) {
                    checkPermission = (Integer) _o;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            checkPermission = 0;
        }
        return checkPermission;
    }*/

   /* @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA:
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //确认权限
                    checkPermissionCamera();
                    isHasSurface = true;
                    onResume();
                } else {
                    if (!AndPermission.hasPermission(MainActivity.this, Manifest.permission.CAMERA)) {
                        AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).create();
                        dialog.setTitle("获取摄像头");
                        dialog.setMessage("缺少摄像头权限，将不能扫码，需要手动设置！\n设置路径：权限管理->相机");
                        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "去设置", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                needCheckPermission = true;
                                AndPermission.defineSettingDialog(MainActivity.this).execute();
                            }
                        });
                        dialog.setCancelable(false);
                        dialog.show();
                    }
                }
                return;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }*/
    private void initView() {
        scanPreview = (SurfaceView) findViewById(R.id.capture_preview);
        scanContainer = (RelativeLayout) findViewById(R.id.capture_container);
        scanCropView = (RelativeLayout) findViewById(R.id.capture_crop_view);
        scanLine = (ImageView) findViewById(R.id.capture_scan_line);
        findViewById(R.id.capture_imageview_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        isHasSurface = false;
        beepManager = new BeepManager(this);

        TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation
                .RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT,
                0.9f);
        animation.setDuration(3000);
        animation.setRepeatCount(-1);
        animation.setRepeatMode(Animation.RESTART);
        scanLine.startAnimation(animation);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isOpenCamera) {
            mHolder = scanPreview.getHolder();
            mCameraManager = new CameraManager(getApplication());
            if (isHasSurface) {
                initCamera(mHolder);
            } else {
                mHolder.addCallback(this);
                mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            }
        }
    }

    @Override
    public void onPause() {
        releaseCamera();
        super.onPause();
        if (scanLine != null) {
            scanLine.clearAnimation();
            scanLine.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //remove SurfaceCallback
        if (!isHasSurface) {
            scanPreview.getHolder().removeCallback(this);
        }
    }

    //region 初始化和回收相关资源
    private void initCamera(SurfaceHolder surfaceHolder) {
        mainHandler = null;
        try {
            mCameraManager.openDriver(surfaceHolder);
            if (mainHandler == null) {
                mainHandler = new MainHandler(this, this, mCameraManager);
            }
        } catch (IOException ioe) {
            Log.e(TAG, "相机被占用", ioe);
        } catch (RuntimeException e) {
            e.printStackTrace();
            Log.e(TAG, "Unexpected error initializing camera");
            displayFrameworkBugMessageAndExit();
        }

    }

    private void displayFrameworkBugMessageAndExit() {
        String per = String.format(getString(R.string.permission), getString(R.string.camera), getString(R.string.camera));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.qr_name));
        builder.setMessage(per);
        builder.setPositiveButton(getString(R.string.i_know), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }

        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
        builder.show();
    }

    private void releaseCamera() {
        if (null != mainHandler) {
            //关闭聚焦,停止预览,清空预览回调,quit子线程looper
            mainHandler.quitSynchronously();
            mainHandler = null;
        }
        //关闭声音
        if (null != beepManager) {
            Log.e(TAG, "releaseCamera: beepManager release");
            beepManager.releaseRing();
            beepManager = null;
        }
        //关闭相机
        if (mCameraManager != null) {
            mCameraManager.closeDriver();
            mCameraManager = null;
        }
    }

    private void requestPermission() {
        AndPermission.with(this)
                .requestCode(100)
                .permission(Manifest.permission.CAMERA)
                .callback(new PermissionListener() {
                    @Override
                    public void onSucceed(int requestCode, @NonNull List<String> grantPermissions) {
                        isOpenCamera = true;
                        isHasSurface = true;
                        onResume();
                        Toast.makeText(MainActivity.this, "授权成功！", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailed(int requestCode, @NonNull List<String> deniedPermissions) {
                        isOpenCamera = false;
                        if (!AndPermission.hasPermission(MainActivity.this, Manifest.permission.CAMERA)) {
                            AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).create();
                            dialog.setTitle("获取摄像头");
                            dialog.setMessage("缺少摄像头权限，将不能扫码，需要手动设置！\n设置路径：权限管理->相机");
                            dialog.setButton(DialogInterface.BUTTON_POSITIVE, "去设置", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    needCheckPermission = true;
                                    AndPermission.defineSettingDialog(MainActivity.this).execute();
                                }
                            });
                            dialog.setCancelable(false);
                            dialog.show();
                        }
                    }
                })
                .rationale(new RationaleListener() {
                    @Override
                    public void showRequestPermissionRationale(int requestCode, final Rationale rationale) {
                        AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).create();
                        dialog.setTitle("缺少权限");
                        dialog.setMessage("缺少摄像头权限，将不能扫码，请授权！");
                        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "授权", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                needCheckPermission = true;
                                rationale.resume();
                            }
                        });
                        dialog.setCancelable(false);
                        dialog.show();
                    }
                })
                .start();
    }

    @Override
    public void ZbarResultString(final String stringResult) {
        MediaPlayer player = MediaPlayer.create(this, R.raw.qrcode);
        player.start();
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, stringResult, Toast.LENGTH_SHORT).show();
                //设置为连续扫描
                mainHandler.restartPreviewAndDecode();
            }
        }, 100);
    }

    @Override
    public Rect initCrop() {
        int cameraWidth = 0;
        int cameraHeight = 0;
        if (null != mCameraManager) {
            cameraWidth = mCameraManager.getCameraResolution().y;
            cameraHeight = mCameraManager.getCameraResolution().x;
        }

        /** 获取布局中扫描框的位置信息 */
        int[] location = new int[2];
        scanCropView.getLocationInWindow(location);

        int cropLeft = location[0];
        int cropTop = location[1] - getStatusBarHeight();

        int cropWidth = scanCropView.getWidth();
        int cropHeight = scanCropView.getHeight();

        /** 获取布局容器的宽高 */
        int containerWidth = scanContainer.getWidth();
        int containerHeight = scanContainer.getHeight();

        /** 计算最终截取的矩形的左上角顶点x坐标 */
        int x = cropLeft * cameraWidth / containerWidth;
        /** 计算最终截取的矩形的左上角顶点y坐标 */
        int y = cropTop * cameraHeight / containerHeight;

        /** 计算最终截取的矩形的宽度 */
        int width = cropWidth * cameraWidth / containerWidth;
        /** 计算最终截取的矩形的高度 */
        int height = cropHeight * cameraHeight / containerHeight;

        /** 生成最终的截取的矩形 */
        mCropRect = new Rect(x, y, width + x, height + y);
        return new Rect(x, y, width + x, height + y);
    }

    @Override
    public Handler getHandler() {
        return mainHandler;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            Log.e(TAG, "*** 没有添加SurfaceHolder的Callback");
        }
        if (isOpenCamera) {
            if (!isHasSurface) {
                isHasSurface = true;
                initCamera(surfaceHolder);
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        isHasSurface = false;
    }

    private int getStatusBarHeight() {
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object obj = c.newInstance();
            Field field = c.getField("status_bar_height");
            int x = Integer.parseInt(field.get(obj).toString());
            return getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
