export const hero = {
  subtitle: '全球多通道资金服务',
  title: '实时收付监控中心',
  description: '聚合 UPI、银行转账与电子钱包，监控费率、额度及风控指标，支持分钟级响应与升级。',
  metrics: [
    { label: '在运营国家', value: '12' },
    { label: '活跃通道', value: '32' },
    { label: '分钟级 SLA', value: '99.95%' }
  ],
  cta: {
    title: '新增业务接入',
    description: '3 分钟提交需求，7×24 团队协助对接。',
    action: '发起接入申请'
  }
};

export const user = {
  name: 'zya',
  role: '系统管理员'
};

export const alerts = {
  openIncidents: 2,
  notifications: 8,
  lastUpdated: '5 分钟前'
};

export const menuItems = [
  {
    id: 'home',
    label: '首页',
    description: '展示全局运行状态、概览指标与最新告警。',
    children: []
  },
  {
    id: 'dashboard',
    label: '仪表板',
    badge: 'live',
    children: [
      { id: 'dashboard-overview', label: '概要', description: '聚合关键指标与告警，快速了解整体健康度。' },
      { id: 'dashboard-channel', label: '通道视图', description: '以通道维度拆分延迟、费率与容量上限。' },
      { id: 'dashboard-merchant', label: '商户视图', description: '按商户监控流量、成本与SLA可用性。' },
      { id: 'dashboard-funds', label: '资金流水', description: '跟踪资金进出、结算节点与余额变化。' },
      { id: 'dashboard-banktrade', label: 'BankTrade', description: 'BankTrade 接口健康度与交易状态。' },
      { id: 'dashboard-bankid', label: 'BankId', description: 'BankId 实名链路成功率与告警。' },
      { id: 'dashboard-bankmonitor', label: 'BankMonitor', description: '银行监控策略、阈值与触发记录。' },
      { id: 'dashboard-agentlog', label: 'AgentAccountLog', description: '代理账户关键操作与风控日志。' }
    ]
  },
  {
    id: 'payments',
    label: '支付',
    children: [
      { id: 'payments-entity', label: '主体配置', description: '业务主体参数、证照与风控配置。' },
      { id: 'payments-channel', label: '通道配置', description: '支付通道路由、权重与限额设置。' },
      { id: 'payments-errors', label: '通道错误记录', description: '通道错误码归档与处理进度。' },
      { id: 'payments-rollback', label: '通道回滚', description: '回滚策略与任务执行记录。' },
      { id: 'payments-reconcile', label: '通道对账', description: 'T+N 对账文件与差异处理情况。' },
      { id: 'payments-throttle', label: '通道限流', description: '限流策略、限额告警与实况。' },
      { id: 'payments-fallback', label: '通道兜底', description: '兜底方案触发记录与手工介入。' },
      { id: 'payments-ifsc-blacklist', label: 'IFSC公户代付黑名单', description: 'IFSC 黑名单名单管理与审核。' },
      { id: 'payments-blacklist', label: '黑名单', description: '全局黑名单录入、校验与同步。' },
      { id: 'payments-profit', label: '利润报表', description: '渠道收入、成本及利润分析。' }
    ]
  },
  {
    id: 'bank-accounts',
    label: '银行户管理',
    children: [
      { id: 'bank-suppliers', label: '供户商', description: '供户商主体信息与对接状态。' },
      { id: 'bank-accounts-list', label: '账户', description: '银行账户生命周期与额度。' },
      { id: 'bank-channel-settings', label: '通道设置', description: '账户绑定通道与分流规则。' },
      { id: 'bank-realtime-dashboard', label: '公户实时面板', description: '公户实时资金与操作状态。' },
      { id: 'bank-ledger', label: '流水', description: '账户流水明细与标签。' },
      { id: 'bank-bookkeeping', label: '记账流水', description: '内部记账、冲补与调账记录。' },
      { id: 'bank-mapping', label: '银行字段映射', description: '银行字段映射与转换模板。' }
    ]
  },
  {
    id: 'orders',
    label: '订单',
    children: [
      { id: 'orders-payin', label: '收款订单', description: '收款订单搜索、状态与重试。' },
      { id: 'orders-payout', label: '出款订单', description: '出款订单、审单与通道回执。' },
      { id: 'orders-rollback', label: '代付回滚', description: '代付回滚任务与赔付跟踪。' },
      { id: 'orders-queue', label: '代付队列', description: '队列健康度、速率与重试。' },
      { id: 'orders-batch', label: '批量代付', description: '批量导入、拆单与执行记录。' },
      { id: 'orders-approval', label: '审批流程', description: '审批链路配置与实例状态。' },
      { id: 'orders-query', label: '查单记录', description: '查单触发、反馈与客服备注。' },
      { id: 'merchant-orders-payin', label: '收款', description: '商户收款订单列表与状态查询。' },
      { id: 'merchant-orders-payout', label: '出款', description: '商户出款订单列表与状态查询。' },
      { id: 'merchant-orders-refund', label: '退款', description: '商户退款订单列表与处理状态。' },
      { id: 'merchant-orders-bill', label: '账单', description: '商户账单汇总与对账。' },
      { id: 'merchant-orders-download', label: '下载', description: '商户订单数据导出与下载记录。' }
    ]
  },
  {
    id: 'merchants',
    label: '商户',
    children: [
      { id: 'merchants-list', label: '列表', description: '商户档案、标签与接入能力。' },
      { id: 'merchants-dashboard', label: '面板', description: '单商户实时经营数据预览。' }
    ]
  },
  {
    id: 'agents',
    label: '代理商',
    children: [
      { id: 'agents-channels', label: '代理商渠道', description: '代理商可用渠道与分成参数。' },
      { id: 'agents-list', label: '代理商列表', description: '代理商资质、风控与合作状态。' }
    ]
  },
  {
    id: 'operations',
    label: '运营工具',
    children: [
      { id: 'ops-sms', label: 'SMS/OTP', description: '短信/OTP 服务开关与模板。' },
      { id: 'ops-forwarder', label: '短信转发APP', description: '短信转发 App 配置与同步状态。' },
      { id: 'ops-bulk', label: '批量操作', description: '批量导入、导出与任务看板。' },
      { id: 'ops-vps-manager', label: 'VPS管理', description: 'VPS 资源、状态与分组。' },
      { id: 'ops-vps', label: 'VPS', description: '具体 VPS 节点监控详情。' },
      { id: 'ops-telegram', label: '电报通知', description: 'Telegram 通知路由与模板。' }
    ]
  },
  {
    id: 'finance',
    label: '财务',
    children: [
      { id: 'finance-billing', label: '账单', description: '对账周期、应收应付与账单状态。' },
      { id: 'finance-withdraw', label: '提现', description: '提现申请、额度与发放情况。' },
      { id: 'finance-agent-billing', label: '代理商账单', description: '代理商结算、分润与发票。' },
      { id: 'finance-system-topup', label: '系统付费充值', description: '系统付费额度与充值订单。' },
      { id: 'finance-agent-withdraw', label: '代理商提现申请', description: '代理商提现审批、支付与反馈。' }
    ]
  },
  {
    id: 'downloads',
    label: '下载管理',
    children: [
      { id: 'downloads-history', label: '历史记录', description: '导出记录、文件有效期与重试。' }
    ]
  },
  {
    id: 'system',
    label: '系统',
    children: [
      { id: 'system-billing', label: '系统账单', description: '平台内部账单与结算周期。' },
      { id: 'system-users', label: '用户', description: '系统用户、角色与权限。' },
      { id: 'system-settings', label: '配置', description: '全局配置中心及灰度参数。' }
    ]
  },
  {
    id: 'withdraw',
    label: '提现',
    children: [
      { id: 'withdraw-apply', label: '申请', description: '提现申请与审批状态。' }
    ]
  },
  {
    id: 'data',
    label: '数据',
    children: [
      { id: 'data-report', label: '报表', description: '商户数据统计与报表导出。' }
    ]
  },
  {
    id: 'docs',
    label: '文档',
    children: [
      { id: 'docs-api', label: 'API文档', description: 'API 接入文档与示例。' },
      { id: 'docs-sign', label: '签名', description: '签名算法说明与验签工具。' }
    ]
  }
];

export const timezones = ['UTC-03:00', 'UTC±00:00', 'UTC+05:30', 'UTC+07:00', 'UTC+08:00'];
export const languages = ['中文', 'English'];

export const dashboardOverview = {
  hero: {
    eyebrow: '仪表板 · 概要',
    title: '跨境支付实时运行摘要',
    description: '聚焦跨国收付成功率、延迟、容量与告警，覆盖重点区域与通道。',
    sync: {
      label: '最后同步',
      value: '37 秒前'
    }
  },
  quickStats: [
    { id: 'volume', label: '今日交易额', value: '¥32,980,000', trend: '+6.3%', trendLabel: '较昨日' },
    { id: 'transactions', label: '交易笔数', value: '182,430', trend: '+11.9%', trendLabel: '高峰 +9%' },
    { id: 'success', label: '成功率', value: '98.45%', trend: '+0.42%', trendLabel: '较 SLA' },
    { id: 'risk', label: '风控拦截', value: '312 次', trend: '-5.1%', trendLabel: '过去 1 小时' }
  ],
  healthMetrics: [
    {
      id: 'latency',
      label: '端到端延迟',
      value: '1.9s',
      target: '目标 1.5s',
      trend: '+0.3s',
      progress: 76
    },
    {
      id: 'approval',
      label: '合规通过率',
      value: '96.2%',
      target: '目标 97%',
      trend: '+0.6%',
      progress: 64
    },
    {
      id: 'sla',
      label: '渠道 SLA 覆盖',
      value: '99.12%',
      target: '目标 99.5%',
      trend: '-0.2%',
      progress: 88
    }
  ],
  flowBreakdown: [
    { id: 'upi', label: '印度 UPI', value: '46%', status: 'up' },
    { id: 'wallet', label: '东南亚钱包', value: '28%', status: 'steady' },
    { id: 'bank', label: '本地银行', value: '18%', status: 'down' },
    { id: 'card', label: '卡组织', value: '8%', status: 'steady' }
  ],
  incidents: [
    {
      id: 'inc-upi',
      severity: '高',
      title: 'UPI OAuth 延迟升高',
      detail: '银行侧授权接口 CPU 饱和',
      owner: '风控',
      eta: '预计 2 小时恢复'
    },
    {
      id: 'inc-bank',
      severity: '中',
      title: '菲律宾通道限速',
      detail: '队列积压 18%',
      owner: '运维',
      eta: '进行手工扩容'
    }
  ],
  maintenance: [
    { id: 'plan-switch', label: '北方通道切换演练', window: '14:00 - 15:00', owner: 'OPS' },
    { id: 'plan-audit', label: '代理商分润复核', window: '整天', owner: 'Clearing' }
  ],
  regionalLatencies: [
    { id: 'india', region: '印度', latency: '1.8s', diff: '+0.3s', status: 'warning' },
    { id: 'vietnam', region: '越南', latency: '1.2s', diff: '-0.1s', status: 'good' },
    { id: 'brazil', region: '巴西', latency: '2.3s', diff: '+0.6s', status: 'critical' },
    { id: 'uae', region: '阿联酋', latency: '1.1s', diff: '+0.05s', status: 'steady' }
  ],
  timeline: [
    { id: 'timeline-1', time: '09:10', title: 'VPS 远程诊断完成', type: 'info' },
    { id: 'timeline-2', time: '09:25', title: 'UPI 队列扩容至 3×', type: 'success' },
    { id: 'timeline-3', time: '09:40', title: '菲律宾银行 API 重连', type: 'warning' }
  ],
  capacity: {
    utilization: '72%',
    change: '+4.1%',
    segments: [
      { id: 'cap-upi', label: 'UPI', value: 82 },
      { id: 'cap-wallet', label: 'Wallet', value: 64 },
      { id: 'cap-bank', label: 'Bank', value: 58 },
      { id: 'cap-card', label: 'Card', value: 44 }
    ]
  },
  actionItems: [
    { id: 'action-routing', title: '通道路由策略回归测试', owner: '支付策略', progress: 60 },
    { id: 'action-aml', title: 'AML 模型回溯', owner: '风控', progress: 35 },
    { id: 'action-transparency', title: '代理商透明度看板', owner: '运营', progress: 80 }
  ],
  notices: [
    { id: 'notice-1', title: '印度线下卡点调优记录已上传 Confluence' },
    { id: 'notice-2', title: '越南钱包费率下调 15bps 即刻生效' },
    { id: 'notice-3', title: 'BankID SDK 版本更新，等待审批' }
  ]
};

export const dashboardChannelView = {
  hero: {
    eyebrow: '仪表板 · 通道视图',
    title: '关键通道路由与健康度',
    description: '聚焦印度 UPI、东南亚钱包与本地银行的实时容量、延迟与风控分布。',
    sync: '同步于 2 秒前'
  },
  filters: [
    { id: 'ALL', label: '全部', active: true },
    { id: 'UPI', label: 'UPI' },
    { id: 'Wallet', label: '钱包' },
    { id: 'Bank', label: '本地银行' },
    { id: 'Card', label: '卡组织' }
  ],
  stats: [
    { id: 'peak', label: '当前峰值 QPS', value: '9.8k', meta: '+11% vs 上小时' },
    { id: 'success', label: '实时成功率', value: '98.32%', meta: 'SLA 97.5%' },
    { id: 'latency', label: '平均延迟', value: '1.7s', meta: '+0.2s India' },
    { id: 'alerts', label: '触发告警', value: '4', meta: '处理中 2' }
  ],
  channels: [
    {
      id: 'upi-1',
      name: 'UPI-北区',
      country: 'India',
      latency: '1.4s',
      success: '99.02%',
      volume: '34%',
      trend: '+4%',
      status: 'green'
    },
    {
      id: 'upi-2',
      name: 'UPI-南区',
      country: 'India',
      latency: '2.4s',
      success: '96.12%',
      volume: '22%',
      trend: '+11%',
      status: 'amber'
    },
    {
      id: 'wallet-vn',
      name: 'Wallet-VN',
      country: 'Vietnam',
      latency: '1.1s',
      success: '98.76%',
      volume: '12%',
      trend: '-2%',
      status: 'green'
    },
    {
      id: 'wallet-id',
      name: 'Wallet-ID',
      country: 'Indonesia',
      latency: '1.8s',
      success: '97.70%',
      volume: '9%',
      trend: '+1%',
      status: 'green'
    },
    {
      id: 'bank-ph',
      name: 'Bank-PH',
      country: 'Philippines',
      latency: '2.9s',
      success: '93.44%',
      volume: '14%',
      trend: '+18%',
      status: 'red'
    },
    {
      id: 'bank-br',
      name: 'Bank-BR',
      country: 'Brazil',
      latency: '2.2s',
      success: '95.55%',
      volume: '9%',
      trend: '-3%',
      status: 'amber'
    }
  ],
  throughput: {
    label: '过去 30 分钟吞吐',
    entries: [
      { time: '10:10', payin: 7200, payout: 6100 },
      { time: '10:15', payin: 8100, payout: 6400 },
      { time: '10:20', payin: 8600, payout: 6700 },
      { time: '10:25', payin: 9100, payout: 6900 },
      { time: '10:30', payin: 9800, payout: 7200 }
    ],
    peak: '9800 / 分钟',
    imbalance: '+14% Payin'
  },
  reroutePlans: [
    {
      id: 'route-upi',
      title: 'UPI 南区降级为备用 ACQ',
      detail: '手工路由 18% 交易至北区',
      eta: '预计 30 分钟',
      owner: 'Router'
    },
    {
      id: 'route-bank',
      title: '菲律宾通道限速策略',
      detail: '维持 850TPS，观察 CPU 回落',
      eta: '持续监控',
      owner: 'OPS'
    }
  ],
  providerLoad: [
    { id: 'prov-1', provider: 'NPCI', load: 82, status: '高' },
    { id: 'prov-2', provider: 'Bank BR Core', load: 64, status: '中' },
    { id: 'prov-3', provider: 'Wallet SEA', load: 48, status: '中' },
    { id: 'prov-4', provider: 'Card Switch', load: 32, status: '低' }
  ],
  checklist: [
    { id: 'check-1', title: 'UPI 重试策略启用', time: '09:50', owner: '风控' },
    { id: 'check-2', title: '钱包批量上下线审批', time: '10:05', owner: '合规' },
    { id: 'check-3', title: '银行网银链路巡检', time: '10:15', owner: 'OPS' }
  ],
  alerts: [
    {
      id: 'alert-upi',
      severity: '高',
      title: 'UPI 南区失败率升高',
      detail: 'SDK 超时率 3.1%，转路北区 15%',
      action: '同步通知代理商'
    },
    {
      id: 'alert-bank',
      severity: '中',
      title: 'Bank-PH CPU 超 85%',
      detail: '限制最大连接 + 队列排空',
      action: '夜间扩容预约'
    }
  ]
};

