package com.example.config;

import com.example.Demo;
import com.example.Variable;
import com.example.entity.Friend;
import com.example.entity.Item;
import com.example.entity.User;
import com.example.mapper.FriendMapper;
import com.example.mapper.UserMapper;
import com.example.service.BankService;
import com.example.service.ItemService;
import com.example.util.LuckUtil;
import com.example.util.MyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PostMapping;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
public class ScheduConfig {

    @Resource
    private UserMapper userMapper;
    @Resource
    private FriendMapper friendMapper;
    @Resource
    private BankService bankService;
    @Resource
    private ItemService itemService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Scheduled(cron = "0 0 0 * * ?")
    public void guanxi(){

        int i = friendMapper.guanxiUp(2, -89, -60);
        int i1 = friendMapper.guanxiUp(5, -59, -10);
        int i2 = friendMapper.guanxiUp(1, -9, -1);
        System.out.println(i + i1 + i2 + "个关系提升了");
        Demo.sendGroupMsg(i + i1 + i2 + "个仇人间关系缓和了");

        String key = "bankTime";

        String timeStr = stringRedisTemplate.opsForValue().get(key);

        if (timeStr == null){
            bank0();
            itemService.flushShop();
            MyUtil.async(this::bank);
        }

    }

    public void bank(){

          MyUtil.sleep(5000L);
          String key = "bankTime";

          String timeStr = stringRedisTemplate.opsForValue().get(key);

          Long time;

          if (timeStr == null){
              time = new Date().getTime() + LuckUtil.randInt(2,14) * 3600 * 1000L;
              stringRedisTemplate.opsForValue().set(key,time.toString());
          }else {
              time = Long.valueOf(timeStr);
          }
          Timer timer = new Timer();
          timer.schedule(new TimerTask() {
              @Override
              public void run() {
                  bank0();
                  itemService.flushShop();
                  stringRedisTemplate.opsForValue().set(key,new Date().getTime() + LuckUtil.randInt(16,32) * 3600 * 1000L + "");
                  bank();
              }
          },new Date(time));
    }

    private void bank0(){
        userMapper.changeBankScore();
        Demo.sendGroupMsg("银行信用已更新");
        userMapper.changeBankMoney();
        Demo.sendGroupMsg("银行利息已结算");
        userMapper.changeBankOverdue();

        List<User> overdueUserList = userMapper.getBankOverdue();
        bankService.qingsuan(overdueUserList);
    }

    @Scheduled(cron = "0 0/30 * * * ?")
    public void tili(){


        userMapper.changeTili();
        userMapper.changeTili2();
        System.out.println("体力恢复成功！"+new Date().toString());
        Demo.sendGroupMsg("体力恢复成功！"+new Date().toString());
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
