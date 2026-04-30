package com.letsvpn.admin.service.system;

import com.baomidou.mybatisplus.extension.service.IService;
import com.letsvpn.admin.entity.SystemFeatureFlag;
import java.util.List;

public interface SystemFeatureFlagService extends IService<SystemFeatureFlag> {

    List<SystemFeatureFlag> listCached();

    void evictCache();
}
