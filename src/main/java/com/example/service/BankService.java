package com.example.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.annotation.CommandMapping;
import com.example.annotation.Times;
import com.example.entity.*;
import com.example.mapper.*;
import com.example.model.Message;
import com.example.model.TypeValue;
import com.example.util.MyUtil;
import com.sobte.cqp.jcq.entity.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.example.Demo.getGroupMemberInfo;
import static com.example.Demo.sendGroupMsg;
import static com.example.Variable.currentGoods;
import static com.example.util.LuckUtil.*;

@CommandMapping
public class BankService {


    @Resource
    private UserItemMapper userItemMapper;

    @Resource
    private ItemMapper itemMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private DumpMapper dumpMapper;

    @Resource
    private PKService pkService;

    @Resource
    private FriendMapper friendMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;


    @CommandMapping(value = {"银行"},menu = {"cd"})
    public Object yh(Message message){
        String cd = MyUtil.getChildMenu("yh");
        return cd;
    }

    @CommandMapping(value = {"银行信息"},menu = {"yh"})
    public Object yhxx(Message message){
        StringBuilder sb = new StringBuilder();
        User user = message.getUser();
        Long bankMoney = user.getBankMoney();

        sb.append(user.getName() + "的银行信息：\n");

        if (bankMoney > 0){
            sb.append("存款：" + bankMoney + "\n");
        }else if(bankMoney < 0){
            sb.append("欠款：" + bankMoney + "\n");
        }
        int day = user.getBankOverdue() - 1;
        day = day < 0 ? 0 : day;
        sb.append("你当前最多可借款：" + user.getBankScore() + "\n");
        sb.append("你当前逾期：" + (day) + "天");

        return sb.toString();
    }

    @CommandMapping(value = {"信用评估"},menu = {"yh"})
    public Object xypg(Message message){

        User user = message.getUser();
        if (user.getBankScore() > 0){
            return -1;
        }

        Integer value = userItemMapper.selectMyValue(user.getQq());

        if (value == null){
            value = 0;
        }

        value += Integer.parseInt(String.valueOf(user.getMoney())) * 4;

        user.setBankScore(value);

        return "你最多能贷款" + value;
    }

    @CommandMapping(value = {"存款*","存钱*"},menu = {"yh"})
    @Times(interval = 3600,limit = 2)
    public Object ck(Message message,Long tMoney){

        if (tMoney < 1){
            return -1;
        }

        User user = message.getUser();

        Long money = user.getMoney();

        if (user.getBankScore() < 1){
            return "请先进行信用评估";
        }

        if (tMoney > money){
            sendGroupMsg("余额不足");
            return -1;
        }

        user.setMoney(money - tMoney);
        user.setBankMoney(user.getBankMoney() + tMoney);

        return "成功存款" + tMoney + "到银行";
    }
    @CommandMapping(value = {"取款*","取钱*"},menu = {"yh"})
    //@Times(interval = 3600,limit = 2)
    public Object qk(Message message,Long tMoney){

        if (tMoney < 1){
            return -1;
        }

        User user = message.getUser();

        Long money = user.getBankMoney();

        if (tMoney > money){
            sendGroupMsg("余额不足");
            return -1;
        }

        user.setMoney(tMoney + user.getMoney());
        user.setBankMoney(money - tMoney);

        return "成功从银行取款" + tMoney;
    }
    @CommandMapping(value = {"贷款*","借钱*"},menu = {"yh"})
    @Times(interval = 3600,limit = 2)
    public Object dk(Message message,Long tMoney){

        if (tMoney < 1){
            return -1;
        }

        User user = message.getUser();

        Long money = user.getBankMoney();

        if (tMoney > money + user.getBankScore()){
            sendGroupMsg("信用不足");
            return -1;
        }

        user.setMoney(tMoney + user.getMoney());
        user.setBankMoney(money - tMoney);

        return "成功从银行贷款" + tMoney;
    }

