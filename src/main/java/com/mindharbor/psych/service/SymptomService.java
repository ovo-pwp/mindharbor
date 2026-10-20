package com.mindharbor.psych.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mindharbor.psych.common.BizException;
import com.mindharbor.psych.dto.SymptomDTO;
import com.mindharbor.psych.entity.SymptomRecord;
import com.mindharbor.psych.mapper.SymptomRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SymptomService {

    private final SymptomRecordMapper symptomMapper;

    public SymptomRecord add(Long userId, SymptomDTO dto) {
        if (dto.getMood() == null || dto.getMood() < 1 || dto.getMood() > 5) {
            throw new BizException("情绪评分需在 1-5 之间");
        }
        SymptomRecord r = new SymptomRecord();
        r.setUserId(userId);
        r.setMood(dto.getMood());
        r.setSeverity(dto.getSeverity() == null ? 3 : dto.getSeverity());
        r.setEnergy(dto.getEnergy() == null ? 3 : dto.getEnergy());
        r.setTriggerEvent(dto.getTriggerEvent());
        r.setSymptoms(dto.getSymptoms() == null ? "" : String.join(",", dto.getSymptoms()));
        r.setCoping(dto.getCoping() == null ? "" : String.join(",", dto.getCoping()));
        r.setNote(dto.getNote());
        r.setRecordDate(dto.getRecordDate() == null ? LocalDate.now() : dto.getRecordDate());
        r.setCreatedAt(LocalDateTime.now());
        symptomMapper.insert(r);
        return r;
    }

    public List<SymptomRecord> list(Long userId, int days) {
        LocalDate from = LocalDate.now().minusDays(days);
        return symptomMapper.selectList(Wrappers.<SymptomRecord>lambdaQuery()
                .eq(SymptomRecord::getUserId, userId)
                .ge(SymptomRecord::getRecordDate, from)
                .orderByDesc(SymptomRecord::getRecordDate)
                .orderByDesc(SymptomRecord::getId));
    }

    public List<SymptomRecord> all(Long userId) {
        return symptomMapper.selectList(Wrappers.<SymptomRecord>lambdaQuery()
                .eq(SymptomRecord::getUserId, userId)
                .orderByDesc(SymptomRecord::getRecordDate)
                .orderByDesc(SymptomRecord::getId));
    }

    /** 趋势数据：按日期升序，供 ECharts 折线图使用 */
    public Map<String, Object> trend(Long userId, int days) {
        LocalDate from = LocalDate.now().minusDays(days);
        List<SymptomRecord> records = symptomMapper.selectList(Wrappers.<SymptomRecord>lambdaQuery()
                .eq(SymptomRecord::getUserId, userId)
                .ge(SymptomRecord::getRecordDate, from)
                .orderByAsc(SymptomRecord::getRecordDate));
        List<String> dates = new ArrayList<>();
        List<Integer> mood = new ArrayList<>();
        List<Integer> severity = new ArrayList<>();
        List<Integer> energy = new ArrayList<>();
        for (SymptomRecord r : records) {
            dates.add(r.getRecordDate().toString());
            mood.add(r.getMood());
            severity.add(r.getSeverity());
            energy.add(r.getEnergy());
        }
        Map<String, Object> data = new HashMap<>();
        data.put("dates", dates);
        data.put("mood", mood);
        data.put("severity", severity);
        data.put("energy", energy);
        data.put("count", records.size());
        data.put("avgMood", mood.stream().mapToInt(Integer::intValue).average().orElse(0));
        data.put("avgSeverity", severity.stream().mapToInt(Integer::intValue).average().orElse(0));
        return data;
    }

    public void delete(Long userId, Long id) {
        SymptomRecord r = symptomMapper.selectById(id);
        if (r == null || !r.getUserId().equals(userId)) {
            throw new BizException("记录不存在");
        }
        symptomMapper.deleteById(id);
    }
}
