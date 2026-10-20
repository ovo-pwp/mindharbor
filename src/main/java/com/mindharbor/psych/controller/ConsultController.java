package com.mindharbor.psych.controller;

import com.mindharbor.psych.common.R;
import com.mindharbor.psych.dto.BindApplyDTO;
import com.mindharbor.psych.dto.BindAuditDTO;
import com.mindharbor.psych.dto.MessageDTO;
import com.mindharbor.psych.entity.ChatMessage;
import com.mindharbor.psych.entity.DoctorPatient;
import com.mindharbor.psych.service.AuthService;
import com.mindharbor.psych.service.ConsultService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ConsultController {

    private final ConsultService consultService;
    private final AuthService authService;

    // ---------- 患者端 ----------

    @PostMapping("/api/consult/apply")
    public R<DoctorPatient> apply(@RequestBody BindApplyDTO dto) {
        return R.ok(consultService.apply(authService.currentUser().getId(), dto));
    }

    @GetMapping("/api/consult/my-doctors")
    public R<List<Map<String, Object>>> myDoctors() {
        return R.ok(consultService.myDoctors(authService.currentUser().getId()));
    }

    @GetMapping("/api/consult/conversations")
    public R<List<Map<String, Object>>> conversations() {
        return R.ok(consultService.conversations(authService.currentUser().getId()));
    }

    @GetMapping("/api/consult/history/{otherId}")
    public R<List<ChatMessage>> history(@PathVariable Long otherId,
                                        @RequestParam(defaultValue = "100") int limit) {
        return R.ok(consultService.history(authService.currentUser().getId(), otherId, limit));
    }

    @PostMapping("/api/consult/messages")
    public R<ChatMessage> send(@RequestBody MessageDTO dto) {
        return R.ok(consultService.send(authService.currentUser().getId(), dto));
    }

    // ---------- 医生端 ----------

    @GetMapping("/api/doctor/workbench/applications")
    public R<List<Map<String, Object>>> applications() {
        return R.ok(consultService.pendingApplications(authService.currentUser().getId()));
    }

    @GetMapping("/api/doctor/workbench/patients")
    public R<List<Map<String, Object>>> patients() {
        return R.ok(consultService.patients(authService.currentUser().getId()));
    }

    @GetMapping("/api/doctor/workbench/patients/{userId}/overview")
    public R<Map<String, Object>> patientOverview(@PathVariable Long userId) {
        return R.ok(consultService.patientOverview(authService.currentUser().getId(), userId));
    }

    @PostMapping("/api/doctor/workbench/audit")
    public R<DoctorPatient> audit(@RequestBody BindAuditDTO dto) {
        return R.ok(consultService.audit(authService.currentUser().getId(), dto));
    }
}
