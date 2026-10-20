package com.mindharbor.psych.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/** 通用审核 / 启用状态 */
public enum CommonStatus {
    PENDING("待审核"),
    APPROVED("已通过"),
    REJECTED("已拒绝"),
    ACTIVE("正常"),
    DISABLED("已停用"),
    PUBLISHED("已发布"),
    DRAFT("草稿");

    @EnumValue
    private final String code;
    private final String label;

    CommonStatus(String label) {
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
