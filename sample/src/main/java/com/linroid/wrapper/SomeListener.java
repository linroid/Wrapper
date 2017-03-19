package com.linroid.wrapper;

import android.support.annotation.UiThread;
import android.view.View;

import com.linroid.wrapper.annotations.WrapperClass;
import com.linroid.wrapper.annotations.WrapperMultiple;

/**
 * @author linroid <linroid@gmail.com>
 * @since 10/03/2017
 */
@WrapperClass
@WrapperMultiple
public interface SomeListener {
    @UiThread
    void onFoo(View view);

    boolean onUserLeave();
}
