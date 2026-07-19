package com.edu.service.impl;

import com.edu.common.PageQuery;
import com.edu.common.PageResult;
import com.edu.exception.BaseException;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.vo.ai.AiFaceCompareRecordVO;
import com.edu.pojo.vo.ai.AiFaceCompareResultVO;
import com.edu.pojo.vo.ai.AiFaceProfileVO;
import com.edu.pojo.vo.ai.AiFaceRegisterResultVO;
import com.edu.service.AiFaceRecognitionService;
import com.edu.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class AiFaceRecognitionServiceImpl implements AiFaceRecognitionService {
    private static final String PROVIDER = "tencent-cloud";
    private static final Duration SESSION_TTL = Duration.ofHours(2);
    private static final ConcurrentMap<Long, FaceExperienceSession> FACE_SESSIONS = new ConcurrentHashMap<>();

    private final TencentFaceClient tencentFaceClient;

    @Override
    public AiFaceProfileVO getProfile() {
        UserInfoDTO user = currentUser();
        cleanupExpiredSessions();
        return toProfileVO(user.getUserId(), FACE_SESSIONS.get(user.getUserId()));
    }

    @Override
    public AiFaceRegisterResultVO registerFace(MultipartFile file) {
        UserInfoDTO user = currentUser();
        String imageBase64 = toBase64(file);
        TencentFaceClient.DetectFaceResult detection = tencentFaceClient.detectFace(imageBase64);
        if (detection.faceCount() <= 0) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "没有检测到清晰人脸，请正对摄像头重新录入");
        }

        LocalDateTime now = LocalDateTime.now();
        FaceExperienceSession session = new FaceExperienceSession();
        session.userId = user.getUserId();
        session.userName = displayName(user);
        session.registeredFaceBase64 = imageBase64;
        session.faceModelVersion = detection.faceModelVersion();
        session.registerRequestId = detection.requestId();
        session.compareCount = 0;
        session.createdTime = now;
        session.updatedTime = now;
        FACE_SESSIONS.put(user.getUserId(), session);

        return AiFaceRegisterResultVO.builder()
                .profile(toProfileVO(user.getUserId(), session))
                .detectedFaceCount(detection.faceCount())
                .provider(PROVIDER)
                .requestId(detection.requestId())
                .message("人脸录入成功，本次体验内可继续进行比对")
                .build();
    }

    @Override
    public AiFaceCompareResultVO compareFace(MultipartFile file) {
        UserInfoDTO user = currentUser();
        cleanupExpiredSessions();
        FaceExperienceSession session = FACE_SESSIONS.get(user.getUserId());
        if (session == null || !StringUtils.hasText(session.registeredFaceBase64)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "请先录入人脸，再进行比对");
        }

        TencentFaceClient.CompareFaceResult compareResult = tencentFaceClient.compareFace(
                session.registeredFaceBase64,
                toBase64(file)
        );
        LocalDateTime now = LocalDateTime.now();
        session.compareCount += 1;
        session.lastCompareScore = compareResult.score();
        session.lastCompareTime = now;
        session.updatedTime = now;
        if (StringUtils.hasText(compareResult.faceModelVersion())) {
            session.faceModelVersion = compareResult.faceModelVersion();
        }

        return AiFaceCompareResultVO.builder()
                .matched(compareResult.matched())
                .score(compareResult.score())
                .threshold(compareResult.threshold())
                .provider(PROVIDER)
                .requestId(compareResult.requestId())
                .message(compareResult.matched() ? "比对通过，是同一人的可能性较高" : "比对未通过，请保持光线充足并重新拍摄")
                .compareAt(now)
                .profile(toProfileVO(user.getUserId(), session))
                .build();
    }

    @Override
    public PageResult<AiFaceCompareRecordVO> listCompareHistory(Integer pageNum, Integer pageSize) {
        return PageResult.of(0, PageQuery.of(pageNum, pageSize), List.of());
    }

    @Override
    public void clearSession() {
        UserInfoDTO user = currentUser();
        FACE_SESSIONS.remove(user.getUserId());
    }

    private AiFaceProfileVO toProfileVO(Long userId, FaceExperienceSession session) {
        if (session == null) {
            return AiFaceProfileVO.builder()
                    .userId(userId)
                    .registered(false)
                    .compareCount(0)
                    .provider(PROVIDER)
                    .build();
        }
        return AiFaceProfileVO.builder()
                .userId(session.userId)
                .userName(session.userName)
                .provider(PROVIDER)
                .faceModelVersion(session.faceModelVersion)
                .compareCount(session.compareCount)
                .lastCompareScore(session.lastCompareScore)
                .lastCompareTime(session.lastCompareTime)
                .createdTime(session.createdTime)
                .updatedTime(session.updatedTime)
                .registered(true)
                .build();
    }

    private String toBase64(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "上传图片不能为空");
        }
        try {
            return Base64.getEncoder().encodeToString(file.getBytes());
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "读取上传图片失败");
        }
    }

    private UserInfoDTO currentUser() {
        UserInfoDTO user = SecurityUtil.getLoginUser();
        if (user == null || user.getUserId() == null) {
            throw new BaseException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return user;
    }

    private String displayName(UserInfoDTO user) {
        if (StringUtils.hasText(user.getRealName())) {
            return user.getRealName();
        }
        return StringUtils.hasText(user.getUsername()) ? user.getUsername() : "用户" + user.getUserId();
    }

    private void cleanupExpiredSessions() {
        LocalDateTime expireBefore = LocalDateTime.now().minus(SESSION_TTL);
        FACE_SESSIONS.entrySet().removeIf(entry -> entry.getValue().updatedTime.isBefore(expireBefore));
    }

    private static class FaceExperienceSession {
        private Long userId;
        private String userName;
        private String registeredFaceBase64;
        private String faceModelVersion;
        private String registerRequestId;
        private int compareCount;
        private Double lastCompareScore;
        private LocalDateTime lastCompareTime;
        private LocalDateTime createdTime;
        private LocalDateTime updatedTime;
    }
}
