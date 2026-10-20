package com.mindharbor.psych.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum BindStatus {
    PENDING("待审核"),
    APPROVED("已绑定"),
    REJECTED("已拒绝");

    @EnumValue
    private final String code;
    private final String label;

    BindStatus(String label) {
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
