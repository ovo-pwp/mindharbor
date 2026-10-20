package com.mindharbor.psych.service;

import com.mindharbor.psych.common.BizException;
import com.mindharbor.psych.dto.PasswordDTO;
import com.mindharbor.psych.dto.ProfileDTO;
import com.mindharbor.psych.entity.User;
import com.mindharbor.psych.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final AuthService authService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public User profile() {
        return authService.currentUser();
    }

    public User updateProfile(ProfileDTO dto) {
        User user = authService.currentUser();
        if (dto.getRealName() != null && !dto.getRealName().isBlank()) {
            user.setRealName(dto.getRealName().trim());
        }
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setGender(dto.getGender());
        user.setAge(dto.getAge());
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        userMapper.updateById(user);
        return user;
    }

    public void changePassword(PasswordDTO dto) {
        User user = authService.currentUser();
        if (!encoder.matches(dto.getOldPassword() == null ? "" : dto.getOldPassword(), user.getPassword())) {
            throw new BizException("原密码不正确");
        }
        if (dto.getNewPassword() == null || dto.getNewPassword().length() < 6) {
            throw new BizException("新密码至少 6 位");
        }
        user.setPassword(encoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
    }
}
