/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ygang.com.zbardemo.zbar.decode;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.CountDownLatch;

import ygang.com.zbardemo.zbar.utils.ZbarResult;

/**
 * This thread does all the heavy lifting of decoding the images.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class DecodeThread extends Thread {


    private final Context activity;

    //扫描结果的回调
    private ZbarResult zbarResult;
    private Handler handler;

    private final CountDownLatch handlerInitLatch;

    DecodeThread(Context activity, ZbarResult zbarResult) {
        this.activity = activity;
        this.zbarResult = zbarResult;
        handlerInitLatch = new CountDownLatch(1);


    }

    Handler getHandler() {
        try {
            handlerInitLatch.await();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        return handler;
    }

    @Override
    public void run() {
        Looper.prepare();
        handler = new DecodeHandler(activity, zbarResult);
        handlerInitLatch.countDown();
        Looper.loop();
    }

}
