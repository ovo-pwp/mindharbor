package com.mindharbor.psych.vo;

import com.mindharbor.psych.entity.Doctor;
import com.mindharbor.psych.entity.User;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Data
public class DoctorVO {

    private Long id;
    private Long userId;
    private String name;
    private String initial;
    private String avatar;
    private String hospital;
    private String title;
    private List<String> specialty;
    private String intro;
    private Integer years;
    private BigDecimal price;
    private Double rating;
    private Integer consultCount;
    private String status;
    private String statusLabel;
    /** 当前登录用户是否已绑定该医生 */
    private Boolean bound = false;
    /** 绑定的审核状态：PENDING / APPROVED / REJECTED */
    private String bindStatus;

    public static DoctorVO of(Doctor d, User u) {
        DoctorVO vo = new DoctorVO();
        vo.setId(d.getId());
        vo.setUserId(d.getUserId());
        vo.setHospital(d.getHospital());
        vo.setTitle(d.getTitle());
        vo.setIntro(d.getIntro());
        vo.setYears(d.getYears());
        vo.setPrice(d.getPrice());
        vo.setRating(d.getRating());
        vo.setConsultCount(d.getConsultCount());
        vo.setStatus(d.getStatus() == null ? null : d.getStatus().name());
        vo.setStatusLabel(d.getStatus() == null ? null : d.getStatus().getLabel());
        vo.setSpecialty(d.getSpecialty() == null
                ? List.of()
                : Arrays.stream(d.getSpecialty().split("[,，、]+"))
                .map(String::trim).filter(s -> !s.isEmpty()).toList());
        if (u != null) {
            vo.setName(u.getRealName());
            vo.setInitial(u.initial());
            vo.setAvatar(u.getAvatar());
        }
        return vo;
    }
}
