package com.bank.ai.rag.vectorstore.adapters;

/**
 * Real deployments implement VectorStore against the org's chosen provider(s)
 * here. Kept as documented stubs rather than full SDK integrations because
 * the exact client library/version must match what's approved for the
 * project (see architecture plan section 51). Each class below is the
 * intended shape:
 *
 *   public class PineconeVectorStore implements VectorStore { ... }
 *   public class WeaviateVectorStore implements VectorStore { ... }
 *   public class MilvusVectorStore implements VectorStore { ... }
 *   public class ChromaVectorStore implements VectorStore { ... }
 *
 * To activate one, implement it against its client SDK, register it as a
 * @Component with a distinct @Qualifier, and change the @Primary selection
 * in VectorStoreConfig from "inMemory" to your provider's qualifier.
 */
public final class VectorStoreProviderNotes {
    private VectorStoreProviderNotes() {
    }
}
