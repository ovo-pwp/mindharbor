package com.mindharbor.psych.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mindharbor.psych.common.BizException;
import com.mindharbor.psych.entity.PracticeLog;
import com.mindharbor.psych.mapper.PracticeLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 引导式练习。
 *
 * 设计原则：练习是「做」不是「打卡」——不记连续天数、不算完成率、不排名。
 * 中途退出也算一次记录（很多人在第 2 轮就走神了，这很正常）。
 */
@Service
@RequiredArgsConstructor
public class PracticeService {

    private final PracticeLogMapper logMapper;

    public static final String BREATHING = "BREATHING";
    public static final String THOUGHT_RECORD = "THOUGHT_RECORD";
    public static final String BODY_SCAN = "BODY_SCAN";

    private static final List<String> VALID = List.of(BREATHING, THOUGHT_RECORD, BODY_SCAN);

    /** 三种练习的元数据，前端据此渲染卡片 */
    public List<Map<String, Object>> types() {
        List<Map<String, Object>> list = new ArrayList<>();

        Map<String, Object> a = new LinkedHashMap<>();
        a.put("type", BREATHING);
        a.put("name", "4-7-8 呼吸");
        a.put("desc", "吸气 4 秒、屏息 7 秒、呼气 8 秒。呼气比吸气长，能直接给神经系统踩刹车。");
        a.put("minutes", 3);
        a.put("hint", "跟着圆圈做，撑不住随时停");
        list.add(a);

        Map<String, Object> b = new LinkedHashMap<>();
        b.put("type", THOUGHT_RECORD);
        b.put("name", "思维记录");
        b.put("desc", "把脑子里那句话摊开看：发生了什么、我当时想什么、证据支持还是反对、换个说法会怎样。");
        b.put("minutes", 8);
        b.put("hint", "写不下去就跳过，不用每条都填满");
        list.add(b);

        Map<String, Object> c = new LinkedHashMap<>();
        c.put("type", BODY_SCAN);
        c.put("name", "身体扫描");
        c.put("desc", "从脚趾到额头，逐段把注意力放过去。不用改变什么，只是注意到。");
        c.put("minutes", 6);
        c.put("hint", "躺着做更容易，睡着了也没关系");
        list.add(c);

        return list;
    }

    public PracticeLog log(Long userId, String type, Integer duration, String content,
                           Integer moodBefore, Integer moodAfter) {
        if (type == null || !VALID.contains(type)) {
            throw new BizException("未知的练习类型");
        }
        PracticeLog log = new PracticeLog();
        log.setUserId(userId);
        log.setType(type);
        log.setDuration(duration == null ? 0 : Math.max(0, duration));
        log.setContent(content == null ? null : content.substring(0, Math.min(content.length(), 3900)));
        log.setMoodBefore(moodBefore);
        log.setMoodAfter(moodAfter);
        log.setCreatedAt(LocalDateTime.now());
        logMapper.insert(log);
        return log;
    }

    public List<PracticeLog> recent(Long userId, int days) {
        return logMapper.selectList(Wrappers.<PracticeLog>lambdaQuery()
                .eq(PracticeLog::getUserId, userId)
                .ge(PracticeLog::getCreatedAt, LocalDate.now().minusDays(days).atStartOfDay())
                .orderByDesc(PracticeLog::getId)
                .last("limit 50"));
    }

    /** 各类练习的次数，仅用于页面上的一句话小结，不做目标值 */
    public Map<String, Object> summary(Long userId) {
        List<PracticeLog> all = logMapper.selectList(Wrappers.<PracticeLog>lambdaQuery()
                .eq(PracticeLog::getUserId, userId)
                .ge(PracticeLog::getCreatedAt, LocalDate.now().minusDays(30).atStartOfDay()));

        Map<String, Long> byType = new LinkedHashMap<>();
        for (String t : VALID) {
            byType.put(t, all.stream().filter(l -> t.equals(l.getType())).count());
        }
        int totalMinutes = all.stream()
                .mapToInt(l -> l.getDuration() == null ? 0 : l.getDuration())
                .sum() / 60;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("byType", byType);
        data.put("totalCount", all.size());
        data.put("totalMinutes", totalMinutes);
        data.put("days", 30);
        return data;
    }
}
