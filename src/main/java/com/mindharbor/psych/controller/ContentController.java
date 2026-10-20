package com.mindharbor.psych.controller;

import com.mindharbor.psych.common.R;
import com.mindharbor.psych.dto.ResourceDTO;
import com.mindharbor.psych.entity.Article;
import com.mindharbor.psych.entity.Notice;
import com.mindharbor.psych.entity.Resource;
import com.mindharbor.psych.service.AuthService;
import com.mindharbor.psych.service.ContentService;
import com.mindharbor.psych.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;
    private final RecommendService recommendService;
    private final AuthService authService;

    @GetMapping("/api/content/resources")
    public R<Map<String, Object>> resources(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "12") long size,
                                            @RequestParam(required = false) String category,
                                            @RequestParam(required = false) String type,
                                            @RequestParam(required = false) String keyword) {
        return R.ok(contentService.resources(page, size, category, type, keyword));
    }

    @GetMapping("/api/content/resources/{id}")
    public R<Resource> resource(@PathVariable Long id) {
        return R.ok(contentService.resourceDetail(authService.currentUser().getId(), id));
    }

    @GetMapping("/api/content/articles")
    public R<Map<String, Object>> articles(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "8") long size,
                                            @RequestParam(required = false) String category,
                                            @RequestParam(required = false) String keyword) {
        return R.ok(contentService.articles(page, size, category, keyword));
    }

    @GetMapping("/api/content/articles/{id}")
    public R<Article> article(@PathVariable Long id) {
        return R.ok(contentService.articleDetail(id));
    }

    @GetMapping("/api/content/notices")
    public R<List<Notice>> notices() {
        return R.ok(contentService.notices());
    }

    @GetMapping("/api/content/recommend")
    public R<List<Resource>> recommend(@RequestParam(defaultValue = "4") int limit) {
        return R.ok(recommendService.recommend(authService.currentUser().getId(), limit));
    }

    // ---------- 医生端：资源发布 ----------

    @PostMapping("/api/doctor/workbench/resources")
    public R<Resource> publish(@RequestBody ResourceDTO dto) {
        return R.ok(contentService.publish(authService.currentUser().getId(), dto));
    }

    @GetMapping("/api/doctor/workbench/resources")
    public R<List<Resource>> myResources() {
        return R.ok(contentService.myResources(authService.currentUser().getId()));
    }
}
