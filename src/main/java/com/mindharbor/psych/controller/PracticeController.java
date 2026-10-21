package com.mindharbor.psych.controller;

import com.mindharbor.psych.common.R;
import com.mindharbor.psych.entity.PracticeLog;
import com.mindharbor.psych.service.AuthService;
import com.mindharbor.psych.service.PracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 引导式练习：呼吸 / 思维记录 / 身体扫描 */
@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService service;
    private final AuthService authService;

    @GetMapping("/types")
    public R<List<Map<String, Object>>> types() {
        return R.ok(service.types());
    }

    @PostMapping("/log")
    public R<PracticeLog> log(@RequestBody Map<String, Object> body) {
        Long userId = authService.currentUser().getId();
        return R.ok(service.log(
                userId,
                body.get("type") == null ? null : String.valueOf(body.get("type")),
                body.get("duration") == null ? 0 : Integer.valueOf(String.valueOf(body.get("duration"))),
                body.get("content") == null ? null : String.valueOf(body.get("content")),
                body.get("moodBefore") == null ? null : Integer.valueOf(String.valueOf(body.get("moodBefore"))),
                body.get("moodAfter") == null ? null : Integer.valueOf(String.valueOf(body.get("moodAfter")))
        ));
    }

    @GetMapping("/recent")
    public R<List<PracticeLog>> recent(@RequestParam(defaultValue = "30") int days) {
        return R.ok(service.recent(authService.currentUser().getId(), days));
    }

    @GetMapping("/summary")
    public R<Map<String, Object>> summary() {
        return R.ok(service.summary(authService.currentUser().getId()));
    }
}
