package com.example.annotation;

/**
 * @author bytedance
 */
public @interface Man {
    String name() default  "";
    int age() default  0;
    Class<? extends ICountry> coutry();
}
