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
import com.example.mapper.UserMapper;
import com.example.model.Goods;
import com.example.model.Message;
import com.example.util.LuckUtil;
import com.example.util.MyUtil;
import com.sobte.cqp.jcq.entity.Member;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import static com.example.Demo.sendGroupMsg;
import static com.example.Variable.currentGoods;
import static com.example.util.LuckUtil.*;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

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
    @CommandMapping(value = {"赠送符卡*"},menu = {"fk"},order = 2)
    @Transactional
    @Times(limit = 2,interval = 1000)
    public Object zsfk(Message message,Long qq2,String itemName){

        User user = message.getUser();

        Item item = itemMapper.selectOne(new QueryWrapper<Item>().eq("name", itemName));

        if (item == null){
            sendGroupMsg("符卡不存在");
            return -1;
        }



        int result = userItemMapper.delete(new QueryWrapper<UserItem>()
                .eq("item_id", item.getId()).eq("qq", user.getQq()).last("limit 1"));

        if (result < 1){
            sendGroupMsg("你没有这张符卡");
            return -1;
        }

        userItemMapper.insert(new UserItem().setQq(qq2).setItemId(item.getId()).setItemName(item.getName()));

        user.setHonor(user.getHonor() + item.getLevelNum() * 1);
        friendMapper.setVal(user.getQq(),qq2,item.getLevelNum() * 2);

        return MessageFormat.format("{0}赠送了{1}符卡：{2}【{3}】！\n你们的关系提升了\n你获得荣誉",
                user.getName(),MyUtil.getCardName(getGroupMemberInfo(qq2)),item.getName(),item.getLevel());
    }

    @CommandMapping(value = {"献祭*"},menu = {"fk"},tili = -50,notes = "献祭符卡进行占星，消耗50体力和一定金币")
    @Transactional
    public Object xj(Message message,String itemName){

        User user = message.getUser();

        Item item = itemMapper.selectOne(new QueryWrapper<Item>().eq("name", itemName));

        if (item == null){
            sendGroupMsg("符卡不存在");
            return -1;
        }

        int value = userItemMapper.selectMyValue(user.getQq());

        value = value / 2 + 100;

        if (value > user.getMoney()){
            sendGroupMsg("你需要" + value + "金币才能进行献祭");
            return -1;
        }

        int result = userItemMapper.delete(new QueryWrapper<UserItem>()
                .eq("item_id", item.getId()).eq("qq", user.getQq()).last("limit 1"));

        if (result < 1){
            sendGroupMsg("你没有这张符卡");
            return -1;
        }

        user.setMoney(user.getMoney() - value);

        user.setHonor(user.getHonor() - item.getLevelNum() * 1);

        sendGroupMsg(MessageFormat.format("{0}消耗{1}金币献祭了{2}【{3}】！\n你失去荣誉",
                user.getName(),value,item.getName(),item.getLevel()));



        return new ModelAndView("zx",(Map)zx0(message,10 + item.getLevelNum() * 5));
    }

    @CommandMapping(value = {"符卡列表"},menu = {"fk"})
    @Times(interval = 3600,limit = 1)
    public Object fklb(Message message,String itemName){

        List<Item> list = itemMapper.selectList(new QueryWrapper<Item>().orderByDesc("value").last("limit 30"));

        Map map = new HashMap();
        map.put("list",list);
        return map;
    }

    @CommandMapping(value = {"符卡改名"},menu = {"fk"})
    @Times
    public Object fklb(Message message,String itemName,String itemName2){

        if (StringUtils.isEmpty(itemName) || StringUtils.isEmpty(itemName2)){
            return -1;
        }

        if ((itemName2.contains("[CQ") || itemName2.length() > 32) && !message.getUser().getQq().equals(new Long("2676056197"))){
            sendGroupMsg("名称不合法");
            return -1;
        }


        Item item = itemMapper.selectOne(new QueryWrapper<Item>().eq("name", itemName));

        if (item == null){
            sendGroupMsg("名称错误");
            return -1;
        }

        if (! item.getQq().equals(message.getUser().getQq())){
            sendGroupMsg("只能改名自己召唤的符卡");
            return -1;
        }
        try{
            itemMapper.updateById(item.setName(itemName2));
        }catch (Exception e){
            sendGroupMsg("名称重复");
            return -1;
        }

        return "修改成功";
    }

    @CommandMapping(value = {"金币占星"},menu = {"fk"},notes = "2700金币抽符卡")
    public Object jbzx(Message message){

        User user = message.getUser();
        Integer sumCount = itemMapper.selectCount(new QueryWrapper<Item>()
                .eq("qq", user.getQq())
        );

        if (sumCount < 1){
            sendGroupMsg("至少召唤一张符卡才能占星！");
            return -1;
        }

        if (user.getMoney() < 2700){
            sendGroupMsg("需要2700金币！");
            return -1;
        }

        user.setMoney(user.getMoney() - 2700);
        return new ModelAndView("zx",(Map)zx0(message,10));
    }
    @CommandMapping(value = {"占星"},menu = {"fk"},notes = "免费抽符卡")
    @Times(interval = 3600 * 20)
    public Object zx(Message message){

        Integer sumCount = itemMapper.selectCount(new QueryWrapper<Item>()
                .eq("qq", message.getUser().getQq())
        );
        if (sumCount < 1){
            sendGroupMsg("至少召唤一张符卡才能占星！");
            return -1;
        }

        return zx0(message,10);

    }
    private Object zx0(Message message,int count){
        User user = message.getUser();

        List<String> list = new ArrayList<>();

        long money = 1;

        for (int i = 0; i < count ; i++) {
            if (trueOrFalse(4.61)){
                Item item = itemMapper.selectOne(new QueryWrapper<Item>().select("*", "rand() rdm").orderByAsc("rdm").last("limit 1"));
                if (item == null){
                    return -1;
                }
                userItemMapper.insert(new UserItem().setItemId(item.getId()).setItemName(item.getName()).setQq(user.getQq()));
                list.add(item.getName() + "【" +item.getLevel()+ "】");
            }else if (trueOrFalse(46.1)){
                if (trueOrFalse(10)){
                    money += randInt(200,500);
                }
                money += randInt(1,30);
            }
        }

        user.setMoney(user.getMoney() + money);
        list.add("金币" + money);


        Map map = new HashMap();
        map.put("list",list);
        map.put("user",user);
        return map;
    }

    @CommandMapping(value = {"实力排名*","实力排行*"},menu = {"cd"})
    public Object slpm(Message message,Integer limit){

        if (limit == null || limit > 10){
            limit = 10;
        }

        User user = message.getUser();

        List<Item> list = userItemMapper.selectAllValue();

        list = list.stream().sorted(Comparator.comparing(Item::getValue).reversed()).limit(limit).collect(Collectors.toList());

        list.forEach(item -> {
            Member info = getGroupMemberInfo(item.getQq());
            item.setName(MyUtil.getCardName(info));
        });

        Map map = new HashMap();
        map.put("list",list);
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
            return -1;
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
            return -1;
        }

        if ((itemName.contains("[CQ")|| itemName.length() > 32) && !message.getUser().getQq().equals(new Long("2676056197"))){
            sendGroupMsg("名称不合法");
            return -1;
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

        if (trueOrFalse(4)){
            userItemMapper.insert(new UserItem().setItemId(item.getId()).setItemName(itemName).setQq(user.getQq()));
            sendGroupMsg("召唤者" + user.getName() + "被" + itemName + "选中了！" );
        }

        return user.getName() + "召唤了" + itemName + "【"+ item.getLevel() +"】";
    }

    private Boolean lock = false;

    @Resource
    private UserMapper userMapper;

    @CommandMapping(value = "拍卖*",menu = "fk")
    public Object pm(Message message,String itemName,Long value){

        if (StringUtils.isEmpty(itemName) || value == null){
            return -1;
        }

        User user = message.getUser();

        synchronized (lock){

            if (currentGoods != null){
                UserItem userItem = currentGoods.getUserItem();
                return "当前正在拍卖" + userItem.getItemName() + "【"+ userItem.getLevel() +"】";
            }

            if (lock){
                return "需要休息一会才能开始下一场拍卖";
            }


            Goods goods = new Goods();


            Item item = itemMapper.selectOne(new QueryWrapper<Item>().eq("name", itemName));

            if (item == null){
                return "符卡不存在";
            }

            UserItem userItem = userItemMapper.selectOne(new QueryWrapper<UserItem>()
                    .eq("item_id",item.getId())
                    .eq("qq",user.getQq())
                    .last("limit 1")
            );

            if (userItem == null){
                return "你没有这张符卡";
            }


            userItem.setItemName(item.getName());
            userItem.setLevel(item.getLevel());
            goods.setUserItem(userItem);
            goods.setPrice(value);
            goods.setLastPrice(value - 1);
            currentGoods = goods;
            lock = true;

            sendGroupMsg(user.getName() + "开始拍卖" + userItem.getItemName() + "【"+ userItem.getLevel() +"】了\n起价" + value + "\n发送出价 + 价格参与拍卖");

        }

        MyUtil.async(() -> {

            MyUtil.sleep(90 * 1000);
            if (currentGoods == null){
                lock = false;
                return;
            }
            UserItem userItem = currentGoods.getUserItem();
            Long price = currentGoods.getLastPrice();
            Long lastQQ = currentGoods.getLastQQ();

            if (lastQQ == null){
                sendGroupMsg(userItem.getItemName() + "【"+ userItem.getLevel() +"】流拍");
                currentGoods = null;
                lock = false;
                return;
            }

            userMapper.changeMoney(price,userItem.getQq());
            userMapper.changeMoney(-price,lastQQ);

            userItem.setQq(lastQQ);
            userItemMapper.updateById(userItem);


            Member info = getGroupMemberInfo(lastQQ);

            sendGroupMsg(MyUtil.getCardName(info) + "以" + price + "金币拍下了" + userItem.getItemName() + "【"+ userItem.getLevel() +"】");

            currentGoods = null;
            lock = false;
        });


        return -1;
    }

    @CommandMapping(value = "出价*")
    public Object cj(Message message,Long value){

        User user = message.getUser();


        if (value == null){
            return -1;
        }


        synchronized (lock){

            if (currentGoods == null){
                return "当前没有正在拍卖的符卡";
            }
            UserItem userItem = currentGoods.getUserItem();

            if (userItem.getQq().equals(user.getQq())){
                return "你不能出价自己拍卖的符卡";
            }

            if (value <= currentGoods.getLastPrice()){
                return "出价必须大于" + currentGoods.getLastPrice();
            }

            if (value > user.getMoney()){
                return "余额不足";
            }

            if (user.getQq().equals(currentGoods.getLastQQ())){
                LocalDateTime now = LocalDateTime.now();
                if (now.minusSeconds(10).isBefore(currentGoods.getLastTime())){
                    return "十秒内不能连续出价";
                }else {
                    userMapper.changeMoney(value,userItem.getQq());
                    userMapper.changeMoney(-value,user.getQq());

                    userItem.setQq(user.getQq());
                    userItemMapper.updateById(userItem);


                    Member info = getGroupMemberInfo(user.getQq());

                    currentGoods = null;

                    return MyUtil.getCardName(info) + "以" + value + "金币拍下了" + userItem.getItemName() + "【"+ userItem.getLevel() +"】";
                }

            }
            currentGoods.setLastPrice(value).setLastTime(LocalDateTime.now()).setLastQQ(user.getQq());
            return user.getName() + "出价" + value;
        }

    }

    @CommandMapping("重置符卡数据")
    @Transactional
    public Object czfksj(Message message){

        User user = message.getUser();
        if (!new Long("471129493").equals(user.getQq())){
            return -1;
        }

        List<Item> items = userItemMapper.selectAllValue();

        items.forEach(item -> {

            userMapper.changeMoney(item.getValue() * 3,item.getQq());

        });

        userItemMapper.delete(null);
        itemMapper.delete(null);

        return "符卡数据已经全部清空，之前拥有符卡已经兑换成金币";
    }

    private Long rdmValue(){
        long value = 0;
        //
        if (trueOrFalse(3)){
            value = randInt(100,1500);
        }else if (trueOrFalse(14)){
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
