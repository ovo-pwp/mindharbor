package com.mindharbor.psych.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("assessment")
public class Assessment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private String description;

    private String instruction;

    private Integer questionCount;

    /** 标准分分级阈值，升序逗号分隔，如 53,63,73 */
    private String thresholds;

    /** 分级名称，如 正常,轻度抑郁,中度抑郁,重度抑郁 */
    private String levels;

    private Boolean enabled;

    private LocalDateTime createdAt;

    public String[] thresholdArray() {
        return thresholds == null || thresholds.isBlank() ? new String[0] : thresholds.split(",");
    }

    public String[] levelArray() {
        return levels == null || levels.isBlank() ? new String[0] : levels.split(",");
    }
}
