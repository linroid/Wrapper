package com.linroid.wrapper.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * 指定这个类需要被 Wrapper 处理
 * @author linroid
 * @since 10/03/2017
 */
@Documented
@Retention(CLASS)
@Target({TYPE})
public @interface WrapperClass {
}
