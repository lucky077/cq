package com.example;


import com.example.annotation.CommandMapping;
import com.example.entity.User;

import com.example.model.Goods;
import com.example.pojo.LastMsgCount;
import com.example.pojo.MethodInvoker;
import org.springframework.core.annotation.AnnotationUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

//全局变量，对象本身应该是final的，没写
public class Variable {


    public static Goods currentGoods = null;


    //消息来源群号
    public static ThreadLocal<Long> fromGroupThreadLocal = new ThreadLocal<>();
    //允许接收消息的群（已弃用）
    public static Set<Long> allowedGroup = new HashSet<>();
    //本地路径
    public static String rootPath = "c:/file";
    //发送图片 临时缓存路径
    public static String imageCachePath = rootPath +"/data/image/cache";
    //需要定时清理的缓存文件集合
    public static List<File> deleteFiles = new CopyOnWriteArrayList<>();
    //连续发同样消息的记录
    public static Map<Long,LastMsgCount> lastMsgCountMap = new ConcurrentHashMap(256);
    //等值匹配指令映射
    public static Map<String, MethodInvoker> commandMapping = new LinkedHashMap<>();
    //模糊匹配指令映射
    public static Map<MethodInvoker,String> commandMappingByLike = new TreeMap<>((o1,o2) -> {
        int order1 = AnnotationUtils.findAnnotation(o1.getMethod(), CommandMapping.class).order();
        int order2 = AnnotationUtils.findAnnotation(o2.getMethod(), CommandMapping.class).order();
        return order1 - order2 > 0 ? -1 : 1;
    });


}
