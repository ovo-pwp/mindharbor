package com.mindharbor.psych.dto;

import lombok.Data;

@Data
public class ReportDTO {

    private Long id;

    private Long userId;

    private String title;

    private String overview;

    private String insight;

    private String riskWarning;

    private String suggestion;

    private String status;

}