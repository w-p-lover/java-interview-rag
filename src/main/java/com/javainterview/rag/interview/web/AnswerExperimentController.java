package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.evaluation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @Profile("storage") @RequestMapping("/api/interviews/evaluation-experiments/answers")
@Tag(name="回答实验", description="固定回答、引用与知识依据评估")
public class AnswerExperimentController {
 private final AnswerExperimentService service;
 public AnswerExperimentController(AnswerExperimentService service){this.service=service;}
 @PostMapping @Operation(summary="启动或复用回答实验") public ResponseEntity<AnswerExperimentService.RunView> start(@RequestBody StartRequest request){if(request==null)throw new IllegalArgumentException("回答实验请求不能为空");return ResponseEntity.accepted().body(service.start(request.name()));}
 @GetMapping public List<AnswerExperimentService.RunView> recent(@RequestParam(defaultValue="20") int limit){return service.recent(limit);}
 @GetMapping("/{runId}") public AnswerExperimentService.RunView find(@PathVariable String runId){return service.find(runId);}
 @PostMapping("/{runId}/resume") public ResponseEntity<AnswerExperimentService.RunView> resume(@PathVariable String runId){return ResponseEntity.accepted().body(service.resume(runId));}
 @GetMapping("/comparison") public AnswerExperimentService.Comparison compare(@RequestParam String baselineRunId,@RequestParam String candidateRunId){return service.compare(baselineRunId,candidateRunId);}
 @PatchMapping("/{candidateRunId}/decision") public AnswerExperimentService.RunView decide(@PathVariable String candidateRunId,@RequestBody DecisionRequest request){if(request==null)throw new IllegalArgumentException("回答实验决策不能为空");return service.decide(candidateRunId,request.baselineRunId(),request.decision(),request.note());}
 @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) public Map<String,String> invalid(IllegalArgumentException e){return Map.of("message",e.getMessage());}
 @ExceptionHandler(NoSuchElementException.class) @ResponseStatus(HttpStatus.NOT_FOUND) public Map<String,String> missing(NoSuchElementException e){return Map.of("message",e.getMessage());}
 @ExceptionHandler(IllegalStateException.class) @ResponseStatus(HttpStatus.CONFLICT) public Map<String,String> conflict(IllegalStateException e){return Map.of("message",e.getMessage());}
 public record StartRequest(String name){} public record DecisionRequest(String baselineRunId,AnswerExperimentDecision decision,String note){}
}