export const dashboardMerchantView = {
  hero: {
    eyebrow: '仪表板 · 商户视图',
    title: '商户实时经营体征',
    description: '关注头部商户的收付表现、风控状态与运营提醒，支撑跨国业务运营决策。',
    sync: '同步于 58 秒前'
  },
  spotlight: [
    {
      id: 'mpay',
      name: 'M-Pay Digital',
      country: 'India',
      gmvp: '¥12.7M',
      success: '98.7%',
      risk: '低',
      alert: 'UPI 通道切换观察中'
    },
    {
      id: 'vnet',
      name: 'V-Net Retail',
      country: 'Vietnam',
      gmvp: '¥6.9M',
      success: '97.4%',
      risk: '中',
      alert: '钱包资金池余量 26%'
    },
    {
      id: 'brtech',
      name: 'BR-Tech',
      country: 'Brazil',
      gmvp: '¥5.1M',
      success: '95.1%',
      risk: '高',
      alert: 'PIX 延迟回落中'
    }
  ],
  kpis: [
    { id: 'active', label: '活跃商户', value: '248', meta: '+12 新增' },
    { id: 'gmv', label: '当日 GMV', value: '¥94.3M', meta: '+8.3% 环比' },
    { id: 'approval', label: '风控放行率', value: '97.2%', meta: '+0.6% 周环比' },
    { id: 'refund', label: '退款率', value: '1.8%', meta: '-0.2% 日内' }
  ],
  merchantTable: [
    {
      id: 'merchant-1',
      name: 'PayRealm',
      region: '印度 / UPI',
      today: '¥4.5M',
      success: '98.9%',
      dispute: '0.3%',
      tier: 'A',
      owner: 'Annie',
      tags: ['Gaming', 'High SLA']
    },
    {
      id: 'merchant-2',
      name: 'LotusMart',
      region: '越南 / Wallet',
      today: '¥3.2M',
      success: '97.1%',
      dispute: '0.6%',
      tier: 'B',
      owner: 'Danny',
      tags: ['Retail', 'Wallet']
    },
    {
      id: 'merchant-3',
      name: 'NovaPay',
      region: '巴西 / PIX',
      today: '¥2.8M',
      success: '95.3%',
      dispute: '1.9%',
      tier: 'A',
      owner: 'Ling',
      tags: ['PIX', 'Attention']
    },
    {
      id: 'merchant-4',
      name: 'SkyTransfer',
      region: '阿联酋 / Bank',
      today: '¥1.9M',
      success: '96.4%',
      dispute: '0.8%',
      tier: 'B',
      owner: 'Sam',
      tags: ['Bank', 'USD']
    }
  ],
  retention: [
    { id: 'ret-1', label: '7 天留存', value: '89%', diff: '+3%' },
    { id: 'ret-2', label: '14 天留存', value: '83%', diff: '+1%' },
    { id: 'ret-3', label: '30 天留存', value: '78%', diff: '-2%' }
  ],
  tickets: [
    { id: 'ticket-1', title: 'BR-Tech PIX 对账差异', owner: 'Clearing', eta: '处理中' },
    { id: 'ticket-2', title: 'V-Net 额度扩容申请', owner: '运营', eta: '30 分钟' },
    { id: 'ticket-3', title: 'PayRealm AML 复核', owner: '风控', eta: '今日内' }
  ],
  campaigns: [
    { id: 'camp-1', name: '印度 Diwali 返利', lift: '+12.4%', status: '进行中' },
    { id: 'camp-2', name: '越南钱包红包', lift: '+5.6%', status: '进行中' },
    { id: 'camp-3', name: '巴西 PIX 手续费减免', lift: '+2.1%', status: '待复盘' }
  ],
  notices: [
    { id: 'note-1', text: '支付策略团队完成 UPI fallback 方案演练。' },
    { id: 'note-2', text: '商户条款更新：新增跨境反洗钱条款。' },
    { id: 'note-3', text: 'Wallet SEA SDK v2.1 已灰度至 60%。' }
  ],
  riskSignals: [
    { id: 'risk-1', merchant: 'NovaPay', signal: '退款率 > 1.5%', action: '拉取风控报告' },
    { id: 'risk-2', merchant: 'LotusMart', signal: 'OTP 失败率 4%', action: '通知本地供应商' },
    { id: 'risk-3', merchant: 'M-Pay Digital', signal: '额度使用 91%', action: '准备补充担保' }
  ]
};

export const dashboardFundsView = {
  hero: {
    eyebrow: '仪表板 · 资金流水',
    title: '跨境资金实时流转',
    description: '展示重点币种的入出款节奏、余额、在途与调度状态，辅助资金调配。',
    sync: '同步于 12 秒前'
  },
  stats: [
    { id: 'balance', label: '全局余额', value: '¥238M', meta: '在途 ¥26M' },
    { id: 'inflow', label: '今日入账', value: '¥84M', meta: '+11% 同期' },
    { id: 'outflow', label: '今日出账', value: '¥79M', meta: '+6% 同期' },
    { id: 'settlement', label: '待结算', value: '¥18M', meta: '48 笔' }
  ],
  balances: [
    { id: 'bal-inr', label: 'INR', value: '¥62M', utilization: 72 },
    { id: 'bal-vnd', label: 'VND', value: '¥38M', utilization: 54 },
    { id: 'bal-brl', label: 'BRL', value: '¥44M', utilization: 81 },
    { id: 'bal-idr', label: 'IDR', value: '¥26M', utilization: 43 },
    { id: 'bal-aed', label: 'AED', value: '¥18M', utilization: 65 }
  ],
  streams: [
    { id: 'stream-in-1', label: 'UPI Inflow', value: '¥32M', trend: '+8%', type: 'in' },
    { id: 'stream-in-2', label: 'Wallet SEA', value: '¥21M', trend: '+4%', type: 'in' },
    { id: 'stream-out-1', label: 'PIX Outflow', value: '¥18M', trend: '+11%', type: 'out' },
    { id: 'stream-out-2', label: 'Bank Payout', value: '¥13M', trend: '-6%', type: 'out' }
  ],
  ledger: [
    {
      id: 'ledger-1',
      time: '10:32',
      channel: 'UPI',
      direction: '入账',
      amount: '+¥4,200,000',
      account: 'INR-Mumbai-01',
      status: '已记账'
    },
    {
      id: 'ledger-2',
      time: '10:28',
      channel: 'Wallet',
      direction: '入账',
      amount: '+¥1,900,000',
      account: 'VND-HCMC-02',
      status: '在途'
    },
    {
      id: 'ledger-3',
      time: '10:26',
      channel: 'Bank',
      direction: '出账',
      amount: '-¥2,100,000',
      account: 'BRL-SaoPaulo-01',
      status: '待复核'
    },
    {
      id: 'ledger-4',
      time: '10:21',
      channel: 'PIX',
      direction: '出账',
      amount: '-¥1,300,000',
      account: 'BRL-Rio-02',
      status: '已记账'
    }
  ],
  pipelines: [
    { id: 'pipe-1', label: '在途清算', items: ['UPI→NPCI', 'Wallet→当地银行', 'PIX→Bacen'] },
    { id: 'pipe-2', label: '待复核', items: ['BR-Tech 资金回补', 'V-Net 批量提款'] }
  ],
  alerts: [
    {
      id: 'fund-alert-1',
      severity: '高',
      title: 'BRL 头寸不足',
      detail: '剩余 19%，预计 40 分钟触底',
      action: '触发跨币补仓'
    },
    {
      id: 'fund-alert-2',
      severity: '中',
      title: 'INR 资金在途超 12%',
      detail: 'Mumbai 银行确认延后',
      action: '通知对方升级通道'
    }
  ],
  transfers: [
    { id: 'transfer-1', title: '资金调拨：INR → BRL', owner: 'Clearing', progress: 64, eta: '13:30' },
    { id: 'transfer-2', title: 'Wallet SEA 回补', owner: 'Ops', progress: 42, eta: '14:10' },
    { id: 'transfer-3', title: 'AED 归集', owner: 'Treasury', progress: 78, eta: '12:50' }
  ]
};

export const dashboardBankTradeView = {
  hero: {
    eyebrow: '仪表板 · BankTrade',
    title: 'BankTrade 接口联动状态',
    description: '监控 BankTrade API 成功率、延迟、调度队列与异常告警，确保银行联机稳定。',
    sync: '同步于 1 分钟前'
  },
  stats: [
    { id: 'bt-volume', label: '今日交易量', value: '182,430', meta: '+9.4% vs 昨日' },
    { id: 'bt-success', label: '成功率', value: '98.12%', meta: 'SLA 97.5%' },
    { id: 'bt-latency', label: '平均延迟', value: '1.6s', meta: '+0.2s' },
    { id: 'bt-errors', label: '错误峰值', value: '312', meta: '自愈 74%' }
  ],
  serviceHealth: [
    { id: 'auth', name: 'Auth', status: 'green', latency: '520ms', success: '99.1%' },
    { id: 'txn', name: 'Transaction', status: 'amber', latency: '1.4s', success: '96.8%' },
    { id: 'recon', name: 'Recon', status: 'green', latency: '820ms', success: '98.7%' },
    { id: 'notify', name: 'Notify', status: 'green', latency: '320ms', success: '99.6%' }
  ],
  queues: [
    { id: 'queue-auth', name: 'AuthQueue', depth: 240, wait: '1.2s' },
    { id: 'queue-settle', name: 'SettleQueue', depth: 420, wait: '2.9s' },
    { id: 'queue-retry', name: 'RetryQueue', depth: 118, wait: '0.8s' }
  ],
  throughput: [
    { time: '10:10', success: 6200, failed: 140 },
    { time: '10:15', success: 6800, failed: 210 },
    { time: '10:20', success: 7200, failed: 260 },
    { time: '10:25', success: 7600, failed: 310 },
    { time: '10:30', success: 8000, failed: 280 }
  ],
  incidents: [
    {
      id: 'bt-inc-1',
      severity: '高',
      title: 'Transaction API 超时波动',
      detail: '银行回执延迟 2.8s',
      action: '手动切换备用线路'
    },
    {
      id: 'bt-inc-2',
      severity: '中',
      title: 'Recon 差异上升',
      detail: '15 笔记录需人工复核',
      action: '清算团队处理中'
    }
  ],
  releaseTimeline: [
    { id: 'bt-rel-1', time: '09:30', title: '部署 v2.13 热修复', owner: 'DevOps' },
    { id: 'bt-rel-2', time: '09:55', title: '重新加载交易缓存', owner: 'BankTrade' },
    { id: 'bt-rel-3', time: '10:20', title: '启用限流保护', owner: 'Risk' }
  ],
  integrations: [
    { id: 'bt-int-1', title: 'BankTrade v2.2 对接', owner: 'Integration', progress: 58, eta: '本周' },
    { id: 'bt-int-2', title: 'NPCI 直连 DR 演练', owner: 'SRE', progress: 32, eta: '周五' },
    { id: 'bt-int-3', title: '账务字段映射清单', owner: 'Finance', progress: 80, eta: '今日' }
  ],
  notices: [
    { id: 'bt-note-1', text: 'BankTrade SDK v2.13 补丁已上线东南亚区域。' },
    { id: 'bt-note-2', text: '银行侧计划 22:00-23:00 日常维护，请提前调度。' }
  ]
};

export const dashboardBankIdView = {
  hero: {
    eyebrow: '仪表板 · BankId',
    title: 'BankId 实名链路实时体征',
    description: '监控 BankId OTP、Auth、Callback 的成功率、延迟与异常，保障实名认证闭环。',
    sync: '同步于 45 秒前'
  },
  stats: [
    { id: 'bid-otp', label: 'OTP 请求', value: '82,430', meta: '+8.3% vs 昨日' },
    { id: 'bid-auth', label: '认证成功率', value: '97.8%', meta: 'SLA 97%' },
    { id: 'bid-latency', label: '平均延迟', value: '1.2s', meta: '+0.1s' },
    { id: 'bid-errors', label: '错误数', value: '214', meta: '处理中 68%' }
  ],
  flows: [
    { id: 'flow-otp', name: 'OTP 下发', success: '98.5%', latency: '820ms', status: 'green' },
    { id: 'flow-auth', name: 'Auth 校验', success: '96.8%', latency: '1.4s', status: 'amber' },
    { id: 'flow-callback', name: 'Callback 回调', success: '99.3%', latency: '620ms', status: 'green' }
  ],
  channels: [
    { id: 'ch-in', country: '印度', operator: 'NPCI', requests: '34%', trend: '+6%' },
    { id: 'ch-vn', country: '越南', operator: 'MoMo', requests: '22%', trend: '+3%' },
    { id: 'ch-id', country: '印尼', operator: 'Telkomsel', requests: '18%', trend: '+1%' },
    { id: 'ch-br', country: '巴西', operator: 'Sicoob', requests: '14%', trend: '-2%' }
  ],
  sessions: [
    { id: 'session-1', merchant: 'NovaPay', region: '印度', otp: '98.3%', auth: '96.2%', fallback: '启用' },
    { id: 'session-2', merchant: 'LotusMart', region: '越南', otp: '97.8%', auth: '98.1%', fallback: '关闭' },
    { id: 'session-3', merchant: 'BR-Tech', region: '巴西', otp: '95.5%', auth: '94.1%', fallback: '启用' }
  ],
  incidents: [
    {
      id: 'bid-inc-1',
      severity: '中',
      title: 'Auth latency 上升',
      detail: '银行侧 CPU 78%，延迟拉长 0.4s',
      action: '已启动备用策略'
    },
    {
      id: 'bid-inc-2',
      severity: '低',
      title: 'Callback 重试 12 次',
      detail: '代理商通知失败，正在重发',
      action: 'OPS 跟进'
    }
  ],
  backlog: [
    { id: 'task-1', title: 'OTP 核心 SDK 升级', owner: '前端', progress: 70, eta: '今日' },
    { id: 'task-2', title: 'Auth 机房扩容', owner: 'SRE', progress: 35, eta: '本周五' },
    { id: 'task-3', title: 'Callback 监控整合', owner: '平台', progress: 55, eta: '下周一' }
  ],
  notices: [
    { id: 'bid-note-1', text: 'BankId API v3.1 beta 已开启灰度，请关注日志。' },
    { id: 'bid-note-2', text: 'Telkomsel 本周六凌晨维护，OTP 将降级备用通道。' }
  ]
};

