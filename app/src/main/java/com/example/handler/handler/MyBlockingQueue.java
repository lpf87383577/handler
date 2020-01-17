package com.example.handler.handler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author Liupengfei
 * @describe 消息队列实例
 * @date on 2019/8/8 16:32
 */
public class MyBlockingQueue implements IMyMessageQueue {
    private BlockingQueue<MyMessage> mQueue;  //消息队列

    public MyBlockingQueue(int init) {
        this.mQueue = new LinkedBlockingDeque<>(init);
    }

    @Override
    public MyMessage next() throws InterruptedException {
        return mQueue.take();
    }

    @Override
    public void enqueueMsg(MyMessage msg) throws InterruptedException {
        mQueue.put(msg);
    }

}
