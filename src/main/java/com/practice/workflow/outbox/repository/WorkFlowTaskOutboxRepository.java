package com.practice.workflow.outbox.repository;

import com.practice.workflow.outbox.domain.WorkflowTaskOutbox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * <p>TODO</p>
 *
 * @author 张浩锐
 * @version V1.0.0
 * @date 2026/6/21 13:56
 */
@Repository
public class WorkFlowTaskOutboxRepository {


    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /**
     *
     * @param workflowTaskOutbox
     */

    public void insertWorkFlowTaskOutbox(WorkflowTaskOutbox workflowTaskOutbox) {

        String sql = "insert into workflow_event (task_id, biz_key, event_type, message_key, payload, status, retry_count, max_retry, last_error, created_at, updated_at, sent_at) " +
                "valus (:taskId, :bizKey, :eventType, :messageKey,:payload, :status, :retryCount, :maxRetry, :lastError, :createdAt, :updatedAt, :sentAt)";

        MapSqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("taskId", workflowTaskOutbox.getTaskId())
                .addValue("bizKey", workflowTaskOutbox.getBizKey())
                .addValue("eventType", workflowTaskOutbox.getEventType().name())
                .addValue("messageKey", workflowTaskOutbox.getMessageKey())
                .addValue("payload", workflowTaskOutbox.getPayload())
                .addValue("status", workflowTaskOutbox.getStatus().name())
                .addValue("retryCount", workflowTaskOutbox.getRetryCount())
                .addValue("maxRetry", workflowTaskOutbox.getMaxRetry())
                .addValue("lastError", workflowTaskOutbox.getLastError())
                .addValue("createdAt", workflowTaskOutbox.getCreatedAt())
                .addValue("updatedAt", workflowTaskOutbox.getUpdatedAt())
                .addValue("sentAt", workflowTaskOutbox.getSentAt());
        namedParameterJdbcTemplate.update(sql, parameterSource);
    }


}
