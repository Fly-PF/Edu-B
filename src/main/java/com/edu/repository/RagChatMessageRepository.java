package com.edu.repository;

import com.edu.pojo.po.RagChatMessagePO;

import java.util.List;

public interface RagChatMessageRepository {
    int insertMessage(RagChatMessagePO message);

    int updateMessage(RagChatMessagePO message);

    List<RagChatMessagePO> selectSessionMessages(Long sessionId);

    List<RagChatMessagePO> selectLatestSessionMessages(Long sessionId, Integer limit);

    List<Long> selectSessionMessageIds(Long sessionId);

    List<Long> selectSessionMessageIdsByMessageIds(Long sessionId, List<String> messageIds);

    int logicalDeleteSessionMessages(Long sessionId);

    int logicalDeleteMessagesByIds(List<Long> messageIds);

    boolean existsActiveQaImage(String objectName);
}
