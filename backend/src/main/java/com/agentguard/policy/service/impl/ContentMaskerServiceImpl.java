package com.agentguard.policy.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.agentguard.policy.dto.MaskConfig;
import com.agentguard.policy.dto.MaskRule;
import com.agentguard.policy.service.ContentMaskerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 内容脱敏服务实现类
 *
 * @author zhuhx
 */
@Slf4j
@Service
public class ContentMaskerServiceImpl implements ContentMaskerService {

    /** 手机号正则 */
    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");

    /** 身份证号正则 */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("\\d{17}[\\dXx]");

    /** 银行卡号正则（16-19位数字） */
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("\\d{16,19}");

    /** 邮箱正则 */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    @Override
    public Object maskContent(Object content, MaskConfig config) {
        if (ObjectUtil.isNull(content) || ObjectUtil.isNull(config)) {
            return content;
        }

        // 处理字符串类型
        if (content instanceof String) {
            return maskStringContent((String) content, config);
        }

        // 处理 Map 类型
        if (content instanceof Map) {
            return maskMapContent((Map<String, Object>) content, config);
        }

        // 处理 JSON 字符串
        String contentStr = content.toString();
        if (JSONUtil.isTypeJSON(contentStr)) {
            return maskStringContent(contentStr, config);
        }

        return content;
    }

    @Override
    public String maskField(String fieldType, String value) {
        if (StrUtil.isBlank(value) || StrUtil.isBlank(fieldType)) {
            return value;
        }

        return switch (fieldType.toLowerCase()) {
            case "phone" -> maskPhone(value);
            case "idcard" -> maskIdCard(value);
            case "bankcard" -> maskBankCard(value);
            case "email" -> maskEmail(value);
            case "name" -> maskName(value);
            case "address" -> maskAddress(value);
            default -> maskWithDefaultRule(value);
        };
    }

    @Override
    public String maskKeywords(String text, List<String> keywords) {
        if (StrUtil.isBlank(text) || CollUtil.isEmpty(keywords)) {
            return text;
        }

        String result = text;
        for (String keyword : keywords) {
            if (StrUtil.isNotBlank(keyword)) {
                result = result.replace(keyword, "***");
            }
        }
        return result;
    }


    /**
     * 对字符串内容进行脱敏
     */
    private String maskStringContent(String content, MaskConfig config) {
        String result = content;

        // 处理敏感字段类型
        if (CollUtil.isNotEmpty(config.getSensitiveFields())) {
            for (String fieldType : config.getSensitiveFields()) {
                result = maskFieldInText(result, fieldType);
            }
        }

        // 处理敏感关键词
        if (CollUtil.isNotEmpty(config.getSensitiveKeywords())) {
            result = maskKeywords(result, config.getSensitiveKeywords());
        }

        return result;
    }

    /**
     * 对 Map 内容进行脱敏
     */
    private Map<String, Object> maskMapContent(Map<String, Object> content, MaskConfig config) {
        Map<String, Object> result = new HashMap<>(content);

        // 处理敏感字段类型
        if (CollUtil.isNotEmpty(config.getSensitiveFields())) {
            for (String fieldType : config.getSensitiveFields()) {
                maskFieldInMap(result, fieldType);
            }
        }

        // 处理自定义脱敏规则
        if (ObjectUtil.isNotNull(config.getMaskRules())) {
            for (Map.Entry<String, MaskRule> entry : config.getMaskRules().entrySet()) {
                String fieldName = entry.getKey();
                MaskRule rule = entry.getValue();
                if (result.containsKey(fieldName)) {
                    Object value = result.get(fieldName);
                    if (value instanceof String) {
                        result.put(fieldName, applyMaskRule((String) value, rule));
                    }
                }
            }
        }

        // 处理敏感关键词（对所有字符串值）
        if (CollUtil.isNotEmpty(config.getSensitiveKeywords())) {
            for (Map.Entry<String, Object> entry : result.entrySet()) {
                if (entry.getValue() instanceof String) {
                    entry.setValue(maskKeywords((String) entry.getValue(), config.getSensitiveKeywords()));
                }
            }
        }

        return result;
    }

    /**
     * 在文本中脱敏指定类型的字段
     */
    private String maskFieldInText(String text, String fieldType) {
        if (StrUtil.isBlank(text)) {
            return text;
        }

        return switch (fieldType.toLowerCase()) {
            case "phone" -> maskPhoneInText(text);
            case "idcard" -> maskIdCardInText(text);
            case "bankcard" -> maskBankCardInText(text);
            case "email" -> maskEmailInText(text);
            default -> text;
        };
    }

