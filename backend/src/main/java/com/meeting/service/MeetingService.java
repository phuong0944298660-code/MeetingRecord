package com.meeting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.meeting.dto.MeetingVO;
import com.meeting.entity.Meeting;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 会议服务接口
 */
public interface MeetingService extends IService<Meeting> {

    /**
     * 获取用户的会议列表
     */
    List<MeetingVO> getMeetingList(Long userId, String keyword);

    /**
     * 获取会议详情
     */
    MeetingVO getMeetingDetail(Long meetingId, Long userId);

    /**
     * 上传会议录音
     */
    Meeting uploadMeeting(MultipartFile file, Long userId);

    /**
     * 更新待办状态
     */
    void toggleTodo(Long meetingId, Integer todoId, Long userId);

    /**
     * 处理会议（调用AI）
     */
    void processMeeting(Long meetingId);
}
