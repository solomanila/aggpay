package com.letsvpn.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.letsvpn.common.core.exception.BizException;
import com.letsvpn.common.data.entity.User;
import com.letsvpn.common.data.entity.UserNode;
import com.letsvpn.common.data.mapper.UserMapper;
import com.letsvpn.common.data.mapper.UserNodeMapper;
import com.letsvpn.user.entity.Node;
import com.letsvpn.user.entity.SubscriptionPlan;
import com.letsvpn.user.entity.UserSubscription;
import com.letsvpn.user.enums.VipLevel;
import com.letsvpn.user.mapper.NodeMapper;
import com.letsvpn.user.mapper.SubscriptionPlanMapper;
import com.letsvpn.user.mapper.UserSubscriptionMapper;
import com.letsvpn.user.service.NodeService;
import com.letsvpn.user.service.UserService;
import com.letsvpn.user.service.WireGuardConfigService;
import com.letsvpn.user.service.WireguardNacosConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserNodeMapper userNodeMapper;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private NodeMapper nodeMapper;

    @Autowired
    private WireGuardConfigService wireGuardConfigService;

    @Autowired
    private WireguardNacosConfigService wireguardNacosConfigService;

    @Autowired
    private UserSubscriptionMapper userSubscriptionMapper;

    @Autowired
    private SubscriptionPlanMapper subscriptionPlanMapper;

    @Override
    @Cacheable(value = "user:info", key = "#username", unless = "#result == null")
    public User findByUsername(String username) {
        return baseMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
    }

    @Override
    @Transactional
    public void initializeNewUser(String userId, String username) {
        log.info("开始初始化新用户: userId={}, username={}", userId, username);

        // 1. 创建用户记录
//        User user = new User();
//        user.setId(Long.parseLong(userId));
//        user.setUsername(username);
//        user.setLevel(VipLevel.FREE.getCode()); // 设置初始等级为免费用户
//
//        try {
//            userMapper.insert(user);
//            log.info("用户记录创建成功: userId={}", userId);
//        } catch (Exception e) {
//            log.error("创建用户记录失败: userId={}", userId, e);
//            throw new BizException("创建用户记录失败: " + e.getMessage());
//        }

        // 2. 获取免费节点列表
        QueryWrapper<Node> nodeQuery = new QueryWrapper<>();
        nodeQuery.eq("is_free", true)  // 免费节点
                .eq("status", 0);      // 状态正常
        List<Node> freeNodes = nodeMapper.selectList(nodeQuery);

        if (freeNodes.isEmpty()) {
            log.warn("没有可用的免费节点，用户 {} 将无法使用VPN服务", username);
            return;
        }

        // 3. 为每个免费节点分配密钥
        for (Node node : freeNodes) {
            try {
                // 使用新的allocateKey方法分配密钥
                UserNode userNode = wireGuardConfigService.allocateKey(Long.valueOf(userId), node.getId());
                
                // 更新Nacos配置
                //wireguardNacosConfigService.publishConfigForNode(node.getId());
                
                log.info("成功为用户 {} 在节点 {} 分配密钥", username, node.getId());
            } catch (Exception e) {
                log.error("为用户 {} 在节点 {} 分配密钥失败", username, node.getId(), e);
                // 继续处理其他节点，不中断整个流程
            }
        }

        log.info("用户初始化完成: userId={}, username={}", userId, username);
    }

    @Override
    public String getCurrentVipPlanType(Long userId) {
        // 查询用户当前有效的订阅记录
        QueryWrapper<UserSubscription> subscriptionQuery = new QueryWrapper<>();
        subscriptionQuery.eq("user_id", userId)
                .eq("status", 1) // 激活状态
                .gt("end_time", LocalDateTime.now()) // 未过期
                .orderByDesc("end_time") // 按结束时间倒序，获取最新的订阅
                .last("LIMIT 1");
        
        UserSubscription activeSubscription = userSubscriptionMapper.selectOne(subscriptionQuery);
        
        if (activeSubscription == null) {
            return null; // 没有有效的VIP订阅
        }
        
        // 查询对应的套餐信息获取type
        SubscriptionPlan plan = subscriptionPlanMapper.selectById(activeSubscription.getPlanId());
        
        return plan != null ? plan.getType() : null;
    }
}
