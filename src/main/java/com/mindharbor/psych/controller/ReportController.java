package com.mindharbor.psych.controller;

import com.mindharbor.psych.common.R;
import com.mindharbor.psych.dto.ReportDTO;
import com.mindharbor.psych.entity.Report;
import com.mindharbor.psych.service.AuthService;
import com.mindharbor.psych.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final AuthService authService;

    @GetMapping("/api/doctor/workbench/reports")
    public R<List<Report>> byDoctor() {
        return R.ok(reportService.listByDoctor(authService.currentUser().getId()));
    }

    @PostMapping("/api/doctor/workbench/reports")
    public R<Report> save(@RequestBody ReportDTO dto) {
        return R.ok(reportService.save(authService.currentUser().getId(), dto));
    }

    @PostMapping("/api/doctor/workbench/reports/{id}/publish")
    public R<Report> publish(@PathVariable Long id) {
        return R.ok(reportService.publish(authService.currentUser().getId(), id));
    }

    /** AI 辅助生成草稿（不落库，前端拿到后填进表单） */
    @GetMapping("/api/doctor/workbench/reports/ai-draft")
    public R<Report> aiDraft(@RequestParam Long userId,
                             @RequestParam(required = false) String name) {
        return R.ok(reportService.aiDraft(authService.currentUser().getId(), userId, name));
    }

    @GetMapping("/api/reports/mine")
    public R<List<Report>> mine() {
        return R.ok(reportService.listByPatient(authService.currentUser().getId()));
    }

    @GetMapping("/api/reports/{id}")
    public R<Report> detail(@PathVariable Long id) {
        return R.ok(reportService.detail(authService.currentUser().getId(), id));
    }
}
