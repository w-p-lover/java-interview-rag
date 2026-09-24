package com.javainterview.rag.modelprovider;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MySqlModelProviderStoreTest {

    @Test
    void usesOptimisticVersionsForProviderAndDefaults() {
        ModelProviderMapper mapper = mock(ModelProviderMapper.class);
        TransactionTemplate transactions = mock(TransactionTemplate.class);
        MySqlModelProviderStore store = new MySqlModelProviderStore(mapper, transactions);
        ModelProvider provider = provider();
        ModelProviderDefaults defaults = new ModelProviderDefaults("primary", "primary", 4,
                LocalDateTime.now());
        when(mapper.update(any(), org.mockito.ArgumentMatchers.eq(3L))).thenReturn(1);
        when(mapper.updateDefaults(any(), org.mockito.ArgumentMatchers.eq(4L))).thenReturn(0);

        assertTrue(store.update(provider, 3));
        assertFalse(store.updateDefaults(defaults, 4));
        verify(mapper).update(any(), org.mockito.ArgumentMatchers.eq(3L));
        verify(mapper).updateDefaults(any(), org.mockito.ArgumentMatchers.eq(4L));
    }

    private ModelProvider provider() {
        LocalDateTime now = LocalDateTime.now();
        return new ModelProvider("primary", "Primary", "https://example.com",
                "nonce", "ciphertext", "chat", "embedding", 1024,
                0.2, true, 3, now, now);
    }
}
