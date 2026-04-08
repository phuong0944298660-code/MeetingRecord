package com.meeting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meeting.entity.Meeting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 会议Mapper
 */
@Mapper
public interface MeetingMapper extends BaseMapper<Meeting> {

    /**
     * 根据用户ID和关键词搜索会议
     */
    @Select("SELECT * FROM meeting WHERE user_id = #{userId} AND deleted = 0 " +
            "AND (title LIKE CONCAT('%', #{keyword}, '%') OR summary LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY created_at DESC")
    List<Meeting> searchByKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);
}
