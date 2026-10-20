package com.mindharbor.psych.controller;

import com.mindharbor.psych.common.R;
import com.mindharbor.psych.dto.ArticleDTO;
import com.mindharbor.psych.dto.NoticeDTO;
import com.mindharbor.psych.entity.*;
import com.mindharbor.psych.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public R<Map<String, Object>> dashboard() {
        return R.ok(adminService.dashboard());
    }

    // ---------- 用户 ----------

    @GetMapping("/users")
    public R<Map<String, Object>> users(@RequestParam(defaultValue = "1") long page,
                                        @RequestParam(defaultValue = "10") long size,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String role) {
        return R.ok(adminService.users(page, size, keyword, role));
    }

    @PostMapping("/users/{id}/toggle")
    public R<Void> toggle(@PathVariable Long id) {
        adminService.toggleUser(id);
        return R.ok();
    }

    // ---------- 医生审核 ----------

    @GetMapping("/doctors")
    public R<List<Map<String, Object>>> doctors(@RequestParam(required = false) String status) {
        return R.ok(adminService.doctors(status));
    }

    @PostMapping("/doctors/{id}/audit")
    public R<Void> auditDoctor(@PathVariable Long id, @RequestBody Map<String, String> body) {
        adminService.auditDoctor(id, body.get("status"), body.get("remark"));
        return R.ok();
    }

    // ---------- 量表 ----------

    @GetMapping("/assessments")
    public R<List<Assessment>> assessments() {
        return R.ok(adminService.assessments());
    }

    @PostMapping("/assessments")
    public R<Assessment> saveAssessment(@RequestBody Assessment assessment) {
        return R.ok(adminService.saveAssessment(assessment));
    }

    @GetMapping("/assessments/{id}/questions")
    public R<List<AssessmentQuestion>> questions(@PathVariable Long id) {
        return R.ok(adminService.questions(id));
    }

    @PostMapping("/questions")
    public R<Void> saveQuestion(@RequestBody AssessmentQuestion question) {
        adminService.saveQuestion(question);
        return R.ok();
    }

    @DeleteMapping("/questions/{id}")
    public R<Void> deleteQuestion(@PathVariable Long id) {
        adminService.deleteQuestion(id);
        return R.ok();
    }

    // ---------- 科普 ----------

    @GetMapping("/articles")
    public R<List<Article>> articles() {
        return R.ok(adminService.articles());
    }

    @PostMapping("/articles")
    public R<Article> saveArticle(@RequestBody ArticleDTO dto) {
        return R.ok(adminService.saveArticle(dto));
    }

    @DeleteMapping("/articles/{id}")
    public R<Void> deleteArticle(@PathVariable Long id) {
        adminService.deleteArticle(id);
        return R.ok();
    }

    // ---------- 公告 ----------

    @GetMapping("/notices")
    public R<List<Notice>> notices() {
        return R.ok(adminService.notices());
    }

    @PostMapping("/notices")
    public R<Notice> saveNotice(@RequestBody NoticeDTO dto) {
        return R.ok(adminService.saveNotice(dto));
    }

    @DeleteMapping("/notices/{id}")
    public R<Void> deleteNotice(@PathVariable Long id) {
        adminService.deleteNotice(id);
        return R.ok();
    }

    // ---------- 资源审核 ----------

    @GetMapping("/resources")
    public R<List<Resource>> resources(@RequestParam(required = false) String status) {
        return R.ok(adminService.resources(status));
    }

    @PostMapping("/resources/{id}/audit")
    public R<Void> auditResource(@PathVariable Long id, @RequestBody Map<String, String> body) {
        adminService.auditResource(id, body.get("status"));
        return R.ok();
    }
}
