package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.Item;
import com.example.entity.UserItem;
import com.example.model.TypeValue;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface UserItemMapper extends BaseMapper<UserItem> {


    @Select("select ui.id,i.name,i.level,i.`type` from useritem ui,item i where i.id = ui.item_id and ui.qq = #{qq} order by i.value desc")
    List<Item> selectList(Object qq);

    @Select("select sum(i.value) from useritem ui,item i where i.id = ui.item_id and ui.qq = #{qq}")
    Integer selectMyValue(Object qq);

    @Select("select ui.qq,sum(i.value) value from useritem ui,item i where i.id = ui.item_id group by ui.qq")
    List<Item> selectAllValue();

    @Select("select i.* from useritem ui,item i where i.id = ui.item_id and ui.qq = #{qq} order by i.value desc limit 1")
    Item selectMax(Object qq);

    @Select("select type,count(0) count,sum(i.level_num) sumLevel,sum(i.`value`) sumValue from useritem ui , item i where ui.item_id = i.id and ui.qq = #{qq} group by type")
    List<TypeValue> selectTypeValue(Object qq);
}
