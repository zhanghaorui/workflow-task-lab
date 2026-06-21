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
