package com.example.handler.handler;

/**
 * @author Liupengfei
 * @describe 自定义轮循器
 * @date on 2019/8/8 16:31
 */
public class MyLooper {
    private static final ThreadLocal<MyLooper> THREAD_LOCAL = new ThreadLocal<>();
    IMyMessageQueue mMessageQueue;
    private static MyLooper mMainLooper;

    public MyLooper() {
        mMessageQueue = new MyBlockingQueue(4);
    }

    public static void prepare() {
        if (null != THREAD_LOCAL.get()) {
            throw new RuntimeException("Only one looper can be created per thread.");
        }
        THREAD_LOCAL.set(new MyLooper());
    }

    public static void prepareMainLooper() {
        prepare();
        synchronized (MyLooper.class) {
            if (null != mMainLooper) {
                throw new RuntimeException("MainLooper has already been prepared.");
            }
            mMainLooper = myLooper();
        }
    }

    public static void loop() {
        final MyLooper looper = myLooper();
        if (null == looper) {
            throw new RuntimeException("No looper! MyLooper.prepare() wasn't called on this thread.");
        }
        for (; ; ) {
            MyMessage msg = null;
            try {
                msg = looper.mMessageQueue.next();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (null != msg) {
                msg.target.handleMessage(msg);
            }
        }
    }

    public static MyLooper getMainLooper() {
        return mMainLooper;
    }

    public static MyLooper myLooper() {
        return THREAD_LOCAL.get();
    }
}
