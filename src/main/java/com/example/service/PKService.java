package com.example.service;


import com.example.annotation.CommandMapping;
import com.example.entity.User;
import com.example.entity.UserItem;
import com.example.mapper.UserItemMapper;
import com.example.mapper.UserMapper;
import com.example.model.Replay;
import com.example.model.TypeValue;
import com.example.util.LuckUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;

import static com.example.util.LuckUtil.*;

//@CommandMapping
public class PKService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserItemMapper userItemMapper;

    public boolean pk(User usera,User userb){
        System.out.println("----start----");
        List<TypeValue> typeValueA = userItemMapper.selectTypeValue(usera.getQq());
        List<TypeValue> typeValueB = userItemMapper.selectTypeValue(userb.getQq());
        usera.setTypeValues(typeValueA);
        userb.setTypeValues(typeValueB);

        System.out.println("A");
        System.out.println(typeValueA);
        System.out.println("B");
        System.out.println(typeValueB);

        System.out.println("--------------");

        double win = 0.0;

        for (String type : types) {
            boolean b = pk0(TypeValue.getOne(typeValueA, type).getSumValue(), TypeValue.getOne(typeValueB, type).getSumValue());
            if (b){
                if ("幽".equals(type) || "暗".equals(type)){
                    win += 15.0;
                }else {
                    win += 14.0;
                }
            }
            System.out.println(type + ":" +  b);
        }

        boolean result = trueOrFalse(win);
        System.out.println("结果" + ":" + win);
        System.out.println("结果" + ":" + result);
        System.out.println("----end----");
        return result;

    }

    private boolean pk0(Integer a,Integer b){
        if (a == null){
            a = 0;
        }
        if (b == null){
            b = 0;
        }
        if (a == 0 && b == 0){
            return LuckUtil.trueOrFalse(50);
        }
        double win = (double)a / (double)(a + b) * 100.0;

        return LuckUtil.trueOrFalse(win);
    }

    public static Set<String> types = new LinkedHashSet<String>(){{
        add("幽");
        add("暗");
        add("灵");
        add("梦");
        add("雪");
        add("月");
        add("幻");
    }};

    public boolean pk_old(User usera,User userb){

        Integer useraVal = userItemMapper.selectMyValue(usera.getQq());
        Integer userbVal = userItemMapper.selectMyValue(userb.getQq());



        return pk0(useraVal,userbVal);

    }



}
