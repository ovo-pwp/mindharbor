package com.mindharbor.psych.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindharbor.psych.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
