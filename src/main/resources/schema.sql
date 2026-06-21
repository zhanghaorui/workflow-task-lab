CREATE TABLE IF NOT EXISTS workflow_task
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    biz_key       VARCHAR(256) NOT NULL UNIQUE,
    project_id    BIGINT       NOT NULL,
    biz_type      VARCHAR(64)  NOT NULL,
    biz_id        VARCHAR(128) NOT NULL,
    source_id     VARCHAR(128) NOT NULL,
    status        VARCHAR(32)  NOT NULL,
    retry_count   INT          NOT NULL DEFAULT 0,
    max_retry     INT          NOT NULL DEFAULT 3,
    worker_id     VARCHAR(128),
    auto_result   TEXT,
    manual_result TEXT,
    final_result  TEXT,
    error_message TEXT,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL,
    started_at    TIMESTAMP,
    finished_at   TIMESTAMP
);

-- 业务幂等键唯一索引，防止重复创建任务，同时加速 bizKey 查询
CREATE UNIQUE INDEX IF NOT EXISTS uidx_workflow_task_biz_key ON workflow_task (biz_key);

-- 工作流事件表（MQ消息事件）
CREATE TABLE IF NOT EXISTS workflow_event
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id     BIGINT       NOT NULL,
    biz_key     VARCHAR(256) NOT NULL,
    event_type  VARCHAR(64)  NOT NULL,
    message_key varchar(64)  NOT NULL,
    payload     TEXT,
    status      VARCHAR(32)  NOT NULL,
    retry_count INT          NOT NULL DEFAULT 0,
    max_retry   INT          NOT NULL DEFAULT 3,
    last_error  TEXT,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,
    sent_at     TIMESTAMP
);

-- 任务关联索引，加速按任务查询事件
CREATE INDEX IF NOT EXISTS idx_workflow_event_task_id ON workflow_event (task_id);

-- 业务键索引，支持幂等检查和快速查询
CREATE INDEX IF NOT EXISTS idx_workflow_event_biz_key ON workflow_event (biz_key);

-- 状态索引，加速待处理事件查询
CREATE INDEX IF NOT EXISTS idx_workflow_event_status ON workflow_event (status);
