package com.agentguard.proxy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 资源 DTO
 * 
 * Tool Schema 请求中的目标资源定义
 *
 * @author zhuhx
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "目标资源")
public class ResourceDTO {

    /** 资源类型 */
    @Schema(description = "资源类型", example = "bank_account")
    private String type;

    /** 资源标识 */
    @Schema(description = "资源标识", example = "acc_12345")
    private String id;
}
