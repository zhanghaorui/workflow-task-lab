package com.practice.workflow.outbox.repository;

import com.practice.workflow.outbox.domain.WorkflowTaskOutbox;
import com.practice.workflow.outbox.enums.OutboxEventType;
import com.practice.workflow.outbox.enums.OutboxStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

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

    private static final WorkFlowOutboxRowMapper ROW_MAPPER = new WorkFlowOutboxRowMapper();

    /**
     *
     * @param wokflowTaskOutbox
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


    public WorkflowTaskOutbox findOutboxByTaskId(Long taskId) {
        String sql = "SELECT * FROM workflow_event where task_id = :taskId";
        MapSqlParameterSource parameterSource = new MapSqlParameterSource("task_id", taskId);
        return namedParameterJdbcTemplate.query(sql, parameterSource, ROW_MAPPER)
                .stream()
                .findFirst()
                .orElse(null);
    }
}


class WorkFlowOutboxRowMapper implements RowMapper<WorkflowTaskOutbox> {
    @Nullable
    @Override
    public WorkflowTaskOutbox mapRow(ResultSet rs, int rowNum) throws SQLException {
        WorkflowTaskOutbox workflowTaskOutbox = new WorkflowTaskOutbox();
        workflowTaskOutbox.setId(rs.getLong("id"));
        workflowTaskOutbox.setTaskId(rs.getLong("task_id"));
        workflowTaskOutbox.setBizKey(rs.getString("biz_key"));
        workflowTaskOutbox.setMessageKey(rs.getString("message_key"));
        workflowTaskOutbox.setEventType(OutboxEventType.valueOf(rs.getString("event_type")));
        workflowTaskOutbox.setPayload(rs.getString("payload"));
        workflowTaskOutbox.setStatus(OutboxStatus.valueOf(rs.getString("status")));
        workflowTaskOutbox.setRetryCount(rs.getInt("retry_count"));
        workflowTaskOutbox.setMaxRetry(rs.getInt("max_retry"));
        workflowTaskOutbox.setLastError(rs.getString("last_error"));
        workflowTaskOutbox.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        workflowTaskOutbox.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        workflowTaskOutbox.setSentAt(rs.getObject("sent_at", LocalDateTime.class));
        return workflowTaskOutbox;
    }
}