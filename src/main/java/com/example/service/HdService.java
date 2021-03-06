package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.Demo;
import com.example.annotation.CommandMapping;
import com.example.annotation.Times;
import com.example.entity.*;
import com.example.mapper.*;
import com.example.model.Message;
import com.example.model.Replay;
import com.example.model.TypeValue;
import com.example.util.FtlUtil;
import com.example.util.H;
import com.example.util.LuckUtil;
import com.example.util.MyUtil;
import com.sobte.cqp.jcq.entity.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.ModelAndView;

import static com.example.Demo.*;
import static com.example.Variable.*;
import javax.annotation.Resource;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.example.util.LuckUtil.*;

//@CommandMapping
public class HdService {

    @Resource
    private FriendMapper friendMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private PKService pkService;
    @Resource
    private ItemMapper itemMapper;
    @Resource
    private UserItemMapper userItemMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Resource
    private DumpMapper dumpMapper;

    //@CommandMapping(value = "捡垃圾",menu = "hd")
    @Times(tip = "24小时内只能使用一次")
    public Object jlj(Message message){
        User user = message.getUser();
        List<Dump> dumps = dumpMapper.selectList(null);
        if (dumps.isEmpty()){
            return "什么都没捡到";
        }
        List<TypeValue> typeValues = userItemMapper.selectTypeValue(user.getQq());
        double you = TypeValue.getOne(typeValues, "幽").getSumLevel() * 2.0;
        if (trueOrFalse(50 + you)){
            return "什么都没捡到";
        }
        Collections.shuffle(dumps);
        Dump dump = dumps.get(0);
        Item item = itemMapper.selectById(dump.getItemId());
        dumpMapper.deleteById(dump.getId());
        userItemMapper.insert(new UserItem().setQq(user.getQq()).setItemId(dump.getItemId()).setItemName(item.toFullName()));
        return user.getName() + "捡到了一张符卡：" + item.toFullName();
    }


