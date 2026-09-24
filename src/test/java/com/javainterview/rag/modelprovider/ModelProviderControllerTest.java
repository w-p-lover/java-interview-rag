package com.javainterview.rag.modelprovider;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModelProviderControllerTest {

    @Test
    void delegatesOverviewAndCapabilityTest() {
        ModelProviderService service = mock(ModelProviderService.class);
        ModelProviderOverview overview = new ModelProviderOverview(Instant.EPOCH, List.of());
        ModelProviderTestResult testResult = new ModelProviderTestResult(
                ModelCapability.CHAT, true, "OK", "连接成功。", "qwen-test",
                null, 12, Instant.EPOCH);
        when(service.overview()).thenReturn(overview);
        when(service.test(ModelCapability.CHAT)).thenReturn(testResult);
        ModelProviderController controller = new ModelProviderController(service);

        assertEquals(overview, controller.overview());
        assertEquals(testResult, controller.test(ModelCapability.CHAT));
        verify(service).overview();
        verify(service).test(ModelCapability.CHAT);
    }
}
