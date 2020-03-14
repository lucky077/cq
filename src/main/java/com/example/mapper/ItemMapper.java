package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.Item;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ItemMapper extends BaseMapper<Item> {



    @Select("SELECT * FROM `item` item,(SELECT type,max(`value`) value FROM `item` group by type) typeMax\n" +
            "where item.type = typeMax.type and item.`value` = typeMax.value and item.type != '幽' and item.type != '暗'")
    List<Item> getMaxValueGroupType();
}
