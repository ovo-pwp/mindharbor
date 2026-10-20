package com.mindharbor.psych.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 微行动记录：用户在某个能量档位下做成的一件小事。
 * 不记连续天数、不算完成率，只留一条「我做过」的痕迹。
 */
@Data
@TableName("micro_action_log")
public class MicroActionLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 当天的能量档位 0-5 */
    private Integer level;

    private String content;

    private LocalDate actionDate;

    private LocalDateTime createdAt;
}
