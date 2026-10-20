package com.mindharbor.psych.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mindharbor.psych.entity.ActionLog;
import com.mindharbor.psych.entity.Resource;
import com.mindharbor.psych.enums.CommonStatus;
import com.mindharbor.psych.mapper.ActionLogMapper;
import com.mindharbor.psych.mapper.ResourceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基于用户的协同过滤推荐（User-CF）。
 * 步骤：构建用户-资源评分矩阵 → 余弦相似度找邻居 → 加权预测未交互资源 → 排序输出。
 * 数据不足时退化为热门推荐（冷启动）。
 */
@Service
@RequiredArgsConstructor
public class RecommendService {

    private final ActionLogMapper actionLogMapper;
    private final ResourceMapper resourceMapper;

    public List<Resource> recommend(Long userId, int limit) {
        List<ActionLog> logs = actionLogMapper.selectList(Wrappers.<ActionLog>lambdaQuery()
                .eq(ActionLog::getTargetType, "RESOURCE"));

        // user -> (resourceId -> score)
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();
        for (ActionLog log : logs) {
            double score = log.getWeight() == null ? 1.0 : log.getWeight();
            matrix.computeIfAbsent(log.getUserId(), k -> new HashMap<>())
                    .merge(log.getTargetId(), score, Double::sum);
        }

        Map<Long, Double> mine = matrix.getOrDefault(userId, Map.of());
        Map<Long, Double> predicted = new HashMap<>();

        if (!mine.isEmpty() && matrix.size() > 1) {
            List<Map.Entry<Long, Double>> similarities = new ArrayList<>();
            for (Map.Entry<Long, Map<Long, Double>> e : matrix.entrySet()) {
                if (e.getKey().equals(userId)) {
                    continue;
                }
                double sim = cosine(mine, e.getValue());
                if (sim > 0) {
                    similarities.add(Map.entry(e.getKey(), sim));
                }
            }
            similarities.sort(Comparator.comparingDouble((Map.Entry<Long, Double> x) -> x.getValue()).reversed());
            List<Map.Entry<Long, Double>> neighbors = similarities.subList(0, Math.min(8, similarities.size()));

            for (Map.Entry<Long, Double> n : neighbors) {
                for (Map.Entry<Long, Double> item : matrix.get(n.getKey()).entrySet()) {
                    if (mine.containsKey(item.getKey())) {
                        continue;
                    }
                    predicted.merge(item.getKey(), n.getValue() * item.getValue(), Double::sum);
                }
            }
        }

        List<Resource> out = new ArrayList<>();
        Set<Long> picked = new HashSet<>();
        if (!predicted.isEmpty()) {
            predicted.entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(limit * 2L)
                    .forEach(e -> {
                        Resource r = resourceMapper.selectById(e.getKey());
                        if (r != null && r.getStatus() == CommonStatus.APPROVED && picked.add(r.getId())) {
                            out.add(r);
                        }
                    });
        }

        // 冷启动 / 补齐：按浏览量取热门
        if (out.size() < limit) {
            List<Resource> hot = resourceMapper.selectList(Wrappers.<Resource>lambdaQuery()
                    .eq(Resource::getStatus, CommonStatus.APPROVED)
                    .orderByDesc(Resource::getViewCount)
                    .last("limit " + (limit * 2)));
            for (Resource r : hot) {
                if (out.size() >= limit) {
                    break;
                }
                if (picked.add(r.getId())) {
                    out.add(r);
                }
            }
        }
        return out.size() > limit ? out.subList(0, limit) : out;
    }

    /** 余弦相似度 */
    private double cosine(Map<Long, Double> a, Map<Long, Double> b) {
        double dot = 0;
        double na = 0;
        double nb = 0;
        for (Map.Entry<Long, Double> e : a.entrySet()) {
            na += e.getValue() * e.getValue();
            Double other = b.get(e.getKey());
            if (other != null) {
                dot += e.getValue() * other;
            }
        }
        for (double v : b.values()) {
            nb += v * v;
        }
        if (na == 0 || nb == 0) {
            return 0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    /** 推荐说明，前端展示「为什么推荐」 */
    public Map<Long, String> reasons(Long userId, List<Resource> list) {
        Map<Long, String> map = new LinkedHashMap<>();
        boolean hasHistory = actionLogMapper.selectCount(Wrappers.<ActionLog>lambdaQuery()
                .eq(ActionLog::getUserId, userId)
                .eq(ActionLog::getTargetType, "RESOURCE")) > 0;
        for (Resource r : list) {
            map.put(r.getId(), hasHistory ? "和你兴趣相近的人也在看" : "近期热门资源");
        }
        return map;
    }
}
