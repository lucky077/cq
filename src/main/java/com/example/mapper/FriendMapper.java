package com.example.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.Friend;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.mapping.StatementType;

public interface FriendMapper extends BaseMapper<Friend> {

    default QueryWrapper<Friend> queryWrapper(Long qq1,Long qq2){
        return new QueryWrapper<Friend>().eq("qq1",qq1).eq("qq2",qq2).or().eq("qq1",qq2).eq("qq2",qq1);
    }
    default QueryWrapper<Friend> queryWrapper(Friend friend){
        return new QueryWrapper<Friend>().eq("qq1",friend.getQq1()).eq("qq2",friend.getQq2()).or().eq("qq1",friend.getQq2()).eq("qq2",friend.getQq1());
    }

    @Update("update friend set val = val + #{val} where val >= #{index} and val <= #{limit}")
    int guanxiUp(@Param("val")Integer val,@Param("index")Integer index,@Param("limit")Integer limit);

    default void setVal(Long qq1,Long qq2,int val){
        Friend friend = getFriend(qq1,qq2);
        int newVal = friend.getVal() + val;
        friend.setVal(newVal);
        if (newVal < -100){
            friend.setVal(100);
        }else if (newVal > 100){
            friend.setVal(100);
        }
        this.updateById(friend);

    }
    default Friend getFriend(Long qq1,Long qq2){
        Friend param = new Friend().setQq1(qq1).setQq2(qq2);
        Friend friend = this.selectOne(this.queryWrapper(param));
        if (friend == null){
            friend = param;
            this.insert(friend);
        }
        return friend;
    }

}
