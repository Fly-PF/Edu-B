package com.edu.repository;

import com.edu.pojo.po.RagKbUserCollectionPO;

public interface RagKbUserCollectionRepository {
    RagKbUserCollectionPO selectCollection(Long userId, Long kbId);

    boolean existsActiveCollection(Long userId, Long kbId);

    int insertCollection(RagKbUserCollectionPO collection);

    int restoreCollection(Long id);

    int cancelCollection(Long userId, Long kbId);
}
