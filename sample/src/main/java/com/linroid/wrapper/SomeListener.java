package com.linroid.wrapper;

import android.content.pm.ApplicationInfo;
import android.support.annotation.UiThread;
import android.view.View;

import com.linroid.wrapper.annotations.Multiple;
import com.linroid.wrapper.annotations.WrapperClass;

/**
 * @author linroid <linroid@gmail.com>
 * @since 10/03/2017
 */
@WrapperClass
@Multiple
public interface SomeListener {
    @UiThread
    void onClick(View view);

    boolean onLongClick(View view, ApplicationInfo info);
}
