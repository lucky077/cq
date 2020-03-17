package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.Demo;
import com.example.entity.User;
import com.example.util.MyUtil;
import com.sobte.cqp.jcq.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.Demo.getFromGroup;
import static com.example.Variable.*;
public interface UserMapper extends BaseMapper<User> {

    @Update("update user set money = money + #{money} where qq = #{qq}")
    void changeMoney(@Param("money") Long money,@Param("qq") Long qq);

    @Update("update user set money = money + bank_money,bank_money = 0,bank_item = 0,bank_overdue = 0")
    void resetBank();

    @Update("update user set tili = tili + 10 where tili < 100")
    void changeTili();
    @Update("update user set tili = 100 where tili > 100")
    void changeTili2();

    @Update("update user set money = money + #{money} where qq = #{qq}")
    void changeMoney(User user);


    @Update("update user set bank_money = bank_money + bank_money * 0.3")
    void changeBankMoney();

    @Update("update user set bank_overdue = bank_overdue + 1 where bank_money < 0")
    void changeBankOverdue();

    @Update("update user set bank_score = bank_score + bank_money * 0.3 where bank_money > 0")
    void changeBankScore();

    @Select("select * from user where bank_overdue > 3 and bank_money < 0")
    List<User> getBankOverdue();

    @Select("select sum(bank_money) from user where bank_money > 20")
    Long getSumBankMoney();


    @Update("update user set bank_money = bank_money - bank_money/#{i}")
    void reduceBankMoney(Object i);

    @Select("select qq , money ,bank_money from user where group_id = #{groupId} and qq > 9999 group by (money + bank_money) desc limit #{limit}")
    List<User> selectListzs(@Param("groupId") Object groupId,@Param("limit") long limit);

    default User selectById0(Serializable id){
        User user = this.selectById(id);
        if (user == null) {
            user = new User().setQq((long)id).setGroupId(getFromGroup()).setCreateDate(new Date()).setCheckDate(new Date(1539860151000l));
            this.insert0(user);
        }
        return user;
    }


    default int insert0(User user){
        return this.insert(user);
    }

    default int updateById0(User oldUser){
        return this.updateById(oldUser);
    }



}
