package com.mindharbor.psych.controller;

import com.mindharbor.psych.common.R;
import com.mindharbor.psych.entity.Assessment;
import com.mindharbor.psych.entity.AssessmentRecord;
import com.mindharbor.psych.service.AssessmentService;
import com.mindharbor.psych.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final AuthService authService;

    @GetMapping
    public R<List<Assessment>> list() {
        return R.ok(assessmentService.listEnabled());
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(assessmentService.detail(id));
    }

    @PostMapping("/{id}/submit")
    public R<AssessmentRecord> submit(@PathVariable Long id, @RequestBody Map<String, List<Integer>> body) {
        return R.ok(assessmentService.submit(authService.currentUser().getId(), id, body.get("answers")));
    }

    @GetMapping("/records/mine")
    public R<List<AssessmentRecord>> myRecords() {
        return R.ok(assessmentService.myRecords(authService.currentUser().getId()));
    }

    @GetMapping("/records/{recordId}")
    public R<AssessmentRecord> record(@PathVariable Long recordId) {
        return R.ok(assessmentService.recordDetail(authService.currentUser().getId(), recordId));
    }
}