export const dashboardBankMonitorView = {
  hero: {
    eyebrow: '仪表板 · BankMonitor',
    title: '银行监控策略与阈值',
    description: '集中展示银行联机策略、实时告警、限额、探活与自动化动作。',
    sync: '同步于 18 秒前'
  },
  stats: [
    { id: 'bm-bank', label: '监控银行数', value: '36', meta: '+4 新增' },
    { id: 'bm-alarms', label: '活跃告警', value: '12', meta: '处理中 5' },
    { id: 'bm-rules', label: '启用策略', value: '82', meta: '禁用 3' },
    { id: 'bm-ping', label: '探活成功率', value: '99.1%', meta: '-0.2% vs 昨日' }
  ],
  banks: [
    { id: 'bank-hdfc', name: 'HDFC', region: 'India', latency: '1.2s', status: 'green', incidents: 1 },
    { id: 'bank-bca', name: 'BCA', region: 'Indonesia', latency: '1.6s', status: 'amber', incidents: 2 },
    { id: 'bank-momo', name: 'MoMo', region: 'Vietnam', latency: '0.9s', status: 'green', incidents: 0 },
    { id: 'bank-bradesco', name: 'Bradesco', region: 'Brazil', latency: '2.2s', status: 'red', incidents: 3 }
  ],
  thresholds: [
    { id: 'th-latency', label: '延迟阈值', value: '2.0s', action: '触发限流' },
    { id: 'th-success', label: '成功率阈值', value: '96%', action: '自动切备用' },
    { id: 'th-queue', label: '队列深度', value: '1500', action: '告警 + 扩容' }
  ],
  alerts: [
    {
      id: 'bm-alert-1',
      severity: '高',
      title: 'Bradesco 探活失败 4 次',
      detail: 'Fallback 已启用',
      action: '联系银行确认'
    },
    {
      id: 'bm-alert-2',
      severity: '中',
      title: 'BCA 成功率 95.4%',
      detail: '低于阈值 0.6%',
      action: '维持限流'
    }
  ],
  automation: [
    { id: 'auto-1', title: 'UPI 夜间负载切换', owner: 'SRE', progress: 72, eta: '明日' },
    { id: 'auto-2', title: '银行探活机器人', owner: 'OPS', progress: 48, eta: '本周' }
  ],
  maintenance: [
    { id: 'maint-1', bank: 'HDFC', window: '22:00-23:00', note: '核心系统切割' },
    { id: 'maint-2', bank: 'Bradesco', window: '02:00-04:00', note: 'DR 演练' }
  ],
  notices: [
    { id: 'bm-note-1', text: 'BankMonitor 新增 webhook 模块，支持 Telegram 通知。' },
    { id: 'bm-note-2', text: '南亚区域探活脚本迁移至新版运行器。' }
  ]
};

export const dashboardAgentLogView = {
  hero: {
    eyebrow: '仪表板 · AgentAccountLog',
    title: '代理账户关键操作日志',
    description: '跟踪代理商账户充值、限额、风控拦截及审批流，确保操作透明可追溯。',
    sync: '同步于 10 秒前'
  },
  stats: [
    { id: 'agent-ops', label: '今日操作', value: '214', meta: '+12% vs 昨日' },
    { id: 'agent-approve', label: '自动通过率', value: '86%', meta: '-2% 日内' },
    { id: 'agent-risk', label: '风控拦截', value: '18', meta: '处理中 5' },
    { id: 'agent-users', label: '活跃代理', value: '64', meta: '在线 38' }
  ],
  filters: ['全部', '充值', '限额', '风控', '审批'],
  operations: [
    {
      id: 'op-1',
      agent: 'Agent SEA-01',
      type: '充值',
      amount: '+¥420,000',
      status: '成功',
      operator: 'zya',
      time: '10:36'
    },
    {
      id: 'op-2',
      agent: 'Agent SEA-02',
      type: '限额',
      amount: '新额度 ¥2,000,000',
      status: '审批中',
      operator: 'amy',
      time: '10:22'
    },
    {
      id: 'op-3',
      agent: 'Agent LATAM-01',
      type: '风控',
      amount: '冻结 ¥380,000',
      status: '拦截',
      operator: 'riskbot',
      time: '10:10'
    },
    {
      id: 'op-4',
      agent: 'Agent IN-03',
      type: '审批',
      amount: '待复核',
      status: '待处理',
      operator: 'huang',
      time: '09:55'
    }
  ],
  approvals: [
    { id: 'ap-1', title: 'Agent SEA-01 额度上调', stage: '复核通过', owner: 'zya', eta: '完成' },
    { id: 'ap-2', title: 'Agent LATAM-01 风控解冻', stage: '自动审批', owner: 'riskbot', eta: '进行中' },
    { id: 'ap-3', title: 'Agent IN-03 人工充值', stage: '等待签核', owner: 'huang', eta: '30 分钟' }
  ],
  insights: [
    { id: 'ins-1', text: 'SEA 代理本周充值同比 +21%，关注余额转化。' },
    { id: 'ins-2', text: 'LATAM 代理风控拦截集中于夜间，建议增配自动化。' }
  ],
  auditTimeline: [
    { id: 'audit-1', time: '10:30', action: '额度策略同步至风控系统', owner: '策略' },
    { id: 'audit-2', time: '10:15', action: '自动审批规则 v2 上线', owner: '平台' },
    { id: 'audit-3', time: '09:40', action: '风控机器人拦截超限操作', owner: 'RiskBot' }
  ]
};

export const paymentsEntityView = {
  hero: {
    eyebrow: '支付 · 主体配置',
    title: '主体参数与风控配置',
    description: '管理主体资质、合同、限额、合规状态与审批流，支持多区域运营。',
    sync: '同步于 2 分钟前'
  },
  stats: [
    { id: 'entity-total', label: '主体数量', value: '42', meta: '+2 新增' },
    { id: 'entity-active', label: '启用', value: '36', meta: '冻结 3' },
    { id: 'entity-compliance', label: '合规通过率', value: '94%', meta: '+1.2% 周环比' },
    { id: 'entity-review', label: '待审批', value: '7', meta: '紧急 2' }
  ],
  filters: ['全部', '启用', '冻结', '审批中'],
  entities: [
    {
      id: 'entity-1',
      name: 'SEA Commerce Pte',
      region: '新加坡',
      industry: '电商',
      status: '启用',
      limit: '¥8,000,000 / 日',
      owner: 'zya'
    },
    {
      id: 'entity-2',
      name: 'India Pay LLP',
      region: '印度',
      industry: '聚合支付',
      status: '审批中',
      limit: '待评估',
      owner: 'anuj'
    },
    {
      id: 'entity-3',
      name: 'LATAM Express',
      region: '巴西',
      industry: '出海游戏',
      status: '冻结',
      limit: '¥2,500,000 / 日',
      owner: 'liao'
    }
  ],
  compliance: [
    { id: 'comp-1', label: 'KYC 完成', percent: 92 },
    { id: 'comp-2', label: '合同盖章', percent: 88 },
    { id: 'comp-3', label: '风控策略配置', percent: 74 }
  ],
  documents: [
    { id: 'doc-1', title: 'SEA Commerce 主体合同', type: 'PDF', status: '有效' },
    { id: 'doc-2', title: 'India Pay 风控函', type: 'DOCX', status: '待更新' },
    { id: 'doc-3', title: 'LATAM Express AML 报告', type: 'PDF', status: '审核中' }
  ],
  notices: [
    { id: 'pay-note-1', text: '主体模板 v3.2 已发布，新增多币种字段。' },
    { id: 'pay-note-2', text: '本周五 18:00 进行合同库维护，注意提前下载。' }
  ],
  approvals: [
    { id: 'appr-1', title: 'India Pay 主体启用审批', stage: '风控复核', owner: 'risk' },
    { id: 'appr-2', title: 'LATAM Express 降额', stage: '法务评估', owner: 'legal' }
  ]
};

export const paymentsChannelView = {
  hero: {
    eyebrow: '支付 · 通道配置',
    title: '通道路由与限额',
    description: '统一管理跨国支付通道的权重、限额、费率、风控与维护状态。',
    sync: '同步于 1 分钟前'
  },
  stats: [
    { id: 'channel-total', label: '启用通道', value: '58', meta: '+3 新上线' },
    { id: 'channel-active', label: '活跃路由', value: '42', meta: '禁用 6' },
    { id: 'channel-rate', label: '平均费率', value: '2.3%', meta: '-0.1% 日内' },
    { id: 'channel-alerts', label: '告警中的通道', value: '4', meta: '紧急 1' }
  ],
  filters: ['全部', 'UPI', 'Wallet', 'Bank', 'Card'],
  channels: [
    {
      id: 'channel-upi',
      name: 'UPI-北区',
      region: '印度',
      weight: '35%',
      limit: '¥5,000,000 / 日',
      fee: '1.2%',
      status: '启用'
    },
    {
      id: 'channel-wallet',
      name: 'Wallet-VN',
      region: '越南',
      weight: '18%',
      limit: '¥2,000,000 / 日',
      fee: '2.5%',
      status: '启用'
    },
    {
      id: 'channel-bank',
      name: 'Bank-PH',
      region: '菲律宾',
      weight: '22%',
      limit: '¥3,200,000 / 日',
      fee: '1.7%',
      status: '限流'
    },
    {
      id: 'channel-card',
      name: 'Card-SEA',
      region: '东南亚',
      weight: '12%',
      limit: '¥1,200,000 / 日',
      fee: '2.9%',
      status: '停用'
    }
  ],
  routing: [
    { id: 'route-1', title: 'SEA 钱包权重调优', owner: '策略', progress: 64 },
    { id: 'route-2', title: 'UPI 夜间限流方案', owner: '风控', progress: 38 }
  ],
  maintenance: [
    { id: 'maint-1', channel: 'UPI-北区', window: '今晚 02:00-03:00', note: '银行升级' },
    { id: 'maint-2', channel: 'Card-SEA', window: '周五 23:00-01:00', note: '清算维护' }
  ],
  notices: [
    { id: 'ch-note-1', text: '通道模板新增 EMI 字段，请同步。' },
    { id: 'ch-note-2', text: '越南钱包费率谈判完成，下周生效。' }
  ]
};

export const paymentsErrorView = {
  hero: {
    eyebrow: '支付 · 通道错误记录',
    title: '通道异常与处理进度',
    description: '聚合通道错误信息、影响范围、处理策略与复盘结论，保证快速自愈。',
    sync: '同步于 30 秒前'
  },
  stats: [
    { id: 'err-today', label: '今日错误', value: '38', meta: '紧急 4' },
    { id: 'err-fixed', label: '已解决', value: '26', meta: '自愈 18' },
    { id: 'err-avg', label: '平均恢复', value: '12m', meta: '-3m 日内' },
    { id: 'err-open', label: '处理中', value: '12', meta: '升级 3' }
  ],
  filters: ['全部', 'UPI', 'Wallet', 'Bank', 'Card'],
  errors: [
    {
      id: 'err-1',
      channel: 'UPI-北区',
      type: '上游超时',
      impact: '3.4%',
      status: '处理中',
      owner: 'ops',
      updated: '10:30'
    },
    {
      id: 'err-2',
      channel: 'Wallet-VN',
      type: '余额不足',
      impact: '停用',
      status: '已解决',
      owner: 'wallet',
      updated: '10:12'
    },
    {
      id: 'err-3',
      channel: 'Bank-PH',
      type: '回调失败',
      impact: '重试 24%',
      status: '监控',
      owner: 'bank',
      updated: '09:58'
    }
  ],
  actions: [
    { id: 'act-1', title: 'UPI 超时限流策略', owner: 'ops', progress: 52 },
    { id: 'act-2', title: 'Wallet 余额告警阈值调高', owner: 'wallet', progress: 78 },
    { id: 'act-3', title: 'Bank 回调重试升级', owner: 'bank', progress: 34 }
  ],
  rootCauses: [
    { id: 'rc-1', channel: 'UPI-北区', cause: '银行侧 CPU 饱和', resolution: '临时分流 + 扩容' },
    { id: 'rc-2', channel: 'Wallet-VN', cause: '供应商资金池不足', resolution: '追加备付 + 风控监控' }
  ],
  notices: [
    { id: 'err-note-1', text: '错误码模板 v2.4 已发布，新增自愈建议。' },
    { id: 'err-note-2', text: 'Bank-PH 下周进行 API 版本升级，请关注回调。' }
  ]
};

