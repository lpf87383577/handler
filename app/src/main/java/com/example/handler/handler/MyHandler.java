package com.example.handler.handler;

/**
 * @author Liupengfei
 * @describe 自定义handler
 * @date on 2019/8/8 16:29
 */
public abstract class MyHandler {
    private IMyMessageQueue mQueue;

    public MyHandler(MyLooper looper) {
        mQueue = looper.mMessageQueue;
    }

    public MyHandler() {
        MyLooper.myLooper();
    }

    public void sendMessage(MyMessage msg) {
        msg.target = this;
        try {
            mQueue.enqueueMsg(msg);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public abstract void handleMessage(MyMessage msg);


}
