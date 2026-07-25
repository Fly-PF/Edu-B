package com.edu.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.edu.pojo.po.RagChatSessionPO;
import com.edu.pojo.po.RagSessionKbRefPO;

public interface RagChatSessionRepository {
    int insertChatSession(RagChatSessionPO chatSession);

    RagChatSessionPO selectUserChatSession(Long sessionId, Long userId);

    IPage<RagChatSessionPO> selectUserChatSessionPage(long pageNum, long pageSize, Long userId);

    int renameUserChatSession(Long sessionId, Long userId, String sessionName);

    int logicalDeleteUserChatSession(Long sessionId, Long userId);

    int logicalDeleteSessionKbRefs(Long sessionId);

    int insertSessionKbRef(RagSessionKbRefPO sessionKbRef);
}
