package com.example.handler;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.example.handler.handler.TestHandler;

public class MainActivity extends AppCompatActivity {

    TestHandler mTestHandler = new TestHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //mTestHandler.testThreadHandler();
        mTestHandler.testMainThreadHandler();

    }

    public void threadHandler(View view){

        mTestHandler.handler.sendEmptyMessage(1);
    }

    public void mainThreadHandler(View view){

        mTestHandler.handler.sendEmptyMessage(1);
    }




}