export const paymentsRollbackView = {
  hero: {
    eyebrow: '支付 · 通道回滚',
    title: '回滚策略与执行记录',
    description: '统一查看手动/自动回滚任务、状态、覆盖范围与复盘，保障风险可控。',
    sync: '同步于 20 秒前'
  },
  stats: [
    { id: 'roll-today', label: '今日回滚任务', value: '12', meta: '自动 7' },
    { id: 'roll-success', label: '成功率', value: '91%', meta: '+3% 日内' },
    { id: 'roll-active', label: '进行中', value: '3', meta: '高优 1' },
    { id: 'roll-pending', label: '待审批', value: '2', meta: '总回滚量 ¥3.2M' }
  ],
  filters: ['全部', '手动', '自动', '审批中', '完成'],
  tasks: [
    {
      id: 'task-1',
      title: 'UPI 自动回滚',
      channel: 'UPI-北区',
      mode: '自动',
      amount: '¥820,000',
      status: '进行中',
      owner: 'riskbot',
      updated: '10:35'
    },
    {
      id: 'task-2',
      title: 'Wallet 手动回滚',
      channel: 'Wallet-VN',
      mode: '手动',
      amount: '¥260,000',
      status: '待审批',
      owner: 'huang',
      updated: '10:20'
    },
    {
      id: 'task-3',
      title: 'Bank-PH 限额回滚',
      channel: 'Bank-PH',
      mode: '自动',
      amount: '¥1,200,000',
      status: '已完成',
      owner: 'ops',
      updated: '09:55'
    }
  ],
  steps: [
    { id: 'step-1', title: '审批通过率', value: '92%', detail: '风控自动审批' },
    { id: 'step-2', title: '流水同步', value: '99%', detail: '已同步结算' },
    { id: 'step-3', title: '回溯告警', value: '3', detail: '需要复盘' }
  ],
  approvals: [
    { id: 'appr-1', title: 'Wallet 回滚申请', stage: '合规审批', owner: 'legal' },
    { id: 'appr-2', title: 'UPI 自动策略调整', stage: '风控评估', owner: 'risk' }
  ],
  notices: [
    { id: 'roll-note-1', text: '回滚工具 v1.4 已上线，支持批量回滚可视化。' },
    { id: 'roll-note-2', text: '建议对 Bank-PH 近期回滚做复盘会议。' }
  ]
};

export const paymentsReconcileView = {
  hero: {
    eyebrow: '支付 · 通道对账',
    title: '对账差异与处理进度',
    description: '聚合通道对账状态、差异原因、补单与财务确认流程，支持多区域多币种。',
    sync: '同步于 1 分钟前'
  },
  stats: [
    { id: 'rec-files', label: '今日对账文件', value: '74', meta: '成功 68' },
    { id: 'rec-diff', label: '差异笔数', value: '12', meta: '金额 ¥420,000' },
    { id: 'rec-closed', label: '已消除', value: '7', meta: '平均 18m' },
    { id: 'rec-open', label: '待处理', value: '5', meta: '升级 2' }
  ],
  filters: ['全部', '待处理', '待财务确认', '已解决'],
  files: [
    {
      id: 'file-1',
      channel: 'UPI-北区',
      currency: 'INR',
      diffCount: 4,
      diffAmount: '₹260,000',
      status: '待财务确认',
      owner: 'finance',
      updated: '10:30'
    },
    {
      id: 'file-2',
      channel: 'Wallet-VN',
      currency: 'VND',
      diffCount: 3,
      diffAmount: '₫820,000,000',
      status: '处理中',
      owner: 'ops',
      updated: '10:12'
    },
    {
      id: 'file-3',
      channel: 'Bank-PH',
      currency: 'PHP',
      diffCount: 5,
      diffAmount: '₱1,200,000',
      status: '已解决',
      owner: 'finance',
      updated: '09:58'
    }
  ],
  actions: [
    { id: 'action-1', title: 'UPI 差异补单', owner: 'ops', progress: 56, eta: '今日' },
    { id: 'action-2', title: 'Wallet 对账脚本优化', owner: 'platform', progress: 34, eta: '周五' },
    { id: 'action-3', title: 'Bank-PH 财务确认', owner: 'finance', progress: 82, eta: '进行中' }
  ],
  timelines: [
    { id: 'tl-1', time: '10:20', title: 'UPI 对账文件上传', owner: '银行' },
    { id: 'tl-2', time: '10:28', title: '触发差异常量告警', owner: '风控' },
    { id: 'tl-3', time: '10:35', title: '财务复核分配', owner: 'Finance' }
  ],
  notices: [
    { id: 'rec-note-1', text: '对账模板 v2.0 已上线，支持多币种自动转换。' },
    { id: 'rec-note-2', text: '本周开展 Wallet 差异专项复盘，请提交资料。' }
  ]
};

export const paymentsThrottleView = {
  hero: {
    eyebrow: '支付 · 通道限流',
    title: '限流策略与实时曲线',
    description: '监控通道限流阈值、触发记录与解限计划，确保稳定产能。',
    sync: '同步于 40 秒前'
  },
  stats: [
    { id: 'thr-active', label: '限流策略', value: '12', meta: '自动 9' },
    { id: 'thr-trigger', label: '今日触发', value: '8', meta: '紧急 2' },
    { id: 'thr-release', label: '已解除', value: '5', meta: '平均 16m' },
    { id: 'thr-impact', label: '受限流量', value: '¥2.1M', meta: '+4% 日内' }
  ],
  filters: ['全部', '自动', '手动', '进行中', '已解除'],
  channels: [
    {
      id: 'thr-upi',
      channel: 'UPI-北区',
      reason: '上游超时',
      limit: '850 TPS',
      status: '进行中',
      owner: 'riskbot',
      updated: '10:36'
    },
    {
      id: 'thr-wallet',
      channel: 'Wallet-VN',
      reason: '余额不足',
      limit: '停用',
      status: '已解除',
      owner: 'ops',
      updated: '10:18'
    },
    {
      id: 'thr-bank',
      channel: 'Bank-PH',
      reason: 'CPU 高',
      limit: '限额 60%',
      status: '进行中',
      owner: 'ops',
      updated: '09:55'
    }
  ],
  curves: [
    { id: 'curve-1', label: 'UPI 限流曲线', detail: '当前 820 TPS / 850 TPS' },
    { id: 'curve-2', label: 'Wallet 限流曲线', detail: '恢复后 1,200 TPS' }
  ],
  plans: [
    { id: 'plan-1', title: 'UPI 扩容计划', owner: 'SRE', progress: 62, eta: '今日' },
    { id: 'plan-2', title: 'Wallet 限流阈值调优', owner: '策略', progress: 44, eta: '周五' }
  ],
  notices: [
    { id: 'thr-note-1', text: '限流策略模板 v1.5 已上线，支持多维条件。' },
    { id: 'thr-note-2', text: '建议对近期 UPI 限流事件复盘。' }
  ]
};

export const paymentsFallbackView = {
  hero: {
    eyebrow: '支付 · 通道兜底',
    title: '兜底策略与执行记录',
    description: '监控通道兜底启用状态、自动切换、应急动作与复盘结论，保障业务连续性。',
    sync: '同步于 50 秒前'
  },
  stats: [
    { id: 'fb-enabled', label: '启用兜底策略', value: '18', meta: '自动 12' },
    { id: 'fb-trigger-today', label: '今日触发', value: '5', meta: '紧急 1' },
    { id: 'fb-success', label: '成功率', value: '94%', meta: '+2% 日内' },
    { id: 'fb-recovery', label: '平均恢复', value: '7m', meta: '-1m' }
  ],
  filters: ['全部', '自动', '手动', '进行中', '已完成'],
  records: [
    {
      id: 'fb-1',
      channel: 'UPI-北区',
      strategy: '自动切 PPC',
      reason: '上游延迟',
      status: '进行中',
      owner: 'riskbot',
      updated: '10:32'
    },
    {
      id: 'fb-2',
      channel: 'Wallet-VN',
      strategy: '手动切备用',
      reason: '余额不足',
      status: '完成',
      owner: 'ops',
      updated: '10:10'
    },
    {
      id: 'fb-3',
      channel: 'Bank-PH',
      strategy: '自动切换合作行',
      reason: '回调失败',
      status: '监控',
      owner: 'ops',
      updated: '09:55'
    }
  ],
  actions: [
    { id: 'fb-act-1', title: 'UPI PPC 兜底扩容', owner: 'SRE', progress: 48, eta: '今日' },
    { id: 'fb-act-2', title: 'Wallet 备用资金补充', owner: 'Finance', progress: 72, eta: '周五' }
  ],
  playbooks: [
    { id: 'fb-play-1', title: 'UPI 兜底手册 v3', status: '已更新' },
    { id: 'fb-play-2', title: 'Wallet 应急指引', status: '待复盘' }
  ],
  notices: [
    { id: 'fb-note-1', text: '兜底策略模板 v2.0 已上线，支持多通道联动。' },
    { id: 'fb-note-2', text: '请对最近 Bank-PH 兜底事件发起复盘。' }
  ]
};

export const paymentsIfscBlacklistView = {
  hero: {
    eyebrow: '支付 · IFSC公户代付黑名单',
    title: 'IFSC 黑名单与复核',
    description: '统一管理公户代付黑名单，涵盖录入、原因、复核进度与同步状态。',
    sync: '同步于 12 秒前'
  },
  stats: [
    { id: 'ifsc-total', label: '黑名单数', value: '682', meta: '+12 今日新增' },
    { id: 'ifsc-active', label: '启用条目', value: '640', meta: '冻结 22' },
    { id: 'ifsc-review', label: '待复核', value: '18', meta: '紧急 4' },
    { id: 'ifsc-sync', label: '同步成功率', value: '99.2%', meta: '+0.3% 周内' }
  ],
  filters: ['全部', '待复核', '启用', '冻结'],
  records: [
    {
      id: 'ifsc-1',
      account: 'HDFC-AC12345',
      reason: '疑似欺诈',
      status: '待复核',
      owner: 'risk',
      created: '10:30',
      updated: '10:35'
    },
    {
      id: 'ifsc-2',
      account: 'ICICI-AC56789',
      reason: '异常退款',
      status: '启用',
      owner: 'ops',
      created: '09:50',
      updated: '10:10'
    },
    {
      id: 'ifsc-3',
      account: 'Axis-AC99887',
      reason: '重复出款',
      status: '冻结',
      owner: 'finance',
      created: '09:30',
      updated: '09:45'
    }
  ],
  actions: [
    { id: 'ifsc-act-1', title: '批量黑名单导入', owner: 'ops', progress: 64, eta: '今日' },
    { id: 'ifsc-act-2', title: '黑名单 API 同步', owner: 'platform', progress: 46, eta: '周四' }
  ],
  audits: [
    { id: 'ifsc-audit-1', time: '10:20', title: '风险模型触发 HDFC 黑名单', owner: 'riskbot' },
    { id: 'ifsc-audit-2', time: '10:05', title: 'ICICI 黑名单复核通过', owner: 'risk' }
  ],
  notices: [
    { id: 'ifsc-note-1', text: 'IFSC 黑名单模板 v2.1 上线，新增理由分类。' },
    { id: 'ifsc-note-2', text: '请对冻结状态条目进行季度复核。' }
  ]
};

export const paymentsBlacklistView = {
  hero: {
    eyebrow: '支付 · 黑名单',
    title: '全局黑名单管理',
    description: '统一管理支付黑名单，覆盖主体、账户、设备及风控动作，支持多团队协同。',
    sync: '同步于 25 秒前'
  },
  stats: [
    { id: 'bl-total', label: '黑名单条目', value: '1,420', meta: '+28 新增' },
    { id: 'bl-active', label: '启用', value: '1,318', meta: '冻结 44' },
    { id: 'bl-review', label: '待复核', value: '36', meta: '紧急 5' },
    { id: 'bl-sync', label: '同步成功率', value: '99.4%', meta: '+0.6% 周内' }
  ],
  filters: ['全部', '主体', '账户', '设备', '待复核'],
  entries: [
    {
      id: 'bl-1',
      target: 'SEA Commerce Pte',
      type: '主体',
      reason: '异常退款',
      status: '启用',
      owner: 'risk',
      updated: '10:42'
    },
    {
      id: 'bl-2',
      target: 'UPI-AC55678',
      type: '账户',
      reason: '代付失败',
      status: '待复核',
      owner: 'ops',
      updated: '10:30'
    },
    {
      id: 'bl-3',
      target: 'Device-ID-8897',
      type: '设备',
      reason: '多账户登录',
      status: '启用',
      owner: 'riskbot',
      updated: '10:05'
    }
  ],
  actions: [
    { id: 'bl-act-1', title: '批量黑名单导入 v2', owner: 'ops', progress: 58, eta: '今日' },
    { id: 'bl-act-2', title: '设备黑名单风控联动', owner: 'platform', progress: 42, eta: '周五' }
  ],
  reviews: [
    { id: 'bl-review-1', title: 'UPI-AC55678 复核', owner: 'risk', eta: '30 分钟' },
    { id: 'bl-review-2', title: 'Device-ID-8897 复核', owner: 'ops', eta: '进行中' }
  ],
  notices: [
    { id: 'bl-note-1', text: '黑名单模板 v3.0 已支持自动去重。' },
    { id: 'bl-note-2', text: '本周需要完成季度黑名单复盘。' }
  ]
};

export const paymentsProfitView = {
  hero: {
    eyebrow: '支付 · 利润报表',
    title: '利润与成本分析',
    description: '聚合支付主体与通道的收入、成本、毛利及趋势，为经营决策提供依据。',
    sync: '同步于 3 分钟前'
  },
  stats: [
    { id: 'profit-today', label: '今日毛利', value: '¥4.2M', meta: '+6.3% 日环比' },
    { id: 'profit-margin', label: '毛利率', value: '18.2%', meta: '+0.8% 日内' },
    { id: 'profit-cost', label: '成本', value: '¥18.9M', meta: '+2.1% 日内' },
    { id: 'profit-volume', label: '交易量', value: '¥23.1M', meta: '+5.2% 日内' }
  ],
  filters: ['全部', '主体', '通道', '区域'],
  summaries: [
    {
      id: 'sum-1',
      name: 'SEA Commerce',
      type: '主体',
      revenue: '¥8.3M',
      cost: '¥6.7M',
      margin: '19.3%',
      trend: '+1.2%'
    },
    {
      id: 'sum-2',
      name: 'UPI-北区',
      type: '通道',
      revenue: '¥5.1M',
      cost: '¥4.1M',
      margin: '16.7%',
      trend: '+0.5%'
    },
    {
      id: 'sum-3',
      name: 'LATAM Express',
      type: '主体',
      revenue: '¥3.4M',
      cost: '¥2.7M',
      margin: '20.4%',
      trend: '-0.4%'
    }
  ],
  charts: [
    { id: 'chart-1', label: '毛利趋势', detail: '近 7 日' },
    { id: 'chart-2', label: '成本构成', detail: '成本占比' }
  ],
  notices: [
    { id: 'profit-note-1', text: '利润报表模板 v1.2 上线，新增区域维度。' },
    { id: 'profit-note-2', text: '请本周完成主体利润复核。' }
  ]
};

