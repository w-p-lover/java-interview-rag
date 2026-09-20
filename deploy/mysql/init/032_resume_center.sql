CREATE TABLE IF NOT EXISTS t_resume_record (
    id CHAR(36) NOT NULL COMMENT '简历记录 ID',
    name VARCHAR(200) NOT NULL COMMENT '简历名称',
    target_role VARCHAR(200) NOT NULL COMMENT '目标岗位',
    current_version_id CHAR(36) NULL COMMENT '当前简历版本 ID',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_resume_record_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历中心记录';

CREATE TABLE IF NOT EXISTS t_resume_version (
    id CHAR(36) NOT NULL COMMENT '简历版本 ID',
    resume_id CHAR(36) NOT NULL COMMENT '简历记录 ID',
    version_number INT NOT NULL COMMENT '记录内版本号',
    content_hash CHAR(64) NOT NULL COMMENT '原始文件 SHA-256',
    original_filename VARCHAR(255) NOT NULL COMMENT '原始文件名',
    content_type VARCHAR(100) NOT NULL COMMENT '解析文件类型',
    file_size BIGINT NOT NULL COMMENT '原始文件字节数',
    full_text MEDIUMTEXT NULL COMMENT '用户明确选择保存的完整正文',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_resume_version_hash (content_hash),
    UNIQUE KEY uk_resume_version_number (resume_id, version_number),
    KEY idx_resume_version_record_created (resume_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历不可变版本';
