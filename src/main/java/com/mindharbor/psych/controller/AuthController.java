package com.mindharbor.psych.controller;

import com.mindharbor.psych.common.R;
import com.mindharbor.psych.dto.LoginDTO;
import com.mindharbor.psych.dto.RegisterDTO;
import com.mindharbor.psych.entity.User;
import com.mindharbor.psych.service.AuthService;
import com.mindharbor.psych.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public R<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        return R.ok(authService.register(dto));
    }

    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    /** 用 token 换当前用户信息（前端刷新时调用） */
    @GetMapping("/me")
    public R<User> me() {
        return R.ok(authService.currentUser());
    }
}