export const bankSuppliersView = {
  hero: {
    eyebrow: '银行户管理 · 供应商',
    title: '银行供应商档案',
    description: '展示银行供应商的资质、账户覆盖、状态与维护计划，支撑跨区域资金调配。',
    sync: '同步于 15 秒前'
  },
  stats: [
    { id: 'sup-total', label: '供应商数量', value: '24', meta: '+2 新增' },
    { id: 'sup-active', label: '启用供应商', value: '21', meta: '维护中 2' },
    { id: 'sup-accounts', label: '账户数量', value: '148', meta: '+12 本周' },
    { id: 'sup-coverage', label: '覆盖国家', value: '9', meta: '+1' }
  ],
  suppliers: [
    {
      id: 'sup-hdfc',
      name: 'HDFC Corporate',
      region: '印度',
      status: '启用',
      accounts: 36,
      contact: 'Anuj',
      notes: 'UPI & NEFT 全量'
    },
    {
      id: 'sup-bca',
      name: 'BCA Treasury',
      region: '印尼',
      status: '维护中',
      accounts: 24,
      contact: 'Rani',
      notes: '对账脚本升级'
    },
    {
      id: 'sup-bradesco',
      name: 'Bradesco FX',
      region: '巴西',
      status: '启用',
      accounts: 18,
      contact: 'Carlos',
      notes: 'PIX & TED'
    }
  ],
  filters: ['全部', '启用', '维护中', '停用'],
  maintenance: [
    { id: 'maint-1', supplier: 'BCA Treasury', window: '周三 22:00-23:00', note: '系统升级' },
    { id: 'maint-2', supplier: 'HDFC Corporate', window: '周五 01:00-02:00', note: '月度巡检' }
  ],
  notices: [
    { id: 'sup-note-1', text: '供应商档案模板 v2.0 已上线，支持合规字段。' },
    { id: 'sup-note-2', text: '请在本周内完成巴西供应商复核。' }
  ]
};

export const bankAccountsView = {
  hero: {
    eyebrow: '银行户管理 · 账户',
    title: '银行账户总览',
    description: '管理银行账户资产、限额、余额、状态与维护计划，保障跨国资金流。',
    sync: '同步于 5 秒前'
  },
  stats: [
    { id: 'acct-total', label: '账户数量', value: '186', meta: '+6 新增' },
    { id: 'acct-active', label: '启用账户', value: '170', meta: '冻结 8' },
    { id: 'acct-balance', label: '总余额', value: '¥82M', meta: '+3% 日内' },
    { id: 'acct-limits', label: '限额使用', value: '68%', meta: '+4% 日内' }
  ],
  accounts: [
    {
      id: 'acct-hdfc-01',
      name: 'HDFC-Mumbai-01',
      region: '印度',
      status: '启用',
      balance: '¥12M',
      limit: '¥20M',
      currency: 'INR',
      owner: 'Anuj'
    },
    {
      id: 'acct-bca-02',
      name: 'BCA-Jakarta-02',
      region: '印尼',
      status: '维护中',
      balance: '¥6M',
      limit: '¥12M',
      currency: 'IDR',
      owner: 'Rani'
    },
    {
      id: 'acct-bradesco-03',
      name: 'Bradesco-SP-03',
      region: '巴西',
      status: '启用',
      balance: '¥4M',
      limit: '¥8M',
      currency: 'BRL',
      owner: 'Carlos'
    }
  ],
  filters: ['全部', '启用', '冻结', '维护中'],
  maintenance: [
    { id: 'acct-maint-1', account: 'BCA-Jakarta-02', window: '今晚 23:00-00:00', note: '对账脚本' },
    { id: 'acct-maint-2', account: 'HDFC-Mumbai-01', window: '周四 02:00-03:00', note: '系统巡检' }
  ],
  notices: [
    { id: 'acct-note-1', text: '账户模板 v1.4 上线，支持动态限额。' },
    { id: 'acct-note-2', text: '请完成季度账户余额复盘。' }
  ]
};

export const bankChannelSettingsView = {
  hero: {
    eyebrow: '银行户管理 · 通道设置',
    title: '账户通道绑定',
    description: '管理银行账户与支付通道的绑定关系、权重与维护计划，保障路由稳定。',
    sync: '同步于 1 秒前'
  },
  stats: [
    { id: 'setting-bindings', label: '绑定关系', value: '82', meta: '+4 新建立' },
    { id: 'setting-active', label: '启用绑定', value: '76', meta: '维护中 3' },
    { id: 'setting-weight', label: '平均权重', value: '34%', meta: '+1% 日内' },
    { id: 'setting-alerts', label: '告警', value: '2', meta: '紧急 0' }
  ],
  bindings: [
    {
      id: 'bind-upi',
      account: 'HDFC-Mumbai-01',
      channel: 'UPI-北区',
      weight: '38%',
      status: '启用',
      fallback: 'PPC',
      updated: '10:36'
    },
    {
      id: 'bind-wallet',
      account: 'BCA-Jakarta-02',
      channel: 'Wallet-VN',
      weight: '26%',
      status: '维护中',
      fallback: '停用',
      updated: '10:12'
    },
    {
      id: 'bind-bank',
      account: 'Bradesco-SP-03',
      channel: 'Bank-PH',
      weight: '18%',
      status: '启用',
      fallback: 'TED',
      updated: '09:58'
    }
  ],
  filters: ['全部', '启用', '维护中', '停用'],
  plans: [
    { id: 'setting-plan-1', title: 'UPI 权重调优', owner: '策略', progress: 52, eta: '今日' },
    { id: 'setting-plan-2', title: 'Wallet 绑定回归测试', owner: '平台', progress: 38, eta: '周五' }
  ],
  notices: [
    { id: 'setting-note-1', text: '通道设置模板 v1.3 已上线，支持多 fallback。' },
    { id: 'setting-note-2', text: '请确认菲律宾账户绑定策略。' }
  ]
};

export const bankRealtimeBoardView = {
  hero: {
    eyebrow: '银行户管理 · 公户实时面板',
    title: '公户实时状态',
    description: '监控重点公户余额、交易速率、风险指标与告警，保障公户运营稳定。',
    sync: '同步于 8 秒前'
  },
  stats: [
    { id: 'board-balance', label: '总余额', value: '¥62M', meta: '+4% 日内' },
    { id: 'board-rate', label: '交易速率', value: '1.8k / min', meta: '+6% 日内' },
    { id: 'board-alerts', label: '告警', value: '3', meta: '高 1 / 中 2' },
    { id: 'board-limits', label: '限额使用', value: '71%', meta: '+3% 日内' }
  ],
  accounts: [
    {
      id: 'board-hdfc',
      name: 'HDFC-Mumbai-01',
      balance: '¥12M',
      status: '启用',
      transactions: '480 / min',
      risk: '低'
    },
    {
      id: 'board-bca',
      name: 'BCA-Jakarta-02',
      balance: '¥6.4M',
      status: '维护中',
      transactions: '220 / min',
      risk: '中'
    },
    {
      id: 'board-bradesco',
      name: 'Bradesco-SP-03',
      balance: '¥4.3M',
      status: '启用',
      transactions: '180 / min',
      risk: '低'
    }
  ],
  filters: ['全部', '启用', '维护中', '告警'],
  alerts: [
    { id: 'board-alert-1', title: 'BCA-Jakarta-02 余额低于阈值', severity: '中', action: '补充资金' },
    { id: 'board-alert-2', title: 'HDFC-Mumbai-01 限额使用 85%', severity: '低', action: '关注' }
  ],
  timeline: [
    { id: 'board-tl-1', time: '10:32', title: 'UPI 资金回补完成', owner: 'Finance' },
    { id: 'board-tl-2', time: '10:15', title: 'BCA 维护开始', owner: 'OPS' },
    { id: 'board-tl-3', time: '10:05', title: 'Bradesco 风险指标恢复', owner: 'Risk' }
  ]
};

export const bankLedgerView = {
  hero: {
    eyebrow: '银行户管理 · 流水',
    title: '银行账户流水',
    description: '跟踪银行账户收付流水、状态、标签与处理进度，便于实时核对。',
    sync: '同步于 18 秒前'
  },
  stats: [
    { id: 'ledger-total', label: '今日流水笔数', value: '2,430', meta: '+12% 日内' },
    { id: 'ledger-credit', label: '入账', value: '¥46M', meta: '+5% 日内' },
    { id: 'ledger-debit', label: '出账', value: '¥39M', meta: '+4% 日内' },
    { id: 'ledger-pending', label: '异常处理', value: '18', meta: '升级 3' }
  ],
  filters: ['全部', '入账', '出账', '异常', '待处理'],
  entries: [
    {
      id: 'ledger-1',
      account: 'HDFC-Mumbai-01',
      type: '入账',
      amount: '+¥1,200,000',
      status: '已记账',
      tag: 'UPI',
      updated: '10:36'
    },
    {
      id: 'ledger-2',
      account: 'BCA-Jakarta-02',
      type: '出账',
      amount: '-¥620,000',
      status: '在途',
      tag: 'Wallet',
      updated: '10:20'
    },
    {
      id: 'ledger-3',
      account: 'Bradesco-SP-03',
      type: '出账',
      amount: '-¥380,000',
      status: '待复核',
      tag: 'PIX',
      updated: '09:58'
    }
  ],
  timeline: [
    { id: 'ledger-tl-1', time: '10:30', title: 'UPI 快速补单', owner: 'OPS' },
    { id: 'ledger-tl-2', time: '10:18', title: 'BCA 出账入队', owner: 'Finance' },
    { id: 'ledger-tl-3', time: '09:55', title: 'Bradesco 待复核分配', owner: 'Risk' }
  ]
};

export const bankBookkeepingView = {
  hero: {
    eyebrow: '银行户管理 · 记账流水',
    title: '记账与调账记录',
    description: '展示内部记账、调账、冲补等流水，支持标签、审批与核对。',
    sync: '同步于 10 秒前'
  },
  stats: [
    { id: 'bk-total', label: '今日记账', value: '¥9.2M', meta: '+8% 日内' },
    { id: 'bk-entries', label: '记账笔数', value: '380', meta: '+12% 日内' },
    { id: 'bk-adjust', label: '调账', value: '26', meta: '审批中 4' },
    { id: 'bk-pending', label: '待复核', value: '6', meta: '紧急 1' }
  ],
  filters: ['全部', '记账', '调账', '冲补', '待复核'],
  entries: [
    {
      id: 'bk-1',
      account: 'HDFC-Mumbai-01',
      type: '记账',
      amount: '+¥320,000',
      tag: '补单',
      status: '已确认',
      owner: 'ops',
      updated: '10:34'
    },
    {
      id: 'bk-2',
      account: 'BCA-Jakarta-02',
      type: '调账',
      amount: '-¥150,000',
      tag: '调账',
      status: '待复核',
      owner: 'finance',
      updated: '10:18'
    },
    {
      id: 'bk-3',
      account: 'Bradesco-SP-03',
      type: '冲补',
      amount: '+¥80,000',
      tag: '冲补',
      status: '已确认',
      owner: 'risk',
      updated: '09:55'
    }
  ],
  approvals: [
    { id: 'bk-approval-1', title: 'BCA 调账审批', owner: 'finance', eta: '30 分钟' },
    { id: 'bk-approval-2', title: 'Bradesco 冲补复核', owner: 'risk', eta: '进行中' }
  ]
};

export const bankMappingView = {
  hero: {
    eyebrow: '银行户管理 · 银行字段映射',
    title: '字段映射模板',
    description: '维护银行接口所需字段、映射规则与版本，确保字段对齐与转换一致。',
    sync: '同步于 6 秒前'
  },
  stats: [
    { id: 'map-banks', label: '银行数量', value: '14', meta: '+1 更新' },
    { id: 'map-templates', label: '模板版本', value: '32', meta: '活跃 21' },
    { id: 'map-fields', label: '字段数', value: '420', meta: '+8 修改' },
    { id: 'map-pending', label: '待上线', value: '3', meta: '紧急 1' }
  ],
  mappings: [
    {
      id: 'map-hdfc',
      bank: 'HDFC',
      version: 'v2.1',
      type: 'UPI',
      status: '启用',
      updated: '10:34',
      owner: 'ops'
    },
    {
      id: 'map-bca',
      bank: 'BCA',
      version: 'v1.9',
      type: 'Wallet',
      status: '待上线',
      updated: '10:12',
      owner: 'platform'
    },
    {
      id: 'map-bradesco',
      bank: 'Bradesco',
      version: 'v1.5',
      type: 'PIX',
      status: '启用',
      updated: '09:55',
      owner: 'ops'
    }
  ],
  filters: ['全部', '启用', '待上线', '停用'],
  approvals: [
    { id: 'map-approval-1', title: 'BCA v1.9 上线审批', owner: 'platform', eta: '今日' },
    { id: 'map-approval-2', title: 'HDFC 字段扩展复核', owner: 'risk', eta: '进行中' }
  ],
  notices: [
    { id: 'map-note-1', text: '映射模板 v3.2 支持动态字段。' },
    { id: 'map-note-2', text: '请本周完成东南亚银行字段复盘。' }
  ]
};

export const ordersCollectionView = {
  hero: {
    eyebrow: '订单 · 收款订单',
    title: '收款订单监控',
    description: '展示收款订单的状态、金额、通道与风险标签，支持快速处理。',
    sync: '同步于 20 秒前'
  },
  stats: [
    { id: 'orders-today', label: '今日订单', value: '12,430', meta: '+13% 日内' },
    { id: 'orders-success', label: '成功率', value: '98.1%', meta: '+0.4% 日内' },
    { id: 'orders-risk', label: '风险订单', value: '24', meta: '处理中 6' },
    { id: 'orders-amount', label: '收款金额', value: '¥32.1M', meta: '+8% 日内' }
  ],
  filters: ['全部', '待支付', '成功', '失败', '风险'],
  orders: [
    {
      id: 'order-1',
      merchant: 'SEA Commerce',
      channel: 'UPI-北区',
      amount: '¥120,000',
      status: '成功',
      risk: '低',
      updated: '10:32'
    },
    {
      id: 'order-2',
      merchant: 'Wallet-VN',
      channel: 'Wallet-VN',
      amount: '¥68,000',
      status: '待支付',
      risk: '中',
      updated: '10:25'
    },
    {
      id: 'order-3',
      merchant: 'LATAM Express',
      channel: 'PIX',
      amount: '¥52,000',
      status: '失败',
      risk: '高',
      updated: '09:58'
    }
  ],
  timeline: [
    { id: 'order-tl-1', time: '10:20', title: 'UPI 订单补单完成', owner: 'OPS' },
    { id: 'order-tl-2', time: '10:10', title: 'Wallet 风险订单复核', owner: 'Risk' }
  ],
  notices: [
    { id: 'order-note-1', text: '订单模板 v2.2 支持多字段过滤。' },
    { id: 'order-note-2', text: '请关注近期 PIX 失败率。' }
  ]
};

