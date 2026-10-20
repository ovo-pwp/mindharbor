package com.mindharbor.psych.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindharbor.psych.common.BizException;
import com.mindharbor.psych.entity.Assessment;
import com.mindharbor.psych.entity.AssessmentQuestion;
import com.mindharbor.psych.entity.AssessmentRecord;
import com.mindharbor.psych.mapper.AssessmentMapper;
import com.mindharbor.psych.mapper.AssessmentQuestionMapper;
import com.mindharbor.psych.mapper.AssessmentRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentMapper assessmentMapper;
    private final AssessmentQuestionMapper questionMapper;
    private final AssessmentRecordMapper recordMapper;
    private final ObjectMapper objectMapper;

    public List<Assessment> listEnabled() {
        return assessmentMapper.selectList(Wrappers.<Assessment>lambdaQuery()
                .eq(Assessment::getEnabled, true).orderByAsc(Assessment::getId));
    }

    public List<Assessment> listAll() {
        return assessmentMapper.selectList(Wrappers.<Assessment>lambdaQuery().orderByAsc(Assessment::getId));
    }

    public Map<String, Object> detail(Long id) {
        Assessment a = assessmentMapper.selectById(id);
        if (a == null) {
            throw new BizException("量表不存在");
        }
        List<AssessmentQuestion> questions = questionMapper.selectList(
                Wrappers.<AssessmentQuestion>lambdaQuery()
                        .eq(AssessmentQuestion::getAssessmentId, id)
                        .orderByAsc(AssessmentQuestion::getSeq));
        Map<String, Object> data = new HashMap<>();
        data.put("assessment", a);
        data.put("questions", questions);
        return data;
    }

    /**
     * 提交答卷并评分。
     * SDS / SAS 均按：粗分 × 1.25 取整得标准分，再按阈值分级。
     */
    public AssessmentRecord submit(Long userId, Long assessmentId, List<Integer> answers) {
        Assessment a = assessmentMapper.selectById(assessmentId);
        if (a == null) {
            throw new BizException("量表不存在");
        }
        List<AssessmentQuestion> questions = questionMapper.selectList(
                Wrappers.<AssessmentQuestion>lambdaQuery()
                        .eq(AssessmentQuestion::getAssessmentId, assessmentId)
                        .orderByAsc(AssessmentQuestion::getSeq));
        if (answers == null || answers.size() != questions.size()) {
            throw new BizException("请完成全部 " + questions.size() + " 道题目");
        }

        int raw = 0;
        List<Integer> scored = new ArrayList<>(answers.size());
        for (int i = 0; i < questions.size(); i++) {
            Integer v = answers.get(i);
            if (v == null || v < 1 || v > 4) {
                throw new BizException("第 " + (i + 1) + " 题答案不合法");
            }
            int score = Boolean.TRUE.equals(questions.get(i).getReverse()) ? 5 - v : v;
            scored.add(score);
            raw += score;
        }

        double standard = Math.round(raw * 1.25 * 10.0) / 10.0;
        String level = resolveLevel(a, standard);

        AssessmentRecord record = new AssessmentRecord();
        record.setUserId(userId);
        record.setAssessmentId(assessmentId);
        record.setAssessmentName(a.getName());
        record.setRawScore(raw);
        record.setStandardScore(standard);
        record.setLevel(level);
        record.setSuggestion(suggestionOf(a.getCode(), level));
        try {
            record.setDetail(objectMapper.writeValueAsString(scored));
        } catch (Exception e) {
            record.setDetail("[]");
        }
        record.setCreatedAt(LocalDateTime.now());
        recordMapper.insert(record);
        return record;
    }

    public String resolveLevel(Assessment a, double standard) {
        String[] thresholds = a.thresholdArray();
        String[] levels = a.levelArray();
        if (levels.length == 0) {
            return "已完成";
        }
        for (int i = 0; i < thresholds.length && i < levels.length - 1; i++) {
            if (standard < Double.parseDouble(thresholds[i].trim())) {
                return levels[i].trim();
            }
        }
        return levels[levels.length - 1].trim();
    }

    private String suggestionOf(String code, String level) {
        boolean normal = level.contains("正常") || level.contains("无");
        if (normal) {
            return "本次测评结果在正常范围内。继续保持规律作息与适度运动，"
                    + "如果近期有明确的压力事件，可以每周做一次自评，观察变化趋势。";
        }
        if (level.contains("轻")) {
            return "结果提示存在轻度情绪困扰。建议：\n"
                    + "1. 保持规律作息，保证 7 小时以上睡眠；\n"
                    + "2. 每天 20 分钟中等强度运动，对情绪有明确改善作用；\n"
                    + "3. 尝试记录情绪日记，识别触发事件；\n"
                    + "4. 若两周后无改善，建议预约一次心理咨询。";
        }
        if (level.contains("中")) {
            return "结果提示中度情绪困扰，已超出自我调节的常见范围。建议尽快预约专业心理咨询师进行评估，"
                    + "同时保持基本作息与进食。如果出现明显的功能受损（无法工作、无法入睡），请及时就医。";
        }
        return "结果提示重度情绪困扰。**强烈建议尽快到精神卫生专科或综合医院心理科就诊**，"
                + "由专业医生进行评估。如果出现伤害自己的念头，请立即联系 400-161-9995 或前往急诊。";
    }

    public List<AssessmentRecord> myRecords(Long userId) {
        return recordMapper.selectList(Wrappers.<AssessmentRecord>lambdaQuery()
                .eq(AssessmentRecord::getUserId, userId)
                .orderByDesc(AssessmentRecord::getCreatedAt));
    }

    public AssessmentRecord recordDetail(Long userId, Long recordId) {
        AssessmentRecord r = recordMapper.selectById(recordId);
        if (r == null) {
            throw new BizException("记录不存在");
        }
        if (!r.getUserId().equals(userId)) {
            throw new BizException(403, "无权查看该记录");
        }
        return r;
    }

    public Assessment require(Long id) {
        Assessment a = assessmentMapper.selectById(id);
        if (a == null) {
            throw new BizException("量表不存在");
        }
        return a;
    }
}
