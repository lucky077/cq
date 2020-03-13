package com.example.config;

import com.example.Variable;
import com.example.entity.Friend;
import com.example.entity.User;
import com.example.mapper.FriendMapper;
import com.example.mapper.UserMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;

@Configuration
public class ScheduConfig {

    @Resource
    private UserMapper userMapper;
    @Resource
    private FriendMapper friendMapper;

    @Scheduled(cron = "0 0 0 * * ?")
    public void guanxi(){

        int i = friendMapper.guanxiUp(2, -89, -60);
        int i1 = friendMapper.guanxiUp(5, -59, -10);
        int i2 = friendMapper.guanxiUp(1, -9, -1);
        System.out.println(i + i1 + i2 + "个关系提升了");


        userMapper.changeBankMoney();
        userMapper.changeBankOverdue();

        List<User> overdueUserList = userMapper.getBankOverdue();


    }
    @Scheduled(cron = "0 0/30 * * * ?")
    public void tili(){


        userMapper.changeTili();
        userMapper.changeTili2();
        System.out.println("体力恢复成功！"+new Date().toString());
        List<File> deleteFiles = Variable.deleteFiles;
        int size = deleteFiles.size();
        int count = 0;
        for (File file : deleteFiles) {
            try{
                boolean delete = file.delete();
                if (delete){
                    count++;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            finally {
                deleteFiles.remove(file);
            }
        }
        System.out.println("总数：" + size + ",成功清理：" + count);
    }

}