export const ordersPayoutView = {
  hero: {
    eyebrow: '订单 · 出款订单',
    title: '出款订单监控',
    description: '跟踪出款订单状态、金额、通道与审批进度，支持快速复核与操作。',
    sync: '同步于 15 秒前'
  },
  stats: [
    { id: 'payout-today', label: '今日出款', value: '¥28.4M', meta: '+7% 日内' },
    { id: 'payout-success', label: '成功率', value: '97.6%', meta: '+0.5% 日内' },
    { id: 'payout-risk', label: '风险订单', value: '18', meta: '升级 4' },
    { id: 'payout-pending', label: '待审批', value: '12', meta: '金额 ¥1.2M' }
  ],
  filters: ['全部', '待审批', '处理中', '成功', '风险'],
  orders: [
    {
      id: 'payout-1',
      merchant: 'SEA Commerce',
      channel: 'HDFC',
      amount: '¥280,000',
      status: '成功',
      owner: 'ops',
      updated: '10:32'
    },
    {
      id: 'payout-2',
      merchant: 'Wallet-VN',
      channel: 'BCA',
      amount: '¥150,000',
      status: '待审批',
      owner: 'finance',
      updated: '10:20'
    },
    {
      id: 'payout-3',
      merchant: 'LATAM Express',
      channel: 'Bradesco',
      amount: '¥96,000',
      status: '处理',
      owner: 'ops',
      updated: '09:58'
    }
  ],
  approvals: [
    { id: 'payout-app-1', title: 'Wallet-VN 出款审批', owner: 'finance', eta: '20 分钟' },
    { id: 'payout-app-2', title: 'LATAM 出款复核', owner: 'risk', eta: '进行中' }
  ],
  notices: [
    { id: 'payout-note-1', text: '出款模板 v1.8 支持批量限额策略。' },
    { id: 'payout-note-2', text: '关注巴西出款失败率。' }
  ]
};

export const ordersRollbackView = {
  hero: {
    eyebrow: '订单 · 代付回滚',
    title: '代付回滚监控',
    description: '跟踪代付回滚任务、金额、进度及审批，确保回滚动作可审计。',
    sync: '同步于 12 秒前'
  },
  stats: [
    { id: 'roll-today', label: '今日回滚', value: '¥1.4M', meta: '+3% 日内' },
    { id: 'roll-tasks', label: '回滚任务', value: '18', meta: '进行中 5' },
    { id: 'roll-auto', label: '自动回滚', value: '62%', meta: '+4% 日内' },
    { id: 'roll-pending', label: '待审批', value: '4', meta: '金额 ¥240k' }
  ],
  filters: ['全部', '自动', '手动', '待审批', '完成'],
  tasks: [
    {
      id: 'roll-1',
      merchant: 'SEA Commerce',
      channel: 'UPI-北区',
      amount: '¥280,000',
      mode: '自动',
      status: '进行中',
      owner: 'riskbot',
      updated: '10:32'
    },
    {
      id: 'roll-2',
      merchant: 'Wallet-VN',
      channel: 'Wallet',
      amount: '¥120,000',
      mode: '手动',
      status: '待审批',
      owner: 'ops',
      updated: '10:18'
    },
    {
      id: 'roll-3',
      merchant: 'LATAM Express',
      channel: 'PIX',
      amount: '¥96,000',
      mode: '自动',
      status: '已完成',
      owner: 'ops',
      updated: '09:58'
    }
  ],
  approvals: [
    { id: 'roll-app-1', title: 'Wallet 手动回滚审批', owner: 'finance', eta: '40 分钟' },
    { id: 'roll-app-2', title: 'UPI 自动回滚复核', owner: 'risk', eta: '进行中' }
  ],
  timeline: [
    { id: 'roll-tl-1', time: '10:20', title: 'UPI 自动回滚触发', owner: 'riskbot' },
    { id: 'roll-tl-2', time: '10:05', title: 'Wallet 手动回滚申请提交', owner: 'ops' }
  ]
};

export const ordersPayoutQueueView = {
  hero: {
    eyebrow: '订单 · 代付队列',
    title: '代付队列监控',
    description: '跟踪代付队列深度、出队速率、重试与风险状态，保障队列健康。',
    sync: '同步于 5 秒前'
  },
  stats: [
    { id: 'queue-depth', label: '当前队列深度', value: '4,320', meta: '-6% 日内' },
    { id: 'queue-rate', label: '出队速率', value: '2.2k/min', meta: '+9% 日内' },
    { id: 'queue-retry', label: '重试队列', value: '420', meta: '自动 68%' },
    { id: 'queue-alerts', label: '告警', value: '2', meta: '高 0 / 中 2' }
  ],
  filters: ['全部', '主队列', '重试队列', '告警'],
  queues: [
    {
      id: 'queue-1',
      channel: 'UPI-北区',
      depth: '1,200',
      rate: '1.1k/min',
      status: '正常',
      retry: '5%',
      updated: '10:32'
    },
    {
      id: 'queue-2',
      channel: 'Wallet-VN',
      depth: '2,600',
      rate: '900/min',
      status: '告警',
      retry: '14%',
      updated: '10:20'
    },
    {
      id: 'queue-3',
      channel: 'PIX',
      depth: '520',
      rate: '200/min',
      status: '正常',
      retry: '8%',
      updated: '09:58'
    }
  ],
  notices: [
    { id: 'queue-note-1', text: '代付队列策略 v1.4 支持多级优先级。' },
    { id: 'queue-note-2', text: 'Wallet 队列已触发告警，关注重试率。' }
  ]
};

export const ordersBatchView = {
  hero: {
    eyebrow: '订单 · 批量代付',
    title: '批量代付任务',
    description: '集中展示批量代付的任务状态、进度、异常与审批，支持快速处理。',
    sync: '同步于 9 秒前'
  },
  stats: [
    { id: 'batch-tasks', label: '今日任务', value: '62', meta: '+8% 日内' },
    { id: 'batch-amount', label: '批量金额', value: '¥6.5M', meta: '+5% 日内' },
    { id: 'batch-complete', label: '完成率', value: '87%', meta: '+2% 日内' },
    { id: 'batch-pending', label: '待审批', value: '6', meta: '金额 ¥480k' }
  ],
  filters: ['全部', '上传中', '执行中', '完成', '异常'],
  tasks: [
    {
      id: 'batch-1',
      merchant: 'SEA Commerce',
      channel: 'UPI',
      amount: '¥1,200,000',
      status: '执行中',
      files: '200 笔',
      owner: 'ops',
      updated: '10:30'
    },
    {
      id: 'batch-2',
      merchant: 'Wallet-VN',
      channel: 'BCA',
      amount: '¥420,000',
      status: '待审批',
      files: '80 笔',
      owner: 'finance',
      updated: '10:18'
    },
    {
      id: 'batch-3',
      merchant: 'LATAM Express',
      channel: 'PIX',
      amount: '¥320,000',
      status: '完成',
      files: '150 笔',
      owner: 'ops',
      updated: '09:55'
    }
  ],
  approvals: [
    { id: 'batch-app-1', title: 'Wallet 批量待审批', owner: 'finance', eta: '30 分钟' },
    { id: 'batch-app-2', title: 'SEA 批量复核', owner: 'risk', eta: '进行中' }
  ],
  notices: [
    { id: 'batch-note-1', text: '批量代付模板 v1.6 支持多文件上传。' },
    { id: 'batch-note-2', text: '关注东南亚批量任务失败率。' }
  ]
};

export const ordersApprovalView = {
  hero: {
    eyebrow: '订单 · 审批流程',
    title: '出款审批队列',
    description: '展示订单审批链路、节点、状态与待办，用于跨团队协作。',
    sync: '同步于 2 分钟前'
  },
  stats: [
    { id: 'approval-total', label: '今日审批', value: '42', meta: '+6% 日内' },
    { id: 'approval-auto', label: '自动通过率', value: '64%', meta: '+3% 周内' },
    { id: 'approval-pending', label: '待审批', value: '12', meta: '紧急 3' },
    { id: 'approval-escalate', label: '升级', value: '2', meta: '处理中' }
  ],
  filters: ['全部', '待审批', '复核', '完成', '升级'],
  approvals: [
    {
      id: 'approval-1',
      merchant: 'SEA Commerce',
      amount: '¥320,000',
      step: '复核',
      status: '进行中',
      owner: 'finance',
      updated: '10:32'
    },
    {
      id: 'approval-2',
      merchant: 'Wallet-VN',
      amount: '¥180,000',
      step: '风控审批',
      status: '待处理',
      owner: 'risk',
      updated: '10:15'
    },
    {
      id: 'approval-3',
      merchant: 'LATAM Express',
      amount: '¥240,000',
      step: '自动审批',
      status: '完成',
      owner: '系统',
      updated: '09:58'
    }
  ],
  timeline: [
    { id: 'approval-tl-1', time: '10:28', title: 'Wallet 风控审批待处理', owner: 'risk' },
    { id: 'approval-tl-2', time: '10:05', title: 'SEA 财务复核接收', owner: 'finance' }
  ]
};

export const ordersQueryView = {
  hero: {
    eyebrow: '订单 · 查单记录',
    title: '查单请求',
    description: '集中展示查单请求、进度、反馈与责任人，支持快速跟进。',
    sync: '同步于 1 分钟前'
  },
  stats: [
    { id: 'query-total', label: '今日查单', value: '64', meta: '+9% 日内' },
    { id: 'query-solved', label: '已解决', value: '48', meta: '平均 16m' },
    { id: 'query-pending', label: '待客服', value: '12', meta: '紧急 3' },
    { id: 'query-risk', label: '升级', value: '4', meta: '处理中' }
  ],
  filters: ['全部', '待回复', '处理中', '完成', '升级'],
  queries: [
    {
      id: 'query-1',
      merchant: 'SEA Commerce',
      channel: 'UPI',
      reason: '到账延迟',
      status: '处理中',
      owner: '客服-Amy',
      updated: '10:32'
    },
    {
      id: 'query-2',
      merchant: 'Wallet-VN',
      channel: 'Wallet',
      reason: '金额差异',
      status: '待回复',
      owner: '客服-Dan',
      updated: '10:15'
    },
    {
      id: 'query-3',
      merchant: 'LATAM Express',
      channel: 'PIX',
      reason: '回执缺失',
      status: '完成',
      owner: '客服-Lina',
      updated: '09:58'
    }
  ],
  timeline: [
    { id: 'query-tl-1', time: '10:20', title: 'UPI 查单反馈银行处理中', owner: 'OPS' },
    { id: 'query-tl-2', time: '10:05', title: 'Wallet 查单分配客服', owner: 'Support' }
  ]
};

export const merchantsListView = {
  hero: {
    eyebrow: '商户 · 列表',
    title: '商户档案概览',
    description: '快速浏览商户资质、状态、标签与余额情况，支持多维筛选。',
    sync: '同步于 1 分钟前'
  },
  stats: [
    { id: 'merchant-total', label: '商户数量', value: '142', meta: '+4 新增' },
    { id: 'merchant-active', label: '启用', value: '128', meta: '冻结 6' },
    { id: 'merchant-balance', label: '总余额', value: '¥22M', meta: '+2% 日内' },
    { id: 'merchant-risk', label: '风险商户', value: '5', meta: '处理中' }
  ],
  filters: ['全部', '启用', '冻结', '高风险'],
  merchants: [
    {
      id: 'merchant-1',
      name: 'SEA Commerce',
      region: '东南亚',
      balance: '¥3.2M',
      status: '启用',
      tag: '电商',
      owner: 'Annie',
      updated: '10:32'
    },
    {
      id: 'merchant-2',
      name: 'Wallet-VN',
      region: '越南',
      balance: '¥1.6M',
      status: '高风险',
      tag: '钱包',
      owner: 'Leo',
      updated: '10:15'
    },
    {
      id: 'merchant-3',
      name: 'LATAM Express',
      region: '巴西',
      balance: '¥2.1M',
      status: '启用',
      tag: '游戏',
      owner: 'Ling',
      updated: '09:58'
    }
  ],
  timeline: [
    { id: 'merchant-tl-1', time: '10:20', title: 'Wallet-VN 风险标签更新', owner: 'Risk' },
    { id: 'merchant-tl-2', time: '10:05', title: 'SEA Commerce 余额回补', owner: 'Finance' }
  ]
};

export const merchantsBoardView = {
  hero: {
    eyebrow: '商户 · 面板',
    title: '商户实时面板',
    description: '聚合重点商户的收付表现、余额、风控与运营指标，支持多维协作。',
    sync: '同步于 30 秒前'
  },
  stats: [
    { id: 'board-amount', label: '今日收款', value: '¥5.8M', meta: '+7% 日内' },
    { id: 'board-payout', label: '今日出款', value: '¥4.2M', meta: '+5% 日内' },
    { id: 'board-uptime', label: '通道成功率', value: '98.3%', meta: '+0.3% 日内' },
    { id: 'board-risk', label: '风险提醒', value: '3', meta: '处理中' }
  ],
  merchants: [
    {
      id: 'board-1',
      name: 'SEA Commerce',
      revenue: '¥3.2M',
      cost: '¥2.3M',
      margin: '28%',
      risk: '低',
      updated: '10:32'
    },
    {
      id: 'board-2',
      name: 'Wallet-VN',
      revenue: '¥1.6M',
      cost: '¥1.2M',
      margin: '25%',
      risk: '中',
      updated: '10:15'
    }
  ],
  filters: ['全部', '电商', '钱包', '游戏'],
  alerts: [
    { id: 'board-alert-1', title: 'Wallet-VN 余额低', action: '通知 Finance' },
    { id: 'board-alert-2', title: 'SEA Commerce 风险标签', action: '确认' }
  ],
  timeline: [
    { id: 'board-tl-1', time: '10:20', title: 'SEA Commerce 余额回补', owner: 'Finance' },
    { id: 'board-tl-2', time: '10:05', title: 'Wallet 风险复核', owner: 'Risk' }
  ]
};

export const agentsChannelsView = {
  hero: {
    eyebrow: '代理商 · 代理商渠道',
    title: '代理商渠道概览',
    description: '查看代理商可用通道、配额、分润与状态，支持多维度协同。',
    sync: '同步于 40 秒前'
  },
  stats: [
    { id: 'agents-total', label: '代理商数量', value: '38', meta: '+2 新增' },
    { id: 'agents-active', label: '启用代理商', value: '34', meta: '冻结 2' },
    { id: 'agents-channels', label: '渠道覆盖', value: '62', meta: '+5 日内' },
    { id: 'agents-risk', label: '风险代理', value: '3', meta: '处理中' }
  ],
  filters: ['全部', '启用', '冻结', '风险'],
  entries: [
    {
      id: 'agent-1',
      name: 'SEA Agent',
      region: '东南亚',
      channels: 12,
      quota: '¥4M',
      status: '启用',
      owner: 'Kenny',
      updated: '10:32'
    },
    {
      id: 'agent-2',
      name: 'LATAM Agent',
      region: '南美',
      channels: 8,
      quota: '¥2.2M',
      status: '风险',
      owner: 'Maria',
      updated: '10:15'
    }
  ],
  notices: [
    { id: 'agents-note-1', text: '代理商渠道模板 v1.3 已上线。' },
    { id: 'agents-note-2', text: '请关注 LATAM 代理商分润调整。' }
  ]
};

