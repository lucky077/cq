package com.example;


import com.alibaba.fastjson.JSON;
import com.example.entity.Item;
import com.example.mapper.ItemMapper;
import com.example.mapper.UserMapper;
import com.example.util.LuckUtil;
import org.apache.tomcat.jni.Local;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@SpringBootApplication
@MapperScan("com.example.mapper")
@EnableScheduling
public class SpringbootApp {



    public static void main(String[] args) throws Exception{




    }






}
