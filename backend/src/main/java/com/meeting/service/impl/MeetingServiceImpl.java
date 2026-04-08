package com.meeting.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meeting.dto.MeetingVO;
import com.meeting.entity.Meeting;
import com.meeting.mapper.MeetingMapper;
import com.meeting.service.KimiService;
import com.meeting.service.MeetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 会议服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingServiceImpl extends ServiceImpl<MeetingMapper, Meeting> implements MeetingService {

    private final MeetingMapper meetingMapper;
    private final KimiService kimiService;

    @Value("${meeting.upload.path:./uploads}")
    private String uploadPath;

    @Override
    public List<MeetingVO> getMeetingList(Long userId, String keyword) {
        List<Meeting> meetings;
        if (StringUtils.hasText(keyword)) {
            meetings = meetingMapper.searchByKeyword(userId, keyword);
        } else {
            meetings = lambdaQuery()
                    .eq(Meeting::getUserId, userId)
                    .orderByDesc(Meeting::getCreatedAt)
                    .list();
        }
        return meetings.stream()
                .map(MeetingVO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public MeetingVO getMeetingDetail(Long meetingId, Long userId) {
        Meeting meeting = lambdaQuery()
                .eq(Meeting::getId, meetingId)
                .eq(Meeting::getUserId, userId)
                .one();

        if (meeting == null) {
            throw new RuntimeException("会议不存在");
        }

        return MeetingVO.fromEntity(meeting);
    }

    @Override
    public Meeting uploadMeeting(MultipartFile file, Long userId) {
        log.info("开始上传文件: {}, userId: {}", file.getOriginalFilename(), userId);
        try {
            // 创建上传目录
            Path uploadDir = Paths.get(uploadPath).toAbsolutePath();
            log.info("上传目录: {}", uploadDir);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                log.info("创建上传目录: {}", uploadDir);
            }

            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String extension = StringUtils.getFilenameExtension(originalFilename);
            String filename = UUID.randomUUID().toString() + "." + extension;
            Path filePath = uploadDir.resolve(filename);

            log.info("保存文件到: {}", filePath);
            // 保存文件
            file.transferTo(filePath.toFile());
            log.info("文件保存成功: {}", filename);

            // 创建会议记录
            Meeting meeting = new Meeting();
            meeting.setUserId(userId);
            meeting.setTitle(StringUtils.stripFilenameExtension(originalFilename));
            meeting.setAudioPath(filePath.toString());
            meeting.setFileSize(file.getSize());
            meeting.setStatus(0); // 处理中

            save(meeting);
            log.info("会议记录创建成功, meetingId: {}", meeting.getId());

            // 异步处理会议
            new Thread(() -> processMeeting(meeting.getId())).start();

            return meeting;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public void toggleTodo(Long meetingId, Integer todoId, Long userId) {
        Meeting meeting = lambdaQuery()
                .eq(Meeting::getId, meetingId)
                .eq(Meeting::getUserId, userId)
                .one();

        if (meeting == null || meeting.getTodos() == null) {
            return;
        }

        List<MeetingVO.TodoItem> todos = JSON.parseArray(meeting.getTodos(), MeetingVO.TodoItem.class);
        for (MeetingVO.TodoItem todo : todos) {
            if (todo.getId().equals(todoId)) {
                todo.setDone(!todo.getDone());
                break;
            }
        }

        meeting.setTodos(JSON.toJSONString(todos));
        updateById(meeting);
    }

    @Override
    public void processMeeting(Long meetingId) {
        try {
            Meeting meeting = getById(meetingId);
            if (meeting == null) {
                return;
            }

            // 调用Kimi API处理，传递 meetingId 用于推送进度
            KimiService.MeetingResult result = kimiService.processAudio(meeting.getAudioPath(), meetingId);

            // 更新会议信息
            meeting.setDuration(result.getDuration());
            meeting.setSummary(result.getOverview() != null ? result.getOverview() : result.getSummary());
            meeting.setTodos(JSON.toJSONString(result.getTodos()));
            meeting.setDecisions(JSON.toJSONString(result.getDecisions()));
            meeting.setTranscript(JSON.toJSONString(result.getSpeakers()));

            // 保存新的结构化会议纪要（topics 和 stats）
            if (result.getTopics() != null) {
                Map<String, Object> minutesData = new HashMap<>();
                minutesData.put("overview", result.getOverview());
                minutesData.put("topics", result.getTopics());
                minutesData.put("stats", result.getStats());
                meeting.setMeetingMinutes(JSON.toJSONString(minutesData));
            }

            meeting.setStatus(1); // 完成

            updateById(meeting);

            log.info("会议处理完成: {}", meetingId);
        } catch (Exception e) {
            log.error("会议处理失败: {}", meetingId, e);
            // 更新状态为失败
            Meeting meeting = new Meeting();
            meeting.setId(meetingId);
            meeting.setStatus(2);
            updateById(meeting);
        }
    }
}
