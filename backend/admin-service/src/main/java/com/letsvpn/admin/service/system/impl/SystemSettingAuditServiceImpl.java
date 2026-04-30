package com.letsvpn.admin.service.system.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.letsvpn.admin.entity.SystemSettingAudit;
import com.letsvpn.admin.mapper.SystemSettingAuditMapper;
import com.letsvpn.admin.service.system.SystemSettingAuditService;
import org.springframework.stereotype.Service;

@Service
public class SystemSettingAuditServiceImpl
        extends ServiceImpl<SystemSettingAuditMapper, SystemSettingAudit>
        implements SystemSettingAuditService {
}
