package com.mindharbor.psych.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mindharbor.psych.enums.CommonStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("doctor")
public class Doctor {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String hospital;

    private String title;

    /** 擅长领域，逗号分隔 */
    private String specialty;

    private String intro;

    private Integer years;

    private BigDecimal price;

    private Double rating;

    private Integer consultCount;

    private String certNo;

    private CommonStatus status;

    private String auditRemark;

    private LocalDateTime createdAt;

    public String[] specialtyList() {
        if (specialty == null || specialty.isBlank()) {
            return new String[0];
        }
        return specialty.split("[,，、]+");
    }
}
