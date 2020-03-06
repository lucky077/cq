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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.Demo.getFromGroup;
import static com.example.Variable.*;
public interface UserMapper extends BaseMapper<User> {

    @Update("update user set money = money + #{money} where qq = #{qq}")
    void changeMoney(@Param("money") Long money,@Param("qq") Long qq);

    @Update("update user set tili = tili + 10 where tili < 100")
    void changeTili();
    @Update("update user set tili = 100 where tili > 100")
    void changeTili2();

    @Update("update user set money = money + #{money} where qq = #{qq}")
    void changeMoney(User user);

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
