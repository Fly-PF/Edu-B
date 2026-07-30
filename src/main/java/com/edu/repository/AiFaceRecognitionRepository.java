package com.edu.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.edu.pojo.po.AiFaceCompareRecordPO;
import com.edu.pojo.po.AiFaceProfilePO;

public interface AiFaceRecognitionRepository {
    AiFaceProfilePO selectProfileByUserId(Long userId);

    int insertProfile(AiFaceProfilePO profile);

    int updateProfile(AiFaceProfilePO profile);

    int insertCompareRecord(AiFaceCompareRecordPO record);

    IPage<AiFaceCompareRecordPO> selectComparePage(long pageNum, long pageSize, Long userId);
}
