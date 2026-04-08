-- 会议纪要应用数据库初始化脚本
CREATE DATABASE IF NOT EXISTS meeting_record CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE meeting_record;

CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '加密密码',
    email VARCHAR(100) COMMENT '邮箱',
    avatar VARCHAR(500) COMMENT '头像URL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS meeting (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(200) COMMENT '会议标题',
    audio_path VARCHAR(500) NOT NULL COMMENT '音频文件路径',
    duration VARCHAR(20) COMMENT '音频时长',
    file_size BIGINT COMMENT '文件大小(字节)',
    summary TEXT COMMENT '会议摘要',
    todos JSON COMMENT '待办事项',
    decisions JSON COMMENT '决策内容',
    transcript JSON COMMENT '逐字稿数据',
    status TINYINT DEFAULT 0 COMMENT '0-处理中 1-完成 2-失败',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议表';

INSERT INTO user (username, password, email) VALUES
('admin', '\$2a\$10\$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', 'admin@example.com'),
('test', '\$2a\$10\$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', 'test@example.com');