    //@CommandMapping(value = {"打劫*","抢劫*"},menu = {"hd"},tili = -30)
    public Object dj(Message message,Long qq2){
        if (qq2 == null || message.getFromQQ().equals(qq2) || qq2 < 10000L){
            return -1;
        }

        User user = message.getUser();
        User user2 = userMapper.selectById0(qq2);
        if (user2 == null){
            return -1;
        }

        Member groupMemberInfo = getGroupMemberInfo(qq2);
        user2.setName(MyUtil.getCardName(groupMemberInfo));


        boolean pk = pkService.pk(user,user2);


        friendMapper.setVal(user.getQq(),qq2,-6);

        double an2 = TypeValue.getOne(user2.getTypeValues(), "暗").getSumLevel() * 2.0;
        double an1 = TypeValue.getOne(user.getTypeValues(), "暗").getSumLevel() * 2.0;
        double you1 = TypeValue.getOne(user.getTypeValues(), "幽").getSumLevel() * 2.0;
        double you2 = TypeValue.getOne(user2.getTypeValues(), "幽").getSumLevel() * 2.0;

        if (pk){
            if (trueOrFalse(20.0 + an2) ){

                String card = "";

                if (trueOrFalse(35.0 - an2 * 1.5)){
                    List<Item> items = userItemMapper.selectList(qq2);
                    Item item;
                    if ((item = getNoUr(items)) != null){
                        Integer id = item.getId();
                        UserItem userItem = userItemMapper.selectById(id);
                        userItemMapper.delete(new UpdateWrapper<UserItem>().eq("id",id));
                        dumpMapper.insert(new Dump().setItemId(userItem.getItemId()));
                        card = "\n跑的匆忙，途中不慎遗失符卡：" + item.toFullName();
                    }

                }
                return user2.getName() + "逃跑了！你什么都没捞到\n你们的关系恶化了" + card;
            }

            long add = user2.getMoney() / randInt(6,15) + 1;
            if (add < 0){
                add = 0;
            }
            user.setMoney(add + user.getMoney());
            user.setHonor(user.getHonor() - 1);
            user2.setMoney(user2.getMoney() - add);
            userMapper.updateById0(user2);

            String card = "";

            if (trueOrFalse(25.0 + you1)){
                List<Item> items = userItemMapper.selectList(qq2);
                Item item;
                if ((item = getNoUr(items)) != null){
                    Integer id = item.getId();
                    userItemMapper.updateById(new UserItem().setId(id).setQq(user.getQq()));
                    card = "\n符卡：" + item.toFullName();
                }

            }

            return MessageFormat.format("你抢走了{0}{1}金币！{2}\n你们的关系恶化了\n你失去荣誉",
                    user2.getName(),add,card);
        }else {
            if (trueOrFalse(35.0)){
                long add = user.getMoney() / randInt(8,20) + 1;
                user2.setMoney(add + user2.getMoney());
                user.setMoney(user.getMoney() - add);
                userMapper.updateById0(user2);

                String card = "";
                if (trueOrFalse(25 + you2 - an1)){
                    List<Item> items = userItemMapper.selectList(user.getQq());
                    Item item;
                    if ((item = getNoUr(items)) != null){
                        Integer id = item.getId();
                        userItemMapper.updateById(new UserItem().setId(id).setQq(user2.getQq()));
                        card = "\n符卡：" + item.toFullName();
                    }

                }

                return MessageFormat.format("{3}什么都没捞到，被{0}抢了{1}金币！{2}\n你们的关系恶化了",
                        user2.getName(),add,card,user.getName());
            }

            return MessageFormat.format("你打不过{0}\n你们的关系恶化了",
                    user2.getName());
        }

    }
    public static Item getNoUr(List<Item> items){
        if(items == null || items.isEmpty()){
            return null;
        }

        items = items.stream().filter(item -> !item.getLevel().equals("UR")).collect(Collectors.toList());

        if (items.isEmpty()){
            return null;
        }

        Collections.shuffle(items);
        return items.get(0);
    }
    //@CommandMapping(value = "查看仇敌*",menu = {"hd"},order = 1)
    public Object ckcd(Message message,Long qq2){

        return new ModelAndView("ckgx",(Map)ckgx(message,qq2,true,"仇敌"));
    }
    //@CommandMapping(value = "查看朋友*",menu = {"hd"},order = 1)
    public Object ckpy(Message message,Long qq2){

        return new ModelAndView("ckgx",(Map)ckgx(message,qq2,false,"朋友"));
    }
    //@CommandMapping(value = "补魔*",menu = {"hd"})
    @Times(interval = 1800,tip = "这种事要适度喔")
    public Object bm(Message message,Long qq2,Integer value){

        if (qq2 == null){
            return -1;
        }

        if (value == null){
            value = 20;
        }

        if(value < 0){
            return -1;
        }

        User user = message.getUser();
        Integer tili = user.getTili();
        if (tili < value){
            sendGroupMsg("体力不支");
            return -1;
        }

        User user1 = userMapper.selectById0(qq2);

        if (user1 == null){
            return -1;
        }

        userMapper.updateById(user1.setTili(user1.getTili() + value));
        userMapper.updateById(user.setTili(tili - value));

        friendMapper.setVal(user.getQq(),qq2,value / 10);

        return "发生了不可描述的事情呢";
    }
    public Object ckgx(Message message,Long qq2,boolean isAsc,String word){
        User user = message.getUser();
        Long qq = message.getFromQQ();
        if (qq2 != null){
            qq = qq2;
            user = userMapper.selectById0(qq2);
            Member info = getGroupMemberInfo(qq2);
            if (info == null){
                return -1;
            }
            user.setName(MyUtil.getCardName(info));
        }

        Long finalQq = qq;
        List<Map<String, Object>> maps = friendMapper.selectMaps(new QueryWrapper<Friend>()
                .and(q -> q.eq("qq1", finalQq).or().eq("qq2", finalQq))
                .lt(isAsc,"val", -10).gt(!isAsc,"val",9).orderBy(true,isAsc,"val"));
        Iterator<Map<String, Object>> iterator = maps.iterator();

        while (iterator.hasNext()){
            Map<String, Object> next = iterator.next();
            String tQQ = null;
            if(next.get("qq1").equals(qq)){
                tQQ = next.get("qq2").toString();
            }else{
                tQQ = next.get("qq1").toString();
            }
            Member info = getGroupMemberInfo(Long.valueOf(tQQ));
            if (info == null){
                iterator.remove();
                continue;
            }
            next.put("name",MyUtil.getCardName(info));
            next.put("qq",tQQ);
        }
        Map data = new HashMap();
        data.put("word",word);
        data.put("list",maps);
        data.put("name",user.getName());

        return data;
    }


