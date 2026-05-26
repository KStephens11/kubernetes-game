package com.k8sgame.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Represents a single game challenge loaded from a YAML file.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Challenge {

    @JsonAlias("challenge_id")
    private String id;
    private int level;
    private int order;
    private String title;
    private String description;

    @JsonProperty("storyContext")
    @JsonAlias("story_context")
    private String storyContext;

    @JsonProperty("successCriteria")
    @JsonAlias("success_criteria")
    private List<Map<String, Object>> successCriteria;

    private List<Map<String, Object>> hints;

    @JsonAlias("initial_state")
    private Map<String, Object> initialState;

    public List<Map<String, Object>> getInitialResources() {
        if (initialState == null) return List.of();
        Object resources = initialState.get("resources");
        if (resources instanceof List<?> list) {
            return list.stream()
                .filter(r -> r instanceof Map)
                .map(r -> (Map<String, Object>) r)
                .collect(java.util.stream.Collectors.toList());
        }
        return List.of();
    }

    public void setInitialResources(List<Map<String, Object>> initialResources) {
        this.initialState = Map.of("resources", initialResources);
    }

    // Getters and setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStoryContext() { return storyContext; }
    public void setStoryContext(String storyContext) { this.storyContext = storyContext; }

    public List<Map<String, Object>> getSuccessCriteria() { return successCriteria; }
    public void setSuccessCriteria(List<Map<String, Object>> successCriteria) { this.successCriteria = successCriteria; }

    public List<Map<String, Object>> getHints() { return hints; }
    public void setHints(List<Map<String, Object>> hints) { this.hints = hints; }
}
