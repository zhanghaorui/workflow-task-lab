package com.practice.workflow.outbox.service.impl;

import com.practice.workflow.outbox.repository.WorkFlowTaskOutboxRepository;
import com.practice.workflow.outbox.service.WorkFlowTaskOutboxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>TODO</p>
 *
 * @author 张浩锐
 * @version V1.0.0
 * @date 2026/6/21 14:17
 */
@Service
@Slf4j
public class WorkFlowTaskOutboxServiceImpl implements WorkFlowTaskOutboxService {

    @Autowired
    private WorkFlowTaskOutboxRepository workFlowTaskOutboxRepository;


}