    /**
     * 在 Map 中脱敏指定类型的字段
     */
    private void maskFieldInMap(Map<String, Object> map, String fieldType) {
        // 根据字段类型查找可能的字段名
        List<String> possibleFieldNames = getPossibleFieldNames(fieldType);

        for (String fieldName : possibleFieldNames) {
            if (map.containsKey(fieldName)) {
                Object value = map.get(fieldName);
                if (value instanceof String) {
                    map.put(fieldName, maskField(fieldType, (String) value));
                }
            }
        }

        // 递归处理嵌套 Map
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map) {
                maskFieldInMap((Map<String, Object>) entry.getValue(), fieldType);
            }
        }
    }

    /**
     * 获取字段类型可能对应的字段名
     */
    private List<String> getPossibleFieldNames(String fieldType) {
        return switch (fieldType.toLowerCase()) {
            case "phone" -> List.of("phone", "mobile", "tel", "telephone", "phoneNumber", "mobilePhone");
            case "idcard" -> List.of("idCard", "idNumber", "identityCard", "idNo", "certificateNo");
            case "bankcard" -> List.of("bankCard", "cardNo", "bankCardNo", "accountNo", "cardNumber");
            case "email" -> List.of("email", "mail", "emailAddress");
            case "name" -> List.of("name", "realName", "userName", "fullName", "customerName");
            case "address" -> List.of("address", "addr", "homeAddress", "workAddress", "detailAddress");
            default -> List.of();
        };
    }


    // ==================== 预设脱敏规则实现 ====================

    /**
     * 手机号脱敏：138****1234
     */
    private String maskPhone(String phone) {
        if (StrUtil.isBlank(phone) || phone.length() < 7) {
            return phone;
        }
        // 保留前3后4
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 身份证号脱敏：110***********1234
     */
    private String maskIdCard(String idCard) {
        if (StrUtil.isBlank(idCard) || idCard.length() < 7) {
            return idCard;
        }
        // 保留前3后4
        int maskLength = idCard.length() - 7;
        return idCard.substring(0, 3) + "*".repeat(maskLength) + idCard.substring(idCard.length() - 4);
    }

    /**
     * 银行卡号脱敏：6222****1234
     */
    private String maskBankCard(String bankCard) {
        if (StrUtil.isBlank(bankCard) || bankCard.length() < 8) {
            return bankCard;
        }
        // 保留前4后4
        return bankCard.substring(0, 4) + "****" + bankCard.substring(bankCard.length() - 4);
    }

    /**
     * 邮箱脱敏：a**@example.com
     */
    private String maskEmail(String email) {
        if (StrUtil.isBlank(email) || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf("@");
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (localPart.length() <= 1) {
            return localPart + "**" + domain;
        }
        // 保留首字母
        return localPart.charAt(0) + "**" + domain;
    }

    /**
     * 姓名脱敏：张**
     */
    private String maskName(String name) {
        if (StrUtil.isBlank(name)) {
            return name;
        }
        if (name.length() == 1) {
            return name;
        }
        // 保留姓
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    /**
     * 地址脱敏：北京市朝阳区******
     */
    private String maskAddress(String address) {
        if (StrUtil.isBlank(address) || address.length() <= 6) {
            return address;
        }
        // 保留前6字符
        return address.substring(0, 6) + "*".repeat(Math.min(6, address.length() - 6));
    }

    /**
     * 默认脱敏规则：保留前后各2字符
     */
    private String maskWithDefaultRule(String value) {
        if (StrUtil.isBlank(value) || value.length() <= 4) {
            return value;
        }
        int maskLength = value.length() - 4;
        return value.substring(0, 2) + "*".repeat(maskLength) + value.substring(value.length() - 2);
    }

    // ==================== 文本中脱敏实现 ====================

    /**
     * 在文本中脱敏手机号
     */
    private String maskPhoneInText(String text) {
        return PHONE_PATTERN.matcher(text).replaceAll(match -> maskPhone(match.group()));
    }

    /**
     * 在文本中脱敏身份证号
     */
    private String maskIdCardInText(String text) {
        return ID_CARD_PATTERN.matcher(text).replaceAll(match -> maskIdCard(match.group()));
    }

    /**
     * 在文本中脱敏银行卡号
     */
    private String maskBankCardInText(String text) {
        return BANK_CARD_PATTERN.matcher(text).replaceAll(match -> maskBankCard(match.group()));
    }

    /**
     * 在文本中脱敏邮箱
     */
    private String maskEmailInText(String text) {
        return EMAIL_PATTERN.matcher(text).replaceAll(match -> maskEmail(match.group()));
    }

    // ==================== 自定义规则实现 ====================

    /**
     * 应用自定义脱敏规则
     */
    private String applyMaskRule(String value, MaskRule rule) {
        if (StrUtil.isBlank(value) || ObjectUtil.isNull(rule)) {
            return value;
        }

        int start = rule.getStart();
        int end = rule.getEnd();
        String maskChar = StrUtil.isBlank(rule.getMaskChar()) ? "*" : rule.getMaskChar();

        if (value.length() <= start + end) {
            return value;
        }

        int maskLength = value.length() - start - end;
        StringBuilder masked = new StringBuilder();

        if (start > 0) {
            masked.append(value, 0, start);
        }

        masked.append(maskChar.repeat(maskLength));

        if (end > 0) {
            masked.append(value.substring(value.length() - end));
        }

        return masked.toString();
    }
}
