package com.mindharbor.psych.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mindharbor.psych.enums.BindStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("doctor_patient")
public class DoctorPatient {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long doctorId;

    private Long userId;

    private BindStatus status;

    private String applyReason;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
