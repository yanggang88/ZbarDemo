package ygang.com.zbardemo.zbar.decode;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import ygang.com.zbardemo.R;
import ygang.com.zbardemo.zbar.camera.CameraManager;
import ygang.com.zbardemo.zbar.utils.ZbarResult;

/**
 * desc:主线程的Handler
 * Author: znq
 * Date: 2016-11-03 15:55
 */
public class MainHandler extends Handler {
    private static final String TAG = "MainHandler";

    private final Context activity;
    //扫描结果的回调
    private ZbarResult zbarResult;
    /**
     * 真正负责扫描任务的核心线程
     */
    private final DecodeThread decodeThread;

    private State state;

    private final CameraManager cameraManager;

    public MainHandler(ZbarResult zbarResult, Context activity, CameraManager cameraManager) {
        this.activity = activity;
        this.zbarResult = zbarResult;
        // 启动扫描线程
        decodeThread = new DecodeThread(activity, zbarResult);
        decodeThread.start();
        state = State.SUCCESS;
        this.cameraManager = cameraManager;

        // 开启相机预览界面.并且自动聚焦
        cameraManager.startPreview();

        restartPreviewAndDecode();
    }

    /**
     * 当前扫描的状态
     */
    private enum State {
        /**
         * 预览
         */
        PREVIEW,
        /**
         * 扫描成功
         */
        SUCCESS,
        /**
         * 结束扫描
         */
        DONE
    }


    @Override
    public void handleMessage(Message msg) {
        if (msg.what == R.id.decode_succeeded) {
            String result = (String) msg.obj;
            if (!TextUtils.isEmpty(result)) {
                zbarResult.ZbarResultString(result);
            }
            state = State.SUCCESS;
        } else if (msg.what == R.id.restart_preview) {
            restartPreviewAndDecode();
        } else if (msg.what == R.id.decode_failed) {
            // We're decoding as fast as possible, so when one decode fails,
            // start another.
            state = State.PREVIEW;
            cameraManager.requestPreviewFrame(decodeThread.getHandler(),
                    R.id.decode);
        }
    }

    /**
     * 完成一次扫描后，只需要再调用此方法即可
     */
    public void restartPreviewAndDecode() {
        if (state == State.SUCCESS) {
            state = State.PREVIEW;
            // 向decodeThread绑定的handler（DecodeHandler)发送解码消息
            cameraManager.requestPreviewFrame(decodeThread.getHandler(),
                    R.id.decode);
        }
    }

    public void quitSynchronously() {
        state = State.DONE;
        cameraManager.stopPreview();
        Message quit = Message.obtain(decodeThread.getHandler(), R.id.quit);
        quit.sendToTarget();

        try {
            // Wait at most half a second; should be enough time, and onPause()
            // will timeout quickly
            decodeThread.join(500L);
        } catch (InterruptedException e) {
            // continue
        }

        // Be absolutely sure we don't send any queued up messages
        removeMessages(R.id.decode_succeeded);
        removeMessages(R.id.decode_failed);
    }

}
