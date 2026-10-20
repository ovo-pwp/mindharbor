package com.mindharbor.psych.vo;

import com.mindharbor.psych.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginVO {

    private String token;

    private User user;

    /** 若该账号是医生，附带医生档案 id */
    private Long doctorId;
}
