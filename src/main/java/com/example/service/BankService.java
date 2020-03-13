package com.example.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.annotation.CommandMapping;
import com.example.entity.Item;
import com.example.entity.User;
import com.example.entity.UserItem;
import com.example.mapper.ItemMapper;
import com.example.mapper.UserItemMapper;
import com.example.mapper.UserMapper;
import com.example.model.Message;
import com.example.util.MyUtil;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

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
    private PKService pkService;


    @CommandMapping(value = {"银行"},menu = {"cd"})
    public Object yh(Message message){
        String cd = MyUtil.getChildMenu("yh");
        return cd;
    }

    @CommandMapping(value = {"存款*","存钱*"},menu = {"yh"})
    public Object ck(Message message,Long tMoney){

        if (tMoney < 1){
            return -1;
        }

        User user = message.getUser();

        Long money = user.getMoney();

        if (tMoney > money){
            sendGroupMsg("余额不足");
            return -1;
        }

        user.setMoney(money - tMoney);
        user.setBankMoney(user.getBankMoney() + tMoney);

        return "成功存款" + tMoney + "到银行";
    }
    @CommandMapping(value = {"取款*","取钱*"},menu = {"yh"})
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
    public Object dk(Message message,Long tMoney){

        if (tMoney < 1){
            return -1;
        }

        User user = message.getUser();

        Long money = user.getBankMoney();

        if (tMoney > money + user.getBankScore()){
            sendGroupMsg("余额不足");
            return -1;
        }

        user.setMoney(tMoney + user.getMoney());
        user.setBankMoney(money - tMoney);

        return "成功从银行贷款" + tMoney;
    }

    @CommandMapping(value = {"还款*","还钱*"},menu = {"yh"})
    public Object hk(Message message,Long tMoney){

        if (tMoney < 1){
            return -1;
        }

        User user = message.getUser();

        Long money = user.getMoney();

        if (tMoney > money){
            sendGroupMsg("余额不足");
            return -1;
        }

        user.setMoney(money - tMoney);
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

        if (bankItem == null){
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

        if (pk){
            long sum = 0;
            int i = randInt(7, 11) + 1;

            sum += 50000 / i;

            userMapper.reduceBankMoney(i);
            Long sumBankMoney = userMapper.getSumBankMoney();
            if (sumBankMoney != null){
                sum += (sumBankMoney / i);
            }
            user.setMoney(user.getMoney() + sum);
            return "成功抢走了银行" + sum + "金币";
        }

        return "";
    }






}
