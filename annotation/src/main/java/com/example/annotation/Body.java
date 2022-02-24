package com.example.annotation;

public @interface Body {
    int weight() default  -1;
    int height() default  -1;
}
