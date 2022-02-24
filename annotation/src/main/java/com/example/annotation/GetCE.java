package com.example.annotation;

public @interface GetCE {
    Class<? extends IAlgorithm> algorithm() default IAlgorithm.class;
}
