package com.mindharbor.psych.controller;

import com.mindharbor.psych.common.R;
import com.mindharbor.psych.dto.SymptomDTO;
import com.mindharbor.psych.entity.SymptomRecord;
import com.mindharbor.psych.service.AuthService;
import com.mindharbor.psych.service.SymptomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/symptoms")
@RequiredArgsConstructor
public class SymptomController {

    private final SymptomService symptomService;
    private final AuthService authService;

    @PostMapping
    public R<SymptomRecord> add(@RequestBody SymptomDTO dto) {
        return R.ok(symptomService.add(authService.currentUser().getId(), dto));
    }

    @GetMapping
    public R<List<SymptomRecord>> list(@RequestParam(defaultValue = "30") int days) {
        Long userId = authService.currentUser().getId();
        return R.ok(days <= 0 ? symptomService.all(userId) : symptomService.list(userId, days));
    }

    @GetMapping("/trend")
    public R<Map<String, Object>> trend(@RequestParam(defaultValue = "30") int days) {
        return R.ok(symptomService.trend(authService.currentUser().getId(), days));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        symptomService.delete(authService.currentUser().getId(), id);
        return R.ok();
    }
}
