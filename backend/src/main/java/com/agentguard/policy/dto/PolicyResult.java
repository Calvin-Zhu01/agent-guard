package com.agentguard.policy.dto;

import com.agentguard.policy.enums.PolicyAction;
import com.agentguard.policy.enums.PolicyType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 策略评估结果
 *
 * @author zhuhx
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "策略评估结果")
public class PolicyResult {

    /** 是否被拦截 */
    @Schema(description = "是否被拦截", example = "true")
    private boolean blocked;

    /** 拦截原因 */
    @Schema(description = "拦截原因", example = "等待审批：大额转账操作需要人工审批")
    private String reason;

    /** 匹配的策略ID */
    @Schema(description = "匹配的策略ID")
    private String policyId;

    /** 匹配的策略名称 */
    @Schema(description = "匹配的策略名称")
    private String policyName;

    /** 策略类型 */
    @Schema(description = "策略类型")
    private PolicyType policyType;

    /** 策略条件配置 */
    @Schema(description = "策略条件配置（JSON字符串）")
    private String policyConditions;

    /** 策略动作 */
    @Schema(description = "策略动作")
    private PolicyAction action;

    /** 是否需要审批 */
    @Schema(description = "是否需要审批", example = "false")
    private boolean requireApproval;

    /** 限流结果（频率限制策略使用） */
    @Schema(description = "限流结果（频率限制策略使用）")
    private RateLimitResult rateLimitResult;

    /**
     * 创建允许通过的结果
     *
     * @return 允许通过的PolicyResult
     */
    public static PolicyResult allow() {
        return PolicyResult.builder()
                .blocked(false)
                .reason(null)
                .action(PolicyAction.ALLOW)
                .requireApproval(false)
                .build();
    }

    /**
     * 创建拦截的结果
     *
     * @param reason 拦截原因
     * @return 被拦截的PolicyResult
     */
    public static PolicyResult block(String reason) {
        return PolicyResult.builder()
                .blocked(true)
                .reason(reason)
                .action(PolicyAction.DENY)
                .requireApproval(false)
                .build();
    }

    /**
     * 创建拦截的结果（带策略信息）
     *
     * @param policyId 策略ID
     * @param policyName 策略名称
     * @param policyType 策略类型
     * @param policyConditions 策略条件配置
     * @param action 策略动作
     * @param reason 拦截原因
     * @return 被拦截的PolicyResult
     */
    public static PolicyResult block(String policyId, String policyName, PolicyType policyType,
                                     String policyConditions, PolicyAction action, String reason) {
        return PolicyResult.builder()
                .blocked(true)
                .reason(reason)
                .policyId(policyId)
                .policyName(policyName)
                .policyType(policyType)
                .policyConditions(policyConditions)
                .action(action)
                .requireApproval(action == PolicyAction.APPROVAL)
                .build();
    }

    /**
     * 创建需要审批的结果
     *
     * @param policyId 策略ID
     * @param policyName 策略名称
     * @param policyType 策略类型
     * @param policyConditions 策略条件配置
     * @param reason 审批原因
     * @return 需要审批的PolicyResult
     */
    public static PolicyResult requireApproval(String policyId, String policyName, PolicyType policyType,
                                               String policyConditions, String reason) {
        return PolicyResult.builder()
                .blocked(true)
                .reason(reason)
                .policyId(policyId)
                .policyName(policyName)
                .policyType(policyType)
                .policyConditions(policyConditions)
                .action(PolicyAction.APPROVAL)
                .requireApproval(true)
                .build();
    }

    /**
     * 创建限流结果
     *
     * @param policyId 策略ID
     * @param policyName 策略名称
     * @param policyType 策略类型
     * @param policyConditions 策略条件配置
     * @param result   限流结果
     * @param reason   原因说明
     * @return 限流的PolicyResult
     */
    public static PolicyResult rateLimit(String policyId, String policyName, PolicyType policyType,
                                         String policyConditions, RateLimitResult result, String reason) {
        return PolicyResult.builder()
                .blocked(!result.isAllowed())
                .reason(reason)
                .policyId(policyId)
                .policyName(policyName)
                .policyType(policyType)
                .policyConditions(policyConditions)
                .action(result.isAllowed() ? PolicyAction.ALLOW : PolicyAction.DENY)
                .requireApproval(false)
                .rateLimitResult(result)
                .build();
    }
}
