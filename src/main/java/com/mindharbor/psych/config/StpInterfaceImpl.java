package com.mindharbor.psych.config;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.mindharbor.psych.entity.User;
import com.mindharbor.psych.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** 给 Sa-Token 提供角色数据源 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UserMapper userMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<String> roles = new ArrayList<>(2);
        try {
            User user = userMapper.selectById(StpUtil.getLoginIdAsLong());
            if (user != null && user.getRole() != null) {
                roles.add(user.getRole().name());
            }
        } catch (Exception ignored) {
        }
        return roles;
    }
}
