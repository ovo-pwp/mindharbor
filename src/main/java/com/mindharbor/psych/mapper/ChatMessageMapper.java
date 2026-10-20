package com.mindharbor.psych.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindharbor.psych.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
