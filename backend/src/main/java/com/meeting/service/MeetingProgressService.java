package com.meeting.service;

import com.meeting.dto.MeetingProgressDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 会议进度推送服务
 * 通过 WebSocket 向前端推送实时处理进度
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingProgressService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 推送进度消息
     *
     * @param meetingId 会议ID
     * @param dto       进度消息
     */
    public void sendProgress(Long meetingId, MeetingProgressDTO dto) {
        String destination = "/topic/meeting/" + meetingId + "/progress";
        log.debug("推送进度到 {}: stage={}, progress={}%", destination, dto.getStage(), dto.getTotalProgress());
        messagingTemplate.convertAndSend(destination, dto);
    }

    /**
     * 发送转录阶段开始
     */
    public void sendWhisperStart(Long meetingId) {
        sendProgress(meetingId, MeetingProgressDTO.whisper(meetingId, 0, "开始语音识别..."));
    }

    /**
     * 发送转录进度
     */
    public void sendWhisperProgress(Long meetingId, int progress, String message) {
        sendProgress(meetingId, MeetingProgressDTO.whisper(meetingId, progress, message));
    }

    /**
     * 发送转录阶段完成
     */
    public void sendWhisperComplete(Long meetingId) {
        sendProgress(meetingId, MeetingProgressDTO.whisper(meetingId, 100, "语音识别完成"));
    }

    /**
     * 发送 AI 处理阶段开始
     */
    public void sendAiStart(Long meetingId) {
        sendProgress(meetingId, MeetingProgressDTO.ai(meetingId, 0, "开始生成会议纪要..."));
    }

    /**
     * 发送 AI 处理进度
     */
    public void sendAiProgress(Long meetingId, int progress, String message) {
        sendProgress(meetingId, MeetingProgressDTO.ai(meetingId, progress, message));
    }

    /**
     * 发送 AI 处理完成
     */
    public void sendAiComplete(Long meetingId) {
        sendProgress(meetingId, MeetingProgressDTO.ai(meetingId, 100, "会议纪要生成完成"));
    }

    /**
     * 发送处理完成
     */
    public void sendCompleted(Long meetingId) {
        sendProgress(meetingId, MeetingProgressDTO.completed(meetingId));
    }

    /**
     * 发送处理错误
     */
    public void sendError(Long meetingId, String error) {
        sendProgress(meetingId, MeetingProgressDTO.error(meetingId, error));
    }
}
