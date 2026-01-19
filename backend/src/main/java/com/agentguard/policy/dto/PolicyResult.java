package com.agentguard.policy.dto;

import com.agentguard.policy.enums.PolicyAction;
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

    /** 策略动作 */
    @Schema(description = "策略动作")
    private PolicyAction action;

    /** 是否需要审批 */
    @Schema(description = "是否需要审批", example = "false")
    private boolean requireApproval;

    /** 脱敏配置（内容保护策略使用） */
    @Schema(description = "脱敏配置（内容保护策略使用）")
    private MaskConfig maskConfig;

    /** 限流结果（频率限制策略使用） */
    @Schema(description = "限流结果（频率限制策略使用）")
    private RateLimitResult rateLimitResult;

    /** 是否需要脱敏处理 */
    @Schema(description = "是否需要脱敏处理", example = "false")
    private boolean requireMask;

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
                .requireMask(false)
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
                .requireMask(false)
                .build();
    }

    /**
     * 创建拦截的结果（带策略信息）
     *
     * @param policyId 策略ID
     * @param action 策略动作
     * @param reason 拦截原因
     * @return 被拦截的PolicyResult
     */
    public static PolicyResult block(String policyId, PolicyAction action, String reason) {
        return PolicyResult.builder()
                .blocked(true)
                .reason(reason)
                .policyId(policyId)
                .action(action)
                .requireApproval(action == PolicyAction.APPROVAL)
                .requireMask(false)
                .build();
    }

    /**
     * 创建需要审批的结果
     *
     * @param policyId 策略ID
     * @param reason 审批原因
     * @return 需要审批的PolicyResult
     */
    public static PolicyResult requireApproval(String policyId, String reason) {
        return PolicyResult.builder()
                .blocked(true)
                .reason(reason)
                .policyId(policyId)
                .action(PolicyAction.APPROVAL)
                .requireApproval(true)
                .requireMask(false)
                .build();
    }

    /**
     * 创建需要脱敏的结果
     *
     * @param policyId 策略ID
     * @param config   脱敏配置
     * @param reason   原因说明
     * @return 需要脱敏的PolicyResult
     */
    public static PolicyResult mask(String policyId, MaskConfig config, String reason) {
        return PolicyResult.builder()
                .blocked(false)
                .reason(reason)
                .policyId(policyId)
                .action(PolicyAction.ALLOW)
                .requireApproval(false)
                .maskConfig(config)
                .requireMask(true)
                .build();
    }

    /**
     * 创建限流结果
     *
     * @param policyId 策略ID
     * @param result   限流结果
     * @param reason   原因说明
     * @return 限流的PolicyResult
     */
    public static PolicyResult rateLimit(String policyId, RateLimitResult result, String reason) {
        return PolicyResult.builder()
                .blocked(!result.isAllowed())
                .reason(reason)
                .policyId(policyId)
                .action(result.isAllowed() ? PolicyAction.ALLOW : PolicyAction.DENY)
                .requireApproval(false)
                .rateLimitResult(result)
                .requireMask(false)
                .build();
    }
}
