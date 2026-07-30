package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.edu.mapper.RagMsgDocRefMapper;
import com.edu.pojo.po.RagMsgDocRefPO;
import com.edu.repository.RagMsgDocRefRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RagMsgDocRefRepositoryImpl implements RagMsgDocRefRepository {
    private final RagMsgDocRefMapper ragMsgDocRefMapper;

    @Override
    public int insertMsgDocRef(RagMsgDocRefPO msgDocRef) {
        return ragMsgDocRefMapper.insert(msgDocRef);
    }

    @Override
    public List<RagMsgDocRefPO> selectMsgDocRefs(List<Long> msgIds) {
        if (msgIds == null || msgIds.isEmpty()) {
            return List.of();
        }
        return ragMsgDocRefMapper.selectList(new LambdaQueryWrapper<RagMsgDocRefPO>()
                .in(RagMsgDocRefPO::getMsgId, msgIds)
                .eq(RagMsgDocRefPO::getDeleted, 0)
                .orderByAsc(RagMsgDocRefPO::getCreateTime)
                .orderByAsc(RagMsgDocRefPO::getId));
    }

    @Override
    public int logicalDeleteMsgDocRefs(List<Long> msgIds) {
        if (msgIds == null || msgIds.isEmpty()) {
            return 0;
        }
        return ragMsgDocRefMapper.update(new LambdaUpdateWrapper<RagMsgDocRefPO>()
                .in(RagMsgDocRefPO::getMsgId, msgIds)
                .eq(RagMsgDocRefPO::getDeleted, 0)
                .set(RagMsgDocRefPO::getDeleted, 1));
    }
}
