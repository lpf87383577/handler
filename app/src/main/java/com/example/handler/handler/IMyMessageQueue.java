package com.example.handler.handler;



/**
 * @author Liupengfei
 * @describe 消息队列的接口
 * @date on 2019/8/8 16:32
 */
public interface IMyMessageQueue {
    MyMessage next() throws InterruptedException;
    void enqueueMsg(MyMessage msg) throws InterruptedException;
}
