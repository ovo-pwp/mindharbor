package com.mindharbor.psych.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 心理练习记录：呼吸引导 / 思维记录 / 身体扫描。
 * 只记录「做过」，不做连续天数与完成率，避免变成考核。
 */
@Data
@TableName("practice_log")
public class PracticeLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** BREATHING / THOUGHT_RECORD / BODY_SCAN */
    private String type;

    /** 秒，思维记录可为 0 */
    private Integer duration;

    /** 思维记录存 JSON，其他存简短备注 */
    private String content;

    private Integer moodBefore;

    private Integer moodAfter;

    private LocalDateTime createdAt;
}
