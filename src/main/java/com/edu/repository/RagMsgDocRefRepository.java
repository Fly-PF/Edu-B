package com.edu.repository;

import com.edu.pojo.po.RagMsgDocRefPO;

import java.util.List;

public interface RagMsgDocRefRepository {
    int insertMsgDocRef(RagMsgDocRefPO msgDocRef);

    List<RagMsgDocRefPO> selectMsgDocRefs(List<Long> msgIds);

    int logicalDeleteMsgDocRefs(List<Long> msgIds);
}
