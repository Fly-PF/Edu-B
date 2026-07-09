package com.edu.service;

import com.edu.pojo.dto.personnel.CreatePersonnelRequest;
import com.edu.pojo.dto.personnel.UpdatePersonnelRequest;
import com.edu.pojo.vo.personnel.CreatePersonnelResultVO;
import com.edu.pojo.vo.personnel.PageResultVO;
import com.edu.pojo.vo.personnel.PersonnelVO;

public interface PersonnelService {
    PageResultVO<PersonnelVO> page(Integer userType, Long pageNum, Long pageSize, String keyword, Integer status);

    CreatePersonnelResultVO create(Integer userType, CreatePersonnelRequest request);

    PersonnelVO detail(Integer userType, Long id);

    void update(Integer userType, Long id, UpdatePersonnelRequest request);

    String updateStatus(Integer userType, Long id, Integer status);

    void delete(Integer userType, Long id);
}
