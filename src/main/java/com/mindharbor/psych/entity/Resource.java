package com.mindharbor.psych.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mindharbor.psych.enums.CommonStatus;
import com.mindharbor.psych.enums.ResourceType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("resource")
public class Resource {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private ResourceType type;

    private String category;

    private String cover;

    private String url;

    private String content;

    private Long uploaderId;

    private CommonStatus status;

    private Integer viewCount;

    private LocalDateTime createdAt;
}
