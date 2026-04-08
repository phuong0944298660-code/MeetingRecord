package com.meeting.dto;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.meeting.entity.Meeting;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会议详情VO
 */
@Data
public class MeetingVO {

    private Long id;
    private String title;
    private String duration;
    private Long fileSize;
    private String summary;
    private List<TodoItem> todos;
    private List<String> decisions;
    private List<Speaker> speakers;
    private Integer status;
    private LocalDateTime createdAt;

    // 新增：飞书风格结构化会议纪要
    private String overview; // 会议整体概述
    private List<Topic> topics; // 按话题分段
    private MeetingStats stats; // 会议统计

    /**
     * 从实体转换
     */
    public static MeetingVO fromEntity(Meeting meeting) {
        MeetingVO vo = new MeetingVO();
        vo.setId(meeting.getId());
        vo.setTitle(meeting.getTitle());
        vo.setDuration(meeting.getDuration());
        vo.setFileSize(meeting.getFileSize());
        vo.setSummary(meeting.getSummary());
        vo.setStatus(meeting.getStatus());
        vo.setCreatedAt(meeting.getCreatedAt());

        // 解析JSON字段
        if (meeting.getTodos() != null) {
            vo.setTodos(JSON.parseObject(meeting.getTodos(), new TypeReference<List<TodoItem>>() {}));
        }
        if (meeting.getDecisions() != null) {
            vo.setDecisions(JSON.parseObject(meeting.getDecisions(), new TypeReference<List<String>>() {}));
        }
        if (meeting.getTranscript() != null) {
            vo.setSpeakers(JSON.parseObject(meeting.getTranscript(), new TypeReference<List<Speaker>>() {}));
        }

        // 解析新的结构化会议纪要（飞书风格）
        if (meeting.getMeetingMinutes() != null) {
            try {
                JSONObject minutesJson = JSON.parseObject(meeting.getMeetingMinutes());
                vo.setOverview(minutesJson.getString("overview"));
                if (minutesJson.containsKey("topics")) {
                    vo.setTopics(JSON.parseObject(minutesJson.getString("topics"), new TypeReference<List<Topic>>() {}));
                }
                if (minutesJson.containsKey("stats")) {
                    vo.setStats(JSON.parseObject(minutesJson.getString("stats"), MeetingStats.class));
                }
            } catch (Exception e) {
                // 解析失败时忽略
            }
        }

        return vo;
    }

    @Data
    public static class TodoItem {
        private Integer id;
        private String content;
        private Boolean done;
        private String assignee; // 负责人
        private Integer topicId; // 关联的话题ID
    }

    @Data
    public static class Speaker {
        private String id;
        private String name;
        private String color;
        private List<Segment> segments;

        @Data
        public static class Segment {
            private String time;
            private String text;
        }
    }

    // 结构化会议纪要 - 飞书风格
    @Data
    public static class MeetingMinutes {
        private String overview; // 会议整体概述
        private List<Topic> topics; // 按话题分段
        private MeetingStats stats; // 会议统计
    }

    @Data
    public static class Topic {
        private Integer id;
        private String title; // 话题标题
        private String timeRange; // 时间范围，如 "00:05:23 - 00:15:40"
        private String summary; // 详细总结（200-500字）
        private List<String> keyPoints; // 关键要点（3-5条）
        private List<String> speakers; // 参与该话题的说话人
        private List<TodoItem> todos; // 该话题相关的待办
        private List<String> decisions; // 该话题相关的决策
    }

    @Data
    public static class MeetingStats {
        private Integer totalSpeakers; // 参与人数
        private String totalDuration; // 总时长
        private List<SpeakerStat> speakerStats; // 各说话人发言统计
    }

    @Data
    public static class SpeakerStat {
        private String name;
        private String speakTime; // 发言时长，如 "00:10:23"
        private Integer percentage; // 占比，如 35
    }
}
