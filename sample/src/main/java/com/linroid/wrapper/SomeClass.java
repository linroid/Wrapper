package com.linroid.wrapper;

import android.support.annotation.UiThread;

import com.linroid.wrapper.annotations.Multiple;
import com.linroid.wrapper.annotations.WrapperClass;

/**
 * @author linroid <linroid@gmail.com>
 * @since 16/03/2017
 */
@WrapperClass
@Multiple
public class SomeClass {
    @UiThread
    void play() {
        System.out.println("test");
    }

    void pause() {

    }
}
