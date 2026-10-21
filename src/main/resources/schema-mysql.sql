-- ============================================================
-- AI 心理健康咨询系统 · MySQL 8 表结构
-- 与 schema.sql（H2 版）字段一致，差异：
--   1. 索引改为建表内联 KEY（MySQL 8 不支持 CREATE INDEX IF NOT EXISTS）
--   2. 引擎与字符集显式声明 utf8mb4
-- 可重复执行（全部 IF NOT EXISTS）
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(100) NOT NULL,
    real_name   VARCHAR(50)  NOT NULL,
    phone       VARCHAR(20),
    email       VARCHAR(80),
    gender      VARCHAR(10),
    age         INT,
    city        VARCHAR(50),
    avatar      VARCHAR(255),
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER',
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS doctor (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT         NOT NULL,
    hospital      VARCHAR(120),
    title         VARCHAR(80),
    specialty     VARCHAR(200),
    intro         TEXT,
    years         INT            DEFAULT 0,
    price         DECIMAL(10, 2) DEFAULT 0,
    rating        DOUBLE         DEFAULT 5.0,
    consult_count INT            DEFAULT 0,
    cert_no       VARCHAR(60),
    status        VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    audit_remark  VARCHAR(300),
    created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_doctor_user (user_id),
    KEY idx_doctor_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS assessment (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    code           VARCHAR(30)  NOT NULL,
    name           VARCHAR(80)  NOT NULL,
    description    VARCHAR(1000),
    instruction    VARCHAR(1000),
    question_count INT          DEFAULT 0,
    thresholds     VARCHAR(60),
    levels         VARCHAR(120),
    enabled        TINYINT(1)   NOT NULL DEFAULT 1,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_assessment_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS assessment_question (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    assessment_id BIGINT       NOT NULL,
    seq           INT          NOT NULL,
    content       VARCHAR(300) NOT NULL,
    reverse       TINYINT(1)   NOT NULL DEFAULT 0,
    KEY idx_question_assessment (assessment_id, seq)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS assessment_record (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    assessment_id   BIGINT       NOT NULL,
    assessment_name VARCHAR(80)  NOT NULL,
    raw_score       INT          NOT NULL,
    standard_score  DOUBLE       NOT NULL,
    level           VARCHAR(30)  NOT NULL,
    detail          VARCHAR(1500),
    suggestion      VARCHAR(2000),
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_record_user (user_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS ai_session (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    title      VARCHAR(120) NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ai_session_user (user_id, updated_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS ai_message (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT      NOT NULL,
    role       VARCHAR(20) NOT NULL,
    content    TEXT,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ai_msg_session (session_id, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS resource (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(150) NOT NULL,
    type        VARCHAR(20)  NOT NULL DEFAULT 'ARTICLE',
    category    VARCHAR(50),
    cover       VARCHAR(255),
    url         VARCHAR(500),
    content     TEXT,
    uploader_id BIGINT,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    view_count  INT          NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_resource_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS article (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    title      VARCHAR(150) NOT NULL,
    category   VARCHAR(50),
    cover      VARCHAR(255),
    summary    VARCHAR(500),
    content    TEXT,
    author     VARCHAR(50),
    view_count INT          NOT NULL DEFAULT 0,
    status     VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS notice (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(150) NOT NULL,
    content      TEXT,
    publisher_id BIGINT,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS symptom_record (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT      NOT NULL,
    mood          INT         NOT NULL DEFAULT 3,
    severity      INT         NOT NULL DEFAULT 3,
    energy        INT         NOT NULL DEFAULT 3,
    trigger_event VARCHAR(500),
    symptoms      VARCHAR(300),
    coping        VARCHAR(300),
    note          VARCHAR(1000),
    record_date   DATE        NOT NULL,
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_symptom_user (user_id, record_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS doctor_patient (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id    BIGINT      NOT NULL,
    user_id      BIGINT      NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    apply_reason VARCHAR(500),
    remark       VARCHAR(300),
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_doctor_patient (doctor_id, user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS chat_message (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_id    BIGINT   NOT NULL,
    to_id      BIGINT   NOT NULL,
    content    TEXT     NOT NULL,
    read_flag  TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_chat_pair (from_id, to_id, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS report (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id    BIGINT       NOT NULL,
    user_id      BIGINT       NOT NULL,
    title        VARCHAR(150) NOT NULL,
    overview     TEXT,
    insight      TEXT,
    risk_warning TEXT,
    suggestion   TEXT,
    status       VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at DATETIME,
    KEY idx_report_doctor (doctor_id),
    KEY idx_report_user (user_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS action_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id   BIGINT      NOT NULL,
    action      VARCHAR(20) NOT NULL,
    weight      DOUBLE      NOT NULL DEFAULT 1,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_action_user (user_id, target_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS micro_action_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    level       INT          NOT NULL,
    content     VARCHAR(120) NOT NULL,
    action_date DATE         NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_micro_user (user_id, action_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS practice_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    type        VARCHAR(32)  NOT NULL,
    duration    INT,
    content     VARCHAR(4000),
    mood_before INT,
    mood_after  INT,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_practice_user (user_id, type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;
