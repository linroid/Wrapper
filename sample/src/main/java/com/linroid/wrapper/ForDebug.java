package com.linroid.wrapper;

import com.linroid.wrapper.annotations.WrapperClass;

/**
 * @author linroid <linroid@gmail.com>
 * @since 18/03/2017
 */
@WrapperClass
public class ForDebug extends ForDebugParent {

    private void privateMethod() {

    }

    protected void protectedMethod() {

    }

    public void publicMethod() {

    }

    @Override
    protected void protectedParentMethod() {
        super.protectedParentMethod();
    }
}
