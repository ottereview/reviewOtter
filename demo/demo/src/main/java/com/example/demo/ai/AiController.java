package com.example.demo.ai;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/ai")
@WebFluxController
public class AiController {

    private final AiClient aiClient;

    @PostMapping("/recommendation/title")
    public Mono<ResponseEntity<AiTitleResponse>> getTitleRecommendation(@RequestBody AiRequest request) {
        return aiClient.recommendTitle(request)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/recommendation/priorities")
    public Mono<ResponseEntity<AiPriorityResponse>> getPriorityRecommendation(@RequestBody AiRequest request) {
        return aiClient.recommendPriority(request)
                .map(ResponseEntity::ok);
    }
    
}