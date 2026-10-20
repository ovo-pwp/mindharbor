package com.mindharbor.psych.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mindharbor.psych.common.BizException;
import com.mindharbor.psych.dto.ResourceDTO;
import com.mindharbor.psych.entity.ActionLog;
import com.mindharbor.psych.entity.Article;
import com.mindharbor.psych.entity.Doctor;
import com.mindharbor.psych.entity.Notice;
import com.mindharbor.psych.entity.Resource;
import com.mindharbor.psych.enums.CommonStatus;
import com.mindharbor.psych.enums.ResourceType;
import com.mindharbor.psych.mapper.ActionLogMapper;
import com.mindharbor.psych.mapper.ArticleMapper;
import com.mindharbor.psych.mapper.NoticeMapper;
import com.mindharbor.psych.mapper.ResourceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final ResourceMapper resourceMapper;
    private final ArticleMapper articleMapper;
    private final NoticeMapper noticeMapper;
    private final ActionLogMapper actionLogMapper;
    private final DoctorService doctorService;

    public Map<String, Object> resources(long pageNo, long pageSize, String category, String type, String keyword) {
        var wrapper = Wrappers.<Resource>lambdaQuery()
                .eq(Resource::getStatus, CommonStatus.APPROVED)
                .orderByDesc(Resource::getCreatedAt);
        if (category != null && !category.isBlank()) {
            wrapper.eq(Resource::getCategory, category);
        }
        if (type != null && !type.isBlank()) {
            wrapper.eq(Resource::getType, ResourceType.valueOf(type));
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Resource::getTitle, keyword);
        }
        IPage<Resource> page = resourceMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", page.getRecords());
        data.put("total", page.getTotal());
        data.put("pages", page.getPages());
        return data;
    }

    public Resource resourceDetail(Long userId, Long id) {
        Resource r = resourceMapper.selectById(id);
        if (r == null) {
            throw new BizException("资源不存在");
        }
        r.setViewCount((r.getViewCount() == null ? 0 : r.getViewCount()) + 1);
        resourceMapper.updateById(r);
        logAction(userId, "RESOURCE", id, "VIEW", 1.0);
        return r;
    }

    public Resource publish(Long doctorUserId, ResourceDTO dto) {
        Doctor doctor = doctorService.requireDoctor(doctorUserId);
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new BizException("请填写资源标题");
        }
        Resource r = new Resource();
        r.setTitle(dto.getTitle().trim());
        r.setType(dto.getType() == null ? ResourceType.ARTICLE : ResourceType.valueOf(dto.getType()));
        r.setCategory(dto.getCategory());
        r.setCover(dto.getCover());
        r.setUrl(dto.getUrl());
        r.setContent(dto.getContent());
        r.setUploaderId(doctor.getUserId());
        r.setStatus(CommonStatus.PENDING);
        r.setViewCount(0);
        r.setCreatedAt(LocalDateTime.now());
        resourceMapper.insert(r);
        return r;
    }

    public List<Resource> myResources(Long doctorUserId) {
        Doctor doctor = doctorService.requireDoctor(doctorUserId);
        return resourceMapper.selectList(Wrappers.<Resource>lambdaQuery()
                .eq(Resource::getUploaderId, doctor.getUserId())
                .orderByDesc(Resource::getCreatedAt));
    }

    public Map<String, Object> articles(long pageNo, long pageSize, String category, String keyword) {
        var wrapper = Wrappers.<Article>lambdaQuery()
                .eq(Article::getStatus, "PUBLISHED")
                .orderByDesc(Article::getCreatedAt);
        if (category != null && !category.isBlank()) {
            wrapper.eq(Article::getCategory, category);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Article::getTitle, keyword);
        }
        IPage<Article> page = articleMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", page.getRecords());
        data.put("total", page.getTotal());
        data.put("pages", page.getPages());
        return data;
    }

    public Article articleDetail(Long id) {
        Article a = articleMapper.selectById(id);
        if (a == null) {
            throw new BizException("文章不存在");
        }
        a.setViewCount((a.getViewCount() == null ? 0 : a.getViewCount()) + 1);
        articleMapper.updateById(a);
        return a;
    }

    public List<Notice> notices() {
        return noticeMapper.selectList(Wrappers.<Notice>lambdaQuery()
                .eq(Notice::getStatus, "PUBLISHED")
                .orderByDesc(Notice::getCreatedAt));
    }

    private void logAction(Long userId, String targetType, Long targetId, String action, double weight) {
        if (userId == null) {
            return;
        }
        ActionLog log = new ActionLog();
        log.setUserId(userId);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setAction(action);
        log.setWeight(weight);
        log.setCreatedAt(LocalDateTime.now());
        actionLogMapper.insert(log);
    }
}
