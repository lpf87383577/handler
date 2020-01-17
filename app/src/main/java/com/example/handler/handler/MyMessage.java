package com.example.handler.handler;

/**
 * @author Liupengfei
 * @describe 消息类
 * @date on 2019/8/8 16:25
 */
public class MyMessage {

    private int code;
    private String msg;
    MyHandler target;

    public MyMessage(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

}
