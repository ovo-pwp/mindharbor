package com.mindharbor.psych.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mindharbor.psych.common.BizException;
import com.mindharbor.psych.dto.LoginDTO;
import com.mindharbor.psych.dto.RegisterDTO;
import com.mindharbor.psych.entity.Doctor;
import com.mindharbor.psych.entity.User;
import com.mindharbor.psych.enums.CommonStatus;
import com.mindharbor.psych.enums.Role;
import com.mindharbor.psych.mapper.DoctorMapper;
import com.mindharbor.psych.mapper.UserMapper;
import com.mindharbor.psych.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final DoctorMapper doctorMapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public LoginVO register(RegisterDTO dto) {
        if (dto.getUsername() == null || dto.getUsername().trim().length() < 3) {
            throw new BizException("账号至少 3 个字符");
        }
        if (dto.getPassword() == null || dto.getPassword().length() < 6) {
            throw new BizException("密码至少 6 位");
        }
        Long exists = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, dto.getUsername().trim()));
        if (exists != null && exists > 0) {
            throw new BizException("该账号已被注册");
        }
        User user = new User();
        user.setUsername(dto.getUsername().trim());
        user.setPassword(encoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName() == null || dto.getRealName().isBlank()
                ? dto.getUsername().trim() : dto.getRealName().trim());
        user.setPhone(dto.getPhone());
        user.setGender(dto.getGender());
        user.setAge(dto.getAge());
        user.setRole(Role.USER);
        user.setStatus(CommonStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return doLogin(user);
    }

    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, dto.getUsername() == null ? "" : dto.getUsername().trim()));
        if (user == null || !encoder.matches(dto.getPassword() == null ? "" : dto.getPassword(), user.getPassword())) {
            throw new BizException("账号或密码不正确");
        }
        if (user.getStatus() == CommonStatus.DISABLED) {
            throw new BizException("该账号已被停用，请联系管理员");
        }
        return doLogin(user);
    }

    private LoginVO doLogin(User user) {
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();
        Long doctorId = null;
        if (user.getRole() == Role.DOCTOR) {
            Doctor doctor = doctorMapper.selectOne(
                    Wrappers.<Doctor>lambdaQuery().eq(Doctor::getUserId, user.getId()).last("limit 1"));
            doctorId = doctor == null ? null : doctor.getId();
        }
        return new LoginVO(token, user, doctorId);
    }

    public void logout() {
        StpUtil.logout();
    }

    public User currentUser() {
        User user = userMapper.selectById(StpUtil.getLoginIdAsLong());
        if (user == null) {
            throw new BizException(401, "账号不存在，请重新登录");
        }
        return user;
    }
}
