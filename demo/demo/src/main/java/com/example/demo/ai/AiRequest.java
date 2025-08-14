package com.ssafy.ottereview.ai.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@NoArgsConstructor  
@AllArgsConstructor                                                                            
@Getter
@Builder
public class AiConventionRequest {
    @JsonProperty("repo_id")
    private Long repoId;
    private String source;
    private String target;
    private Rules rules;

    @Getter
    @Builder
    public static class Rules {
        @JsonProperty("file_names")
        private String fileNames;
        @JsonProperty("function_names")
        private String functionNames;
        @JsonProperty("variable_names")
        private String variableNames;
        @JsonProperty("class_names")
        private String classNames;
        @JsonProperty("constant_names")
        private String constantNames;
    }
}
