package com.example.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface CommandMapping {

    String[] value() default "";
    String notes() default "";
    String[] menu() default "";
    int order() default -1;
    int tili() default 0 ;

}
