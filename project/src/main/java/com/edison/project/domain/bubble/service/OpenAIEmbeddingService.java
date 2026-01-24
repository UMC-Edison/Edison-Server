package com.edison.project.domain.bubble.service;

import com.pgvector.PGvector;
import com.google.gson.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAIEmbeddingService implements EmbeddingService {

    @Value("${openai.secret_key}")
    private String openaiApiKey;

    // Best Practice: Reuse the client
    private final OkHttpClient httpClient = new OkHttpClient();
    private final Gson gson = new Gson();

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/embeddings";
    private static final String EMBEDDING_MODEL = "text-embedding-3-small";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    public PGvector embed(String text) {
        try {
            if (text == null || text.trim().isEmpty()) {
                log.warn("Empty text provided for embedding");
                return createZeroVector();
            }

            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", EMBEDDING_MODEL);
            requestBody.addProperty("input", text);

            // Create request body
            RequestBody body = RequestBody.create(gson.toJson(requestBody), JSON);

            Request request = new Request.Builder()
                    .url(OPENAI_API_URL)
                    .header("Authorization", "Bearer " + openaiApiKey)
                    .post(body)
                    .build();

            // Execute call
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("OpenAI API error: {} - {}", response.code(), response.message());
                    return createZeroVector();
                }

                if (response.body() == null) return createZeroVector();

                String responseStr = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseStr, JsonObject.class);

                JsonArray dataArray = jsonResponse.getAsJsonArray("data");
                if (dataArray == null || dataArray.isEmpty()) {
                    return createZeroVector();
                }

                JsonObject firstData = dataArray.get(0).getAsJsonObject();
                JsonArray embedding = firstData.getAsJsonArray("embedding");

                float[] vector = new float[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) {
                    vector[i] = embedding.get(i).getAsFloat();
                }

                return new PGvector(vector);
            }

        } catch (IOException e) {
            log.error("Failed to get embedding from OpenAI", e);
            return createZeroVector();
        }
    } // <--- This closing brace was missing in your code!

    @Override
    public PGvector[] embedBatch(String[] texts) {
        PGvector[] results = new PGvector[texts.length];
        for (int i = 0; i < texts.length; i++) {
            results[i] = embed(texts[i]);
        }
        return results;
    }

    private PGvector createZeroVector() {
        // OpenAI text-embedding-3-small is 1536 dimensions
        float[] zeroVector = new float[1536];
        Arrays.fill(zeroVector, 0f);
        return new PGvector(zeroVector);
    }
}