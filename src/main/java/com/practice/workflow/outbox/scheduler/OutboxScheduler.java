package com.practice.workflow.outbox.scheduler;

import com.alibaba.fastjson2.JSONObject;
import com.practice.workflow.messaging.message.WorkflowTaskMessage;
import com.practice.workflow.messaging.publisher.WorkflowTaskMessagePublisher;
import com.practice.workflow.outbox.domain.WorkflowTaskOutbox;
import com.practice.workflow.outbox.enums.OutboxStatus;
import com.practice.workflow.outbox.repository.WorkFlowTaskOutboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox 消息发送定时任务
 *
 * <p>定期扫描 workflow_task_outbox 表中 PENDING 状态的事件，
 * 发送至 RabbitMQ，成功后更新状态为 SENT，失败则记录错误并增加重试次数。
 *
 * @author 张浩锐
 * @version V1.0.0
 * @date 2026/6/21 10:45
 */
@Component
@Slf4j
public class OutboxScheduler {


    @Autowired
    private WorkFlowTaskOutboxRepository workFlowTaskOutboxRepository;


    @Autowired
    private WorkflowTaskMessagePublisher workflowTaskMessagePublisher;

    /**
     * 执行 Outbox 消息发送任务
     *
     * <p>cron 表达式：每 2 秒执行一次
     * <pre>
     *   秒    分    时    日    月    周
     *   0/2   *     *     *     *     ?
     * </pre>
     *
     * <p>执行逻辑：
     * 1. 查询 status = 'PENDING' 且 retry_count < max_retry 的记录
     * 2. 逐条发送至 RabbitMQ
     * 3. 发送成功：更新 status = 'SENT', sent_at = now()
     * 4. 发送失败：更新 retry_count++, last_error = 异常信息
     * 若 retry_count >= max_retry，则 status = 'FAILED'
     */
    @Scheduled(cron = "${workflow.outbox.cron:0/2 * * * * ?}")
    public void sendOutboxMessages() {
        log.debug("[OutboxScheduler] 开始执行 Outbox 消息发送任务...");
        List<WorkflowTaskOutbox> outboxList = workFlowTaskOutboxRepository.findPendingOutBoxList();
        if (CollectionUtils.isEmpty(outboxList)) {
            log.debug("没有PENDING任务");
            return;
        }

        outboxList.forEach(item -> {
            WorkflowTaskMessage workflowTaskMessage = new WorkflowTaskMessage();
            workflowTaskMessage = JSONObject.parseObject(item.getPayload(), WorkflowTaskMessage.class);
            try {
                workflowTaskMessagePublisher.publishTaskMessage(workflowTaskMessage);
                item.setSentAt(LocalDateTime.now());
                item.setUpdatedAt(LocalDateTime.now());
                item.setStatus(OutboxStatus.SENT);
                workFlowTaskOutboxRepository.markSent(item);
                log.info("[OutboxScheduler] 消息发送成功: id={}, taskId={}, bizKey={}",
                        item.getId(), item.getTaskId(), item.getBizKey());
            } catch (Exception e) {
                log.error("[OutboxScheduler] 消息发送失败: id={}, taskId={}, bizKey={}, error={}",
                        item.getId(), item.getTaskId(), item.getBizKey(), e.getMessage(), e);
                workFlowTaskOutboxRepository.markFailed(
                        item.getId(),
                        e.getMessage(),
                        LocalDateTime.now()
                );
            }
        });

        log.debug("[OutboxScheduler] Outbox 消息发送任务完成");
    }
}