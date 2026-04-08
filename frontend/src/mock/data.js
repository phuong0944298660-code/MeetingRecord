// 假数据 - 用于前端开发测试

export const mockMeetings = [
  {
    id: 1,
    title: '产品周会',
    duration: '45分钟',
    fileSize: 25480000,
    summary: '本次会议讨论了Q2产品规划，确定了主要功能模块和上线时间节点。各负责人明确了分工，预计下周五完成第一版开发。会上还对现有问题进行了梳理，制定了优化方案。',
    todos: [
      { id: 1, content: '完成需求文档编写', done: true },
      { id: 2, content: '安排开发排期', done: false },
      { id: 3, content: '协调设计资源', done: false },
      { id: 4, content: '准备测试用例', done: false }
    ],
    decisions: [
      '确定下周五（4月10日）上线第一版',
      '采用微服务架构进行重构',
      '下季度重点投入AI功能开发'
    ],
    speakers: [
      {
        id: 'speaker_1',
        name: '说话人 A',
        color: '#409EFF',
        segments: [
          { time: '00:02:15', text: '我觉得这个方案可行，我们需要在下周五之前完成第一版的开发工作，然后安排测试。时间比较紧，大家需要加班配合。' },
          { time: '00:08:30', text: '关于技术选型，我建议大家统一使用Vue3，这样后续维护会比较方便。' }
        ]
      },
      {
        id: 'speaker_2',
        name: '说话人 B',
        color: '#67C23A',
        segments: [
          { time: '00:05:23', text: '同意，我会负责后端接口的开发，预计需要3天时间。前端部分需要谁来对接？' },
          { time: '00:12:45', text: '接口文档我明天早上发出来，大家看一下有没有问题。' }
        ]
      },
      {
        id: 'speaker_3',
        name: '说话人 C',
        color: '#E6A23C',
        segments: [
          { time: '00:06:15', text: '前端我来负责，下周一可以开始。' }
        ]
      },
      {
        id: 'speaker_4',
        name: '说话人 D',
        color: '#F56C6C',
        segments: [
          { time: '00:15:20', text: '测试这边需要提前知道具体的上线时间，我们好安排测试资源。另外，建议开发过程中就进行自测，减少后期的bug。' }
        ]
      }
    ],
    status: 1,
    createdAt: '2026-04-03 14:00:00'
  },
  {
    id: 2,
    title: '技术评审会议',
    duration: '60分钟',
    fileSize: 38920000,
    summary: '对新的架构方案进行了评审，讨论了技术难点和解决方案。决定采用渐进式迁移策略，先从小模块开始试点。',
    todos: [
      { id: 1, content: '编写技术方案文档', done: true },
      { id: 2, content: '搭建测试环境', done: true },
      { id: 3, content: '进行性能压测', done: false }
    ],
    decisions: [
      '采用渐进式迁移策略',
      '引入Redis缓存层',
      '数据库分库分表方案推迟到下季度'
    ],
    speakers: [
      {
        id: 'speaker_1',
        name: '说话人 A',
        color: '#409EFF',
        segments: [
          { time: '00:03:00', text: '这个架构方案整体上是可行的，但是性能这块还需要进一步验证。' }
        ]
      },
      {
        id: 'speaker_2',
        name: '说话人 B',
        color: '#67C23A',
        segments: [
          { time: '00:10:20', text: '我建议先拿一个小模块做试点，验证方案的可行性，然后再全面推广。' }
        ]
      }
    ],
    status: 1,
    createdAt: '2026-04-02 10:00:00'
  },
  {
    id: 3,
    title: '项目启动会',
    duration: '30分钟',
    fileSize: 15200000,
    summary: '新项目启动，明确了项目目标、团队成员和各自职责。确定了沟通机制和周报制度。',
    todos: [
      { id: 1, content: '创建项目文档', done: true },
      { id: 2, content: '申请开发资源', done: false }
    ],
    decisions: [
      '项目正式启动',
      '每周五下午进行周会',
      '使用飞书进行日常沟通'
    ],
    speakers: [
      {
        id: 'speaker_1',
        name: '说话人 A',
        color: '#409EFF',
        segments: [
          { time: '00:01:00', text: '欢迎大家加入这个项目，我们的目标是在两个月内完成MVP版本。' }
        ]
      }
    ],
    status: 1,
    createdAt: '2026-04-01 09:30:00'
  },
  {
    id: 4,
    title: '季度总结会',
    duration: '90分钟',
    fileSize: 56800000,
    summary: 'Q1季度总结，回顾了各项目进展和成果。表彰了优秀团队，分析了存在的问题，制定了Q2改进计划。',
    todos: [
      { id: 1, content: '整理Q1数据报告', done: false },
      { id: 2, content: '制定Q2 OKR', done: false }
    ],
    decisions: [
      'Q2重点提升用户体验',
      '增加研发投入20%',
      '启动校招计划'
    ],
    speakers: [],
    status: 0,
    createdAt: '2026-03-28 14:00:00'
  }
]

// 模拟上传进度
export function simulateUpload(callback) {
  let progress = 0
  const interval = setInterval(() => {
    progress += Math.random() * 15
    if (progress >= 100) {
      progress = 100
      clearInterval(interval)
      callback(progress, true)
    } else {
      callback(progress, false)
    }
  }, 200)
  return interval
}

// 模拟AI处理
export function simulateProcessing(callback) {
  let progress = 0
  const interval = setInterval(() => {
    progress += Math.random() * 10
    if (progress >= 100) {
      progress = 100
      clearInterval(interval)
      callback(progress, true)
    } else {
      callback(progress, false)
    }
  }, 500)
  return interval
}
