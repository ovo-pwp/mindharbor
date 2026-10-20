package com.mindharbor.psych.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("report")
public class Report {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long doctorId;

    private Long userId;

    private String title;

    /** 总体概述 */
    private String overview;

    /** 数据洞察 */
    private String insight;

    /** 风险预警 */
    private String riskWarning;

    /** 改善建议 */
    private String suggestion;

    /** DRAFT / PUBLISHED */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;
}