    @CommandMapping(value = {"还款*","还钱*"},menu = {"yh"})
    //@Times(interval = 3600,limit = 2)
    public Object hk(Message message,Long tMoney){

        if (tMoney < 1){
            return -1;
        }

        User user = message.getUser();

        if (user.getBankMoney() > -1){
            return "你没有借款信息";
        }

        Long money = user.getMoney();

        if (tMoney > money){
            sendGroupMsg("余额不足");
            return -1;
        }

        user.setMoney(money - tMoney);
        if (tMoney * 1.0 >= -user.getBankMoney() / 2.0){
            user.setBankOverdue(0);
        }
        user.setBankMoney(user.getBankMoney() + tMoney);



        return "成功还款" + tMoney + "到银行";
    }

    @CommandMapping(value = {"派遣*"},menu = {"yh"},notes = "符卡驻扎到银行")
    public Object pq(Message message,String itemName){

        User user = message.getUser();

        Integer bankItem = user.getBankItem();
        if (bankItem != 0){
            sendGroupMsg("每人只能支援银行一张符卡");
            return -1;
        }

        Item item = itemMapper.selectOne(new QueryWrapper<Item>().eq("name", itemName));

        if (item == null){
            sendGroupMsg("符卡不存在");
            return -1;
        }



        UserItem userItem = userItemMapper.selectOne(new QueryWrapper<UserItem>()
                .eq("item_id", item.getId()).eq("qq", user.getQq()).last("limit 1"));

        if (userItem == null){
            sendGroupMsg("你没有这张符卡");
            return -1;
        }

        userItemMapper.updateById(new UserItem().setId(userItem.getId()).setQq(-1L));
        user.setBankItem(userItem.getId());

        return user.getName() + "已将" + item.toFullName() + "派遣至银行";
    }

    @CommandMapping(value = {"召回"},menu = {"yh"},notes = "召回派遣至银行的符卡")
    @Transactional
    public Object zh(Message message){

        User user = message.getUser();
        Integer bankItem = user.getBankItem();

        if (bankItem == 0){
            sendGroupMsg("你在银行没有符卡");
            return -1;
        }

        userItemMapper.updateById(new UserItem().setId(bankItem).setQq(user.getQq()));
        user.setBankItem(0);

        UserItem userItem = userItemMapper.selectById(bankItem);
        Item item = itemMapper.selectById(userItem.getItemId());

        return user.getName() + "已将" + item.toFullName() + "从银行召回";
    }

    @CommandMapping(value = {"打劫银行","抢劫银行"},tili = -30,menu = {"yh"},notes = "")
    @Transactional
    public Object djyh(Message message){

        User user = message.getUser();
        User user2 = new User().setQq(-1L);

        boolean pk = pkService.pk(user, user2);

        double an = TypeValue.getOne(user.getTypeValues(), "暗").getSumLevel() * 2.0;

        String bankMoneyStr = redisTemplate.opsForValue().get("bankMoney");
        Integer bankMoney = 0;
        if (bankMoneyStr == null){
            redisTemplate.opsForValue().set("bankMoney","0");
        }else {
            bankMoney = Integer.valueOf(bankMoneyStr);
        }
        if (pk){
            long sum = 0;
            int i = randInt(7, 11) + 1;

            sum += (60000 + bankMoney) / i;

            userMapper.reduceBankMoney(i);
            Long sumBankMoney = userMapper.getSumBankMoney();
            if (sumBankMoney != null){
                sum += (sumBankMoney / i);
            }
            user.setMoney(user.getMoney() + sum);
            user.setHonor(user.getHonor() - 1);
            bankMoney = bankMoney - bankMoney / i;
            redisTemplate.opsForValue().set("bankMoney",bankMoney.toString());
            return "成功抢走了银行" + sum + "金币";
        }else {
            if (trueOrFalse(60.0 + an)){
                String card = "";
                if (trueOrFalse(50.0 - an * 1.5)){
                    List<Item> items = userItemMapper.selectList(user.getQq());
                    Item item;
                    if ((item = HdService.getNoUr(items)) != null){
                        Integer id = item.getId();
                        UserItem userItem = userItemMapper.selectById(id);
                        userItemMapper.delete(new UpdateWrapper<UserItem>().eq("id",id));
                        dumpMapper.insert(new Dump().setItemId(userItem.getItemId()));
                        card = "\n跑的匆忙，途中不慎遗失符卡：" + item.toFullName();
                    }
                }
                return "什么都没捞到，但是侥幸逃跑了" + card;
            } else {
                String key = "bankBan:" + user.getQq();
                int add = 0;
                if (user.getHonor() < 0){
                    add = add - user.getHonor();
                }
                int min = randInt(30, 120 + add);
                redisTemplate.opsForValue().set(key,"1",min, TimeUnit.MINUTES);
                int i = randInt(100,3000);
                user.setBankMoney(user.getBankMoney() - i);
                bankMoney = bankMoney + i;
                redisTemplate.opsForValue().set("bankMoney",bankMoney.toString());
                return "你打不过银行,被关进监狱" + min + "分钟,罚款" + i;
            }
        }


    }

