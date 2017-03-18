package com.linroid.wrapper;

import android.support.annotation.UiThread;
import android.view.MenuItem;
import android.view.View;

import com.linroid.wrapper.annotations.WrapperGenerator;
import com.linroid.wrapper.annotations.WrapperMultiple;

/**
 * @author linroid <linroid@gmail.com>
 * @since 16/03/2017
 */
@WrapperGenerator(
        values = {
                View.OnClickListener.class,
                View.OnLongClickListener.class,
                MenuItem.OnMenuItemClickListener.class,
                View.OnScrollChangeListener.class
        }
)
@UiThread
@WrapperMultiple
public class SomeGenerator {
}
