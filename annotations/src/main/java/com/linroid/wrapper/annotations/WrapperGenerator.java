package com.linroid.wrapper.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * 可以处理多个类，或者用来处理已经存在无法修改的类/接口(比如 Android SDK 中的)
 * @author linroid
 * @since 16/03/2017
 */
@Documented
@Retention(CLASS)
@Target({TYPE})
public @interface WrapperGenerator {
    /**
     * 需要被处理的类
     */
    Class<?>[] values() default {};
}
