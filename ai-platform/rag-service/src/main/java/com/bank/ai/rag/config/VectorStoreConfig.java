package com.bank.ai.rag.config;

import com.bank.ai.rag.vectorstore.InMemoryVectorStore;
import com.bank.ai.rag.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Single place that decides which VectorStore implementation is active.
 * Swap InMemoryVectorStore for a real provider adapter here when one is
 * implemented (see VectorStoreProviderNotes).
 */
@Configuration
public class VectorStoreConfig {

    @Bean
    @Primary
    public VectorStore activeVectorStore(InMemoryVectorStore inMemoryVectorStore) {
        return inMemoryVectorStore;
    }
}