export const agentsListView = {
  hero: {
    eyebrow: '代理商 · 列表',
    title: '代理商档案',
    description: '浏览代理商的覆盖区域、配额、风险与负责人，支持多维筛选。',
    sync: '同步于 2 分钟前'
  },
  stats: [
    { id: 'agent-total', label: '代理商数量', value: '38', meta: '+2 新增' },
    { id: 'agent-active', label: '启用', value: '34', meta: '冻结 2' },
    { id: 'agent-quota', label: '总配额', value: '¥18M', meta: '+4% 日内' },
    { id: 'agent-risk', label: '风险代理', value: '3', meta: '处理中' }
  ],
  filters: ['全部', '启用', '冻结', '风险'],
  agents: [
    {
      id: 'agent-list-1',
      name: 'SEA Agent',
      region: '东南亚',
      quota: '¥4M',
      status: '启用',
      owner: 'Kenny',
      updated: '10:32'
    },
    {
      id: 'agent-list-2',
      name: 'LATAM Agent',
      region: '南美',
      quota: '¥2.2M',
      status: '风险',
      owner: 'Maria',
      updated: '10:15'
    },
    {
      id: 'agent-list-3',
      name: 'India Agent',
      region: '印度',
      quota: '¥3M',
      status: '启用',
      owner: 'Anuj',
      updated: '09:58'
    }
  ]
};

export const opsSmsView = {
  hero: {
    eyebrow: '运营工具 · SMS/OTP',
    title: '短信/OTP 面板',
    description: '集中呈现短信与 OTP 渠道的发送量、成功率、延迟与告警，支持快速切换策略。',
    sync: '同步于 12 秒前'
  },
  stats: [
    { id: 'sms-total', label: '今日发送', value: '320k', meta: '+8% 日内' },
    { id: 'sms-success', label: '送达率', value: '97.6%', meta: '+0.3% 日内' },
    { id: 'sms-otp', label: 'OTP 成功率', value: '98.2%', meta: '+0.5% 日内' },
    { id: 'sms-alerts', label: '告警', value: '2', meta: '处理中 1' }
  ],
  filters: ['全部', '短信', 'OTP', '告警'],
  channels: [
    {
      id: 'sms-channel-1',
      provider: 'Twilio',
      region: '全球',
      volume: '110k',
      success: '98.1%',
      latency: '2.1s',
      status: '正常'
    },
    {
      id: 'sms-channel-2',
      provider: 'Exotel',
      region: '印度',
      volume: '90k',
      success: '97.4%',
      latency: '2.9s',
      status: '波动'
    }
  ],
  alerts: [
    { id: 'sms-alert-1', title: '印度 OTP 成功率下降', action: '切换备用' },
    { id: 'sms-alert-2', title: '巴西短信延迟', action: '通知供应商' }
  ]
};

export const opsAppForwarderView = {
  hero: {
    eyebrow: '运营工具 · 短信转发APP',
    title: '短信转发面板',
    description: '查看短信转发设备、连接状态、转发速率与告警，支持快速处理。',
    sync: '同步于 5 秒前'
  },
  stats: [
    { id: 'app-devices', label: '在线设备', value: '62', meta: '离线 3' },
    { id: 'app-rate', label: '转发速率', value: '2.8k/min', meta: '+7% 日内' },
    { id: 'app-success', label: '成功率', value: '97.3%', meta: '+0.4% 日内' },
    { id: 'app-alerts', label: '告警', value: '2', meta: '处理中' }
  ],
  filters: ['全部', '在线', '离线', '告警'],
  devices: [
    {
      id: 'app-1',
      name: 'SEA-Server-01',
      region: '东南亚',
      rate: '1.2k/min',
      status: '在线',
      latency: '1.8s'
    },
    {
      id: 'app-2',
      name: 'IN-Server-02',
      region: '印度',
      rate: '1.1k/min',
      status: '告警',
      latency: '3.2s'
    }
  ],
  alerts: [
    { id: 'app-alert-1', title: 'IN-Server-02 延迟升高', action: '重启设备' },
    { id: 'app-alert-2', title: 'SEA-Server-01 电量低', action: '通知运维' }
  ]
};

export const opsBatchView = {
  hero: {
    eyebrow: '运营工具 · 批量操作',
    title: '批量任务中心',
    description: '执行批量导入、导出、代付等操作，查看任务状态与审批进度。',
    sync: '同步于 15 秒前'
  },
  stats: [
    { id: 'batch-tasks', label: '今日任务', value: '42', meta: '+6% 日内' },
    { id: 'batch-complete', label: '完成任务', value: '34', meta: '平均 12m' },
    { id: 'batch-pending', label: '待审批', value: '6', meta: '紧急 1' },
    { id: 'batch-failed', label: '失败任务', value: '2', meta: '处理中' }
  ],
  filters: ['全部', '导入', '导出', '代付', '待审批'],
  tasks: [
    {
      id: 'ops-batch-1',
      name: 'SEA 批量代付',
      type: '代付',
      count: '200',
      status: '执行中',
      owner: 'ops',
      updated: '10:32'
    },
    {
      id: 'ops-batch-2',
      name: 'Wallet 批量导出',
      type: '导出',
      count: '80',
      status: '待审批',
      owner: 'finance',
      updated: '10:15'
    }
  ],
  approvals: [
    { id: 'ops-batch-app-1', title: 'Wallet 导出审批', owner: 'finance', eta: '20 分钟' }
  ]
};

export const opsVpsManageView = {
  hero: {
    eyebrow: '运营工具 · VPS管理',
    title: 'VPS 实时面板',
    description: '查看 VPS 在线状态、资源与告警，支持运维快速响应。',
    sync: '同步于 6 秒前'
  },
  stats: [
    { id: 'vps-online', label: '在线 VPS', value: '42', meta: '离线 2' },
    { id: 'vps-load', label: '平均负载', value: '58%', meta: '+4% 日内' },
    { id: 'vps-traffic', label: '流量', value: '1.8Gbps', meta: '+7% 日内' },
    { id: 'vps-alerts', label: '告警', value: '3', meta: '处理中 1' }
  ],
  filters: ['全部', '在线', '维护中', '告警'],
  vpsList: [
    {
      id: 'vps-1',
      name: 'SEA-VPS-01',
      region: '东南亚',
      load: '62%',
      status: '在线',
      ip: '10.0.0.1'
    },
    {
      id: 'vps-2',
      name: 'IN-VPS-02',
      region: '印度',
      load: '78%',
      status: '告警',
      ip: '10.3.2.5'
    }
  ],
  alerts: [
    { id: 'vps-alert-1', title: 'IN-VPS-02 CPU 高', action: '重启/通知运维' },
    { id: 'vps-alert-2', title: 'SEA-VPS-01 磁盘接近 90%', action: '扩容' }
  ]
};

export const opsVpsView = {
  hero: {
    eyebrow: '运营工具 · VPS',
    title: 'VPS 列表',
    description: '查看 VPS 节点状态、负载与告警，支持快速管理与操作。',
    sync: '同步于 8 秒前'
  },
  stats: [
    { id: 'vps-total', label: '节点总数', value: '68', meta: '+4 新增' },
    { id: 'vps-online', label: '在线', value: '64', meta: '维护中 2' },
    { id: 'vps-risk', label: '告警节点', value: '3', meta: '处理中 1' },
    { id: 'vps-load', label: '平均负载', value: '56%', meta: '+3% 日内' }
  ],
  filters: ['全部', '在线', '维护', '告警'],
  vpsList: [
    {
      id: 'vps-list-1',
      name: 'SEA-VPS-01',
      region: '东南亚',
      load: '60%',
      status: '在线',
      ip: '10.0.0.1'
    },
    {
      id: 'vps-list-2',
      name: 'IN-VPS-02',
      region: '印度',
      load: '82%',
      status: '告警',
      ip: '10.2.1.5'
    }
  ],
  alerts: [
    { id: 'vps-list-alert-1', title: 'IN-VPS-02 CPU 高', action: '重启设备' }
  ]
};

export const financeWithdrawView = {
  hero: {
    eyebrow: '财务 · 提现',
    title: '提现任务监控',
    description: '查看提现申请、金额、状态与负责人，支持快速复核与处理。',
    sync: '同步于 3 分钟前'
  },
  stats: [
    { id: 'withdraw-today', label: '今日提现', value: '¥2.4M', meta: '+5% 日内' },
    { id: 'withdraw-tasks', label: '申请笔数', value: '64', meta: '+8% 日内' },
    { id: 'withdraw-pending', label: '待审批', value: '12', meta: '紧急 2' },
    { id: 'withdraw-risk', label: '风险拦截', value: '3', meta: '处理中' }
  ],
  filters: ['全部', '待审批', '处理中', '完成', '异常'],
  requests: [
    {
      id: 'withdraw-1',
      merchant: 'SEA Commerce',
      amount: '¥320,000',
      channel: 'HDFC',
      status: '审批中',
      owner: 'finance',
      updated: '10:32'
    },
    {
      id: 'withdraw-2',
      merchant: 'Wallet-VN',
      amount: '¥180,000',
      channel: 'BCA',
      status: '处理中',
      owner: 'ops',
      updated: '10:15'
    },
    {
      id: 'withdraw-3',
      merchant: 'LATAM Express',
      amount: '¥150,000',
      channel: 'PIX',
      status: '完成',
      owner: 'finance',
      updated: '09:58'
    }
  ],
  approvals: [
    { id: 'withdraw-app-1', title: 'SEA Commerce 提现审批', owner: 'finance', eta: '20 分钟' }
  ]
};

export const opsTelegramView = {
  hero: {
    eyebrow: '运营工具 · 电报通知',
    title: 'Telegram 通知面板',
    description: '管理电报通知路由、订阅与告警状态，保证消息及时送达。',
    sync: '同步于 20 秒前'
  },
  stats: [
    { id: 'tg-channels', label: '通知频道', value: '24', meta: '+2 新增' },
    { id: 'tg-active', label: '活跃订阅', value: '3.8k', meta: '+6% 日内' },
    { id: 'tg-alerts', label: '告警推送', value: '48', meta: '+4% 日内' },
    { id: 'tg-fail', label: '失败推送', value: '2', meta: '处理中' }
  ],
  filters: ['全部', '告警', '运营', '风控'],
  channels: [
    {
      id: 'tg-1',
      name: 'Ops Alert',
      type: '告警',
      subscribers: '1.2k',
      status: '正常',
      updated: '10:32'
    },
    {
      id: 'tg-2',
      name: 'Risk Alert',
      type: '风控',
      subscribers: '980',
      status: '波动',
      updated: '10:15'
    }
  ],
  alerts: [
    { id: 'tg-alert-1', title: 'Risk Alert 推送失败 2 次', action: '重新推送' }
  ]
};

export const financeBillingView = {
  hero: {
    eyebrow: '财务 · 账单',
    title: '账单与应收应付',
    description: '管理账单周期、应收应付、对账状态与审核进度。',
    sync: '同步于 1 分钟前'
  },
  stats: [
    { id: 'bill-total', label: '今日生成', value: '82', meta: '+9% 日内' },
    { id: 'bill-complete', label: '已结算', value: '68', meta: '平均 1.8d' },
    { id: 'bill-pending', label: '待审核', value: '12', meta: '紧急 2' },
    { id: 'bill-amount', label: '待收金额', value: '¥3.8M', meta: '+4% 日内' }
  ],
  filters: ['全部', '待审核', '已结算', '异常'],
  bills: [
    {
      id: 'bill-1',
      subject: 'SEA Commerce',
      cycle: '2024-03',
      payable: '¥820,000',
      receivable: '¥780,000',
      status: '已结算',
      owner: 'finance',
      updated: '10:32'
    },
    {
      id: 'bill-2',
      subject: 'Wallet-VN',
      cycle: '2024-03',
      payable: '¥460,000',
      receivable: '¥430,000',
      status: '待审核',
      owner: 'finance',
      updated: '10:18'
    }
  ],
  approvals: [
    { id: 'bill-app-1', title: 'Wallet-VN 账单审核', owner: 'finance', eta: '30 分钟' }
  ]
};

export const financeAgentBillingView = {
  hero: {
    eyebrow: '财务 · 代理商账单',
    title: '代理商账单概览',
    description: '查看代理商结算情况、分润与账单状态，支持多维协作。',
    sync: '同步于 2 分钟前'
  },
  stats: [
    { id: 'agent-bill-total', label: '本月账单', value: '120', meta: '+8% 月环比' },
    { id: 'agent-bill-amount', label: '应付金额', value: '¥4.6M', meta: '+5% 日内' },
    { id: 'agent-bill-settled', label: '已结算', value: '86', meta: '平均 2.1d' },
    { id: 'agent-bill-pending', label: '待复核', value: '14', meta: '紧急 3' }
  ],
  filters: ['全部', '待复核', '已结算', '风险'],
  bills: [
    {
      id: 'agent-bill-1',
      agent: 'SEA Agent',
      cycle: '2024-03',
      amount: '¥820,000',
      status: '已结算',
      owner: 'finance',
      updated: '10:32'
    },
    {
      id: 'agent-bill-2',
      agent: 'LATAM Agent',
      cycle: '2024-03',
      amount: '¥520,000',
      status: '待复核',
      owner: 'finance',
      updated: '10:18'
    }
  ]
};

export const financeSystemTopupView = {
  hero: {
    eyebrow: '财务 · 系统付费充值',
    title: '系统充值记录',
    description: '查看系统付费充值订单、额度与状态，支持快速复核与操作。',
    sync: '同步于 4 分钟前'
  },
  stats: [
    { id: 'topup-today', label: '今日充值', value: '¥920,000', meta: '+5% 日内' },
    { id: 'topup-orders', label: '充值订单', value: '42', meta: '+7% 日内' },
    { id: 'topup-pending', label: '待审核', value: '8', meta: '紧急 1' },
    { id: 'topup-success', label: '成功率', value: '98.2%', meta: '+0.4% 日内' }
  ],
  filters: ['全部', '待审核', '处理中', '完成', '失败'],
  orders: [
    {
      id: 'topup-1',
      company: 'Ops System',
      amount: '¥320,000',
      method: '银行转账',
      status: '待审核',
      owner: 'finance',
      updated: '10:32'
    },
    {
      id: 'topup-2',
      company: 'Risk System',
      amount: '¥180,000',
      method: '现金',
      status: '完成',
      owner: 'finance',
      updated: '10:05'
    }
  ]
};

export const financeAgentWithdrawView = {
  hero: {
    eyebrow: '财务 · 代理商提现申请',
    title: '代理商提现面板',
    description: '查看代理商提现申请、状态与负责人，支持快速审批与处理。',
    sync: '同步于 1 分钟前'
  },
  stats: [
    { id: 'agent-withdraw-total', label: '今日申请', value: '34', meta: '+5% 日内' },
    { id: 'agent-withdraw-amount', label: '申请金额', value: '¥1.8M', meta: '+4% 日内' },
    { id: 'agent-withdraw-pending', label: '待审批', value: '9', meta: '紧急 2' },
    { id: 'agent-withdraw-risk', label: '风险拦截', value: '2', meta: '处理中' }
  ],
  filters: ['全部', '待审批', '处理中', '完成', '异常'],
  requests: [
    {
      id: 'agent-withdraw-1',
      agent: 'SEA Agent',
      amount: '¥320,000',
      method: '银行转账',
      status: '审批中',
      owner: 'finance',
      updated: '10:20'
    },
    {
      id: 'agent-withdraw-2',
      agent: 'LATAM Agent',
      amount: '¥180,000',
      method: 'PIX',
      status: '处理中',
      owner: 'ops',
      updated: '10:05'
    }
  ]
};

