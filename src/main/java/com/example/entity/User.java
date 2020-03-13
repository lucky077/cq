package com.example.entity;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.example.model.TypeValue;
import com.example.util.MyUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.omg.PortableInterceptor.INACTIVE;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(exclude = {"checkDate","createDate","name","typeValues"})
public class User implements Cloneable{

    @TableId
    private Long qq;

    private Long groupId;

    @TableField(exist = false)
    private String name;

    @TableField(exist = false)
    private List<TypeValue> typeValues;

    private Long money = 0l;
    //体力
    private Integer tili = 100;
    //荣誉
    private Integer honor = 0;

    private Integer checkDay = 0;

    private Long bankMoney = 0L;

    private Integer bankItem = 0;

    private Integer bankScore = 0;

    private Integer bankOverdue;

    private Date checkDate;

    private Date createDate;



    public User setAll(User user){
        BeanUtils.copyProperties(user,this);
        return this;
    }

    public boolean equalsAndSetNull(Object obj){

        return MyUtil.equalsAndSetNull(this,obj,"qq","typeValues");
    }

    @Override
    public User clone(){
        try {
            return (User) super.clone();
        } catch (CloneNotSupportedException e) {
            //unreachable,class has implemented the Cloneable
        }
        return null;
    }
}
