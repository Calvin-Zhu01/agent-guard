package com.agentguard.agent.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.agentguard.agent.dto.AgentCreateDTO;
import com.agentguard.agent.dto.AgentDTO;
import com.agentguard.agent.dto.AgentUpdateDTO;
import com.agentguard.agent.entity.AgentDO;
import com.agentguard.agent.enums.AgentStatus;
import com.agentguard.agent.mapper.AgentMapper;
import com.agentguard.agent.service.AgentService;
import com.agentguard.common.exception.BusinessException;
import com.agentguard.common.exception.ErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agent 服务实现类
 *
 * @author zhuhx
 */
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private static final String API_KEY_PREFIX = "ag_";

    private final AgentMapper agentMapper;

    @Override
    @Transactional
    public AgentDTO create(AgentCreateDTO dto) {
        AgentDO agentDO = BeanUtil.copyProperties(dto, AgentDO.class);
        agentDO.setApiKey(generateApiKey());
        agentDO.setStatus(AgentStatus.ENABLED);
        agentMapper.insert(agentDO);
        return BeanUtil.copyProperties(agentDO, AgentDTO.class);
    }

    @Override
    public AgentDTO getById(String id) {
        AgentDO agentDO = agentMapper.selectById(id);
        if (ObjectUtil.isNull(agentDO)) {
            throw new BusinessException(ErrorCode.AGENT_NOT_FOUND);
        }
        return BeanUtil.copyProperties(agentDO, AgentDTO.class);
    }

    @Override
    public IPage<AgentDTO> page(Page<AgentDTO> page, String keyword) {
        LambdaQueryWrapper<AgentDO> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(AgentDO::getName, keyword)
                    .or().like(AgentDO::getDescription, keyword));
        }
        wrapper.orderByDesc(AgentDO::getCreatedAt);

        Page<AgentDO> entityPage = new Page<>(page.getCurrent(), page.getSize());
        Page<AgentDO> result = agentMapper.selectPage(entityPage, wrapper);

        return result.convert(entity -> BeanUtil.copyProperties(entity, AgentDTO.class));
    }

    @Override
    @Transactional
    public AgentDTO update(String id, AgentUpdateDTO dto) {
        AgentDO agentDO = agentMapper.selectById(id);
        if (ObjectUtil.isNull(agentDO)) {
            throw new BusinessException(ErrorCode.AGENT_NOT_FOUND);
        }

        // 使用 Hutool 忽略空值拷贝
        BeanUtil.copyProperties(dto, agentDO, CopyOptions.create().ignoreNullValue());
        
        agentDO.setUpdatedAt(java.time.LocalDateTime.now());

        agentMapper.updateById(agentDO);
        return BeanUtil.copyProperties(agentDO, AgentDTO.class);
    }

    @Override
    @Transactional
    public void delete(String id) {
        if (agentMapper.deleteById(id) == 0) {
            throw new BusinessException(ErrorCode.AGENT_NOT_FOUND);
        }
    }

    @Override
    public AgentDTO getByApiKey(String apiKey) {
        AgentDO agentDO = agentMapper.selectOne(
                new LambdaQueryWrapper<AgentDO>().eq(AgentDO::getApiKey, apiKey)
        );
        if (ObjectUtil.isNull(agentDO)) {
            return null;
        }
        return BeanUtil.copyProperties(agentDO, AgentDTO.class);
    }

    private String generateApiKey() {
        return API_KEY_PREFIX + IdUtil.simpleUUID();
    }
}
