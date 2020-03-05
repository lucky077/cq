package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.Item;
import com.example.entity.UserItem;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface UserItemMapper extends BaseMapper<UserItem> {


    @Select("select i.name,i.level from useritem ui,item i where i.id = ui.item_id and ui.qq = #{qq} order by i.value desc")
    List<Map> selectList(Object qq);

    @Select("select sum(i.value) from useritem ui,item i where i.id = ui.item_id and ui.qq = #{qq}")
    int selectMyValue(Object qq);

    @Select("select i.* from useritem ui,item i where i.id = ui.item_id and ui.qq = #{qq} order by i.value desc limit 1")
    Item selectMax(Object qq);

}
