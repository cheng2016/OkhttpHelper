package com.icloud.sdk.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.icloud.sdk.view.LoadingBar;
import com.tencent.mm.opensdk.utils.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Created by admin on 2019/6/14.
 * <p>
 * 模仿Async-Http通信库写出的okhttp子线程跨主线程通信类
 * <p>
 * 主要参考AsyncHttpClient、AsyncHttpResponseHandler类
 * <p>
 * loadingBar 加载框
 */
public class OkHttpUtil {
    private static final String TAG = "OkHttpUtil";
    private static LoadingBar loadingBar;
    private static volatile boolean isSetLoading = false;
    private static OkHttpClient client;

    static {
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);
        client = new OkHttpClient.Builder().writeTimeout(30 * 1000, TimeUnit.MILLISECONDS)
                .readTimeout(20 * 1000, TimeUnit.MILLISECONDS)
                .connectTimeout(15 * 1000, TimeUnit.MILLISECONDS)
                //设置拦截器，显示日志信息
                .addNetworkInterceptor(httpLoggingInterceptor)
                .build();
    }

    private OkHttpUtil() {
    }
    
    public static void post(final Context ctx, String url, String jsonStr, final Callback responseHandler) {
        isSetLoading = true;
        loadingBar = new LoadingBar(ctx);
        LogUtil.d("post", "url:" + url + "\njsonStr:" + jsonStr);
        MediaType mediaType = MediaType.parse("text/x-markdown; charset=utf-8");
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(mediaType, Util.generatingSign(jsonStr).toString()))
                .build();
        client.newCall(request).enqueue(responseHandler);
    }
    
    public static void postNoLoading(Context ctx, String url, String jsonStr, final Callback responseHandler) {
        isSetLoading = false;
        LogUtil.d("post", "url:" + url + "\njsonStr:" + jsonStr);
        MediaType mediaType = MediaType.parse("text/x-markdown; charset=utf-8");
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(mediaType, Util.generatingSign(jsonStr).toString()))
                .build();
        client.newCall(request).enqueue(responseHandler);
    }
