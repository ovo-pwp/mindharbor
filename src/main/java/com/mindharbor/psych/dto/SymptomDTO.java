package com.mindharbor.psych.dto;

import lombok.Data;

@Data
public class SymptomDTO {

    /** 情绪 1-5 */
    private Integer mood;

    /** 严重程度 1-5 */
    private Integer severity;

    /** 精力 1-5 */
    private Integer energy;

    private String triggerEvent;

    private java.util.List<String> symptoms;

    private java.util.List<String> coping;

    private String note;

    private java.time.LocalDate recordDate;

}