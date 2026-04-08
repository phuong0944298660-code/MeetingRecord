package com.meeting.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 会议处理进度 DTO
 * 用于 WebSocket 实时推送
 */
@Data
@Builder
public class MeetingProgressDTO {

    /**
     * 会议ID
     */
    private Long meetingId;

    /**
     * 阶段：whisper(语音识别)、ai(生成纪要)、completed(完成)、error(失败)
     */
    private String stage;

    /**
     * 阶段描述：用于前端显示
     */
    private String stageName;

    /**
     * 当前阶段进度：0-100
     */
    private Integer progress;

    /**
     * 总体进度：0-100
     */
    private Integer totalProgress;

    /**
     * 状态消息
     */
    private String message;

    /**
     * 错误信息（失败时）
     */
    private String error;

    // 预定义的阶段常量
    public static final String STAGE_WHISPER = "whisper";
    public static final String STAGE_AI = "ai";
    public static final String STAGE_COMPLETED = "completed";
    public static final String STAGE_ERROR = "error";

    /**
     * 创建转录阶段的进度消息
     */
    public static MeetingProgressDTO whisper(Long meetingId, Integer progress, String message) {
        return MeetingProgressDTO.builder()
                .meetingId(meetingId)
                .stage(STAGE_WHISPER)
                .stageName("语音识别中")
                .progress(progress)
                .totalProgress(progress / 2) // 转录占总体50%
                .message(message)
                .build();
    }

    /**
     * 创建 AI 处理阶段的进度消息
     */
    public static MeetingProgressDTO ai(Long meetingId, Integer progress, String message) {
        return MeetingProgressDTO.builder()
                .meetingId(meetingId)
                .stage(STAGE_AI)
                .stageName("生成会议纪要中")
                .progress(progress)
                .totalProgress(50 + progress / 2) // AI占总体50%，从50%开始
                .message(message)
                .build();
    }

    /**
     * 创建完成消息
     */
    public static MeetingProgressDTO completed(Long meetingId) {
        return MeetingProgressDTO.builder()
                .meetingId(meetingId)
                .stage(STAGE_COMPLETED)
                .stageName("处理完成")
                .progress(100)
                .totalProgress(100)
                .message("会议处理完成")
                .build();
    }

    /**
     * 创建错误消息
     */
    public static MeetingProgressDTO error(Long meetingId, String error) {
        return MeetingProgressDTO.builder()
                .meetingId(meetingId)
                .stage(STAGE_ERROR)
                .stageName("处理失败")
                .progress(0)
                .totalProgress(0)
                .message("处理失败: " + error)
                .error(error)
                .build();
    }
}
