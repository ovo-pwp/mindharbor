package com.mindharbor.psych.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mindharbor.psych.common.BizException;
import com.mindharbor.psych.entity.MicroActionLog;
import com.mindharbor.psych.mapper.MicroActionLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 「今天的小事」。
 *
 * 设计原则（针对抑郁状态下的快感缺失与动力缺失）：
 * 1. 按当天能量给选项，选低一档不算失败
 * 2. 只记录「做过」，不记连续天数、不算完成率、不做排名
 * 3. 不出现「未完成」这个概念
 */
@Service
@RequiredArgsConstructor
public class MicroActionService {

    private final MicroActionLogMapper logMapper;

    /** 6 个能量档位：档位 -> 描述 + 可选小事 */
    private static final Map<Integer, String[]> LEVEL_META = new LinkedHashMap<>();
    private static final Map<Integer, List<String>> OPTIONS = new LinkedHashMap<>();

    static {
        LEVEL_META.put(0, new String[]{"起不来床", "只做床上能做的，别勉强"});
        LEVEL_META.put(1, new String[]{"只能撑几分钟", "动一下就算数"});
        LEVEL_META.put(2, new String[]{"能撑十几分钟", "挑一件最不费劲的"});
        LEVEL_META.put(3, new String[]{"能出门走走了", "别计划太多，做一件就回"});
        LEVEL_META.put(4, new String[]{"状态还不错", "趁现在做一件"});
        LEVEL_META.put(5, new String[]{"今天挺好", "做点什么都可以"});

        OPTIONS.put(0, List.of(
                "把窗户打开一条缝",
                "喝一杯温水",
                "坐起来靠着待 5 分钟",
                "拉开窗帘",
                "把手机充上电"
        ));
        OPTIONS.put(1, List.of(
                "洗把脸",
                "换掉身上那件衣服",
                "下楼拿个快递",
                "洗一个碗",
                "把桌上的空瓶收掉"
        ));
        OPTIONS.put(2, List.of(
                "洗个热水澡",
                "煮一碗面",
                "在阳台站 10 分钟",
                "把垃圾拎出去",
                "整理一个抽屉",
                "听完整一张专辑"
        ));
        OPTIONS.put(3, List.of(
                "楼下走 10 分钟",
                "买一样想吃的",
                "给一个人发条消息",
                "做一顿简单的饭",
                "晒 15 分钟太阳"
        ));
        OPTIONS.put(4, List.of(
                "出门见个朋友",
                "去超市逛一圈",
                "做完一件拖了很久的小事",
                "运动 20 分钟"
        ));
        OPTIONS.put(5, List.of(
                "去一个没去过的地方",
                "把今天的状态告诉一个人",
                "为明天准备一件小事"
        ));
    }

    /** 取某一档的选项（带档位说明） */
    public Map<String, Object> options(int level) {
        if (level < 0 || level > 5) {
            throw new BizException("能量档位需在 0-5 之间");
        }
        String[] meta = LEVEL_META.get(level);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("level", level);
        data.put("label", meta[0]);
        data.put("hint", meta[1]);
        data.put("items", OPTIONS.get(level));
        return data;
    }

    /** 六个档位的概要，用于前端一次性渲染 */
    public List<Map<String, Object>> levels() {
        List<Map<String, Object>> list = new ArrayList<>();
        LEVEL_META.forEach((level, meta) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("level", level);
            m.put("label", meta[0]);
            m.put("hint", meta[1]);
            m.put("count", OPTIONS.get(level).size());
            list.add(m);
        });
        return list;
    }

    public MicroActionLog log(Long userId, Integer level, String content) {
        if (level == null || level < 0 || level > 5) {
            throw new BizException("能量档位需在 0-5 之间");
        }
        if (content == null || content.isBlank()) {
            throw new BizException("请选择一件事");
        }
        MicroActionLog log = new MicroActionLog();
        log.setUserId(userId);
        log.setLevel(level);
        log.setContent(content.trim());
        log.setActionDate(LocalDate.now());
        log.setCreatedAt(LocalDateTime.now());
        logMapper.insert(log);
        return log;
    }

    /** 最近做过的事（时间流，不做统计） */
    public List<MicroActionLog> recent(Long userId, int days) {
        return logMapper.selectList(Wrappers.<MicroActionLog>lambdaQuery()
                .eq(MicroActionLog::getUserId, userId)
                .ge(MicroActionLog::getActionDate, LocalDate.now().minusDays(days))
                .orderByDesc(MicroActionLog::getId)
                .last("limit 50"));
    }

    /** 今天已经记过哪些（前端用来标记「已记下」） */
    public List<String> todayDone(Long userId) {
        return logMapper.selectList(Wrappers.<MicroActionLog>lambdaQuery()
                        .eq(MicroActionLog::getUserId, userId)
                        .eq(MicroActionLog::getActionDate, LocalDate.now()))
                .stream().map(MicroActionLog::getContent).toList();
    }
}
