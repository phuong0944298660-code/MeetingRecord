package com.meeting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 会议纪要应用启动类
 */
@SpringBootApplication
@EnableAsync
public class MeetingRecordApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeetingRecordApplication.class, args);
    }
}
