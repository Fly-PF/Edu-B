package com.edu.service;

import com.edu.common.PageResult;
import com.edu.pojo.vo.ai.AiFaceCompareRecordVO;
import com.edu.pojo.vo.ai.AiFaceCompareResultVO;
import com.edu.pojo.vo.ai.AiFaceProfileVO;
import com.edu.pojo.vo.ai.AiFaceRegisterResultVO;
import org.springframework.web.multipart.MultipartFile;

public interface AiFaceRecognitionService {
    AiFaceProfileVO getProfile();

    AiFaceRegisterResultVO registerFace(MultipartFile file);

    AiFaceCompareResultVO compareFace(MultipartFile file);

    PageResult<AiFaceCompareRecordVO> listCompareHistory(Integer pageNum, Integer pageSize);

    void clearSession();
}
