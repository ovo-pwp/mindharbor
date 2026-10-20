package com.mindharbor.psych.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mindharbor.psych.enums.CommonStatus;
import com.mindharbor.psych.enums.Role;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    @JsonIgnore
    private String password;

    private String realName;

    private String phone;

    private String email;

    private String gender;

    private Integer age;

    private String city;

    private String avatar;

    private Role role;

    private CommonStatus status;

    private LocalDateTime createdAt;

    public String initial() {
        return realName == null || realName.isEmpty() ? "?" : realName.substring(0, 1);
    }
}
