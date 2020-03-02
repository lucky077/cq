package com.example.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface Times {

    int limit() default 1;
    long interval() default 3600 * 23;
    String tip() default "超出次数限制啦[CQ:face,id=100]";

}
