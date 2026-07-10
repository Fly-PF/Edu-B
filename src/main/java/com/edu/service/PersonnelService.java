package com.edu.service;

import com.edu.common.PageResult;
import com.edu.pojo.dto.personnel.CreatePersonnelRequest;
import com.edu.pojo.dto.personnel.UpdatePersonnelRequest;
import com.edu.pojo.vo.personnel.CreatePersonnelResultVO;
import com.edu.pojo.vo.personnel.PersonnelVO;

public interface PersonnelService {
    PageResult<PersonnelVO> page(Integer userType, Integer pageNum, Integer pageSize, String keyword, Integer status);

    CreatePersonnelResultVO create(Integer userType, CreatePersonnelRequest request);

    PersonnelVO detail(Integer userType, Long id);

    void update(Integer userType, Long id, UpdatePersonnelRequest request);

    String updateStatus(Integer userType, Long id, Integer status);

    void delete(Integer userType, Long id);
}
