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

    /**
     * 根据 API Key 获取 Agent 信息（用于代理服务，返回未脱敏的真实密钥）
     *
     * @param apiKey API密钥
     * @return Agent信息（包含真实的LLM API Key）
     */
    AgentDTO getByApiKeyForProxy(String apiKey);
}
