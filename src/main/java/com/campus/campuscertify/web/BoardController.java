package com.campus.campuscertify.web;

import com.campus.campuscertify.domain.Activity;
import com.campus.campuscertify.domain.EvaluationResponse;
import com.campus.campuscertify.service.BoardService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BoardController {

    private final BoardService service;

    public BoardController(BoardService service) {
        this.service = service;
    }

    @GetMapping("/activities")
    public List<Activity> activities() {
        return service.activities();
    }

    @GetMapping("/participants")
    public List<ParticipantDto> participants() {
        return currentParticipants();
    }

    @PostMapping("/participants")
    public List<ParticipantDto> add(@RequestBody ParticipantDto request) {
        service.addOrUpdate(request.toDomain());
        return currentParticipants();
    }

    /** Upsert keyed on the path id: LinkedHashMap.put keeps the row in its original display position. */
    @PutMapping("/participants/{id}")
    public List<ParticipantDto> update(@PathVariable String id, @RequestBody ParticipantDto request) {
        service.addOrUpdate(request.toDomain(id));
        return currentParticipants();
    }

    @DeleteMapping("/participants/{id}")
    public ResponseEntity<List<ParticipantDto>> delete(@PathVariable String id) {
        return service.deleteParticipant(id)
                ? ResponseEntity.ok(currentParticipants())
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/reset")
    public List<ParticipantDto> reset() {
        service.reset();
        return currentParticipants();
    }

    /** Always 200: validation errors are domain output, not HTTP faults, so the UI has one render path. */
    @PostMapping("/evaluate")
    public EvaluationResponse evaluate() {
        return service.evaluate();
    }

    private List<ParticipantDto> currentParticipants() {
        return service.participants().stream().map(ParticipantDto::from).toList();
    }
}
