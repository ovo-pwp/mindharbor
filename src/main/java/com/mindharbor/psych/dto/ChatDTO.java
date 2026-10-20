package com.mindharbor.psych.dto;

import lombok.Data;

@Data
public class ChatDTO {

    /** 为空则新建会话 */
    private Long sessionId;

    private String content;

}