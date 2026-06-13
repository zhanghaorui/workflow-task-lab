package com.practice.workflow.repository;

import com.practice.workflow.domain.WorkflowTask;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>TODO</p>
 *
 * @author 张浩锐
 * @version V1.0.0
 * @date 2026/6/8 22:43
 */
@Repository
public class WorkflowTaskRepository {


    private final ConcurrentHashMap<Long, WorkflowTask> taskMap = new ConcurrentHashMap<Long, WorkflowTask>();


    private final ConcurrentHashMap<String, Long> bizKeyIndex = new ConcurrentHashMap<String, Long>();

    private final AtomicLong idGenerator = new AtomicLong(0L);

    public synchronized long insertTask(WorkflowTask workflowTask) {
        long l = idGenerator.getAndIncrement();
        taskMap.put(l, workflowTask);
        bizKeyIndex.put(workflowTask.getBizKey(), l);
        return l;
    }

    public WorkflowTask getWorkflowTaskById(long id) {
        if (taskMap.containsKey(id)) {
            return taskMap.get(id);
        }
        return null;
    }

    public WorkflowTask findWorkflowTaskByBizKey(String bizKey) {
        if (!bizKeyIndex.containsKey(bizKey)) {
            return null;
        }
        return taskMap.getOrDefault(bizKeyIndex.get(bizKey), null);
    }
}
