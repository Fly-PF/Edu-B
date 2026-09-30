package com.edu.problemsolving.session;

import com.edu.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProblemLessonSessionRegistry {
    private final Map<String, ProblemLessonSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> transportSessions = new ConcurrentHashMap<>();

    public ProblemLessonSession create(Long userId, Long problemId, Integer problemVersion, Map<String, Object> initialScene) {
        String clientSessionId = UUID.randomUUID().toString();
        ProblemLessonSession session = ProblemLessonSession.builder().clientSessionId(clientSessionId)
                .wsToken(UUID.randomUUID().toString()).userId(userId).problemId(problemId)
                .problemVersion(problemVersion).initialScene(initialScene).build();
        sessions.put(clientSessionId, session);
        return session;
    }

    public ProblemLessonSession connect(String clientSessionId, String wsToken) {
        ProblemLessonSession session = require(clientSessionId);
        if (!session.getWsToken().equals(wsToken)) {
            throw new BaseException(HttpStatus.UNAUTHORIZED, "讲题会话令牌无效");
        }
        return session;
    }

    public ProblemLessonSession require(String clientSessionId) {
        ProblemLessonSession session = sessions.get(clientSessionId);
        if (session == null) throw new BaseException(HttpStatus.NOT_FOUND, "讲题会话已结束");
        return session;
    }

    public void bindTransport(String transportSessionId, String clientSessionId) {
        transportSessions.put(transportSessionId, clientSessionId);
    }

    public void removeTransport(String transportSessionId) {
        String clientSessionId = transportSessions.remove(transportSessionId);
        if (clientSessionId != null) remove(clientSessionId);
    }

    public void remove(String clientSessionId) {
        sessions.remove(clientSessionId);
        transportSessions.entrySet().removeIf(item -> item.getValue().equals(clientSessionId));
    }
}
