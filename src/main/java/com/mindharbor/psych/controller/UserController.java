package com.mindharbor.psych.controller;

import com.mindharbor.psych.common.R;
import com.mindharbor.psych.dto.PasswordDTO;
import com.mindharbor.psych.dto.ProfileDTO;
import com.mindharbor.psych.entity.User;
import com.mindharbor.psych.service.AuthService;
import com.mindharbor.psych.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @GetMapping("/profile")
    public R<User> profile() {
        return R.ok(userService.profile());
    }

    @PutMapping("/profile")
    public R<User> update(@RequestBody ProfileDTO dto) {
        return R.ok(userService.updateProfile(dto));
    }

    @PostMapping("/password")
    public R<Void> password(@RequestBody PasswordDTO dto) {
        userService.changePassword(dto);
        return R.ok();
    }

    @GetMapping("/me")
    public R<User> me() {
        return R.ok(authService.currentUser());
    }
}
