package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.Demo;
import com.example.annotation.CommandMapping;
import com.example.annotation.Times;
import com.example.entity.Item;
import com.example.entity.User;
import com.example.entity.UserItem;
import com.example.mapper.FriendMapper;
import com.example.mapper.ItemMapper;
import com.example.mapper.UserItemMapper;
import com.example.model.Message;
import com.example.util.LuckUtil;
import com.example.util.MyUtil;
import com.sobte.cqp.jcq.entity.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.ModelAndView;

import static com.example.util.LuckUtil.*;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.Demo.*;

@CommandMapping
public class ItemService {

    @Resource
    private ItemMapper itemMapper;
    @Resource
    private UserItemMapper userItemMapper;
    @Resource
    private FriendMapper friendMapper;


    @CommandMapping(value = {"符卡"},menu = {"cd"},order = 1)
    public Object fk(Message message){
        String fk = MyUtil.getChildMenu("fk");
        return fk;
    }
    @CommandMapping(value = {"赠送符卡"},menu = {"fk"},order = 1)
    @Transactional
    @Times(limit = 2,interval = 1000)
    public Object zsfk(Message message,Long qq2,String itemName){

        User user = message.getUser();

        Item item = itemMapper.selectOne(new QueryWrapper<Item>().eq("name", itemName));

        if (item == null){
            return "符卡不存在";
        }

        int result = userItemMapper.delete(new QueryWrapper<UserItem>()
                .eq("item_id", item.getId()).eq("qq", user.getQq()));

        if (result < 1){
            return "你没有这张符卡";
        }

        userItemMapper.insert(new UserItem().setQq(qq2).setItemId(item.getId()).setItemName(item.getName()));

        user.setHonor(user.getHonor() + item.getLevelNum() * 1);
        friendMapper.setVal(user.getQq(),qq2,item.getLevelNum() * 2);

        return "成功";
    }

    @CommandMapping(value = {"符卡列表"},menu = {"fk"},tili = -60)
    public Object fklb(Message message,String itemName){

        List<Item> list = itemMapper.selectList(new QueryWrapper<Item>().orderByDesc("value").last("limit 50"));

        Map map = new HashMap();
        map.put("list",list);
        return map;
    }

    @CommandMapping(value = {"符卡改名"},menu = {"fk"},tili = -30)
    public Object fklb(Message message,String itemName,String itemName2){

        if (StringUtils.isEmpty(itemName) || StringUtils.isEmpty(itemName2)){
            return null;
        }

        Item item = itemMapper.selectOne(new QueryWrapper<Item>().eq("name", itemName));

        if (item == null){
            return "名称错误";
        }

        if (! item.getQq().equals(message.getUser().getQq())){
            return "只能改名自己召唤的符卡";
        }

        itemMapper.updateById(new Item().setId(item.getId()).setName(itemName2));

        return "修改成功";
    }


    @CommandMapping(value = {"占星"},menu = {"fk"},notes = "抽符卡")
    @Times(interval = 3600 * 20)
    public Object zx(Message message){

        User user = message.getUser();

        List<String> list = new ArrayList<>();

        long money = 1;

        for (int i = 0; i < 10 ; i++) {
            if (trueOrFalse(4.61)){
                Item item = itemMapper.selectOne(new QueryWrapper<Item>().select("*", "rand() rdm").orderByAsc("rdm").last("limit 1"));
                userItemMapper.insert(new UserItem().setItemId(item.getId()).setItemName(item.getName()).setQq(user.getQq()));
                list.add(item.getName() + "【" +item.getLevel()+ "】");
            }else if (trueOrFalse(46.1)){
                if (trueOrFalse(10)){
                    money += randInt(200,400);
                }
                money += randInt(1,20);
            }
        }

        user.setMoney(user.getMoney() + money);
        list.add("金币" + money);


        Map map = new HashMap();
        map.put("list",list);
        map.put("user",user);
        return map;
    }

    @CommandMapping(value = {"我召唤的符卡"},menu = {"fk"})
    public Object wzhdfk(Message message){

        User user = message.getUser();

        List<Item> list = itemMapper.selectList(new QueryWrapper<Item>()
                .eq("qq",user.getQq())
                .orderByDesc("value").last("limit 50"));

        Map map = new HashMap();
        map.put("list",list);
        return new ModelAndView("fklb",map);
    }

    @CommandMapping(value = {"查看符卡*"},menu = {"fk"},order = 1)
    public Object ckfk(Message message,Long qq2){

        if (qq2 == null){
            qq2 = message.getFromQQ();
        }

        Member info = getGroupMemberInfo(qq2);

        if (info == null){
            return null;
        }

        List<Map> list = userItemMapper.selectList(qq2);

        Map map = new HashMap();
        map.put("list",list);
        map.put("userName",MyUtil.getCardName(info));
        return map;
    }

    @CommandMapping(value = {".召唤*"},menu = {"fk"},notes = "召唤新符卡到池中")
    public Object zh(Message message,String itemName){

        if (StringUtils.isEmpty(itemName)){
            return null;
        }

        User user = message.getUser();

        Integer sumCount = itemMapper.selectCount(new QueryWrapper<Item>()
                .eq("qq", user.getQq())
                );

        if (sumCount > 4){
            return "最多只能召唤五张符卡";
        }


        Integer count = itemMapper.selectCount(new QueryWrapper<Item>()
                .eq("qq", user.getQq())
                .apply("DATE_FORMAT(create_date,'%Y-%m-%d') = CURDATE()"));

        if (count > 0){
            return "每天仅能召唤一次";
        }
        Long value = rdmValue();
        Integer levelNum = getLevelNum(value);
        Item item = new Item().setQq(user.getQq()).setName(itemName).setValue(value).setLevel(levelNumMap.get(levelNum)).setLevelNum(levelNum);

        try{
            itemMapper.insert(item);
        }catch (Exception e){
            return "名称重复";
        }

        if (trueOrFalse(3.0)){
            userItemMapper.insert(new UserItem().setItemId(item.getId()).setItemName(itemName).setQq(user.getQq()));
            sendGroupMsg("召唤者" + user.getName() + "被" + itemName + "选中了！" );
        }

        return user.getName() + "召唤了" + itemName + "【"+ item.getLevel() +"】";
    }


    private Long rdmValue(){
        long value = 0;
        //
        if (trueOrFalse(3)){
            value = randInt(100,1500);
        }else if (trueOrFalse(15)){
            value = randInt(50,500);
        }else {
            value = randInt(1,100);
        }


        return value;
    }

    Map<Integer,String> levelNumMap = new HashMap<Integer,String> (){{
        put(1,"N");
        put(2,"R");
        put(3,"SR");
        put(4,"SSR");
        put(5,"UR");
    }};

    private Integer getLevelNum(long value){

        Integer level = 1;

        if (value >= 70 && value < 150){
            level = 2;
        }else if(value >= 150 && value < 400){
            level = 3;
        }else if(value >= 400 && value < 800){
            level = 4;
        }else if(value >= 800){
            level = 5;
        }

        return level;
    }




}
