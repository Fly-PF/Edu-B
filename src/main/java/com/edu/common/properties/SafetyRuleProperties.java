package com.edu.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "edu.safety.rules")
public class SafetyRuleProperties {
    private List<String> cheatingPhrases = new ArrayList<>();
    private List<String> cheatingEvasionPhrases = new ArrayList<>();
    private List<String> learningSupportPhrases = new ArrayList<>();
    private List<String> ageInappropriatePhrases = new ArrayList<>();
    private List<String> promptAttackPhrases = new ArrayList<>();
    private List<String> privacyCompositePhrases = new ArrayList<>();
    private List<String> allowPhrases = new ArrayList<>();
}
