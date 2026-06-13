package com.practice.workflow;

import com.alibaba.fastjson2.JSONObject;
import com.practice.workflow.domain.WorkflowTask;
import com.practice.workflow.service.WorkflowTaskService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
class WorkflowTaskLabApplicationTests {

    @Test
    void contextLoads() {
    }


    @Autowired
    private WorkflowTaskService workflowTaskService;

    @Test
    public void testCreateTask() {
        Long projectId = 100L;
        String bizType = "QC";
        String sourceId = "mq";
        String bizId = "b";
        WorkflowTask task = workflowTaskService.createTask(projectId, bizType, bizId, sourceId);
        log.info(JSONObject.toJSONString(task));
    }
}

