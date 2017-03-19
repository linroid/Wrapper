package com.linroid.wrapper;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        SomeListenerWrapper wrapper = new SomeListenerWrapper(listener);
////        SomeListener wrapper = new SomeListenerWrapper(handler, listener); // 自己指定一个 Handler
//        wrapper.setWrapper(listener); // 设置你实现的 listener
//        wrapper.onFoo(view); // 调用方法
//
//        SomeListenerMultiWrapper multiWrapper = new SomeListenerMultiWrapper();
////        SomeListenerMultiWrapper multiWrapper = new SomeListenerMultiWrapper(handler); // 自己指定一个 Handler
//        multiWrapper.addWrapper(listener); // 添加 listener
//        multiWrapper.onFoo(view); // 调用方法
    }
}
