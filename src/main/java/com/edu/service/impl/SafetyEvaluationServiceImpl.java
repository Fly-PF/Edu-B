package com.edu.service.impl;

import com.edu.exception.UserErrorException;
import com.edu.pojo.dto.safety.SafetyEvaluationResultDTO;
import com.edu.pojo.dto.safety.SafetyEvaluationRunRequest;
import com.edu.pojo.dto.safety.SafetyEvaluationSampleRequest;
import com.edu.pojo.dto.safety.SafetyGatewayRequest;
import com.edu.pojo.dto.safety.SafetyGatewayResponse;
import com.edu.pojo.enums.safety.SafetyDecision;
import com.edu.pojo.enums.safety.SafetyRiskType;
import com.edu.service.safety.SafetyEvaluationService;
import com.edu.service.safety.SafetyGatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PreDestroy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SafetyEvaluationServiceImpl implements SafetyEvaluationService {
    private static final int MAX_PARALLELISM = 4;
    private static final ExecutorService EVALUATION_EXECUTOR = Executors.newFixedThreadPool(
            Math.max(2, Math.min(MAX_PARALLELISM, Runtime.getRuntime().availableProcessors())),
            new NamedThreadFactory()
    );

    private final SafetyGatewayService safetyGatewayService;

    @Override
    public SafetyEvaluationResultDTO runEvaluation(SafetyEvaluationRunRequest request) {
        if (request == null || request.getSamples() == null || request.getSamples().isEmpty()) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "评测样本不能为空");
        }

        List<SafetyEvaluationResultDTO.SampleResultItem> sampleResults = new ArrayList<>();
        Map<SafetyRiskType, Confusion> confusionMap = new EnumMap<>(SafetyRiskType.class);
        for (SafetyRiskType riskType : SafetyRiskType.values()) {
            confusionMap.put(riskType, new Confusion());
        }

        int decisionCorrectCount = 0;
        int riskExactMatchCount = 0;
        int evidenceEvaluatedCount = 0;
        int evidenceMatchCount = 0;
        boolean recordSamples = Boolean.TRUE.equals(request.getRecordSamples());

        List<CompletableFuture<SampleEvaluationOutcome>> futures = new ArrayList<>();
        for (SafetyEvaluationSampleRequest sample : request.getSamples()) {
            validateSample(sample);
            SafetyGatewayRequest gatewayRequest = sample.getRequest().toBuilder()
                    .recordLog(recordSamples)
                    .build();
            futures.add(CompletableFuture.supplyAsync(
                    () -> evaluateSample(sample, gatewayRequest),
                    EVALUATION_EXECUTOR
            ));
        }

        for (CompletableFuture<SampleEvaluationOutcome> future : futures) {
            SampleEvaluationOutcome outcome;
            try {
                outcome = future.join();
            } catch (CompletionException ex) {
                Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                if (cause instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new IllegalStateException("安全评测样本执行失败", cause);
            }

            SafetyEvaluationSampleRequest sample = outcome.sample();
            SafetyGatewayResponse actual = outcome.actual();

            boolean decisionMatched = sample.getExpectedDecision() == actual.getDecision();
            if (decisionMatched) {
                decisionCorrectCount++;
            }

            Set<SafetyRiskType> expectedRiskTypes = normalizedRiskTypes(sample.getExpectedRiskTypes());
            Set<SafetyRiskType> actualRiskTypes = normalizedRiskTypes(actual.getRiskTypes());
            boolean riskTypesMatched = expectedRiskTypes.equals(actualRiskTypes);
            if (riskTypesMatched) {
                riskExactMatchCount++;
            }

            boolean evidenceLevelEvaluated = sample.getExpectedEvidenceLevel() != null;
            boolean evidenceLevelMatched = evidenceLevelEvaluated
                    && sample.getExpectedEvidenceLevel() == actual.getEvidenceLevel();
            if (evidenceLevelEvaluated) {
                evidenceEvaluatedCount++;
                if (evidenceLevelMatched) {
                    evidenceMatchCount++;
                }
            }

            updateConfusionMatrix(confusionMap, expectedRiskTypes, actualRiskTypes);

            sampleResults.add(SafetyEvaluationResultDTO.SampleResultItem.builder()
                    .sampleId(firstNonBlank(sample.getSampleId(), buildAutoSampleId(sampleResults.size())))
                    .expectedDecision(sample.getExpectedDecision())
                    .actualDecision(actual.getDecision())
                    .decisionMatched(decisionMatched)
                    .expectedRiskTypes(new ArrayList<>(expectedRiskTypes))
                    .actualRiskTypes(new ArrayList<>(actualRiskTypes))
                    .riskTypesMatched(riskTypesMatched)
                    .expectedEvidenceLevel(sample.getExpectedEvidenceLevel())
                    .actualEvidenceLevel(actual.getEvidenceLevel())
                    .evidenceLevelMatched(evidenceLevelEvaluated ? evidenceLevelMatched : null)
                    .reason(actual.getReason())
                    .suggestion(actual.getSuggestion())
                    .build());
        }

        int totalSamples = sampleResults.size();
        List<SafetyEvaluationResultDTO.RiskMetricItem> riskMetrics = buildRiskMetrics(confusionMap);
        double macroRecall = average(riskMetrics.stream().map(SafetyEvaluationResultDTO.RiskMetricItem::getRecall).toList());
        double macroFpr = average(riskMetrics.stream().map(SafetyEvaluationResultDTO.RiskMetricItem::getFalsePositiveRate).toList());
        double macroFnr = average(riskMetrics.stream().map(SafetyEvaluationResultDTO.RiskMetricItem::getFalseNegativeRate).toList());

        return SafetyEvaluationResultDTO.builder()
                .runName(firstNonBlank(request.getRunName(), "safety-evaluation-run"))
                .totalSamples(totalSamples)
                .decisionCorrectCount(decisionCorrectCount)
                .decisionAccuracy(rate(decisionCorrectCount, totalSamples))
                .riskExactMatchCount(riskExactMatchCount)
                .riskExactMatchRate(rate(riskExactMatchCount, totalSamples))
                .evidenceEvaluatedCount(evidenceEvaluatedCount)
                .evidenceMatchCount(evidenceMatchCount)
                .evidenceMatchRate(rate(evidenceMatchCount, evidenceEvaluatedCount))
                .macroRecall(macroRecall)
                .macroFalsePositiveRate(macroFpr)
                .macroFalseNegativeRate(macroFnr)
                .riskMetrics(riskMetrics)
                .sampleResults(sampleResults)
                .build();
    }

    private void validateSample(SafetyEvaluationSampleRequest sample) {
        if (sample == null) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "评测样本不能包含空项");
        }
        if (sample.getRequest() == null) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "评测样本缺少安全检测请求");
        }
        if (sample.getExpectedDecision() == null) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "评测样本缺少期望处置结果");
        }
    }

    private void updateConfusionMatrix(Map<SafetyRiskType, Confusion> confusionMap,
                                       Set<SafetyRiskType> expectedRiskTypes,
                                       Set<SafetyRiskType> actualRiskTypes) {
        for (Map.Entry<SafetyRiskType, Confusion> entry : confusionMap.entrySet()) {
            SafetyRiskType riskType = entry.getKey();
            Confusion confusion = entry.getValue();
            boolean expected = expectedRiskTypes.contains(riskType);
            boolean actual = actualRiskTypes.contains(riskType);
            if (expected && actual) {
                confusion.tp++;
            } else if (!expected && actual) {
                confusion.fp++;
            } else if (expected) {
                confusion.fn++;
            } else {
                confusion.tn++;
            }
        }
    }

    private List<SafetyEvaluationResultDTO.RiskMetricItem> buildRiskMetrics(Map<SafetyRiskType, Confusion> confusionMap) {
        return Arrays.stream(SafetyRiskType.values())
                .map(riskType -> {
                    Confusion confusion = confusionMap.get(riskType);
                    return SafetyEvaluationResultDTO.RiskMetricItem.builder()
                            .riskType(riskType)
                            .truePositive(confusion.tp)
                            .falsePositive(confusion.fp)
                            .trueNegative(confusion.tn)
                            .falseNegative(confusion.fn)
                            .recall(rate(confusion.tp, confusion.tp + confusion.fn))
                            .falsePositiveRate(rate(confusion.fp, confusion.fp + confusion.tn))
                            .falseNegativeRate(rate(confusion.fn, confusion.fn + confusion.tp))
                            .build();
                })
                .toList();
    }

    private Set<SafetyRiskType> normalizedRiskTypes(List<SafetyRiskType> riskTypes) {
        if (riskTypes == null || riskTypes.isEmpty()) {
            return Set.of();
        }
        return riskTypes.stream()
                .filter(v -> v != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private double average(List<Double> values) {
        List<Double> filtered = values.stream().filter(v -> v != null).toList();
        if (filtered.isEmpty()) {
            return 0.0d;
        }
        return filtered.stream().mapToDouble(Double::doubleValue).average().orElse(0.0d);
    }

    private double rate(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0.0d;
        }
        return (double) numerator / denominator;
    }

    private String buildAutoSampleId(int index) {
        return "sample-" + (index + 1);
    }

    private SampleEvaluationOutcome evaluateSample(SafetyEvaluationSampleRequest sample, SafetyGatewayRequest gatewayRequest) {
        SafetyGatewayResponse actual = safetyGatewayService.evaluate(gatewayRequest);
        return new SampleEvaluationOutcome(sample, actual);
    }

    private String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        if (StringUtils.hasText(second)) {
            return second.trim();
        }
        return null;
    }

    private static final class Confusion {
        private int tp;
        private int fp;
        private int tn;
        private int fn;
    }

    private record SampleEvaluationOutcome(
            SafetyEvaluationSampleRequest sample,
            SafetyGatewayResponse actual
    ) {
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger index = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "safety-eval-" + index.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }

    @PreDestroy
    public void shutdownExecutor() {
        EVALUATION_EXECUTOR.shutdown();
    }
}
