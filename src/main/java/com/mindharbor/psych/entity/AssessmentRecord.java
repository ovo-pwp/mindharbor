package com.mindharbor.psych.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("assessment_record")
public class AssessmentRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long assessmentId;

    private String assessmentName;

    private Integer rawScore;

    private Double standardScore;

    private String level;

    /** 逐题得分 JSON */
    private String detail;

    private String suggestion;

    private LocalDateTime createdAt;
}
