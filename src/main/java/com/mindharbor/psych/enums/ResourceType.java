package com.mindharbor.psych.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum ResourceType {
    ARTICLE("图文"),
    VIDEO("视频"),
    AUDIO("音频"),
    EXERCISE("练习");

    @EnumValue
    private final String code;
    private final String label;

    ResourceType(String label) {
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
