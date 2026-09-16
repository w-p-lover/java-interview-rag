package com.javainterview.rag.knowledge.token;

import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;

import java.util.Objects;

/**
 * 为文档切分和模型上下文提供一致的确定性 Token 估算。
 * JTokkit 使用 OpenAI 编码，只用于稳定预算，不代表 DashScope/Qwen 的精确计费 Token。
 */
public final class TokenBudget {

    private final TokenCountEstimator estimator;

    public TokenBudget() {
        this(new JTokkitTokenCountEstimator());
    }

    public TokenBudget(TokenCountEstimator estimator) {
        this.estimator = Objects.requireNonNull(estimator, "Token 估算器不能为空");
    }

    public int count(String text) {
        return estimator.estimate(Objects.requireNonNull(text, "待估算文本不能为空"));
    }

    public boolean fits(String text, int maxTokens) {
        validateLimit(maxTokens);
        return count(text) <= maxTokens;
    }

    public String truncate(String text, int maxTokens) {
        Objects.requireNonNull(text, "待截断文本不能为空");
        validateLimit(maxTokens);
        if (text.isEmpty() || fits(text, maxTokens)) {
            return text;
        }

        int codePointCount = text.codePointCount(0, text.length());
        int low = 0;
        int high = codePointCount;
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            int end = text.offsetByCodePoints(0, middle);
            if (count(text.substring(0, end)) <= maxTokens) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        if (low == 0) {
            return text.substring(0, text.offsetByCodePoints(0, 1));
        }
        return text.substring(0, text.offsetByCodePoints(0, low));
    }

    private void validateLimit(int maxTokens) {
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("Token 上限必须大于 0");
        }
    }
}
