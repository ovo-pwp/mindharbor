package com.mindharbor.psych.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("symptom_record")
public class SymptomRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 情绪 1-5 */
    private Integer mood;

    /** 严重程度 1-5 */
    private Integer severity;

    /** 精力水平 1-5 */
    private Integer energy;

    private String triggerEvent;

    /** 生理症状，逗号分隔 */
    private String symptoms;

    /** 应对方式，逗号分隔 */
    private String coping;

    private String note;

    private LocalDate recordDate;

    private LocalDateTime createdAt;
}
