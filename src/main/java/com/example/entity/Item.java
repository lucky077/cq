package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class Item {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    //幽 暗 灵 梦 雪 月 幻
    private String type;

    private Long value;

    private Long qq;

    private String level;

    private Integer levelNum;

    private Date createDate;

    public String toFullName(){
        return name + "【" + type + "】【" + level + "】";
    }


}
