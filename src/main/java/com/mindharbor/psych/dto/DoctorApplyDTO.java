package com.mindharbor.psych.dto;

import lombok.Data;

@Data
public class DoctorApplyDTO {

    private String hospital;

    private String title;

    private String specialty;

    private String intro;

    private Integer years;

    private java.math.BigDecimal price;

    private String certNo;

}