    @CommandMapping(value = {"劫狱"},tili = -20,menu = {"yh"},notes = "")
    @Transactional
    public Object jy(Message message){

        Set<String> keys = redisTemplate.keys("bankBan:*");
        if (keys.isEmpty()){
            sendGroupMsg("监狱没有人");
            return -1;
        }


        User user = message.getUser();
        String bankMoneyStr = redisTemplate.opsForValue().get("bankMoney");
        Integer bankMoney = 0;
        if (bankMoneyStr == null){
            redisTemplate.opsForValue().set("bankMoney","0");
        }else {
            bankMoney = Integer.valueOf(bankMoneyStr);
        }
        User user2 = new User().setQq(-1L);

        boolean pk = pkService.pk(user, user2);
        double an = TypeValue.getOne(user.getTypeValues(), "暗").getSumLevel() * 2.0;
        if (pk){

            for (String key : keys) {
                redisTemplate.delete(key);
                key = key.replace("bankBan:","");
                long qq = Long.valueOf(key);
                user.setHonor(user.getHonor());
                friendMapper.setVal(qq,user.getQq(),12);
            }
            redisTemplate.delete("times:jlj:" + user.getQq().toString());
            return "你拯救了" + keys.size() + "人，你们的关系提升了，你获得荣誉\n你的捡垃圾CD被重置";

        }else {
            if (trueOrFalse(60 + an)){
                return "劫狱失败";
            }else {
                String key = "bankBan:" + user.getQq();

                int add = 0;
                if (user.getHonor() < 0){
                    add = add - user.getHonor();
                }

                int min = randInt(30, 120 + add);
                redisTemplate.opsForValue().set(key,"1",min, TimeUnit.MINUTES);
                int i = randInt(100,3000);
                user.setBankMoney(user.getBankMoney() - i);
                bankMoney = bankMoney + i;
                redisTemplate.opsForValue().set("bankMoney",bankMoney.toString());
                return "劫狱失败,你被关进监狱" + min + "分钟,罚款" + i;
            }
        }

    }

    public void qingsuan(List<User> users){

        users.forEach(user -> {
            Long qq = user.getQq();
            Member info = getGroupMemberInfo(qq);
            String name = MyUtil.getCardName(info);
            Long bankMoney = user.getBankMoney();
            Long money = user.getMoney();
            if (money >= -bankMoney){
                user.setMoney(money + bankMoney);
                bankMoney = 0L;
            }else if (money > 0){
                user.setMoney(0L);
                bankMoney += money;
            }
            sendGroupMsg(name + "的财产已经被银行清算");


            List<Item> items = userItemMapper.selectList(qq);

            for (Item item : items) {
                if (bankMoney >= 0){
                    break;
                }
                long value = item.getValue() * 3;
                userItemMapper.updateById(new UserItem().setId(item.getId()).setQq(-1L));
                bankMoney += value;
                sendGroupMsg(name + "的符卡："+ item.toFullName() +"已经被银行清算");
            }

            if (bankMoney < 0 && user.getBankItem() != 0){
                user.setBankItem(0);
                sendGroupMsg(name + "派遣至银行的符卡已被清算");
            }

            if (bankMoney < 0){
                user.setBankScore(0);
                sendGroupMsg("银行不再信任" + name);
            }
            user.setBankOverdue(0);
            user.setBankMoney(bankMoney = 0L);
            userMapper.updateById0(user);

        });

    }






}
