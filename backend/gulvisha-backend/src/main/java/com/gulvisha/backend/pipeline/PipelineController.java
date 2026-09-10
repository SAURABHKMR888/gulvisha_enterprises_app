package com.gulvisha.backend.pipeline;

import com.gulvisha.backend.security.UserContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pipeline-stages")
public class PipelineController {
    private final PipelineStageRepository repository;

    public PipelineController(PipelineStageRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<PipelineStage> list() {
        return repository.findAllByOrganizationIdOrderBySortOrder(UserContext.getOrganizationId());
    }

    @PostMapping
    public ResponseEntity<PipelineStage> create(@Valid @RequestBody PipelineStageRequest request) {
        PipelineStage stage = new PipelineStage(
            UserContext.getOrganizationId(), request.name(), request.sortOrder(), request.defaultStage()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(stage));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PipelineStage> update(@PathVariable UUID id, @Valid @RequestBody PipelineStageRequest request) {
        PipelineStage stage = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Stage not found: " + id));
        if (!stage.getOrganizationId().equals(UserContext.getOrganizationId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        stage.setName(request.name());
        stage.setSortOrder(request.sortOrder());
        stage.setDefaultStage(request.defaultStage());
        return ResponseEntity.ok(repository.save(stage));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        PipelineStage stage = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Stage not found: " + id));
        if (!stage.getOrganizationId().equals(UserContext.getOrganizationId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    public record PipelineStageRequest(
        String name,
        int sortOrder,
        boolean defaultStage
    ) {}
}