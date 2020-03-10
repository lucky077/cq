package com.example.service;


import com.example.annotation.CommandMapping;
import com.example.entity.User;
import com.example.entity.UserItem;
import com.example.mapper.UserItemMapper;
import com.example.mapper.UserMapper;
import com.example.model.Replay;
import com.example.util.LuckUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import static com.example.util.LuckUtil.*;

@CommandMapping
public class PKService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserItemMapper userItemMapper;


    public boolean pk(User usera,User userb){

        Integer useraVal = userItemMapper.selectMyValue(usera.getQq());
        Integer userbVal = userItemMapper.selectMyValue(userb.getQq());

        if (useraVal == null){
            useraVal = 0;
        }

        if (userbVal == null){
            userbVal = 0;
        }

        if (useraVal == 0 && userbVal == 0){
            return LuckUtil.trueOrFalse(50);
        }


        double win = (double)useraVal / (double)(userbVal + useraVal) * 100.0;

        System.out.println(win);


        return LuckUtil.trueOrFalse(win);

    }



}
