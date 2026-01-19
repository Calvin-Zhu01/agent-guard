package com.agentguard.policy.engine;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.agentguard.policy.dto.MaskConfig;
import com.agentguard.policy.dto.MaskRule;
import com.agentguard.policy.dto.PolicyDTO;
import com.agentguard.policy.dto.PolicyResult;
import com.agentguard.policy.dto.RateLimitResult;
import com.agentguard.policy.enums.PolicyAction;
import com.agentguard.policy.enums.PolicyScope;
import com.agentguard.policy.enums.PolicyType;
import com.agentguard.policy.service.PolicyService;
import com.agentguard.policy.service.RateLimiterService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * 配置化策略引擎实现
 * 
 * 从数据库加载已启用策略，支持URL模式匹配和条件评估
 * 支持Agent级别策略优先匹配
 *
 * @author zhuhx
 */
@Slf4j
@Primary
@Component("configurablePolicyEngine")
@RequiredArgsConstructor
public class ConfigurablePolicyEngine implements PolicyEngine {

    private final PolicyService policyService;
    private final RateLimiterService rateLimiterService;

    /** 缓存的策略列表（按优先级降序排列） */
    private final CopyOnWriteArrayList<PolicyDTO> cachedPolicies = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        refreshPolicies();
    }

    @Override
    public PolicyResult evaluate(String targetUrl, String method, Map<String, String> headers, 
                                  Map<String, Object> body, String agentId) {
        return evaluate(targetUrl, method, headers, body, agentId, null);
    }

    /**
     * 评估请求是否符合策略（带客户端IP）
     * 
     * 策略评估顺序：
     * 1. 首先评估Agent级别策略（scope=AGENT且agentId匹配）
     * 2. 然后评估全局策略（scope=GLOBAL）
     * Agent级别策略优先于全局策略
     *
     * @param targetUrl 目标URL
     * @param method HTTP方法
     * @param headers 请求头
     * @param body 请求体
     * @param agentId Agent ID
     * @param clientIp 客户端IP（用于频率限制）
     * @return 策略评估结果
     */
    public PolicyResult evaluate(String targetUrl, String method, Map<String, String> headers, 
                                  Map<String, Object> body, String agentId, String clientIp) {
        // 获取按优先级排序的策略列表（Agent级别优先）
        List<PolicyDTO> sortedPolicies = getSortedPoliciesForAgent(agentId);

        // 按优先级顺序评估策略
        for (PolicyDTO policy : sortedPolicies) {
            if (matchesPolicy(policy, targetUrl, method, headers, body)) {
                PolicyResult result = createResult(policy, targetUrl, headers, body, clientIp);
                // 如果策略结果是阻止或需要特殊处理，立即返回
                if (result.isBlocked() || result.isRequireApproval() || result.isRequireMask() 
                        || result.getRateLimitResult() != null) {
                    return result;
                }
            }
        }
        // 无匹配策略，默认允许
        return PolicyResult.allow();
    }

    /**
     * 获取针对特定Agent排序后的策略列表
     * 
     * 排序规则：
     * 1. Agent级别策略（scope=AGENT且agentId匹配）优先
     * 2. 同级别内按priority降序排列
     *
     * @param agentId Agent ID
     * @return 排序后的策略列表
     */
    private List<PolicyDTO> getSortedPoliciesForAgent(String agentId) {
        List<PolicyDTO> agentPolicies = new ArrayList<>();
        List<PolicyDTO> globalPolicies = new ArrayList<>();

        for (PolicyDTO policy : cachedPolicies) {
            PolicyScope scope = policy.getScope();
            
            // 如果scope为null，视为全局策略（兼容旧数据）
            if (ObjectUtil.isNull(scope) || scope == PolicyScope.GLOBAL) {
                globalPolicies.add(policy);
            } else if (scope == PolicyScope.AGENT) {
                // Agent级别策略需要匹配agentId
                if (StrUtil.isNotBlank(agentId) && agentId.equals(policy.getAgentId())) {
                    agentPolicies.add(policy);
                }
            }
        }

        // Agent级别策略优先，然后是全局策略
        List<PolicyDTO> result = new ArrayList<>();
        result.addAll(agentPolicies);
        result.addAll(globalPolicies);
        return result;
    }

    @Override
    public void refreshPolicies() {
        try {
            List<PolicyDTO> policies = policyService.getEnabledPolicies();
            // 按优先级降序排序
            policies.sort(Comparator.comparingInt(PolicyDTO::getPriority).reversed());
            cachedPolicies.clear();
            cachedPolicies.addAll(policies);
            log.info("Refreshed {} enabled policies", policies.size());
        } catch (Exception e) {
            log.error("Failed to refresh policies", e);
        }
    }


    /**
     * 检查请求是否匹配策略
     *
     * @param policy 策略
     * @param targetUrl 目标URL
     * @param method HTTP方法
     * @param headers 请求头
     * @param body 请求体
     * @return 是否匹配
     */
    private boolean matchesPolicy(PolicyDTO policy, String targetUrl, String method,
                                   Map<String, String> headers, Map<String, Object> body) {
        if (StrUtil.isBlank(policy.getConditions())) {
            return false;
        }

        try {
            JSONObject conditions = JSONUtil.parseObj(policy.getConditions());
            return evaluateConditions(conditions, targetUrl, method, headers, body);
        } catch (Exception e) {
            log.warn("Failed to parse policy conditions for policy {}: {}", policy.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * 评估条件
     *
     * @param conditions 条件JSON
     * @param targetUrl 目标URL
     * @param method HTTP方法
     * @param headers 请求头
     * @param body 请求体
     * @return 是否满足条件
     */
    private boolean evaluateConditions(JSONObject conditions, String targetUrl, String method,
                                        Map<String, String> headers, Map<String, Object> body) {
        // 检查URL模式
        String urlPattern = conditions.getStr("urlPattern");
        if (StrUtil.isNotBlank(urlPattern) && !matchesUrlPattern(targetUrl, urlPattern)) {
            return false;
        }

        // 检查HTTP方法
        String methodCondition = conditions.getStr("method");
        if (StrUtil.isNotBlank(methodCondition) && !methodCondition.equalsIgnoreCase(method)) {
            return false;
        }

        // 检查请求体条件
        if (conditions.containsKey("bodyConditions")) {
            List<JSONObject> bodyConditions = conditions.getBeanList("bodyConditions", JSONObject.class);
            if (CollUtil.isNotEmpty(bodyConditions) && !evaluateBodyConditions(bodyConditions, body)) {
                return false;
            }
        }

        // 检查请求头条件
        if (conditions.containsKey("headerConditions")) {
            List<JSONObject> headerConditions = conditions.getBeanList("headerConditions", JSONObject.class);
            if (CollUtil.isNotEmpty(headerConditions) && !evaluateHeaderConditions(headerConditions, headers)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 匹配URL模式
     *
     * @param targetUrl 目标URL
     * @param urlPattern URL正则模式
     * @return 是否匹配
     */
    private boolean matchesUrlPattern(String targetUrl, String urlPattern) {
        if (StrUtil.isBlank(targetUrl)) {
            return false;
        }
        try {
            Pattern pattern = Pattern.compile(urlPattern);
            return pattern.matcher(targetUrl).find();
        } catch (PatternSyntaxException e) {
            log.warn("Invalid URL pattern: {}", urlPattern);
            return false;
        }
    }

    /**
     * 评估请求体条件
     *
     * @param bodyConditions 请求体条件列表
     * @param body 请求体
     * @return 是否满足所有条件
     */
    private boolean evaluateBodyConditions(List<JSONObject> bodyConditions, Map<String, Object> body) {
        if (CollUtil.isEmpty(body)) {
            return false;
        }

        for (JSONObject condition : bodyConditions) {
            String field = condition.getStr("field");
            String operator = condition.getStr("operator");
            Object expectedValue = condition.get("value");

            if (StrUtil.isBlank(field) || StrUtil.isBlank(operator)) {
                continue;
            }

            Object actualValue = getNestedValue(body, field);
            if (!evaluateCondition(actualValue, operator, expectedValue)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 评估请求头条件
     *
     * @param headerConditions 请求头条件列表
     * @param headers 请求头
     * @return 是否满足所有条件
     */
    private boolean evaluateHeaderConditions(List<JSONObject> headerConditions, Map<String, String> headers) {
        if (CollUtil.isEmpty(headers)) {
            return false;
        }

        for (JSONObject condition : headerConditions) {
            String field = condition.getStr("field");
            String operator = condition.getStr("operator");
            Object expectedValue = condition.get("value");

            if (StrUtil.isBlank(field) || StrUtil.isBlank(operator)) {
                continue;
            }

            String actualValue = headers.get(field);
            if (!evaluateCondition(actualValue, operator, expectedValue)) {
                return false;
            }
        }
        return true;
    }


    /**
     * 获取嵌套字段值（支持点号分隔的路径）
     *
     * @param map 数据Map
     * @param field 字段路径（如 "user.name"）
     * @return 字段值
     */
    @SuppressWarnings("unchecked")
    private Object getNestedValue(Map<String, Object> map, String field) {
        if (map == null || StrUtil.isBlank(field)) {
            return null;
        }

        String[] parts = field.split("\\.");
        Object current = map;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
        }
        return current;
    }

    /**
     * 评估单个条件
     *
     * @param actualValue 实际值
     * @param operator 操作符
     * @param expectedValue 期望值
     * @return 是否满足条件
     */
    private boolean evaluateCondition(Object actualValue, String operator, Object expectedValue) {
        if (actualValue == null) {
            return "isNull".equals(operator);
        }

        return switch (operator.toLowerCase()) {
            case "eq", "equals" -> compareEquals(actualValue, expectedValue);
            case "ne", "notequals" -> !compareEquals(actualValue, expectedValue);
            case "gt" -> compareNumeric(actualValue, expectedValue) > 0;
            case "gte", "ge" -> compareNumeric(actualValue, expectedValue) >= 0;
            case "lt" -> compareNumeric(actualValue, expectedValue) < 0;
            case "lte", "le" -> compareNumeric(actualValue, expectedValue) <= 0;
            case "contains" -> String.valueOf(actualValue).contains(String.valueOf(expectedValue));
            case "startswith" -> String.valueOf(actualValue).startsWith(String.valueOf(expectedValue));
            case "endswith" -> String.valueOf(actualValue).endsWith(String.valueOf(expectedValue));
            case "matches" -> matchesPattern(String.valueOf(actualValue), String.valueOf(expectedValue));
            case "in" -> isInList(actualValue, expectedValue);
            case "notin" -> !isInList(actualValue, expectedValue);
            case "isnull" -> false; // actualValue is not null at this point
            case "isnotnull" -> true;
            default -> {
                log.warn("Unknown operator: {}", operator);
                yield false;
            }
        };
    }

    /**
     * 比较相等
     */
    private boolean compareEquals(Object actual, Object expected) {
        if (actual == null && expected == null) {
            return true;
        }
        if (actual == null || expected == null) {
            return false;
        }
        // 尝试数值比较
        if (isNumeric(actual) && isNumeric(expected)) {
            return compareNumeric(actual, expected) == 0;
        }
        return String.valueOf(actual).equals(String.valueOf(expected));
    }

    /**
     * 数值比较
     */
    private int compareNumeric(Object actual, Object expected) {
        BigDecimal actualNum = toBigDecimal(actual);
        BigDecimal expectedNum = toBigDecimal(expected);
        if (actualNum == null || expectedNum == null) {
            return String.valueOf(actual).compareTo(String.valueOf(expected));
        }
        return actualNum.compareTo(expectedNum);
    }

    /**
     * 检查是否为数值
     */
    private boolean isNumeric(Object value) {
        if (value instanceof Number) {
            return true;
        }
        if (value instanceof String) {
            return NumberUtil.isNumber((String) value);
        }
        return false;
    }

    /**
     * 转换为BigDecimal
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Number) {
                return NumberUtil.toBigDecimal((Number) value);
            }
            if (value instanceof String && NumberUtil.isNumber((String) value)) {
                return NumberUtil.toBigDecimal((String) value);
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    /**
     * 正则匹配
     */
    private boolean matchesPattern(String value, String pattern) {
        try {
            return Pattern.compile(pattern).matcher(value).matches();
        } catch (PatternSyntaxException e) {
            log.warn("Invalid regex pattern: {}", pattern);
            return false;
        }
    }

    /**
     * 检查值是否在列表中
     */
    @SuppressWarnings("unchecked")
    private boolean isInList(Object actual, Object expected) {
        if (expected instanceof List) {
            return ((List<Object>) expected).stream()
                    .anyMatch(item -> compareEquals(actual, item));
        }
        return compareEquals(actual, expected);
    }

    /**
     * 根据策略创建结果
     * 
     * 根据策略类型调用不同的处理逻辑：
     * - ACCESS_CONTROL: 访问控制，优先使用 conditions JSON 中的 action 字段
     * - CONTENT_PROTECTION: 内容保护，创建脱敏配置
     * - RATE_LIMIT: 频率限制，调用限流服务
     * - APPROVAL: 人工审批
     *
     * @param policy 匹配的策略
     * @param targetUrl 目标URL
     * @param headers 请求头
     * @param body 请求体
     * @param clientIp 客户端IP
     * @return 策略结果
     */
    private PolicyResult createResult(PolicyDTO policy, String targetUrl, Map<String, String> headers,
                                       Map<String, Object> body, String clientIp) {
        PolicyType policyType = policy.getType();
        
        if (ObjectUtil.isNull(policyType)) {
            // 默认按访问控制处理
            return createAccessControlResult(policy);
        }

        return switch (policyType) {
            case ACCESS_CONTROL -> createAccessControlResult(policy);
            case CONTENT_PROTECTION -> createContentProtectionResult(policy, targetUrl);
            case RATE_LIMIT -> createRateLimitResult(policy, targetUrl, headers, body, clientIp);
            case APPROVAL -> createApprovalResult(policy);
        };
    }

    /**
     * 创建访问控制策略结果
     */
    private PolicyResult createAccessControlResult(PolicyDTO policy) {
        PolicyAction action = resolveAction(policy);
        String reason = buildReason(policy, action);

        return switch (action) {
            case ALLOW -> PolicyResult.allow();
            case DENY -> PolicyResult.block(policy.getId(), action, reason);
            case APPROVAL -> PolicyResult.requireApproval(policy.getId(), reason);
            case MASK -> PolicyResult.block(policy.getId(), action, reason);
            case RATE_LIMIT -> PolicyResult.block(policy.getId(), action, reason);
        };
    }

    /**
     * 创建内容保护策略结果
     * 
     * 解析 sensitiveFields、sensitiveKeywords、maskRules 配置，创建 MaskConfig
     */
    private PolicyResult createContentProtectionResult(PolicyDTO policy, String targetUrl) {
        if (StrUtil.isBlank(policy.getConditions())) {
            log.warn("Content protection policy {} has no conditions", policy.getId());
            return PolicyResult.allow();
        }

        try {
            JSONObject conditions = JSONUtil.parseObj(policy.getConditions());
            
            // 检查 URL 模式是否匹配
            String urlPattern = conditions.getStr("urlPattern");
            if (StrUtil.isNotBlank(urlPattern) && !matchesUrlPattern(targetUrl, urlPattern)) {
                return PolicyResult.allow();
            }

            // 解析脱敏配置
            MaskConfig maskConfig = parseMaskConfig(conditions);
            
            if (ObjectUtil.isNull(maskConfig) || 
                    (CollUtil.isEmpty(maskConfig.getSensitiveFields()) 
                            && CollUtil.isEmpty(maskConfig.getSensitiveKeywords())
                            && CollUtil.isEmpty(maskConfig.getMaskRules()))) {
                log.warn("Content protection policy {} has no mask configuration", policy.getId());
                return PolicyResult.allow();
            }

            String reason = buildReason(policy, PolicyAction.MASK);
            return PolicyResult.mask(policy.getId(), maskConfig, reason);
        } catch (Exception e) {
            log.error("Failed to parse content protection policy {}: {}", policy.getId(), e.getMessage());
            return PolicyResult.allow();
        }
    }

    /**
     * 解析脱敏配置
     */
    private MaskConfig parseMaskConfig(JSONObject conditions) {
        MaskConfig config = new MaskConfig();

        // 解析 sensitiveFields
        if (conditions.containsKey("sensitiveFields")) {
            List<String> sensitiveFields = conditions.getBeanList("sensitiveFields", String.class);
            config.setSensitiveFields(sensitiveFields);
        }

        // 解析 sensitiveKeywords
        if (conditions.containsKey("sensitiveKeywords")) {
            List<String> sensitiveKeywords = conditions.getBeanList("sensitiveKeywords", String.class);
            config.setSensitiveKeywords(sensitiveKeywords);
        }

        // 解析 maskRules
        if (conditions.containsKey("maskRules")) {
            JSONObject maskRulesJson = conditions.getJSONObject("maskRules");
            if (ObjectUtil.isNotNull(maskRulesJson)) {
                Map<String, MaskRule> maskRules = new HashMap<>();
                for (String fieldName : maskRulesJson.keySet()) {
                    JSONObject ruleJson = maskRulesJson.getJSONObject(fieldName);
                    if (ObjectUtil.isNotNull(ruleJson)) {
                        MaskRule rule = new MaskRule();
                        rule.setStart(ruleJson.getInt("start", 0));
                        rule.setEnd(ruleJson.getInt("end", 0));
                        rule.setMaskChar(ruleJson.getStr("maskChar", "*"));
                        maskRules.put(fieldName, rule);
                    }
                }
                config.setMaskRules(maskRules);
            }
        }

        // 解析 urlPattern
        config.setUrlPattern(conditions.getStr("urlPattern"));

        return config;
    }

    /**
     * 创建频率限制策略结果
     * 
     * 解析 windowSeconds、maxRequests、keyExtractor、urlPattern 配置，调用限流服务
     */
    private PolicyResult createRateLimitResult(PolicyDTO policy, String targetUrl, 
                                                Map<String, String> headers, Map<String, Object> body, 
                                                String clientIp) {
        if (StrUtil.isBlank(policy.getConditions())) {
            log.warn("Rate limit policy {} has no conditions", policy.getId());
            return PolicyResult.allow();
        }

        try {
            JSONObject conditions = JSONUtil.parseObj(policy.getConditions());

            // 检查 URL 模式是否匹配
            String urlPattern = conditions.getStr("urlPattern");
            if (StrUtil.isNotBlank(urlPattern) && !rateLimiterService.matchUrl(targetUrl, urlPattern)) {
                return PolicyResult.allow();
            }

            // 解析限流配置
            int windowSeconds = conditions.getInt("windowSeconds", 60);
            int maxRequests = conditions.getInt("maxRequests", 100);
            String keyExtractor = conditions.getStr("keyExtractor", "ip");

            // 提取限流键
            String rateLimitKey = policy.getId() + ":" + 
                    rateLimiterService.extractKey(keyExtractor, headers, body, clientIp);

            // 检查限流
            RateLimitResult rateLimitResult = rateLimiterService.checkLimit(rateLimitKey, windowSeconds, maxRequests);

            String reason = rateLimitResult.isAllowed() 
                    ? null 
                    : buildRateLimitReason(policy, rateLimitResult);

            return PolicyResult.rateLimit(policy.getId(), rateLimitResult, reason);
        } catch (Exception e) {
            log.error("Failed to evaluate rate limit policy {}: {}", policy.getId(), e.getMessage());
            // 限流评估失败，降级为允许通过
            return PolicyResult.allow();
        }
    }

    /**
     * 构建限流原因
     */
    private String buildRateLimitReason(PolicyDTO policy, RateLimitResult result) {
        String policyName = StrUtil.isNotBlank(policy.getName()) ? policy.getName() : policy.getId();
        return String.format("请求被限流（策略：%s）：%s", policyName, result.getReason());
    }

    /**
     * 创建人工审批策略结果
     */
    private PolicyResult createApprovalResult(PolicyDTO policy) {
        String reason = buildReason(policy, PolicyAction.APPROVAL);
        return PolicyResult.requireApproval(policy.getId(), reason);
    }

    /**
     * 根据策略创建结果（兼容旧方法）
     * 
     * 优先使用 conditions JSON 中的 action 字段，如果不存在则使用策略记录中的 action
     *
     * @param policy 匹配的策略
     * @return 策略结果
     * @deprecated 使用 {@link #createResult(PolicyDTO, String, Map, Map, String)} 代替
     */
    @Deprecated
    private PolicyResult createResult(PolicyDTO policy) {
        return createAccessControlResult(policy);
    }

    /**
     * 解析策略的 action
     * 
     * 优先从 conditions JSON 中读取 action 字段，如果不存在或无效则使用策略记录中的 action
     *
     * @param policy 策略
     * @return 解析后的 action
     */
    private PolicyAction resolveAction(PolicyDTO policy) {
        // 尝试从 conditions JSON 中读取 action
        if (StrUtil.isNotBlank(policy.getConditions())) {
            try {
                JSONObject conditions = JSONUtil.parseObj(policy.getConditions());
                String conditionAction = conditions.getStr("action");
                if (StrUtil.isNotBlank(conditionAction)) {
                    // 尝试解析为 PolicyAction 枚举
                    PolicyAction parsedAction = parseAction(conditionAction);
                    if (parsedAction != null) {
                        log.debug("Using action from conditions: {} for policy {}", parsedAction, policy.getId());
                        return parsedAction;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse action from conditions for policy {}: {}", policy.getId(), e.getMessage());
            }
        }
        // 回退到策略记录中的 action
        return policy.getAction();
    }

    /**
     * 解析 action 字符串为枚举
     *
     * @param actionStr action 字符串
     * @return PolicyAction 枚举，如果无效则返回 null
     */
    private PolicyAction parseAction(String actionStr) {
        if (StrUtil.isBlank(actionStr)) {
            return null;
        }
        try {
            return PolicyAction.valueOf(actionStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid action value: {}", actionStr);
            return null;
        }
    }

    /**
     * 构建拦截原因
     *
     * @param policy 策略
     * @param action 解析后的 action
     * @return 原因描述
     */
    private String buildReason(PolicyDTO policy, PolicyAction action) {
        String actionDesc = switch (action) {
            case DENY -> "请求被拒绝";
            case APPROVAL -> "需要审批";
            case MASK -> "内容需要脱敏处理";
            case RATE_LIMIT -> "请求被限流";
            default -> "策略拦截";
        };

        if (StrUtil.isNotBlank(policy.getDescription())) {
            return actionDesc + "：" + policy.getDescription();
        }
        return actionDesc + "：" + policy.getName();
    }
}
