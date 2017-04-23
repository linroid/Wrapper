package com.linroid.wrapper;

/**
 * @author linroid <linroid@gmail.com>
 * @since 22/03/2017
 */
public class SomeProducer {
    private SomeListenerWrapper wrapper = new SomeListenerWrapper(null);

    public void setListener(SomeListener listener) {
        wrapper.setWrapper(listener);
    }

}