    //@CommandMapping(value = "赠送*",menu = {"hd"})
    @Times(limit = 2,interval = 100)
    public Object zs(Message message,Long qq2,Long num){
        Long fromQQ = message.getFromQQ();
        User user1 = message.getUser();
        if (fromQQ == qq2){
            return -1;
        }
        if (num == null){
            num = 1l;
        }

        if (user1.getBankOverdue() > 1){
            return  "您的金币已被银行冻结";
        }


        if (user1.getMoney() < num + (long)(num * 0.05) || num < 1){
             return  "余额不足！";
        }


        User user2 = userMapper.selectById0(qq2);
        user1.setMoney(user1.getMoney() - num - (long)(num * 0.05));
        user2.setMoney(user2.getMoney() + num);
        userMapper.updateById0(user2);

        Member qq1Info = getGroupMemberInfo(fromQQ);
        Member qq2Info = getGroupMemberInfo(qq2);
        if (qq2Info == null){
            return -1;
        }
        int l = (int)(num / 50);
        if (l < 1){
            l = 1;
        }else if(l > 10){
            l = 10;
        }
        friendMapper.setVal(fromQQ,qq2,l);

        Map data = new HashMap();
        data.put("name1",MyUtil.getCardName(qq1Info));
        data.put("name2",MyUtil.getCardName(qq2Info));
        data.put("num",num);
        return data;
    }

    ////@CommandMapping(value = "挑衅*",tili = -2,menu = {"hd"})
    public Object tx(Message message,Long qq2){
        Long fromQQ = message.getFromQQ();
        Member qq1Info = getGroupMemberInfo(fromQQ);
        Member qq2Info = getGroupMemberInfo(qq2);
        if (qq2Info == null || fromQQ == qq2){
            return -1;
        }
        friendMapper.setVal(fromQQ,qq2,-2);
        Map data = new HashMap();
        data.put("name1",MyUtil.getCardName(qq1Info));
        data.put("name2",MyUtil.getCardName(qq2Info));
        return data;
    }

    //@CommandMapping(value = {"查看*","属性*"},menu = {"hd"})
    public Object sx(Message message,Long qq2){
        Long fromQQ = message.getFromQQ();
        Long targetQQ = message.getFromQQ();
        if (qq2 != null){
            targetQQ = qq2;
        }
        User user = userMapper.selectById0(targetQQ);
        Member info = getGroupMemberInfo(targetQQ);
        if (info == null){
            return -1;
        }

        Item item = userItemMapper.selectMax(targetQQ);


        byte[] head = MyUtil.getHead(targetQQ);
        String fileName = H.writeFile(imageCachePath,head,".jpg");
        String imgCq = CC.image("cache/" + fileName);

        Map map = new HashMap();
        map.put("user",user);
        map.put("item",item);
        map.put("head",imgCq);
        map.put("name",MyUtil.getCardName(info));

        if (qq2 != null && qq2 != fromQQ){
            Friend friend = friendMapper.getFriend(fromQQ, qq2);
            map.put("tail",getTail(friend.getVal(),info));
        }



        return map;
    }

    Timer timer = new Timer();

    //@CommandMapping(value = {"打工"},menu = {"hd"},tili = -20)
    @Times(interval = 3600,tip = "每小时只能打工一次")
    public Object dg(Message message){

        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                int add = randInt(200, 500);
                User user = userMapper.selectById0(message.getUser().getQq());
                user.setMoney(user.getMoney() + add);
                userMapper.updateById0(user);

                sendGroupMsg(message.getUser().getName() +  "打工结束~\n收益：" + add + "金币\n" + Demo.CC.at(user.getQq()));
            }
        },1000 * 600);

        return message.getUser().getName() +  "开始打工~";
    }

    private String getTail(int guanxi,Member member){

        String ta = member.getGender() == 0 ? "她" : "他";

        System.out.println(member);

        String str = " 面 无 表 情 的 看 着 你";

        if (guanxi >= -10 && guanxi < 0){
            str = " 意 味 深 长 地 看 着 你";
        }else if (guanxi >= -20 && guanxi < -10){
            str = " 警 惕 地 看 着 你";
        }else if (guanxi >= -30 && guanxi < -20){
            str = " 开 始 厌 恶 你";
        }else if (guanxi >= -50 && guanxi < -30){
            str = " 非 常 讨 厌 你";
        }else if (guanxi >= -70 && guanxi < -50){
            str = " 狠 狠 地 盯 着 你";
        }else if (guanxi >= -90 && guanxi < -70){
            str = " 对 你 充 满 敌 意";
        }else if (guanxi < -90){
            str = " 与 你 势 同 水 火";
        }else if (guanxi >= 5 && guanxi < 15){
            str = " 开 始 接 受 你";
        }else if (guanxi >= 15 && guanxi < 25){
            str = " 友 善 地 看 着 你";
        }else if (guanxi >= 25 && guanxi < 45){
            str = " 亲 切 地 看 着 你";
        }else if (guanxi >= 45 && guanxi < 60){
            str = " 热 切 地 望 着 你";
        }else if (guanxi >= 60 && guanxi < 80){
            str = " 友 好 地 望 着 你";
        }else if (guanxi >= 80){
            str = " 非 常 支 持 你";
        }

        String tail = ta + str  + "(关系" + guanxi +")";

        return tail;
    }





}
