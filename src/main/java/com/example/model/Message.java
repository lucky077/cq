package com.example.model;

import com.example.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    Integer subType;
    Integer msgId;
    Long fromGroup;
    Long fromQQ;
    String fromAnonymous;
    String msg;
    Integer font;
    User user;
}
