package com.mindharbor.psych.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mindharbor.psych.common.BizException;
import com.mindharbor.psych.dto.BindApplyDTO;
import com.mindharbor.psych.dto.BindAuditDTO;
import com.mindharbor.psych.dto.MessageDTO;
import com.mindharbor.psych.entity.AssessmentRecord;
import com.mindharbor.psych.entity.ChatMessage;
import com.mindharbor.psych.entity.Doctor;
import com.mindharbor.psych.entity.DoctorPatient;
import com.mindharbor.psych.entity.SymptomRecord;
import com.mindharbor.psych.entity.User;
import com.mindharbor.psych.enums.BindStatus;
import com.mindharbor.psych.enums.CommonStatus;
import com.mindharbor.psych.mapper.AssessmentRecordMapper;
import com.mindharbor.psych.mapper.ChatMessageMapper;
import com.mindharbor.psych.mapper.DoctorMapper;
import com.mindharbor.psych.mapper.DoctorPatientMapper;
import com.mindharbor.psych.mapper.SymptomRecordMapper;
import com.mindharbor.psych.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConsultService {

    private final DoctorPatientMapper bindMapper;
    private final DoctorMapper doctorMapper;
    private final UserMapper userMapper;
    private final ChatMessageMapper chatMapper;
    private final SymptomRecordMapper symptomMapper;
    private final AssessmentRecordMapper assessmentRecordMapper;
    private final DoctorService doctorService;

    /** 患者申请绑定医生 */
    @Transactional
    public DoctorPatient apply(Long userId, BindApplyDTO dto) {
        Doctor doctor = doctorMapper.selectById(dto.getDoctorId());
        if (doctor == null || doctor.getStatus() != CommonStatus.APPROVED) {
            throw new BizException("医生不存在或未通过审核");
        }
        DoctorPatient exists = bindMapper.selectOne(Wrappers.<DoctorPatient>lambdaQuery()
                .eq(DoctorPatient::getDoctorId, dto.getDoctorId())
                .eq(DoctorPatient::getUserId, userId).last("limit 1"));
        if (exists != null) {
            if (exists.getStatus() == BindStatus.APPROVED) {
                throw new BizException("你已经绑定该医生");
            }
            if (exists.getStatus() == BindStatus.PENDING) {
                throw new BizException("申请已提交，请等待医生确认");
            }
            exists.setStatus(BindStatus.PENDING);
            exists.setApplyReason(dto.getReason());
            exists.setRemark(null);
            exists.setUpdatedAt(LocalDateTime.now());
            bindMapper.updateById(exists);
            return exists;
        }
        DoctorPatient bp = new DoctorPatient();
        bp.setDoctorId(dto.getDoctorId());
        bp.setUserId(userId);
        bp.setStatus(BindStatus.PENDING);
        bp.setApplyReason(dto.getReason());
        bp.setCreatedAt(LocalDateTime.now());
        bp.setUpdatedAt(LocalDateTime.now());
        bindMapper.insert(bp);
        return bp;
    }

    /** 患者视角：我的医生 / 申请状态 */
    public List<Map<String, Object>> myDoctors(Long userId) {
        List<DoctorPatient> list = bindMapper.selectList(Wrappers.<DoctorPatient>lambdaQuery()
                .eq(DoctorPatient::getUserId, userId)
                .orderByDesc(DoctorPatient::getUpdatedAt));
        List<Map<String, Object>> out = new ArrayList<>();
        for (DoctorPatient bp : list) {
            Doctor d = doctorMapper.selectById(bp.getDoctorId());
            User u = d == null ? null : userMapper.selectById(d.getUserId());
            Map<String, Object> m = new HashMap<>();
            m.put("bindId", bp.getId());
            m.put("doctorId", bp.getDoctorId());
            m.put("status", bp.getStatus().name());
            m.put("statusLabel", bp.getStatus().getLabel());
            m.put("remark", bp.getRemark());
            m.put("appliedAt", bp.getCreatedAt());
            if (d != null) {
                m.put("name", u == null ? "咨询师" : u.getRealName());
                m.put("hospital", d.getHospital());
                m.put("title", d.getTitle());
                m.put("specialty", d.getSpecialty());
                m.put("doctorUserId", d.getUserId());
            }
            out.add(m);
        }
        return out;
    }

    /** 医生视角：待审核申请 */
    public List<Map<String, Object>> pendingApplications(Long doctorUserId) {
        Doctor doctor = doctorService.requireDoctor(doctorUserId);
        List<DoctorPatient> list = bindMapper.selectList(Wrappers.<DoctorPatient>lambdaQuery()
                .eq(DoctorPatient::getDoctorId, doctor.getId())
                .eq(DoctorPatient::getStatus, BindStatus.PENDING)
                .orderByDesc(DoctorPatient::getCreatedAt));
        return toPatientMaps(list);
    }

    /** 医生视角：已绑定患者 */
    public List<Map<String, Object>> patients(Long doctorUserId) {
        Doctor doctor = doctorService.requireDoctor(doctorUserId);
        List<DoctorPatient> list = bindMapper.selectList(Wrappers.<DoctorPatient>lambdaQuery()
                .eq(DoctorPatient::getDoctorId, doctor.getId())
                .eq(DoctorPatient::getStatus, BindStatus.APPROVED)
                .orderByDesc(DoctorPatient::getUpdatedAt));
        return toPatientMaps(list);
    }

    private List<Map<String, Object>> toPatientMaps(List<DoctorPatient> list) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (DoctorPatient bp : list) {
            User u = userMapper.selectById(bp.getUserId());
            Map<String, Object> m = new HashMap<>();
            m.put("bindId", bp.getId());
            m.put("userId", bp.getUserId());
            m.put("name", u == null ? "用户" : u.getRealName());
            m.put("initial", u == null ? "?" : u.initial());
            m.put("gender", u == null ? null : u.getGender());
            m.put("age", u == null ? null : u.getAge());
            m.put("status", bp.getStatus().name());
            m.put("statusLabel", bp.getStatus().getLabel());
            m.put("applyReason", bp.getApplyReason());
            m.put("remark", bp.getRemark());
            m.put("appliedAt", bp.getCreatedAt());
            out.add(m);
        }
        return out;
    }

    /** 医生查看已绑定患者的档案：基本信息 + 情绪记录 + 测评历史 */
    public Map<String, Object> patientOverview(Long doctorUserId, Long patientUserId) {
        Doctor doctor = doctorService.requireDoctor(doctorUserId);
        Long bound = bindMapper.selectCount(Wrappers.<DoctorPatient>lambdaQuery()
                .eq(DoctorPatient::getDoctorId, doctor.getId())
                .eq(DoctorPatient::getUserId, patientUserId)
                .eq(DoctorPatient::getStatus, BindStatus.APPROVED));
        if (bound == null || bound == 0) {
            throw new BizException(403, "未与该患者建立咨询关系");
        }
        User u = userMapper.selectById(patientUserId);
        Map<String, Object> data = new HashMap<>();
        data.put("user", u);
        data.put("symptoms", symptomMapper.selectList(Wrappers.<SymptomRecord>lambdaQuery()
                .eq(SymptomRecord::getUserId, patientUserId)
                .orderByDesc(SymptomRecord::getRecordDate)
                .last("limit 30")));
        data.put("assessments", assessmentRecordMapper.selectList(
                Wrappers.<AssessmentRecord>lambdaQuery()
                        .eq(AssessmentRecord::getUserId, patientUserId)
                        .orderByDesc(AssessmentRecord::getCreatedAt)
                        .last("limit 10")));
        return data;
    }

    /** 医生审核绑定申请 */
    @Transactional
    public DoctorPatient audit(Long doctorUserId, BindAuditDTO dto) {
        Doctor doctor = doctorService.requireDoctor(doctorUserId);
        DoctorPatient bp = bindMapper.selectById(dto.getId());
        if (bp == null || !bp.getDoctorId().equals(doctor.getId())) {
            throw new BizException("申请记录不存在");
        }
        if (bp.getStatus() != BindStatus.PENDING) {
            throw new BizException("该申请已处理");
        }
        BindStatus target = "APPROVED".equalsIgnoreCase(dto.getStatus()) ? BindStatus.APPROVED : BindStatus.REJECTED;
        bp.setStatus(target);
        bp.setRemark(dto.getRemark());
        bp.setUpdatedAt(LocalDateTime.now());
        bindMapper.updateById(bp);
        return bp;
    }

    /** 发送消息：双方必须已建立绑定关系 */
    @Transactional
    public ChatMessage send(Long fromId, MessageDTO dto) {
        if (dto.getContent() == null || dto.getContent().isBlank()) {
            throw new BizException("消息不能为空");
        }
        if (dto.getContent().length() > 1000) {
            throw new BizException("单条消息不要超过 1000 字");
        }
        if (dto.getToId() == null || dto.getToId().equals(fromId)) {
            throw new BizException("收件人不正确");
        }
        if (!isBound(fromId, dto.getToId())) {
            throw new BizException("你们还没有建立医患关系，无法发送消息");
        }
        ChatMessage msg = new ChatMessage();
        msg.setFromId(fromId);
        msg.setToId(dto.getToId());
        msg.setContent(dto.getContent().trim());
        msg.setReadFlag(false);
        msg.setCreatedAt(LocalDateTime.now());
        chatMapper.insert(msg);
        return msg;
    }

    private boolean isBound(Long a, Long b) {
        User ua = userMapper.selectById(a);
        User ub = userMapper.selectById(b);
        if (ua == null || ub == null) {
            return false;
        }
        Long doctorUserId;
        Long patientId;
        if (ua.getRole() == com.mindharbor.psych.enums.Role.DOCTOR && ub.getRole() == com.mindharbor.psych.enums.Role.USER) {
            doctorUserId = a;
            patientId = b;
        } else if (ub.getRole() == com.mindharbor.psych.enums.Role.DOCTOR && ua.getRole() == com.mindharbor.psych.enums.Role.USER) {
            doctorUserId = b;
            patientId = a;
        } else {
            return false;
        }
        Doctor doctor = doctorService.myDoctorProfile(doctorUserId);
        if (doctor == null) {
            return false;
        }
        Long c = bindMapper.selectCount(Wrappers.<DoctorPatient>lambdaQuery()
                .eq(DoctorPatient::getDoctorId, doctor.getId())
                .eq(DoctorPatient::getUserId, patientId)
                .eq(DoctorPatient::getStatus, BindStatus.APPROVED));
        return c != null && c > 0;
    }

    /** 会话列表 */
    public List<Map<String, Object>> conversations(Long userId) {
        List<ChatMessage> all = chatMapper.selectList(Wrappers.<ChatMessage>lambdaQuery()
                .and(w -> w.eq(ChatMessage::getFromId, userId).or().eq(ChatMessage::getToId, userId))
                .orderByDesc(ChatMessage::getId));
        Map<Long, ChatMessage> latest = new LinkedHashMap<>();
        Map<Long, Integer> unread = new HashMap<>();
        for (ChatMessage m : all) {
            Long other = m.getFromId().equals(userId) ? m.getToId() : m.getFromId();
            latest.putIfAbsent(other, m);
            if (m.getToId().equals(userId) && !Boolean.TRUE.equals(m.getReadFlag())) {
                unread.merge(other, 1, Integer::sum);
            }
        }
        List<Map<String, Object>> out = new ArrayList<>();
        latest.forEach((otherId, msg) -> {
            User u = userMapper.selectById(otherId);
            Map<String, Object> m = new HashMap<>();
            m.put("userId", otherId);
            m.put("name", u == null ? "用户" : u.getRealName());
            m.put("initial", u == null ? "?" : u.initial());
            m.put("role", u == null ? null : u.getRole().getLabel());
            m.put("lastMessage", msg.getContent());
            m.put("lastTime", msg.getCreatedAt());
            m.put("unread", unread.getOrDefault(otherId, 0));
            out.add(m);
        });
        out.sort(Comparator.comparing(m -> ((LocalDateTime) m.get("lastTime")), Comparator.reverseOrder()));
        return out;
    }

    /** 聊天记录（并标记已读） */
    public List<ChatMessage> history(Long userId, Long otherId, int limit) {
        List<ChatMessage> list = chatMapper.selectList(Wrappers.<ChatMessage>lambdaQuery()
                .and(w -> w.nested(n -> n.eq(ChatMessage::getFromId, userId).eq(ChatMessage::getToId, otherId))
                        .or()
                        .nested(n -> n.eq(ChatMessage::getFromId, otherId).eq(ChatMessage::getToId, userId)))
                .orderByAsc(ChatMessage::getId));
        if (list.size() > limit) {
            list = new ArrayList<>(list.subList(list.size() - limit, list.size()));
        }
        // 标记对方发来的消息为已读
        chatMapper.update(null, Wrappers.<ChatMessage>lambdaUpdate()
                .eq(ChatMessage::getFromId, otherId)
                .eq(ChatMessage::getToId, userId)
                .eq(ChatMessage::getReadFlag, false)
                .set(ChatMessage::getReadFlag, true));
        return list;
    }
}
