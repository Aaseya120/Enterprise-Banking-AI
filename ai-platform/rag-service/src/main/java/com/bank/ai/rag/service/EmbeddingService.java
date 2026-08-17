package com.bank.ai.rag.service;

import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Turns text into a vector. This demo implementation uses a deterministic
 * bag-of-words hashing embedding (dimension 256) so ingestion/search work
 * end-to-end with zero external dependencies. Swap the body of embed() for
 * a call to your chosen embedding model/provider in production -- the
 * VectorStore contract and everything downstream is unaffected either way.
 */
@Service
public class EmbeddingService {

    private static final int DIMENSIONS = 256;

    public float[] embed(String text) {
        float[] vector = new float[DIMENSIONS];
        if (text == null || text.isBlank()) {
            return vector;
        }
        for (String token : text.toLowerCase(Locale.ROOT).split("\\W+")) {
            if (token.isBlank()) {
                continue;
            }
            int bucket = Math.floorMod(token.hashCode(), DIMENSIONS);
            vector[bucket] += 1.0f;
        }
        normalize(vector);
        return vector;
    }

    private void normalize(float[] vector) {
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm == 0) {
            return;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) (vector[i] / norm);
        }
    }
}
