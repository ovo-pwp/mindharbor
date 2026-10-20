package com.mindharbor.psych.dto;

import lombok.Data;

@Data
public class RegisterDTO {

    /** 登录账号 */
    private String username;

    /** 密码 */
    private String password;

    /** 姓名 */
    private String realName;

    private String phone;

    private String gender;

    private Integer age;

}