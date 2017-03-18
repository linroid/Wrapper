package com.linroid.wrapper;

import android.support.annotation.UiThread;

import com.linroid.wrapper.annotations.WrapperMultiple;
import com.linroid.wrapper.annotations.WrapperClass;

/**
 * @author linroid <linroid@gmail.com>
 * @since 16/03/2017
 */
@WrapperClass
@WrapperMultiple
public class SomeClass {
    @UiThread
    void play() {
        System.out.println("test");
    }

    void pause() {

    }
}
