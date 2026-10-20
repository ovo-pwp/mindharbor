package com.mindharbor.psych.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article")
public class Article {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String category;

    private String cover;

    private String summary;

    /** Markdown 正文 */
    private String content;

    private String author;

    private Integer viewCount;

    private String status;

    private LocalDateTime createdAt;
}