export const downloadsHistoryView = {
  hero: {
    eyebrow: '下载管理 · 历史记录',
    title: '导出记录面板',
    description: '查看导出任务、文件状态与有效期，支持快速重试/下载。',
    sync: '同步于 10 秒前'
  },
  stats: [
    { id: 'downloads-today', label: '今日导出', value: '48', meta: '+4% 日内' },
    { id: 'downloads-success', label: '成功', value: '42', meta: '平均 2.8m' },
    { id: 'downloads-failed', label: '失败', value: '3', meta: '待重试' },
    { id: 'downloads-valid', label: '有效文件', value: '120', meta: '+6% 日内' }
  ],
  filters: ['全部', '生成中', '已完成', '失败'],
  records: [
    {
      id: 'dl-1',
      name: 'SEA 账单导出',
      type: '账单',
      status: '已完成',
      expires: '2024-03-28',
      owner: 'finance',
      updated: '10:20'
    },
    {
      id: 'dl-2',
      name: 'Wallet 风控导出',
      type: '风控',
      status: '失败',
      expires: '-',
      owner: 'risk',
      updated: '10:05'
    }
  ]
};

export const systemBillingView = {
  hero: {
    eyebrow: '系统 · 系统账单',
    title: '内部账单面板',
    description: '查看内部账单、结算周期与状态，支持多维筛选与跟进。',
    sync: '同步于 3 分钟前'
  },
  stats: [
    { id: 'sys-bill-total', label: '今日账单', value: '64', meta: '+6% 日内' },
    { id: 'sys-bill-amount', label: '账单金额', value: '¥2.8M', meta: '+4% 日内' },
    { id: 'sys-bill-settled', label: '已结算', value: '48', meta: '平均 1.5d' },
    { id: 'sys-bill-pending', label: '待复核', value: '10', meta: '升级 2' }
  ],
  filters: ['全部', '待复核', '已结算', '异常'],
  bills: [
    {
      id: 'sys-bill-1',
      subject: 'Ops Service',
      cycle: '2024-03',
      amount: '¥520,000',
      status: '已结算',
      owner: 'ops',
      updated: '10:32'
    },
    {
      id: 'sys-bill-2',
      subject: 'Risk Service',
      cycle: '2024-03',
      amount: '¥260,000',
      status: '待复核',
      owner: 'risk',
      updated: '10:12'
    }
  ]
};

export const systemUsersView = {
  hero: {
    eyebrow: '系统 · 用户',
    title: '系统用户管理',
    description: '按 users.html 交互复刻的演示界面，涵盖筛选、列表、重点用户与操作追踪。',
    sync: '同步于 5 分钟前 · SSO 通道正常',
    actions: [
      { id: 'create', label: '新增用户', variant: 'primary' },
      { id: 'sync', label: '同步外部 SSO' }
    ],
    highlights: [
      { id: 'mfa', label: '已开启 MFA', value: '96%' },
      { id: 'pending', label: '待处理邀请', value: '6' }
    ]
  },
  stats: [
    { id: 'users-total', label: '用户总数', value: '128', meta: '+4 新增' },
    { id: 'users-active', label: '活跃', value: '112', meta: '冻结 3' },
    { id: 'users-admin', label: '管理员', value: '18', meta: '风险 1' },
    { id: 'users-login', label: '今日登录', value: '64', meta: '+7% 日内' }
  ],
  filters: {
    note: '支持登录账号 + 状态 + 角色组合查询',
    fields: [
      { id: 'account', label: '登录账号', placeholder: '手机号 / 邮箱 / ID' },
      { id: 'nickname', label: '用户昵称', placeholder: '模糊匹配' },
      { id: 'role', label: '角色', placeholder: '管理员 / 审核 / 观察' },
      { id: 'group', label: '所属组', placeholder: 'Ops / Finance / Risk' }
    ],
    dateRange: {
      start: '2024-05-10 00:00',
      end: '2024-05-16 23:59'
    },
    statuses: ['全部', '活跃', '冻结', '禁用', '待审核', '风控观察']
  },
  table: {
    columns: ['用户', '角色', '状态', '最近登录', '所属组', 'IP / 终端'],
    total: '128',
    updated: '10:45 自动同步',
    batchActions: ['批量冻结', '批量重置 MFA'],
    page: '第 1 / 7 页',
    rows: [
      {
        id: 'sys-user-1',
        name: 'OpsAdmin',
        account: 'ops.admin@example.com',
        tags: ['MFA', '白名单'],
        role: '运维',
        level: '全局管理员',
        status: '活跃',
        risk: '低风险',
        lastLogin: '10:32 · UTC+05:30',
        location: '印度孟买',
        group: 'OPS 中台',
        owner: 'SRE 团队',
        ip: '59.18.22.104',
        client: 'Chrome · Mac'
      },
      {
        id: 'sys-user-2',
        name: 'RiskBot',
        account: 'risk.bot@test.io',
        tags: ['Bot', '自动审批'],
        role: '风控',
        level: '系统机器人',
        status: '活跃',
        risk: '观察',
        lastLogin: '10:12 · UTC+08:00',
        location: '新加坡',
        group: 'RISK Engine',
        owner: '风控策略',
        ip: '103.91.44.9',
        client: '服务账号'
      },
      {
        id: 'sys-user-3',
        name: 'FinanceQA',
        account: 'finance.qa@example.com',
        tags: ['MFA', '只读'],
        role: '财务',
        level: '账务审核',
        status: '冻结',
        risk: '人工复核中',
        lastLogin: '09:02 · UTC+07:00',
        location: '越南胡志明',
        group: 'Finance Settlement',
        owner: '结算团队',
        ip: '27.66.210.4',
        client: 'Edge · Win11'
      }
    ]
  },
  focusUser: {
    name: 'OpsAdmin',
    avatar: 'OA',
    role: '运维管理员',
    owner: 'OPS 中台 · UTC+05:30',
    email: 'ops.admin@example.com',
    meta: '来源：SSO / 2022-08-12 创建',
    tags: ['MFA 开启', '白名单', '监控通知'],
    stats: [
      { id: 'login', label: '最近登录', value: '10:32', meta: '59.18.* · Chrome' },
      { id: 'mfa', label: 'MFA', value: '开启', meta: 'OTP · 正常' },
      { id: 'created', label: '创建', value: '2022-08-12', meta: 'risk_admin' }
    ],
    permissions: ['仪表板读写', '订单审批', '风控只读', '系统配置读'],
    quickLinks: ['查看审计日志', '调整权限', '移交负责人']
  },
  timeline: {
    description: '精确到秒的安全审计 · 5 分钟前刷新',
    items: [
      {
        id: 'timeline-1',
        time: '10:32:08',
        event: '登录成功',
        detail: '来自印度孟买 · 59.18.* · Chrome'
      },
      {
        id: 'timeline-2',
        time: '10:30:41',
        event: '批量导出用户',
        detail: '生成导出任务 #10219'
      },
      {
        id: 'timeline-3',
        time: '10:18:05',
        event: '修改角色',
        detail: 'finance.audit 提升为 财务管理员'
      },
      {
        id: 'timeline-4',
        time: '09:55:22',
        event: '重置密码',
        detail: '对 user.chen 发送重置链接'
      }
    ]
  }
};

export const systemSettingsView = {
  hero: {
    eyebrow: '系统 · 设置',
    title: '系统配置中心',
    description: '按照 settings.html 行为还原的演示页，覆盖全局参数、Telegram 表单、开关与配置表。',
    sync: '同步于 10 秒前 · 操作员 ops_admin'
  },
  stats: [
    { id: 'settings-total', label: '配置项', value: '142', meta: '+4 新增' },
    { id: 'settings-gray', label: '灰度参数', value: '18', meta: '运行中 3' },
    { id: 'settings-risk', label: '风险配置', value: '12', meta: '待审核' },
    { id: 'settings-release', label: '版本号', value: 'v4.0.1', meta: '本周更新' }
  ],
  overview: [
    { id: 'timezone', label: '默认时区', value: 'UTC+05:30', meta: '账单 / 审计基准' },
    { id: 'language', label: '默认语言', value: '中文', meta: 'English 可切换' },
    { id: 'release', label: '当前版本', value: 'v4.0.1', meta: '2024-05-15 部署' },
    { id: 'contact', label: '值班联系人', value: 'ops@testpays.com', meta: 'SRE' }
  ],
  filters: {
    note: '可按状态过滤配置项，默认为全部。',
    options: ['全部', '生效', '灰度', '待发布']
  },
  configActions: ['新增配置项', '导出配置', '同步到生产'],
  configs: [
    {
      id: 'config-1',
      name: 'PaymentsSwitch',
      version: 'v4.0.1',
      scope: '支付',
      status: '生效',
      owner: 'ops',
      updated: '10:20'
    },
    {
      id: 'config-2',
      name: 'RiskGrayParam',
      version: 'v3.2.0',
      scope: '风控',
      status: '灰度',
      owner: 'risk',
      updated: '10:05'
    },
    {
      id: 'config-3',
      name: 'MerchantFeatureFlag',
      version: 'v2.6.7',
      scope: '商户',
      status: '待发布',
      owner: 'product',
      updated: '09:48'
    }
  ],
  forms: [
    {
      id: 'global',
      title: '基础配置',
      description: '对齐 settings.html 中“默认时区/语言”相关设置，仅展示示例值。',
      fields: [
        {
          id: 'default-timezone',
          label: '控制台默认时区',
          value: 'UTC+05:30',
          note: '影响账单、日志时间展示'
        },
        {
          id: 'default-language',
          label: '控制台默认语言',
          value: '中文',
          note: 'English 可在页眉切换'
        },
        {
          id: 'system-domain',
          label: '系统域名',
          value: 'admin.testpays.com',
          note: '仅供展示，不可编辑',
          disabled: true
        },
        {
          id: 'webhook',
          label: 'Webhook 回调地址',
          value: 'https://webhook.testpays.com/api/callback',
          note: '用于回传风控、代付事件'
        }
      ]
    },
    {
      id: 'telegram',
      title: 'Telegram 配置',
      description: '模拟 settings.html 的电报机器人、登录验证与频道设置。',
      fields: [
        {
          id: 'vendor-token',
          label: 'Telegram 机器人 Token(供户商)',
          value: '',
          placeholder: '请输入 Telegram 机器人 Token',
          note: '设置失败请检查 Token 是否正确'
        },
        {
          id: 'telegram-login',
          label: 'Telegram 登录验证',
          value: 'OFF',
          note: '登录启用 Telegram 二次确认',
          status: '已关闭'
        },
        {
          id: 'merchant-bot',
          label: '商户机器人 Username',
          value: 'testpay5_bot',
          disabled: true
        },
        {
          id: 'ledger-chat',
          label: '记账电报群 ID',
          value: '',
          placeholder: '示例：-100123456',
          note: '配置后可推送记账提醒'
        }
      ]
    }
  ],
  toggles: [
    {
      id: 'toggle-otp',
      label: 'OTP 登录校验',
      description: '强制后台登录输入短信 OTP，提升安全性。',
      status: '开启',
      tone: 'positive'
    },
    {
      id: 'toggle-ip',
      label: 'IP 白名单',
      description: '限制后台登录来源 IP。',
      status: '关闭',
      tone: 'warning'
    },
    {
      id: 'toggle-gray',
      label: '灰度发布',
      description: '启用灰度阶段，逐步推送到生产节点。',
      status: '运行中',
      tone: 'info'
    }
  ],
  auditTrail: [
    { id: 'audit-1', time: '10:32', actor: 'ops_admin', action: '更新默认时区为 UTC+05:30' },
    { id: 'audit-2', time: '10:05', actor: 'risk_admin', action: '编辑 RiskGrayParam 为灰度' },
    { id: 'audit-3', time: '09:58', actor: 'ops_bot', action: '同步 PaymentsSwitch 到生产' }
  ],
  quickLinks: ['查看配置文档', '提交变更', '下载操作日志']
};

export const summaryStats = [
  {
    id: 'today-payins',
    label: '今日收款成功',
    value: '¥1,392,220',
    meta: '成功率 98.1%',
    trend: 12.4,
    trendLabel: '环比'
  },
  {
    id: 'today-payouts',
    label: '今日出款成功',
    value: '¥982,566',
    meta: '人均到账 1.2 分钟',
    trend: 6.2,
    trendLabel: '环比'
  },
  {
    id: 'risk-events',
    label: '风控事件',
    value: '14',
    meta: '已自愈 11 条',
    trend: -3.1,
    trendLabel: '同比'
  },
  {
    id: 'uptime',
    label: '通道可用率',
    value: '99.985%',
    meta: '窗口期 15 分钟',
    trend: 0.02,
    trendLabel: '周变化'
  }
];

export const channels = [
  {
    id: 'Indonesia-001',
    country: '印度尼西亚',
    countryTag: 'Indonesia',
    status: '平稳',
    risk: '低',
    businessTypes: ['IGaming', 'Drama', 'Social Entertainment'],
    payinRate: '6.5%',
    payoutRate: '3.5%',
    period: 'T+1',
    coverage: '本地银行, QRIS',
    limit: '单笔 80,000 IDR',
    description: '强实时路由，适配 12 家头部银行，支付确认 3 秒内返回。',
    priority: true
  },
  {
    id: 'India-003',
    country: '印度',
    countryTag: 'India',
    status: '监控',
    risk: '中',
    businessTypes: ['UPI', 'Streaming'],
    payinRate: '1.4%',
    payoutRate: '0.2%',
    period: 'T1',
    coverage: 'UPI & FAST',
    limit: '单笔 50,000 INR',
    description: '多轨路由 UPI，峰值 TPS 5k，支持二维码与 Intent。',
    priority: false
  },
  {
    id: 'Brazil-002',
    country: '巴西',
    countryTag: 'Brazil',
    status: '紧急',
    risk: '高',
    businessTypes: ['PIX', 'Digital Goods'],
    payinRate: '2.1%',
    payoutRate: '0.6%',
    period: 'T+0',
    coverage: 'PIX 即时',
    limit: '单笔 10,000 BRL',
    description: 'PIX 支付响应延迟上升，需要关注对账差异。',
    priority: true
  },
  {
    id: 'Vietnam-001',
    country: '越南',
    countryTag: 'Vietnam',
    status: '平稳',
    risk: '低',
    businessTypes: ['Wallet', 'Gaming'],
    payinRate: '2.4%',
    payoutRate: '0.8%',
    period: 'T+0',
    coverage: 'MoMo, ZaloPay',
    limit: '单笔 150,000 VND',
    description: '双活通道，历史可用率 99.98%。',
    priority: false
  }
];
