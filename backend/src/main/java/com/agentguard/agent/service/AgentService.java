package com.agentguard.agent.service;

import com.agentguard.agent.dto.AgentCreateDTO;
import com.agentguard.agent.dto.AgentDTO;
import com.agentguard.agent.dto.AgentUpdateDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * Agent 服务接口
 *
 * @author zhuhx
 */
public interface AgentService {

    AgentDTO create(AgentCreateDTO dto);

    AgentDTO getById(String id);

    IPage<AgentDTO> page(Page<AgentDTO> page, String keyword);

    AgentDTO update(String id, AgentUpdateDTO dto);

    void delete(String id);

    AgentDTO getByApiKey(String apiKey);
}
