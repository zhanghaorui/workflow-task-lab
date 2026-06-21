package com.practice.workflow.repository;

import com.practice.workflow.common.enums.WorkflowTaskStatus;
import com.practice.workflow.domain.WorkflowTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 工作流任务数据访问层
 *
 * @author 张浩锐
 * @version V1.0.0
 * @date 2026/6/8 22:43
 */
@Repository
public class WorkflowTaskRepository {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private static final WorkFlowRowMapper ROW_MAPPER = new WorkFlowRowMapper();

    public long insertWorkFlowTask(WorkflowTask workflowTask) {
        String sql = "INSERT INTO workflow_task " +
                "(biz_key, project_id, biz_type, biz_id, source_id, status, retry_count, max_retry, " +
                "worker_id, auto_result, manual_result, final_result, error_message, created_at, updated_at, started_at, finished_at) " +
                "VALUES (:bizKey, :projectId, :bizType, :bizId, :sourceId, :status, :retryCount, :maxRetry, " +
                ":workerId, :autoResult, :manualResult, :finalResult, :errorMessage, :createdAt, :updatedAt, :startedAt, :finishedAt)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("bizKey", workflowTask.getBizKey())
                .addValue("projectId", workflowTask.getProjectId())
                .addValue("bizType", workflowTask.getBizType())
                .addValue("bizId", workflowTask.getBizId())
                .addValue("sourceId", workflowTask.getSourceId())
                .addValue("status", workflowTask.getStatus().name())  // 枚举 → 字符串
                .addValue("retryCount", workflowTask.getRetryCount())
                .addValue("maxRetry", workflowTask.getMaxRetry())
                .addValue("workerId", workflowTask.getWorkerId())
                .addValue("autoResult", workflowTask.getAutoResult())
                .addValue("manualResult", workflowTask.getManualResult())
                .addValue("finalResult", workflowTask.getFinalResult())
                .addValue("errorMessage", workflowTask.getErrorMessage())
                .addValue("createdAt", workflowTask.getCreatedAt())
                .addValue("updatedAt", workflowTask.getUpdatedAt())
                .addValue("startedAt", workflowTask.getStartedAt())
                .addValue("finishedAt", workflowTask.getFinishedAt());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        namedParameterJdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public WorkflowTask findById(Long id) {
        String sql = "SELECT * FROM workflow_task WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public WorkflowTask findByBizKey(String bizKey) {
        String sql = "SELECT * FROM workflow_task WHERE biz_key = :bizKey";
        MapSqlParameterSource params = new MapSqlParameterSource("bizKey", bizKey);
        return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER)
                .stream()
                .findFirst()
                .orElse(null);
    }


    public int tryStartTask(WorkflowTask workflowTask) {
        String sql = "UPDATE workflow_task set status = :status, worker_id =:workerId, started_at=:startedAt, updated_at=:updatedAt  where id=:id and status in ('WAITING', 'AUTO_PROCESS_FAILED')\n" +
                "  and retry_count < max_retry";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("status", WorkflowTaskStatus.PROCESSING.name())
                .addValue("workerId", workflowTask.getWorkerId())
                .addValue("startedAt", workflowTask.getStartedAt())
                .addValue("updatedAt", workflowTask.getUpdatedAt())
                .addValue("id", workflowTask.getId());
        int update = namedParameterJdbcTemplate.update(sql, params);
        return update;
    }


