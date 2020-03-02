package com.example.service;


import com.example.annotation.CommandMapping;
import com.example.entity.User;
import com.example.mapper.UserMapper;
import com.example.model.Replay;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import static com.example.util.LuckUtil.*;

@CommandMapping
public class PKService {

    @Resource
    private UserMapper userMapper;


    public boolean pk(User usera,User user1a){


       return true;

    }



}
