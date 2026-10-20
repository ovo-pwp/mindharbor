package com.mindharbor.psych.controller;

import com.mindharbor.psych.common.R;
import com.mindharbor.psych.entity.MicroActionLog;
import com.mindharbor.psych.service.AuthService;
import com.mindharbor.psych.service.MicroActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 「今天的小事」：按能量档位选一件小事，做完记一笔 */
@RestController
@RequestMapping("/api/micro")
@RequiredArgsConstructor
public class MicroActionController {

    private final MicroActionService service;
    private final AuthService authService;

    @GetMapping("/levels")
    public R<List<Map<String, Object>>> levels() {
        return R.ok(service.levels());
    }

    @GetMapping("/options")
    public R<Map<String, Object>> options(@RequestParam(defaultValue = "2") int level) {
        return R.ok(service.options(level));
    }

    @PostMapping("/log")
    public R<MicroActionLog> log(@RequestBody Map<String, Object> body) {
        Long userId = authService.currentUser().getId();
        Integer level = body.get("level") == null ? 2 : Integer.valueOf(String.valueOf(body.get("level")));
        return R.ok(service.log(userId, level, String.valueOf(body.getOrDefault("content", ""))));
    }

    @GetMapping("/recent")
    public R<List<MicroActionLog>> recent(@RequestParam(defaultValue = "14") int days) {
        return R.ok(service.recent(authService.currentUser().getId(), days));
    }

    @GetMapping("/today")
    public R<List<String>> today() {
        return R.ok(service.todayDone(authService.currentUser().getId()));
    }
}
