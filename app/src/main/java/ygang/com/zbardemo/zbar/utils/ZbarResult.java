package ygang.com.zbardemo.zbar.utils;

import android.graphics.Rect;
import android.os.Handler;

/**
 * author : ygang
 * email : 1334045135@qq.com
 * date : 2019/3/30 10:43
 **/
public interface ZbarResult {
    public void ZbarResultString(String stringResult);

    //region  初始化截取的矩形区域
    public Rect initCrop();

    public Handler getHandler();
}
