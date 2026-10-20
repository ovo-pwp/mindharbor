package com.mindharbor.psych.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mindharbor.psych.ai.QwenClient;
import com.mindharbor.psych.common.BizException;
import com.mindharbor.psych.dto.ReportDTO;
import com.mindharbor.psych.entity.AssessmentRecord;
import com.mindharbor.psych.entity.Doctor;
import com.mindharbor.psych.entity.Report;
import com.mindharbor.psych.entity.SymptomRecord;
import com.mindharbor.psych.mapper.AssessmentRecordMapper;
import com.mindharbor.psych.mapper.ReportMapper;
import com.mindharbor.psych.mapper.SymptomRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportMapper reportMapper;
    private final SymptomRecordMapper symptomMapper;
    private final AssessmentRecordMapper assessmentRecordMapper;
    private final DoctorService doctorService;
    private final QwenClient qwenClient;

    public List<Report> listByDoctor(Long doctorUserId) {
        Doctor doctor = doctorService.requireDoctor(doctorUserId);
        return reportMapper.selectList(Wrappers.<Report>lambdaQuery()
                .eq(Report::getDoctorId, doctor.getId())
                .orderByDesc(Report::getCreatedAt));
    }

    public List<Report> listByPatient(Long userId) {
        return reportMapper.selectList(Wrappers.<Report>lambdaQuery()
                .eq(Report::getUserId, userId)
                .eq(Report::getStatus, "PUBLISHED")
                .orderByDesc(Report::getPublishedAt));
    }

    public Report detail(Long currentUserId, Long reportId) {
        Report r = reportMapper.selectById(reportId);
        if (r == null) {
            throw new BizException("报告不存在");
        }
        if (!r.getUserId().equals(currentUserId)) {
            Doctor doctor = doctorService.myDoctorProfile(currentUserId);
            if (doctor == null || !doctor.getId().equals(r.getDoctorId())) {
                throw new BizException(403, "无权查看该报告");
            }
        }
        return r;
    }

    @Transactional
    public Report save(Long doctorUserId, ReportDTO dto) {
        Doctor doctor = doctorService.requireDoctor(doctorUserId);
        if (dto.getUserId() == null) {
            throw new BizException("请选择报告对应的患者");
        }
        Report report;
        if (dto.getId() != null) {
            report = reportMapper.selectById(dto.getId());
            if (report == null || !report.getDoctorId().equals(doctor.getId())) {
                throw new BizException("报告不存在或无权限");
            }
            if ("PUBLISHED".equals(report.getStatus())) {
                throw new BizException("已发布的报告不能再修改");
            }
        } else {
            report = new Report();
            report.setDoctorId(doctor.getId());
            report.setUserId(dto.getUserId());
            report.setStatus("DRAFT");
            report.setCreatedAt(LocalDateTime.now());
        }
        report.setTitle(dto.getTitle() == null || dto.getTitle().isBlank()
                ? "心理评估报告" : dto.getTitle());
        report.setOverview(dto.getOverview());
        report.setInsight(dto.getInsight());
        report.setRiskWarning(dto.getRiskWarning());
        report.setSuggestion(dto.getSuggestion());
        if (report.getId() == null) {
            reportMapper.insert(report);
        } else {
            reportMapper.updateById(report);
        }
        return report;
    }

    @Transactional
    public Report publish(Long doctorUserId, Long reportId) {
        Doctor doctor = doctorService.requireDoctor(doctorUserId);
        Report report = reportMapper.selectById(reportId);
        if (report == null || !report.getDoctorId().equals(doctor.getId())) {
            throw new BizException("报告不存在或无权限");
        }
        report.setStatus("PUBLISHED");
        report.setPublishedAt(LocalDateTime.now());
        reportMapper.updateById(report);
        return report;
    }

    /**
     * AI 辅助生成报告草稿：汇总近 30 天症状记录与测评历史，交给大模型写成四段式报告。
     * 未配置 API Key 时用统计模板生成，保证功能可用。
     */
    public Report aiDraft(Long doctorUserId, Long patientUserId, String patientName) {
        Doctor doctor = doctorService.requireDoctor(doctorUserId);
        LocalDate from = LocalDate.now().minusDays(30);
        List<SymptomRecord> symptoms = symptomMapper.selectList(Wrappers.<SymptomRecord>lambdaQuery()
                .eq(SymptomRecord::getUserId, patientUserId)
                .ge(SymptomRecord::getRecordDate, from)
                .orderByAsc(SymptomRecord::getRecordDate));
        List<AssessmentRecord> assessments = assessmentRecordMapper.selectList(
                Wrappers.<AssessmentRecord>lambdaQuery()
                        .eq(AssessmentRecord::getUserId, patientUserId)
                        .orderByDesc(AssessmentRecord::getCreatedAt)
                        .last("limit 5"));

        double avgMood = symptoms.stream().mapToInt(SymptomRecord::getMood).average().orElse(0);
        double avgSeverity = symptoms.stream().mapToInt(SymptomRecord::getSeverity).average().orElse(0);
        double avgEnergy = symptoms.stream().mapToInt(SymptomRecord::getEnergy).average().orElse(0);
        String triggers = symptoms.stream()
                .map(SymptomRecord::getTriggerEvent)
                .filter(s -> s != null && !s.isBlank())
                .distinct().limit(5).collect(Collectors.joining("；"));
        String assessText = assessments.isEmpty() ? "近 30 天无量表测评记录"
                : assessments.stream()
                .map(a -> a.getAssessmentName() + "（标准分 " + a.getStandardScore() + "，" + a.getLevel() + "）")
                .collect(Collectors.joining("；"));

        Report report = new Report();
        report.setDoctorId(doctor.getId());
        report.setUserId(patientUserId);
        report.setTitle((patientName == null ? "患者" : patientName) + " · 阶段性心理评估报告");
        report.setStatus("DRAFT");
        report.setCreatedAt(LocalDateTime.now());

        if (qwenClient.available()) {
            String prompt = """
                    患者姓名：%s
                    近 30 天症状记录：%d 条；平均情绪 %,.1f/5，平均困扰程度 %.1f/5，平均精力 %.1f/5
                    主要触发事件：%s
                    测评历史：%s

                    请输出一份阶段性心理评估报告，严格按以下四个标题分段，每段 120-200 字，语气专业克制，不做医学诊断：
                    【总体概述】
                    【数据洞察】
                    【风险预警】
                    【改善建议】
                    """.formatted(patientName, symptoms.size(), avgMood, avgSeverity, avgEnergy,
                    triggers.isBlank() ? "未记录" : triggers, assessText);
            String text = qwenClient.complete(
                    "你是一位资深心理咨询师，擅长根据来访者的情绪记录与量表数据撰写阶段性评估报告。",
                    prompt);
            if (text != null && !text.isBlank()) {
                String[] sections = splitSections(text);
                report.setOverview(sections[0]);
                report.setInsight(sections[1]);
                report.setRiskWarning(sections[2]);
                report.setSuggestion(sections[3]);
                return report;
            }
        }

        // 本地统计模板
        report.setOverview("本次报告覆盖 %s 近 30 天的情绪与症状记录，共 %d 条自评数据，以及 %d 次量表测评。"
                .formatted(patientName, symptoms.size(), assessments.size()));
        report.setInsight(symptoms.isEmpty()
                ? "近 30 天暂无症状记录，数据不足以形成趋势判断。建议先建立每日情绪记录习惯，再复评。"
                : "平均情绪评分 %.1f/5，平均困扰程度 %.1f/5，平均精力 %.1f/5。触发事件集中在：%s。"
                .formatted(avgMood, avgSeverity, avgEnergy, triggers.isBlank() ? "未明确记录" : triggers));
        boolean high = avgSeverity >= 4 || assessments.stream().anyMatch(a -> a.getLevel().contains("重"));
        report.setRiskWarning(high
                ? "困扰程度处于较高区间，或量表结果提示中重度水平，建议提高随访频率，必要时转介精神科评估。"
                : "目前未见明显高风险信号。若出现睡眠显著恶化、持续两周以上的情绪低落或自伤念头，需立即升级干预。");
        report.setSuggestion("1. 保持每日情绪记录，重点关注触发事件与应对方式的对应关系；\n"
                + "2. 每周至少 3 次、每次 30 分钟的中等强度运动；\n"
                + "3. 针对高频触发场景做一次认知重构练习；\n"
                + "4. 两周后复测量表，对比标准分变化。");
        return report;
    }

    private String[] splitSections(String text) {
        String[] keys = {"【总体概述】", "【数据洞察】", "【风险预警】", "【改善建议】"};
        String[] out = new String[4];
        for (int i = 0; i < keys.length; i++) {
            int start = text.indexOf(keys[i]);
            if (start < 0) {
                out[i] = i == 0 ? text : "";
                continue;
            }
            start += keys[i].length();
            int end = text.length();
            for (int j = i + 1; j < keys.length; j++) {
                int next = text.indexOf(keys[j]);
                if (next > start) {
                    end = next;
                    break;
                }
            }
            out[i] = text.substring(start, end).trim();
        }
        return out;
    }
}
