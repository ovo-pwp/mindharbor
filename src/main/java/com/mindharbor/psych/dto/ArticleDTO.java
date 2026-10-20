package com.mindharbor.psych.dto;

import lombok.Data;

@Data
public class ArticleDTO {

    private Long id;

    private String title;

    private String category;

    private String cover;

    private String summary;

    private String content;

    private String author;

}