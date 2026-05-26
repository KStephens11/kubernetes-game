package com.k8sgame.service;

import com.k8sgame.model.Challenge;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads challenge YAML files from {@code classpath:challenges/**} at startup
 * and provides indexed access by ID and ordered lists by level.
 */
@Service
public class ChallengeLoader {

    private static final Logger logger = LoggerFactory.getLogger(ChallengeLoader.class);

    private final Map<String, Challenge> challengesById = new LinkedHashMap<>();
    private final List<Challenge> orderedChallenges = new ArrayList<>();

    @PostConstruct
    public void loadChallenges() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources("classpath:challenges/**/*.yaml");
            List<Resource> sorted = Arrays.stream(resources)
                    .sorted(Comparator.comparing(r -> r.getFilename()))
                    .collect(Collectors.toList());

            for (Resource resource : sorted) {
                try {
                    Challenge challenge = mapper.readValue(resource.getInputStream(), Challenge.class);
                    challengesById.put(challenge.getId(), challenge);
                    orderedChallenges.add(challenge);
                    logger.debug("Loaded challenge: {}", challenge.getId());
                } catch (IOException e) {
                    logger.error("Failed to load challenge from {}: {}", resource.getFilename(), e.getMessage());
                }
            }

            // Sort by level then order
            orderedChallenges.sort(Comparator.comparingInt(Challenge::getLevel)
                    .thenComparingInt(Challenge::getOrder));

            logger.info("Loaded {} challenges", orderedChallenges.size());
        } catch (IOException e) {
            logger.warn("No challenge files found or error scanning: {}", e.getMessage());
        }
    }

    /**
     * Returns a challenge by its ID.
     *
     * @param id the challenge ID
     * @return an Optional containing the challenge, or empty if not found
     */
    public Optional<Challenge> getChallengeById(String id) {
        return Optional.ofNullable(challengesById.get(id));
    }

    /**
     * Returns all challenges sorted by level and order.
     */
    public List<Challenge> getAllChallenges() {
        return Collections.unmodifiableList(orderedChallenges);
    }

    /**
     * Returns challenges for a specific level.
     */
    public List<Challenge> getChallengesForLevel(int level) {
        return orderedChallenges.stream()
                .filter(c -> c.getLevel() == level)
                .collect(Collectors.toList());
    }

    /**
     * Returns the challenge at the given index in the ordered list.
     */
    public Optional<Challenge> getChallengeAtIndex(int index) {
        if (index < 0 || index >= orderedChallenges.size()) return Optional.empty();
        return Optional.of(orderedChallenges.get(index));
    }

    /**
     * Returns the total number of loaded challenges.
     */
    public int getTotalChallenges() {
        return orderedChallenges.size();
    }
}
