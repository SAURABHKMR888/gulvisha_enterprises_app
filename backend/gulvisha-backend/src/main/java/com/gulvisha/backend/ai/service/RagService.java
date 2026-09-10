package com.gulvisha.backend.ai.service;

import com.gulvisha.backend.ai.dto.AiKnowledgeDocumentDto;
import com.gulvisha.backend.ai.dto.AiKnowledgeIngestRequest;
import com.gulvisha.backend.ai.entity.AiConfiguration;
import com.gulvisha.backend.ai.entity.AiKnowledgeChunk;
import com.gulvisha.backend.ai.entity.AiKnowledgeDocument;
import com.gulvisha.backend.ai.provider.AiEmbeddingRequest;
import com.gulvisha.backend.ai.provider.AiEmbeddingResponse;
import com.gulvisha.backend.ai.provider.AiProvider;
import com.gulvisha.backend.ai.provider.AiProviderFactory;
import com.gulvisha.backend.ai.repository.AiConfigurationRepository;
import com.gulvisha.backend.ai.repository.AiKnowledgeChunkRepository;
import com.gulvisha.backend.ai.repository.AiKnowledgeDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Retrieval-Augmented Generation (RAG) pipeline:
 * ingest documents → chunk → embed per chunk (via the org's configured provider)
 * → at query time embed the question and return the most similar chunks,
 * which the chat flow injects as grounding context.
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    /** Target chunk size in characters (~200 tokens). */
    private static final int CHUNK_SIZE = 800;
    /** Overlap between consecutive chunks so sentences spanning a boundary stay retrievable. */
    private static final int CHUNK_OVERLAP = 120;
    /** Minimum cosine similarity for a chunk to be considered relevant. */
    private static final double MIN_SIMILARITY = 0.45;

    private final AiKnowledgeDocumentRepository documentRepository;
    private final AiKnowledgeChunkRepository chunkRepository;
    private final AiConfigurationRepository configRepository;
    private final AiProviderFactory providerFactory;

    public RagService(AiKnowledgeDocumentRepository documentRepository,
                      AiKnowledgeChunkRepository chunkRepository,
                      AiConfigurationRepository configRepository,
                      AiProviderFactory providerFactory) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.configRepository = configRepository;
        this.providerFactory = providerFactory;
    }

    /** A retrieved chunk with its similarity score and source document title. */
    public record RagHit(String documentTitle, String content, double score) {}

    public List<AiKnowledgeDocumentDto> listDocuments(UUID orgId) {
        return documentRepository.findAllByOrganizationIdOrderByUpdatedAtDesc(orgId).stream()
                .map(AiKnowledgeDocumentDto::fromEntity).toList();
    }

    /**
     * Ingests a document: chunks the content, embeds every chunk with the
     * organization's configured provider, and stores the vectors.
     */
    @Transactional
    public AiKnowledgeDocumentDto ingest(UUID orgId, AiKnowledgeIngestRequest request) {
        AiConfiguration config = requireProviderConfig(orgId);
        AiProvider provider = providerFactory.getProvider(config.getProvider());

        AiKnowledgeDocument doc = new AiKnowledgeDocument(orgId, request.title().trim(), request.content());
        doc.setStatus(AiKnowledgeDocument.STATUS_EMBEDDING);
        doc = documentRepository.save(doc);

        try {
            List<String> chunks = chunkText(request.content());
            if (chunks.isEmpty()) {
                throw new IllegalArgumentException("Document content is empty after chunking");
            }
            AiEmbeddingResponse embeddings = provider.embed(new AiEmbeddingRequest(
                    null, chunks, config.getApiKey(), config.getBaseUrl()));

            List<AiKnowledgeChunk> entities = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                AiKnowledgeChunk chunk = new AiKnowledgeChunk(orgId, doc.getId(), i, chunks.get(i));
                chunk.setEmbedding(embeddings.embeddings().get(i));
                chunk.setEmbeddingModel(embeddings.model());
                entities.add(chunk);
            }
            chunkRepository.saveAll(entities);

            doc.setStatus(AiKnowledgeDocument.STATUS_READY);
            doc.setChunkCount(entities.size());
            doc.setError(null);
            log.info("Ingested knowledge doc '{}' for org {}: {} chunks, model {}",
                    doc.getTitle(), orgId, entities.size(), embeddings.model());
            return AiKnowledgeDocumentDto.fromEntity(documentRepository.save(doc));
        } catch (Exception e) {
            doc.setStatus(AiKnowledgeDocument.STATUS_FAILED);
            doc.setError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            doc.setChunkCount(0);
            documentRepository.save(doc);
            chunkRepository.deleteByDocumentId(doc.getId());
            log.error("Ingest failed for doc '{}': {}", doc.getTitle(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Knowledge ingestion failed: " + doc.getError());
        }
    }

    /** Re-chunks and re-embeds an existing document (e.g. after provider/model change). */
    @Transactional
    public AiKnowledgeDocumentDto reindex(UUID orgId, UUID documentId) {
        AiConfiguration config = requireProviderConfig(orgId);
        AiProvider provider = providerFactory.getProvider(config.getProvider());
        AiKnowledgeDocument doc = documentRepository.findByIdAndOrganizationId(documentId, orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Knowledge document not found"));

        doc.setStatus(AiKnowledgeDocument.STATUS_EMBEDDING);
        documentRepository.save(doc);
        chunkRepository.deleteByDocumentId(doc.getId());

        try {
            List<String> chunks = chunkText(doc.getContent());
            if (chunks.isEmpty()) {
                throw new IllegalArgumentException("Document content is empty after chunking");
            }
            AiEmbeddingResponse embeddings = provider.embed(new AiEmbeddingRequest(
                    null, chunks, config.getApiKey(), config.getBaseUrl()));
            List<AiKnowledgeChunk> entities = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                AiKnowledgeChunk chunk = new AiKnowledgeChunk(orgId, doc.getId(), i, chunks.get(i));
                chunk.setEmbedding(embeddings.embeddings().get(i));
                chunk.setEmbeddingModel(embeddings.model());
                entities.add(chunk);
            }
            chunkRepository.saveAll(entities);
            doc.setStatus(AiKnowledgeDocument.STATUS_READY);
            doc.setChunkCount(entities.size());
            doc.setError(null);
            return AiKnowledgeDocumentDto.fromEntity(documentRepository.save(doc));
        } catch (Exception e) {
            doc.setStatus(AiKnowledgeDocument.STATUS_FAILED);
            doc.setError(e.getMessage());
            doc.setChunkCount(0);
            documentRepository.save(doc);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reindex failed: " + e.getMessage());
        }
    }

    @Transactional
    public void delete(UUID orgId, UUID documentId) {
        AiKnowledgeDocument doc = documentRepository.findByIdAndOrganizationId(documentId, orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Knowledge document not found"));
        chunkRepository.deleteByDocumentId(doc.getId());
        documentRepository.delete(doc);
    }

    /**
     * Retrieves the most relevant chunks for a question. Returns an empty list
     * when the org has no READY knowledge or AI is not enabled, so chat still works.
     */
    public List<RagHit> retrieve(UUID orgId, String query, int topK) {
        List<AiKnowledgeChunk> chunks = chunkRepository.findAllByOrganizationId(orgId);
        if (chunks.isEmpty() || query == null || query.isBlank()) {
            return List.of();
        }
        AiConfiguration config = configRepository.findByOrganizationId(orgId).orElse(null);
        if (config == null || !config.isEnabled()) {
            return List.of();
        }
        AiProvider provider;
        try {
            provider = providerFactory.getProvider(config.getProvider());
        } catch (IllegalArgumentException e) {
            return List.of();
        }

        float[] q;
        try {
            AiEmbeddingResponse queryEmbedding = provider.embed(new AiEmbeddingRequest(
                    null, List.of(query), config.getApiKey(), config.getBaseUrl()));
            q = queryEmbedding.embeddings().get(0);
        } catch (Exception e) {
            log.warn("RAG query embedding failed, answering without context: {}", e.getMessage());
            return List.of();
        }

        List<RagHit> hits = new ArrayList<>();
        for (AiKnowledgeChunk chunk : chunks) {
            float[] v = chunk.getEmbedding();
            if (v == null || v.length == 0 || v.length != q.length) {
                continue; // dimension mismatch (model changed) — chunk needs reindex
            }
            double score = cosineSimilarity(q, v);
            if (score >= MIN_SIMILARITY) {
                String title = documentRepository.findById(chunk.getDocumentId())
                        .map(AiKnowledgeDocument::getTitle).orElse("Unknown document");
                hits.add(new RagHit(title, chunk.getContent(), score));
            }
        }
        return hits.stream()
                .sorted(Comparator.comparingDouble(RagHit::score).reversed())
                .limit(topK)
                .toList();
    }

    private AiConfiguration requireProviderConfig(UUID orgId) {
        AiConfiguration config = configRepository.findByOrganizationId(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "AI is not configured. Set up a provider in AI → Configuration first."));
        if (!config.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "AI is disabled. Enable it in AI → Configuration before ingesting knowledge.");
        }
        if ("gemini".equals(config.getProvider()) || "openai".equals(config.getProvider())) {
            if (config.getApiKey() == null || config.getApiKey().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No API key set for provider '" + config.getProvider() + "'. Add it in AI → Configuration.");
            }
        }
        return config;
    }

    /**
     * Splits text into chunks of up to {@link #CHUNK_SIZE} characters on paragraph
     * boundaries, with {@link #CHUNK_OVERLAP} characters of tail overlap.
     */
    static List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

        String normalized = text.replace("\r\n", "\n").trim();
        String[] paragraphs = normalized.split("\n\\s*\n");

        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            String p = paragraph.trim();
            if (p.isEmpty()) continue;

            // A single paragraph larger than the chunk size gets hard-split.
            if (p.length() > CHUNK_SIZE) {
                if (current.length() > 0) {
                    chunks.add(current.toString().trim());
                    current = new StringBuilder();
                }
                for (int i = 0; i < p.length(); i += CHUNK_SIZE - CHUNK_OVERLAP) {
                    int end = Math.min(i + CHUNK_SIZE, p.length());
                    chunks.add(p.substring(i, end).trim());
                    if (end >= p.length()) break;
                }
                continue;
            }

            if (current.length() > 0 && current.length() + p.length() + 2 > CHUNK_SIZE) {
                chunks.add(current.toString().trim());
                // Keep the tail as overlap for continuity.
                String tail = current.toString();
                String overlap = tail.length() > CHUNK_OVERLAP
                        ? tail.substring(tail.length() - CHUNK_OVERLAP) : tail;
                current = new StringBuilder(overlap.trim()).append("\n\n");
            }
            if (current.length() > 0) current.append("\n\n");
            current.append(p);
        }
        if (current.length() > 0) {
            String last = current.toString().trim();
            if (!last.isEmpty()) chunks.add(last);
        }
        return chunks;
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
