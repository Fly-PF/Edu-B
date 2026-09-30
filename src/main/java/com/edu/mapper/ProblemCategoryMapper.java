package com.edu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edu.pojo.po.problem.ProblemCategoryPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProblemCategoryMapper extends BaseMapper<ProblemCategoryPO> {
}
