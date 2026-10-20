package com.mindharbor.psych.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mindharbor.psych.common.BizException;
import com.mindharbor.psych.dto.DoctorApplyDTO;
import com.mindharbor.psych.entity.Doctor;
import com.mindharbor.psych.entity.DoctorPatient;
import com.mindharbor.psych.entity.User;
import com.mindharbor.psych.enums.BindStatus;
import com.mindharbor.psych.enums.CommonStatus;
import com.mindharbor.psych.enums.Role;
import com.mindharbor.psych.mapper.DoctorMapper;
import com.mindharbor.psych.mapper.DoctorPatientMapper;
import com.mindharbor.psych.mapper.UserMapper;
import com.mindharbor.psych.vo.DoctorVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorMapper doctorMapper;
    private final UserMapper userMapper;
    private final DoctorPatientMapper doctorPatientMapper;
    private final AuthService authService;

    public Map<String, Object> page(String keyword, String specialty, long pageNo, long pageSize) {
        var wrapper = Wrappers.<Doctor>lambdaQuery()
                .eq(Doctor::getStatus, CommonStatus.APPROVED)
                .orderByDesc(Doctor::getRating)
                .orderByDesc(Doctor::getConsultCount);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Doctor::getSpecialty, keyword).or().like(Doctor::getTitle, keyword)
                    .or().like(Doctor::getHospital, keyword));
        }
        if (specialty != null && !specialty.isBlank()) {
            wrapper.like(Doctor::getSpecialty, specialty);
        }
        IPage<Doctor> result = doctorMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);

        List<Long> userIds = result.getRecords().stream().map(Doctor::getUserId).toList();
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));

        Long currentUserId = authService.currentUser().getId();
        Map<Long, DoctorPatient> bindMap = new HashMap<>();
        if (!result.getRecords().isEmpty()) {
            List<Long> doctorIds = result.getRecords().stream().map(Doctor::getId).toList();
            doctorPatientMapper.selectList(Wrappers.<DoctorPatient>lambdaQuery()
                            .eq(DoctorPatient::getUserId, currentUserId)
                            .in(DoctorPatient::getDoctorId, doctorIds))
                    .forEach(bp -> bindMap.put(bp.getDoctorId(), bp));
        }

        List<DoctorVO> vos = new ArrayList<>();
        for (Doctor d : result.getRecords()) {
            DoctorVO vo = DoctorVO.of(d, userMap.get(d.getUserId()));
            DoctorPatient bp = bindMap.get(d.getId());
            if (bp != null) {
                vo.setBound(bp.getStatus() == BindStatus.APPROVED);
                vo.setBindStatus(bp.getStatus().name());
            }
            vos.add(vo);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("records", vos);
        data.put("total", result.getTotal());
        data.put("pages", result.getPages());
        data.put("current", result.getCurrent());
        return data;
    }

    public DoctorVO detail(Long id) {
        Doctor d = doctorMapper.selectById(id);
        if (d == null) {
            throw new BizException("医生不存在");
        }
        User u = userMapper.selectById(d.getUserId());
        DoctorVO vo = DoctorVO.of(d, u);
        DoctorPatient bp = doctorPatientMapper.selectOne(Wrappers.<DoctorPatient>lambdaQuery()
                .eq(DoctorPatient::getUserId, authService.currentUser().getId())
                .eq(DoctorPatient::getDoctorId, id).last("limit 1"));
        if (bp != null) {
            vo.setBound(bp.getStatus() == BindStatus.APPROVED);
            vo.setBindStatus(bp.getStatus().name());
        }
        return vo;
    }

    /** 申请成为咨询师 */
    @Transactional
    public Doctor apply(DoctorApplyDTO dto) {
        User user = authService.currentUser();
        if (user.getRole() == Role.DOCTOR) {
            throw new BizException("你已经是咨询师账号");
        }
        if (user.getRole() == Role.ADMIN) {
            throw new BizException("管理员账号无需申请");
        }
        Doctor exists = doctorMapper.selectOne(Wrappers.<Doctor>lambdaQuery()
                .eq(Doctor::getUserId, user.getId()).last("limit 1"));
        if (exists != null && exists.getStatus() == CommonStatus.PENDING) {
            throw new BizException("申请已提交，请等待审核");
        }
        Doctor doctor = exists == null ? new Doctor() : exists;
        doctor.setUserId(user.getId());
        doctor.setHospital(dto.getHospital());
        doctor.setTitle(dto.getTitle());
        doctor.setSpecialty(dto.getSpecialty());
        doctor.setIntro(dto.getIntro());
        doctor.setYears(dto.getYears() == null ? 0 : dto.getYears());
        doctor.setPrice(dto.getPrice() == null ? BigDecimal.ZERO : dto.getPrice());
        doctor.setCertNo(dto.getCertNo());
        doctor.setStatus(CommonStatus.PENDING);
        doctor.setAuditRemark(null);
        if (doctor.getRating() == null) {
            doctor.setRating(5.0);
        }
        if (doctor.getConsultCount() == null) {
            doctor.setConsultCount(0);
        }
        if (doctor.getId() == null) {
            doctor.setCreatedAt(LocalDateTime.now());
            doctorMapper.insert(doctor);
        } else {
            doctorMapper.updateById(doctor);
        }
        return doctor;
    }

    public Doctor myDoctorProfile(Long userId) {
        return doctorMapper.selectOne(Wrappers.<Doctor>lambdaQuery()
                .eq(Doctor::getUserId, userId).last("limit 1"));
    }

    public Doctor requireDoctor(Long userId) {
        Doctor d = myDoctorProfile(userId);
        if (d == null) {
            throw new BizException("当前账号还不是咨询师，请先提交入驻申请");
        }
        return d;
    }
}
