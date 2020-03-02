package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.Demo;
import com.example.Variable;
import com.example.entity.Item;
import com.example.entity.User;
import com.example.mapper.ItemMapper;
import com.example.mapper.UserMapper;
import com.example.util.GoogleApi;
import com.example.util.H;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import static com.example.Variable.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class MyController {


    @Autowired
    private StringRedisTemplate redisTemplate;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ItemMapper itemMapper;


    @RequestMapping("push/{qq}")
    @ResponseBody
    public Object info(@PathVariable("qq")Long qq,@RequestParam String msg){

        return Demo.CQ.sendPrivateMsg(qq,msg);
    }


    @GetMapping("info")
    public Object info(@RequestParam String id, Map data){

        User user = userMapper.selectById0(Long.valueOf(id));
//        if (user == null){
//            return "error";
//        }
//        List<Integer> itemUsed0 = user.getItemUsed0();
//        Map<Integer, String> idNameMap = getIdNameMap(itemUsed0);
//
//        List<String> itemNames = new ArrayList<>(itemUsed0.size());
//        for (Integer itemId : itemUsed0) {
//           itemNames.add(idNameMap.get(itemId));
//        }
//
//        data.put("user",user);
//        data.put("items",itemNames);
//        data.put("headImg","http://q1.qlogo.cn/g?b=qq&nk=" + id + "&s=640");


        return "info";
    }

    @GetMapping("item/list")
    public Object itemList(@RequestParam Integer sale, Map data){
        if (sale == null){
            sale = 1;
        }

        List<Item> items = itemMapper.selectList(new QueryWrapper<Item>().eq("sale", sale));


        data.put("list",items);
        data.put("sale",sale);


        return "item";
    }


    @GetMapping("replay/{id}")
    @ResponseBody
    public Object replay(@PathVariable("id") String id){
        String s = redisTemplate.opsForValue().get(id);
        if (s == null){
            s = "该记录已失效";
        }
        return s;
    }

    @GetMapping("ts/{w}")
    @ResponseBody
    public Object test(@PathVariable("w") String w,@Nullable Long id,@Nullable Double s){
        GoogleApi.ttsspeed = s.toString();
        byte[] translate = GoogleApi.translate(w);
        String path = rootPath +"/data/record";
        String fileName = H.writeFile(path, translate, ".amr");
        String record = Demo.CC.record(fileName,false);
        if (id == null){
            id = 492155062l;
        }
        System.out.println(record);
        Demo.CQ.sendGroupMsg(id,record);
        new File(path + "/" +fileName).delete();
        return translate.length;
    }

    private Map<Integer,String> getIdNameMap(List<Integer> itemUsed0){
        Map<Integer,String> map = new HashMap<>();
        List<Item> items = itemMapper.selectList(new QueryWrapper<Item>().in(itemUsed0.size() > 0,"id",itemUsed0));

        items.forEach(item -> {
            map.put(item.getId(),item.getName());
        });
        return map;
    }

   // @PostConstruct
    public void test(){

    }

}
