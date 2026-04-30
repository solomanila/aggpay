package com.letsvpn.admin.service.system;

import com.baomidou.mybatisplus.extension.service.IService;
import com.letsvpn.admin.entity.SystemSetting;
import java.util.List;

public interface SystemSettingService extends IService<SystemSetting> {

    List<SystemSetting> listCached();

    SystemSetting saveOrUpdateSetting(SystemSetting setting, Long operatorId, String changeType);

    void evictCache();
}
