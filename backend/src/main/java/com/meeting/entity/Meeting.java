package com.meeting.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会议记录实体
 */
@Data
@TableName("meeting")
public class Meeting {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String audioPath;

    private String duration;

    private Long fileSize;

    private String summary;

    private String todos;

    private String decisions;

    private String transcript;

    /**
     * 结构化会议纪要（飞书风格）：包含topics、stats等
     */
    private String meetingMinutes;

    /**
     * 状态：0-处理中，1-完成，2-失败
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
