package com.edu.service;

import com.edu.pojo.dto.block.BlockProjectSaveRequest;
import com.edu.pojo.vo.block.BlockProjectVO;

import java.util.List;

public interface BlockProjectService {
    List<BlockProjectVO> listMine();
    List<BlockProjectVO> listGallery(String keyword);
    BlockProjectVO getProject(Long projectId);
    BlockProjectVO createProject(BlockProjectSaveRequest request);
    BlockProjectVO updateProject(Long projectId, BlockProjectSaveRequest request);
    BlockProjectVO publishProject(Long projectId);
    BlockProjectVO remixProject(Long projectId);
}
