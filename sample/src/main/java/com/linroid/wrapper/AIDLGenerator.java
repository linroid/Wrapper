package com.linroid.wrapper;

import android.support.annotation.UiThread;

import com.linroid.wrapper.annotations.WrapperGenerator;
import com.linroid.wrapper.annotations.WrapperMultiple;

/**
 * @author linroid <linroid@gmail.com>
 * @since 18/03/2017
 */
@WrapperGenerator(
        values = {
                ISomeServiceInterface.class
        }
)
@UiThread
@WrapperMultiple
public class AIDLGenerator {
}
