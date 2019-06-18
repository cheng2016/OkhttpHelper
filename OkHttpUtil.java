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
    
    public abstract static class SimpleResponseHandler implements Callback {
        private static final int START = -1;
        private static final int FINISH = 2;
        private Handler handler;

        public SimpleResponseHandler() {
            Looper looper = Looper.myLooper();
            if (looper == Looper.getMainLooper()) {
                this.handler = new ResultHandler(this, looper);
            } else {
                this.handler = new ResultHandler(this, Looper.getMainLooper());
            }
            sendTagetMessage(START);
        }
        
         public void handleMessage(Message message) {
            switch (message.what) {
                case START:
                    onStart();
                    break;
                case 0:
                    Object[] objects = (Object[]) message.obj;
                    onSuccess((Call) objects[0], (Response) objects[1]);
                    sendTagetMessage(FINISH);
                    break;
                case 1:
                    onFailer((Exception) message.obj);
                    sendTagetMessage(FINISH);
                    break;
                case FINISH:
                    onFinish();
                    break;
                default:
                    break;
            }
        }
        
        void sendTagetMessage(int what) {
            Message msg = handler.obtainMessage();
            msg.what = what;
            msg.sendToTarget();
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            Log.d(TAG, "onResponse current Thread: " + Thread.currentThread().getName());
            Message msg = handler.obtainMessage();
            msg.what = 0;
            msg.obj = new Object[]{call, response};
            msg.sendToTarget();
        }
        
        @Override
        public void onFailure(Call call, IOException e) {
            Log.d(TAG, "onFailure current Thread: " + Thread.currentThread().getName());
            Message msg = handler.obtainMessage();
            msg.what = 1;
            msg.obj = e;
            msg.sendToTarget();
        }

        public void onStart() {
            if (isSetLoading) {
                loadingBar.show();
            }
        }

        public void onFinish() {
            if (isSetLoading) {
                loadingBar.cancel();
            }
        }
        
        public abstract void onSuccess(Call call, Response response);

        public abstract void onFailer(Exception e);
    }

    private static class ResultHandler extends Handler {
        SimpleResponseHandler responseHandler;

        ResultHandler(SimpleResponseHandler handler, Looper looper) {
            super(looper);
            this.responseHandler = handler;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            responseHandler.handleMessage(msg);
        }
    }
}
