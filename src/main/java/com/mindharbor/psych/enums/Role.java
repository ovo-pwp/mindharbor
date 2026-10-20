package com.mindharbor.psych.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum Role {
    USER("患者"),
    DOCTOR("医生"),
    ADMIN("管理员");

    @EnumValue
    private final String code;
    private final String label;

    Role(String label) {
        this.code = name();
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
