package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.Demo;
import com.example.annotation.CommandMapping;
import com.example.annotation.Times;
import com.example.entity.Item;
import com.example.entity.Shop;
import com.example.entity.User;
import com.example.entity.UserItem;
import com.example.mapper.*;
import com.example.model.Goods;
import com.example.model.Message;
import com.example.util.FtlUtil;
import com.example.util.LuckUtil;
import com.example.util.MyUtil;
import com.sobte.cqp.jcq.entity.Member;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
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
import java.lang.reflect.Array;
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
    @Resource
    private DumpMapper dumpMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;



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

        if (user.getBankOverdue() > 1){
            return  "您的符卡已被银行冻结";
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

        return MessageFormat.format("{0}赠送了{1}符卡：{2}！\n你们的关系提升了\n你获得荣誉",
                user.getName(),MyUtil.getCardName(getGroupMemberInfo(qq2)),item.toFullName());
    }

    @CommandMapping(value = {"献祭*"},menu = {"fk"},notes = "献祭符卡进行占星，消耗一定金币")
    @Transactional
    public Object xj(Message message,String itemName){

        User user = message.getUser();

        if (user.getBankOverdue() > 1){
            return  "您的符卡已被银行冻结";
        }

        int value = userItemMapper.selectMyValue(user.getQq());

        value = value / 2 + 100;

        if (value > user.getMoney()){
            sendGroupMsg("你需要" + value + "金币才能进行献祭");
            return -1;
        }

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

        user.setMoney(user.getMoney() - value);

        sendGroupMsg(MessageFormat.format("{0}消耗{1}金币献祭了{2}！",
                user.getName(),value,item.toFullName()));



        return new ModelAndView("zx",(Map)zx0(message,10 + item.getLevelNum() * 5,2));
    }

    @CommandMapping(value = {"符卡列表*"},menu = {"fk"})
    @Times(interval = 3600,limit = 1)
    public Object fklb(Message message,String type){

        boolean condition = true;

        condition = !StringUtils.isEmpty(type);

        if (condition && !PKService.types.contains(type)){
            return -1;
        }

        List<Item> list = itemMapper.selectList(new QueryWrapper<Item>().eq(condition,"type",type).orderByDesc("value").last("limit 30"));

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

        if (isNotName(itemName)){
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

    @CommandMapping(value = {"金币占星"},menu = {"fk"},notes = "5888金币抽符卡（保底一张卡）")
    public Object jbzx(Message message){

        User user = message.getUser();

        if (user.getBankOverdue() > 1){
            return  "您的金币已被银行冻结";
        }

        Integer sumCount = itemMapper.selectCount(new QueryWrapper<Item>()
                .eq("qq", user.getQq())
        );

        if (sumCount < 1){
            sendGroupMsg("至少召唤一张符卡才能占星！");
            return -1;
        }

        if (user.getMoney() < 5888){
            sendGroupMsg("需要5888金币！");
            return -1;
        }

        user.setMoney(user.getMoney() - 5888);
        return new ModelAndView("zx",(Map)zx0(message,10,1));
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

        return zx0(message,10,0);

    }
    private Object zx0(Message message,int count,int from){
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
                list.add(item.toFullName());
            }else if (trueOrFalse(46.1)){
                if (trueOrFalse(10)){
                    money += randInt(200,500);
                }
                money += randInt(1,30);
            }else if(from == 1 && trueOrFalse(1)){
                money += 30000;
            }else if(from == 0 && trueOrFalse(0.5)){
                money += 30000;
            }
        }

        if(from == 1 && list.isEmpty()){
            Item item = itemMapper.selectOne(new QueryWrapper<Item>().select("*", "rand() rdm").orderByAsc("rdm").last("limit 1"));
            if (item == null){
                return -1;
            }
            userItemMapper.insert(new UserItem().setItemId(item.getId()).setItemName(item.getName()).setQq(user.getQq()));
            list.add(item.toFullName());
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

        List<Item> list = userItemMapper.selectList(qq2);

        Map map = new HashMap();
        map.put("list",list);
        map.put("userName",MyUtil.getCardName(info));
        return map;
    }

    //@PostConstruct
    public void createDefault(){
        String[] names = {"御坂美琴","时崎狂三","白井黑子","无极剑圣","一方通行","梦梦","娜娜","伊莉雅","狂热者","探机"
                ,"泽拉图","凯瑞甘","阿塔尼斯","鸢一折纸","四糸乃","四糸奈","五河琴里","夜刀神十香","上条当麻","亚丝娜","末日使者"
                ,"萌王","蕾姆","初音未来","栗山未来","喜羊羊","五更琉璃","珂朵莉","西行寺幽幽子","芙兰朵露","蕾米莉亚","贞德"
                ,"金色之暗","伊卡洛斯","十六夜咲夜","楪祈","博丽灵梦"};


        for (String name : names) {
            Integer levelNum = rdmLevelNum();
            Long value = getValue(levelNum);
            String type = getType();
            Item item = new Item().setQq(0L).setName(name).setType(type).setValue(value).setLevel(levelNumMap.get(levelNum)).setLevelNum(levelNum);

            try{
                itemMapper.insert(item);
            }catch (Exception e){
            }
        }

    }

    @CommandMapping(value = {".召唤*"},menu = {"fk"},notes = "召唤新符卡到池中")
    public Object zh(Message message,String itemName){

        if (StringUtils.isEmpty(itemName)){
            return -1;
        }

        if (isNotName(itemName)){
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

        Integer levelNum = rdmLevelNum();
        Long value = getValue(levelNum);
        String type = getType();
        Item item = new Item().setQq(user.getQq()).setName(itemName).setType(type).setValue(value).setLevel(levelNumMap.get(levelNum)).setLevelNum(levelNum);

        try{
            itemMapper.insert(item);
        }catch (Exception e){
            return "名称重复";
        }

        if (trueOrFalse(10)){
            userItemMapper.insert(new UserItem().setItemId(item.getId()).setItemName(item.toFullName()).setQq(user.getQq()));
            sendGroupMsg("召唤者" + user.getName() + "被" + itemName + "选中了！" );
        }

        return user.getName() + "召唤了" + item.toFullName();
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

        if (user.getBankOverdue() > 2){
            return  "您的符卡已被银行冻结";
        }

        synchronized (lock){

            if (currentGoods != null){
                UserItem userItem = currentGoods.getUserItem();
                return "当前正在拍卖" + userItem.getItemName();
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


            userItem.setItemName(item.toFullName());
            goods.setUserItem(userItem);
            goods.setPrice(value);
            goods.setLastPrice(value - 1);
            currentGoods = goods;
            lock = true;

            sendGroupMsg(user.getName() + "开始拍卖" + userItem.getItemName() + "了\n起价" + value + "\n发送出价 + 价格参与!");

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
                sendGroupMsg(userItem.getItemName() + "流拍");
                currentGoods = null;
                lock = false;
                return;
            }

            UserItem check = userItemMapper.selectById(userItem.getId());
            if (check == null || !check.getQq().equals(userItem.getQq())){
                sendGroupMsg(itemName + "交易失败！~");
                currentGoods = null;
                lock = false;
            }

            userMapper.changeMoney(price,userItem.getQq());
            userMapper.changeMoney(-price,lastQQ);

            userItem.setQq(lastQQ);
            userItemMapper.updateById(userItem);


            Member info = getGroupMemberInfo(lastQQ);

            sendGroupMsg(MyUtil.getCardName(info) + "以" + price + "金币拍下了" + userItem.getItemName());

            currentGoods = null;
            lock = false;
        });


        return -1;
    }
    @Resource
    private ShopMapper shopMapper;

    public void flushShop(){
        List<Item> items = itemMapper.selectList(new QueryWrapper<Item>().orderByAsc("rand()").last("limit 10"));
        shopMapper.delete(null);
        int number = 1;
        for (Item item : items) {
            int i = randInt(8, 18);
            String type = item.getType();
            if ("暗".equals(type) || "幽".equals(type)){
                i *= 2;
            }
            shopMapper.insert(new Shop().setPrice(item.getValue() * i).setItemId(item.getId()).setItemName(item.toFullName()).setNumber(number));
            number++;
        }
        sendGroupMsg(FtlUtil.render("sd",sd(null)));
        sendGroupMsg("商店已刷新");
    }

    @CommandMapping(value = "商店",menu = "fk")
    public Object sd(Message message){

        List<Shop> shops = shopMapper.selectList(new QueryWrapper<Shop>().orderByDesc("price"));

        Map map = new HashMap();
        map.put("list",shops);
        return map;
    }

    @CommandMapping(value = "购买*",menu = "fk")
    public synchronized Object gm(Message message,Integer number){

        User user = message.getUser();

        if (user.getBankOverdue() > 1){
            return  "您的金币已被银行冻结";
        }

        Shop shop = shopMapper.selectOne(new QueryWrapper<Shop>().eq("number", number));
        if (shop == null){
            return "购买失败";
        }

        if (user.getMoney() < shop.getPrice()){
            return "余额不足";
        }

        shopMapper.deleteById(shop.getId());

        userItemMapper.insert(new UserItem().setItemName(shop.getItemName()).setItemId(shop.getItemId()).setQq(user.getQq()));
        user.setMoney(user.getMoney() - shop.getPrice());

        return user.getName() + "花费" + shop.getPrice() + "金币购买了" + shop.getItemName();
    }

    @CommandMapping(value = "出价*")
    public Object cj(Message message,Long value){

        User user = message.getUser();

        if (user.getBankOverdue() > 1){
            return  "您的金币已被银行冻结";
        }

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

                    UserItem check = userItemMapper.selectById(userItem.getId());
                    if (check == null || !check.getQq().equals(userItem.getQq())){
                        sendGroupMsg(userItem.getItemName() + "交易失败！~");
                        currentGoods = null;
                        return -1;
                    }

                    userMapper.changeMoney(value,userItem.getQq());
                    userMapper.changeMoney(-value,user.getQq());

                    userItem.setQq(user.getQq());
                    userItemMapper.updateById(userItem);


                    Member info = getGroupMemberInfo(user.getQq());

                    currentGoods = null;

                    return MyUtil.getCardName(info) + "以" + value + "金币拍下了" + userItem.getItemName();
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

            userMapper.changeMoney(item.getValue() * 2L,item.getQq());

        });

        userItemMapper.delete(null);
        itemMapper.delete(null);
        dumpMapper.delete(null);
        createDefault();

        List<Item> itemList = itemMapper.getMaxValueGroupType();

        if (itemList.size() == 5){
            for (Item item : itemList) {
                userItemMapper.insert(new UserItem().setQq(-1L).setItemId(item.getId()).setItemName(item.toFullName()));
            }
        }else {
            for (Item item : itemList) {
                userItemMapper.insert(new UserItem().setQq(-1L).setItemId(item.getId()).setItemName(item.toFullName()));
                userItemMapper.insert(new UserItem().setQq(-1L).setItemId(item.getId()).setItemName(item.toFullName()));
            }
        }

        redisTemplate.execute(new RedisCallback() {
            public String doInRedis(RedisConnection connection) throws DataAccessException {
                connection.flushDb();
                return "ok";
            }
        });


        return "符卡数据已经全部清空，之前拥有符卡已经兑换成金币";
    }

    private static long getValue(int i){
        long value = 0;

        if (i == 5){
            value = randInt(800,1999);
        } else if(i == 4){
            value = randInt(500,799);
        } else if(i == 3){
            value = randInt(250,499);
        } else if(i == 2){
            value = randInt(100,249);
        } else if(i == 1){
            value = randInt(1,99);
        }

        return value;
    }



    private static Map<Integer,String> levelNumMap = new HashMap<Integer,String> (){{
        put(1,"N");
        put(2,"R");
        put(3,"SR");
        put(4,"SSR");
        put(5,"UR");
    }};

    private static String getType() {
        return typeNumMap.get(getOne(Arrays.asList(3.0,3.0,18.8,18.8,18.8,18.8,18.8)) + 1);
    }

    private static Map<Integer,String> typeNumMap = new HashMap<Integer,String> (){{
        put(1,"幽");
        put(2,"暗");
        put(3,"灵");
        put(4,"梦");
        put(5,"雪");
        put(6,"月");
        put(7,"幻");
    }};

    private static Integer rdmLevelNum(){

        List<Double> list = Arrays.asList(46.0,30.0,15.0,6.0,3.0);
        return getOne(list) + 1;
    }

    private static boolean isNotName(String name){
        return (name.contains("[CQ")|| name.length() > 32) || name.contains("【") || name.contains("】" )|| name.contains("[")||name.contains("]");
    }



}