    /**
     * 标记任务失败（合并 AUTO_PROCESS_FAILED 和 FAILED 逻辑）
     * <p>
     * 条件：status = PROCESSING 且 worker_id 匹配
     * 逻辑：
     * - retry_count + 1
     * - 如果新的 retry_count >= max_retry：status = FAILED, 设置 finished_at
     * - 否则：status = AUTO_PROCESS_FAILED, 不设置 finished_at
     *
     * @param taskId      任务 ID
     * @param workerId    当前持有者 ID
     * @param errorMessage 错误信息
     * @param updatedAt   更新时间
     * @param finishedAt  失败时间（仅当达到最大重试次数时使用）
     * @return 影响行数，0 表示状态或 workerId 不匹配
     */
    public int failTask(Long taskId, String workerId, String errorMessage, LocalDateTime updatedAt, LocalDateTime finishedAt) {
        String sql = "UPDATE workflow_task SET " +
                "status = CASE WHEN retry_count + 1 >= max_retry THEN :statusFailed ELSE :statusAutoFailed END, " +
                "retry_count = retry_count + 1, " +
                "finished_at = CASE WHEN retry_count + 1 >= max_retry THEN :finishedAt ELSE finished_at END, " +
                "updated_at = :updatedAt, " +
                "error_message = :errorMessage " +
                "WHERE id = :id AND status = :statusProcessing AND worker_id = :workerId";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("statusFailed", WorkflowTaskStatus.FAILED.name())
                .addValue("statusAutoFailed", WorkflowTaskStatus.AUTO_PROCESS_FAILED.name())
                .addValue("statusProcessing", WorkflowTaskStatus.PROCESSING.name())
                .addValue("finishedAt", finishedAt)
                .addValue("updatedAt", updatedAt)
                .addValue("errorMessage", errorMessage)
                .addValue("id", taskId)
                .addValue("workerId", workerId);

        return namedParameterJdbcTemplate.update(sql, params);
    }

    public int finishAutoProcess(Long taskId, String workerId, String autoResult, LocalDateTime updatedAt) {
        String sql = "UPDATE workflow_task set status = :status,  updated_at=:updatedAt, auto_result=:autoResult  where id=:id and worker_id = :workerId   and status = 'PROCESSING'";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("status", WorkflowTaskStatus.WAIT_MANUAL_REVIEW.name())
                .addValue("updatedAt", updatedAt)
                .addValue("autoResult", autoResult)
                .addValue("id", taskId)
                .addValue("workerId", workerId);
        return namedParameterJdbcTemplate.update(sql, params);
    }

    public int reviewTask(WorkflowTask workflowTask) {
        String sql = "UPDATE workflow_task set status = :status,  updated_at=:updatedAt, finished_at=:finishedAt, manual_result = :manualResult, final_result = :finalResult  where id=:id and status = 'WAIT_MANUAL_REVIEW'";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("status", workflowTask.getStatus().name())
                .addValue("updatedAt", workflowTask.getUpdatedAt())
                .addValue("finishedAt", workflowTask.getFinishedAt())
                .addValue("manualResult", workflowTask.getManualResult())
                .addValue("finalResult", workflowTask.getFinalResult())
                .addValue("id", workflowTask.getId());
        return namedParameterJdbcTemplate.update(sql, params);
    }
}

/**
 * WorkflowTask ResultSet 行映射器
 */
class WorkFlowRowMapper implements RowMapper<WorkflowTask> {

    @Override
    public WorkflowTask mapRow(ResultSet rs, int rowNum) throws SQLException {
        WorkflowTask task = new WorkflowTask();
        task.setId(rs.getLong("id"));
        task.setBizKey(rs.getString("biz_key"));
        task.setProjectId(rs.getLong("project_id"));
        task.setBizType(rs.getString("biz_type"));
        task.setBizId(rs.getString("biz_id"));
        task.setSourceId(rs.getString("source_id"));
        task.setStatus(WorkflowTaskStatus.valueOf(rs.getString("status")));
        task.setRetryCount(rs.getInt("retry_count"));
        task.setMaxRetry(rs.getInt("max_retry"));
        task.setWorkerId(rs.getString("worker_id"));
        task.setAutoResult(rs.getString("auto_result"));
        task.setManualResult(rs.getString("manual_result"));
        task.setFinalResult(rs.getString("final_result"));
        task.setErrorMessage(rs.getString("error_message"));
        task.setCreatedAt(rs.getObject("created_at", java.time.LocalDateTime.class));
        task.setUpdatedAt(rs.getObject("updated_at", java.time.LocalDateTime.class));
        task.setStartedAt(rs.getObject("started_at", java.time.LocalDateTime.class));
        task.setFinishedAt(rs.getObject("finished_at", java.time.LocalDateTime.class));
        return task;
    }
}