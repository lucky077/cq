package com.example.model;

import com.example.entity.UserItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class Goods {

    private UserItem userItem;

    private Long price = 0L;

    private Long lastPrice = 0L;

    private Long lastQQ;

    private LocalDateTime lastTime;

}
