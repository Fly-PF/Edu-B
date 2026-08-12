package com.edu.service.impl;

import com.edu.common.properties.AIModelProperties;
import com.edu.exception.BaseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TencentFaceClient {
    private static final String SERVICE = "iai";
    private static final String ALGORITHM = "TC3-HMAC-SHA256";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private final AIModelProperties aiModelProperties;
    private final ObjectMapper objectMapper;

    public DetectFaceResult detectFace(String imageBase64) {
        JsonNode response = invoke("DetectFace", Map.of(
                "Image", normalizeBase64(imageBase64),
                "NeedFaceAttributes", 0,
                "NeedQualityDetection", 1,
                "MaxFaceNum", 1
        ));
        JsonNode responseNode = response.path("Response");
        int faceCount = responseNode.path("FaceInfos").isArray() ? responseNode.path("FaceInfos").size() : 0;
        return new DetectFaceResult(
                faceCount,
                responseNode.path("RequestId").asText(""),
                responseNode.path("FaceModelVersion").asText("")
        );
    }

    public CompareFaceResult compareFace(String imageA, String imageB) {
        JsonNode response = invoke("CompareFace", Map.of(
                "ImageA", normalizeBase64(imageA),
                "ImageB", normalizeBase64(imageB)
        ));
        JsonNode responseNode = response.path("Response");
        double score = responseNode.path("Score").asDouble(0D);
        double threshold = aiModelProperties.getFace().getTencent().getCompareThreshold();
        return new CompareFaceResult(
                score,
                score >= threshold,
                threshold,
                responseNode.path("RequestId").asText(""),
                responseNode.path("FaceModelVersion").asText("")
        );
    }

    public boolean isConfigured() {
        AIModelProperties.Tencent tencent = aiModelProperties.getFace().getTencent();
        return StringUtils.hasText(tencent.getSecretId()) && StringUtils.hasText(tencent.getSecretKey());
    }

    private JsonNode invoke(String action, Map<String, Object> payload) {
        AIModelProperties.Tencent tencent = aiModelProperties.getFace().getTencent();
        if (!isConfigured()) {
            throw new BaseException(HttpStatus.SERVICE_UNAVAILABLE, "未配置腾讯云人脸识别密钥");
        }

        String host = trimEndSlash(tencent.getEndpoint());
        String region = StringUtils.hasText(tencent.getRegion()) ? tencent.getRegion().trim() : "ap-guangzhou";
        String version = StringUtils.hasText(tencent.getVersion()) ? tencent.getVersion().trim() : "2020-03-03";
        String body;
        try {
            body = objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "构造人脸识别请求失败");
        }

        long timestamp = Instant.now().getEpochSecond();
        String date = DATE_FORMATTER.format(Instant.ofEpochSecond(timestamp));
        String canonicalRequest = buildCanonicalRequest(host, body);
        String credentialScope = date + "/" + SERVICE + "/tc3_request";
        String stringToSign = ALGORITHM + "\n" + timestamp + "\n" + credentialScope + "\n" + sha256Hex(canonicalRequest);
        String signature = sign(tencent.getSecretKey(), date, stringToSign);
        String authorization = ALGORITHM
                + " Credential=" + tencent.getSecretId() + "/" + credentialScope
                + ", SignedHeaders=content-type;host"
                + ", Signature=" + signature;

        try {
            int timeout = Math.max(1, tencent.getTimeoutSeconds() == null ? 20 : tencent.getTimeoutSeconds()) * 1_000;
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(timeout);
            requestFactory.setReadTimeout(timeout);
            String responseBody = RestClient.builder()
                    .baseUrl("https://" + host)
                    .requestFactory(requestFactory)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, authorization)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
                    .defaultHeader(HttpHeaders.HOST, host)
                    .defaultHeader("X-TC-Action", action)
                    .defaultHeader("X-TC-Version", version)
                    .defaultHeader("X-TC-Timestamp", String.valueOf(timestamp))
                    .defaultHeader("X-TC-Region", region)
                    .defaultHeader("X-TC-Charset", "utf-8")
                    .build()
                    .post()
                    .uri("/")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(responseBody == null ? "{}" : responseBody);
            JsonNode errorNode = root.path("Response").path("Error");
            if (!errorNode.isMissingNode() && !errorNode.isNull()) {
                String code = errorNode.path("Code").asText("TencentFaceError");
                String message = errorNode.path("Message").asText("人脸识别调用失败");
                throw buildTencentError(code, message);
            }
            return root;
        } catch (RestClientException ex) {
            throw new BaseException(HttpStatus.BAD_GATEWAY, "腾讯云人脸识别调用失败：" + ex.getMessage());
        } catch (BaseException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.BAD_GATEWAY, "解析腾讯云人脸识别结果失败：" + ex.getMessage());
        }
    }

    private BaseException buildTencentError(String code, String message) {
        return switch (code) {
            case "InvalidParameterValue.NoFaceInPhoto" -> new BaseException(
                    HttpStatus.BAD_REQUEST,
                    "图片中没有检测到人脸，请正对摄像头、保持光线充足后重新拍摄"
            );
            case "InvalidParameterValue.ImageEmpty",
                    "InvalidParameterValue.ImageSizeTooLarge",
                    "InvalidParameterValue.InvalidImage",
                    "FailedOperation.ImageDecodeFailed" -> new BaseException(
                    HttpStatus.BAD_REQUEST,
                    "图片无法识别，请重新拍摄或更换清晰的人脸图片"
            );
            case "InvalidParameterValue.FaceQualityNotQualified" -> new BaseException(
                    HttpStatus.BAD_REQUEST,
                    "人脸图片质量不足，请保持人脸完整、清晰并重新拍摄"
            );
            case "AuthFailure.SecretIdNotFound",
                    "AuthFailure.SignatureFailure",
                    "AuthFailure.TokenFailure",
                    "UnauthorizedOperation" -> new BaseException(
                    HttpStatus.UNAUTHORIZED,
                    "腾讯云人脸识别密钥或权限配置不正确，请检查 SecretId、SecretKey 和 CAM 授权"
            );
            case "ResourceUnavailable.NotExist" -> new BaseException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "腾讯云人脸识别服务尚未开通，请先在控制台开通后再体验"
            );
            case "LimitExceeded",
                    "LimitExceeded.ErrorFaceNumExceed",
                    "RequestLimitExceeded" -> new BaseException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "人脸识别调用过于频繁，请稍后再试"
            );
            default -> new BaseException(HttpStatus.BAD_GATEWAY, code + "：" + message);
        };
    }

    private String buildCanonicalRequest(String host, String body) {
        String hashedPayload = sha256Hex(body);
        return "POST\n/\n\ncontent-type:application/json; charset=utf-8\nhost:" + host + "\n\ncontent-type;host\n" + hashedPayload;
    }

    private String sign(String secretKey, String date, String stringToSign) {
        byte[] secretDate = hmacSha256(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmacSha256(secretDate, SERVICE);
        byte[] secretSigning = hmacSha256(secretService, "tc3_request");
        return bytesToHex(hmacSha256(secretSigning, stringToSign));
    }

    private byte[] hmacSha256(byte[] key, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("签名失败", ex);
        }
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashed);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 计算失败", ex);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(Character.forDigit((value >> 4) & 0xF, 16));
            builder.append(Character.forDigit(value & 0xF, 16));
        }
        return builder.toString();
    }

    private String normalizeBase64(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String text = value.trim();
        int commaIndex = text.indexOf(',');
        if (text.startsWith("data:image/") && commaIndex >= 0) {
            text = text.substring(commaIndex + 1);
        }
        return Base64.getEncoder().encodeToString(Base64.getDecoder().decode(text));
    }

    private String trimEndSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "iai.tencentcloudapi.com";
        }
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    public record DetectFaceResult(int faceCount, String requestId, String faceModelVersion) {
    }

    public record CompareFaceResult(double score, boolean matched, double threshold, String requestId, String faceModelVersion) {
    }
}
