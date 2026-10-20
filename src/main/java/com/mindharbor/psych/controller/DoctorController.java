package com.mindharbor.psych.controller;

import com.mindharbor.psych.common.R;
import com.mindharbor.psych.dto.DoctorApplyDTO;
import com.mindharbor.psych.entity.Doctor;
import com.mindharbor.psych.service.AuthService;
import com.mindharbor.psych.service.DoctorService;
import com.mindharbor.psych.vo.DoctorVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;
    private final AuthService authService;

    @GetMapping
    public R<Map<String, Object>> list(@RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String specialty,
                                       @RequestParam(defaultValue = "1") long page,
                                       @RequestParam(defaultValue = "9") long size) {
        return R.ok(doctorService.page(keyword, specialty, page, size));
    }

    @GetMapping("/{id}")
    public R<DoctorVO> detail(@PathVariable Long id) {
        return R.ok(doctorService.detail(id));
    }

    /** 提交入驻申请（普通用户） */
    @PostMapping("/apply")
    public R<Doctor> apply(@RequestBody DoctorApplyDTO dto) {
        return R.ok(doctorService.apply(dto));
    }

    /** 查看自己的入驻状态 */
    @GetMapping("/my-apply")
    public R<Doctor> myApply() {
        return R.ok(doctorService.myDoctorProfile(authService.currentUser().getId()));
    }
}
