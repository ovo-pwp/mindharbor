package com.mindharbor.psych.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mindharbor.psych.common.BizException;
import com.mindharbor.psych.dto.ArticleDTO;
import com.mindharbor.psych.dto.DoctorApplyDTO;
import com.mindharbor.psych.dto.NoticeDTO;
import com.mindharbor.psych.entity.*;
import com.mindharbor.psych.enums.CommonStatus;
import com.mindharbor.psych.enums.Role;
import com.mindharbor.psych.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final DoctorMapper doctorMapper;
    private final AssessmentMapper assessmentMapper;
    private final AssessmentQuestionMapper questionMapper;
    private final AssessmentRecordMapper recordMapper;
    private final ResourceMapper resourceMapper;
    private final ArticleMapper articleMapper;
    private final NoticeMapper noticeMapper;
    private final SymptomRecordMapper symptomMapper;
    private final ChatMessageMapper chatMapper;

    public Map<String, Object> dashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("userCount", userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getRole, Role.USER)));
        data.put("doctorCount", doctorMapper.selectCount(Wrappers.<Doctor>lambdaQuery()
                .eq(Doctor::getStatus, CommonStatus.APPROVED)));
        data.put("pendingDoctorCount", doctorMapper.selectCount(Wrappers.<Doctor>lambdaQuery()
                .eq(Doctor::getStatus, CommonStatus.PENDING)));
        data.put("pendingResourceCount", resourceMapper.selectCount(Wrappers.<Resource>lambdaQuery()
                .eq(Resource::getStatus, CommonStatus.PENDING)));
        data.put("assessmentCount", recordMapper.selectCount(null));
        data.put("resourceCount", resourceMapper.selectCount(Wrappers.<Resource>lambdaQuery()
                .eq(Resource::getStatus, CommonStatus.APPROVED)));
        data.put("articleCount", articleMapper.selectCount(null));
        data.put("symptomCount", symptomMapper.selectCount(null));
        data.put("messageCount", chatMapper.selectCount(null));
        data.put("todayUsers", userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .ge(User::getCreatedAt, LocalDate.now().atStartOfDay())));

        // 近 7 天测评趋势
        List<String> dates = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            dates.add(d.toString().substring(5));
            counts.add(recordMapper.selectCount(Wrappers.<AssessmentRecord>lambdaQuery()
                    .ge(AssessmentRecord::getCreatedAt, d.atStartOfDay())
                    .lt(AssessmentRecord::getCreatedAt, d.plusDays(1).atStartOfDay())));
        }
        data.put("trendDates", dates);
        data.put("trendCounts", counts);

        // 测评分级分布
        List<AssessmentRecord> all = recordMapper.selectList(null);
        Map<String, Long> levelDist = all.stream()
                .collect(Collectors.groupingBy(AssessmentRecord::getLevel, Collectors.counting()));
        data.put("levelDist", levelDist);

        data.put("recentUsers", userMapper.selectList(Wrappers.<User>lambdaQuery()
                .orderByDesc(User::getCreatedAt).last("limit 6")));
        return data;
    }

    public Map<String, Object> users(long pageNo, long pageSize, String keyword, String role) {
        var wrapper = Wrappers.<User>lambdaQuery().orderByDesc(User::getCreatedAt);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getRealName, keyword));
        }
        if (role != null && !role.isBlank()) {
            wrapper.eq(User::getRole, Role.valueOf(role));
        }
        IPage<User> page = userMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", page.getRecords());
        data.put("total", page.getTotal());
        data.put("pages", page.getPages());
        return data;
    }

    public void toggleUser(Long id) {
        User u = userMapper.selectById(id);
        if (u == null) {
            throw new BizException("用户不存在");
        }
        if (u.getRole() == Role.ADMIN) {
            throw new BizException("不能停用管理员账号");
        }
        u.setStatus(u.getStatus() == CommonStatus.ACTIVE ? CommonStatus.DISABLED : CommonStatus.ACTIVE);
        userMapper.updateById(u);
    }

    public List<Map<String, Object>> doctors(String status) {
        var wrapper = Wrappers.<Doctor>lambdaQuery().orderByDesc(Doctor::getCreatedAt);
        if (status != null && !status.isBlank()) {
            wrapper.eq(Doctor::getStatus, CommonStatus.valueOf(status));
        }
        List<Doctor> list = doctorMapper.selectList(wrapper);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Doctor d : list) {
            User u = userMapper.selectById(d.getUserId());
            Map<String, Object> m = new HashMap<>();
            m.put("doctor", d);
            m.put("user", u);
            out.add(m);
        }
        return out;
    }

    @Transactional
    public void auditDoctor(Long doctorId, String status, String remark) {
        Doctor d = doctorMapper.selectById(doctorId);
        if (d == null) {
            throw new BizException("医生档案不存在");
        }
        CommonStatus target = "APPROVED".equalsIgnoreCase(status) ? CommonStatus.APPROVED : CommonStatus.REJECTED;
        d.setStatus(target);
        d.setAuditRemark(remark);
        doctorMapper.updateById(d);
        User u = userMapper.selectById(d.getUserId());
        if (u != null) {
            u.setRole(target == CommonStatus.APPROVED ? Role.DOCTOR : Role.USER);
            userMapper.updateById(u);
        }
    }

    public List<Assessment> assessments() {
        return assessmentMapper.selectList(Wrappers.<Assessment>lambdaQuery().orderByAsc(Assessment::getId));
    }

    @Transactional
    public Assessment saveAssessment(Assessment a) {
        if (a.getId() == null) {
            a.setCreatedAt(LocalDateTime.now());
            assessmentMapper.insert(a);
        } else {
            assessmentMapper.updateById(a);
        }
        Long count = questionMapper.selectCount(Wrappers.<AssessmentQuestion>lambdaQuery()
                .eq(AssessmentQuestion::getAssessmentId, a.getId()));
        a.setQuestionCount(count == null ? 0 : count.intValue());
        assessmentMapper.updateById(a);
        return a;
    }

    public List<AssessmentQuestion> questions(Long assessmentId) {
        return questionMapper.selectList(Wrappers.<AssessmentQuestion>lambdaQuery()
                .eq(AssessmentQuestion::getAssessmentId, assessmentId)
                .orderByAsc(AssessmentQuestion::getSeq));
    }

    @Transactional
    public void saveQuestion(AssessmentQuestion q) {
        if (q.getId() == null) {
            questionMapper.insert(q);
        } else {
            questionMapper.updateById(q);
        }
    }

    public void deleteQuestion(Long id) {
        questionMapper.deleteById(id);
    }

    public List<Article> articles() {
        return articleMapper.selectList(Wrappers.<Article>lambdaQuery().orderByDesc(Article::getCreatedAt));
    }

    @Transactional
    public Article saveArticle(ArticleDTO dto) {
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new BizException("标题不能为空");
        }
        Article a;
        if (dto.getId() != null) {
            a = articleMapper.selectById(dto.getId());
            if (a == null) {
                throw new BizException("文章不存在");
            }
        } else {
            a = new Article();
            a.setCreatedAt(LocalDateTime.now());
            a.setViewCount(0);
            a.setStatus("PUBLISHED");
        }
        a.setTitle(dto.getTitle());
        a.setCategory(dto.getCategory());
        a.setCover(dto.getCover());
        a.setSummary(dto.getSummary());
        a.setContent(dto.getContent());
        a.setAuthor(dto.getAuthor() == null ? "平台编辑部" : dto.getAuthor());
        if (a.getId() == null) {
            articleMapper.insert(a);
        } else {
            articleMapper.updateById(a);
        }
        return a;
    }

    public void deleteArticle(Long id) {
        articleMapper.deleteById(id);
    }

    public List<Notice> notices() {
        return noticeMapper.selectList(Wrappers.<Notice>lambdaQuery().orderByDesc(Notice::getCreatedAt));
    }

    @Transactional
    public Notice saveNotice(NoticeDTO dto) {
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new BizException("标题不能为空");
        }
        Notice n;
        if (dto.getId() != null) {
            n = noticeMapper.selectById(dto.getId());
            if (n == null) {
                throw new BizException("公告不存在");
            }
        } else {
            n = new Notice();
            n.setCreatedAt(LocalDateTime.now());
            n.setStatus("PUBLISHED");
        }
        n.setTitle(dto.getTitle());
        n.setContent(dto.getContent());
        if (n.getId() == null) {
            noticeMapper.insert(n);
        } else {
            noticeMapper.updateById(n);
        }
        return n;
    }

    public void deleteNotice(Long id) {
        noticeMapper.deleteById(id);
    }

    public List<Resource> resources(String status) {
        var wrapper = Wrappers.<Resource>lambdaQuery().orderByDesc(Resource::getCreatedAt);
        if (status != null && !status.isBlank()) {
            wrapper.eq(Resource::getStatus, CommonStatus.valueOf(status));
        }
        return resourceMapper.selectList(wrapper);
    }

    public void auditResource(Long id, String status) {
        Resource r = resourceMapper.selectById(id);
        if (r == null) {
            throw new BizException("资源不存在");
        }
        r.setStatus("APPROVED".equalsIgnoreCase(status) ? CommonStatus.APPROVED : CommonStatus.REJECTED);
        resourceMapper.updateById(r);
    }
}
