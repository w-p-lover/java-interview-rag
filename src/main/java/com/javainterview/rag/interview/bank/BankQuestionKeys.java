package com.javainterview.rag.interview.bank;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;

public final class BankQuestionKeys {
    private BankQuestionKeys() { }

    public static String normalize(String question) {
        return Normalizer.normalize(question, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT).replaceAll("[\\p{P}\\p{Z}\\s]", "");
    }

    public static String contentKey(BankQuestion question) {
        String contract = String.join("\u0000", question.source(), question.documentVersion(),
                question.skill().name(), question.difficulty().name(), normalize(question.question()));
        return sha256(contract);
    }

    public static String activeTaskKey(String source, String documentVersion) {
        return sha256(String.join("\u0000", source, documentVersion));
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}
