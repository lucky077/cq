package com.example.pojo;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

@Data
public class LastMsgCount {

    public volatile String msg;
    public AtomicInteger count;

    public LastMsgCount(String msg){
        this.msg = msg;
        count = new AtomicInteger(1);
    }

}
