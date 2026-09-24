package com.javainterview.rag.modelprovider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProviderApiKeyCipherTest {

    @Test
    void encryptsWithUniqueNonceAndDecrypts() {
        ProviderApiKeyCipher cipher = new ProviderApiKeyCipher("test-provider-master-key");

        ProviderApiKeyCipher.EncryptedValue first = cipher.encrypt("sk-sensitive");
        ProviderApiKeyCipher.EncryptedValue second = cipher.encrypt("sk-sensitive");

        assertNotEquals(first.nonce(), second.nonce());
        assertNotEquals(first.ciphertext(), second.ciphertext());
        assertEquals("sk-sensitive", cipher.decrypt(first.nonce(), first.ciphertext()));
        assertEquals("sk-sensitive", cipher.decrypt(second.nonce(), second.ciphertext()));
    }

    @Test
    void rejectsCiphertextFromAnotherMasterKey() {
        ProviderApiKeyCipher first = new ProviderApiKeyCipher("first-master-key");
        ProviderApiKeyCipher second = new ProviderApiKeyCipher("second-master-key");
        ProviderApiKeyCipher.EncryptedValue encrypted = first.encrypt("sk-sensitive");

        assertThrows(IllegalStateException.class,
                () -> second.decrypt(encrypted.nonce(), encrypted.ciphertext()));
    }
}
