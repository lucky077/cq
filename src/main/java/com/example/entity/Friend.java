package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.val;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class Friend {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Long qq1;
    private Long qq2;
    private Integer val = 0;

}
