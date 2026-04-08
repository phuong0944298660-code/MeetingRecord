package com.meeting.controller;

import com.meeting.dto.MeetingVO;
import com.meeting.dto.Result;
import com.meeting.entity.Meeting;
import com.meeting.service.MeetingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会议控制器
 */
@RestController
@RequestMapping("/api/meeting")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MeetingController {

    private final MeetingService meetingService;

    /**
     * 获取会议列表
     */
    @GetMapping("/list")
    public Result<List<MeetingVO>> list(
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        List<MeetingVO> meetings = meetingService.getMeetingList(userId, keyword);
        return Result.success(meetings);
    }

    /**
     * 获取会议详情
     */
    @GetMapping("/{id}")
    public Result<MeetingVO> detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        MeetingVO meeting = meetingService.getMeetingDetail(id, userId);
        return Result.success(meeting);
    }

    /**
     * 上传会议录音
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        Meeting meeting = meetingService.uploadMeeting(file, userId);

        Map<String, Object> data = new HashMap<>();
        data.put("meetingId", meeting.getId());
        data.put("status", meeting.getStatus());

        return Result.success(data);
    }

    /**
     * 切换待办状态
     */
    @PostMapping("/{id}/todo/{todoId}/toggle")
    public Result<Void> toggleTodo(
            @PathVariable Long id,
            @PathVariable Integer todoId,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        meetingService.toggleTodo(id, todoId, userId);
        return Result.success();
    }

    /**
     * 删除会议
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        Meeting meeting = meetingService.getById(id);
        if (meeting == null || !meeting.getUserId().equals(userId)) {
            return Result.error("会议不存在");
        }
        meetingService.removeById(id);
        return Result.success();
    }

    /**
     * 获取当前用户ID（从请求属性中获取）
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId != null) {
            return (Long) userId;
        }
        // 开发测试阶段返回固定用户ID
        return 1L;
    }
}
