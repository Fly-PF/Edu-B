package com.edu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.exception.BaseException;
import com.edu.mapper.BlockProjectMapper;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.dto.block.BlockProjectSaveRequest;
import com.edu.pojo.po.BlockProjectPO;
import com.edu.pojo.vo.block.BlockProjectVO;
import com.edu.service.BlockProjectService;
import com.edu.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BlockProjectServiceImpl implements BlockProjectService {
    private static final int PRIVATE = 0;
    private static final int PUBLIC = 1;
    private static final int NOT_DELETED = 0;

    private final BlockProjectMapper projectMapper;

    @Override
    public List<BlockProjectVO> listMine() {
        Long userId = requireUser().getUserId();
        return projectMapper.selectList(baseQuery()
                        .eq(BlockProjectPO::getOwnerId, userId)
                        .orderByDesc(BlockProjectPO::getUpdateTime)
                        .orderByDesc(BlockProjectPO::getId))
                .stream()
                .map(project -> toView(project, true))
                .toList();
    }

    @Override
    public List<BlockProjectVO> listGallery(String keyword) {
        return projectMapper.selectList(baseQuery()
                        .eq(BlockProjectPO::getVisibility, PUBLIC)
                        .and(StringUtils.hasText(keyword), query -> query
                                .like(BlockProjectPO::getTitle, keyword.trim())
                                .or()
                                .like(BlockProjectPO::getDescription, keyword.trim())
                                .or()
                                .like(BlockProjectPO::getOwnerName, keyword.trim()))
                        .orderByDesc(BlockProjectPO::getPublishedTime)
                        .orderByDesc(BlockProjectPO::getId))
                .stream()
                .map(project -> toView(project, false))
                .toList();
    }

    @Override
    public BlockProjectVO getProject(Long projectId) {
        BlockProjectPO project = requireProject(projectId);
        Long currentUserId = requireUser().getUserId();
        boolean isOwner = currentUserId.equals(project.getOwnerId());
        if (!isOwner && !Integer.valueOf(PUBLIC).equals(project.getVisibility())) {
            throw new BaseException(HttpStatus.FORBIDDEN, "Project is private");
        }
        if (!isOwner) {
            projectMapper.updateById(BlockProjectPO.builder()
                    .id(project.getId())
                    .viewCount((project.getViewCount() == null ? 0 : project.getViewCount()) + 1)
                    .build());
            project.setViewCount((project.getViewCount() == null ? 0 : project.getViewCount()) + 1);
        }
        return toView(project, true);
    }

    @Override
    public BlockProjectVO createProject(BlockProjectSaveRequest request) {
        UserInfoDTO user = requireUser();
        BlockProjectPO project = BlockProjectPO.builder()
                .ownerId(user.getUserId())
                .ownerName(resolveUserName(user))
                .title(request.getTitle().trim())
                .description(normalizeDescription(request.getDescription()))
                .workspaceJson(request.getWorkspaceJson())
                .stageJson(request.getStageJson())
                .thumbnailData(request.getThumbnailData())
                .visibility(PRIVATE)
                .remixCount(0)
                .viewCount(0)
                .deleted(NOT_DELETED)
                .build();
        projectMapper.insert(project);
        return toView(project, true);
    }

    @Override
    public BlockProjectVO updateProject(Long projectId, BlockProjectSaveRequest request) {
        BlockProjectPO project = requireOwnedProject(projectId);
        project.setTitle(request.getTitle().trim());
        project.setDescription(normalizeDescription(request.getDescription()));
        project.setWorkspaceJson(request.getWorkspaceJson());
        project.setStageJson(request.getStageJson());
        project.setThumbnailData(request.getThumbnailData());
        projectMapper.updateById(project);
        return toView(project, true);
    }

    @Override
    public BlockProjectVO publishProject(Long projectId) {
        BlockProjectPO project = requireOwnedProject(projectId);
        project.setVisibility(PUBLIC);
        if (project.getPublishedTime() == null) {
            project.setPublishedTime(LocalDateTime.now());
        }
        projectMapper.updateById(project);
        return toView(project, true);
    }

    @Override
    public BlockProjectVO remixProject(Long projectId) {
        BlockProjectPO source = requireProject(projectId);
        if (!Integer.valueOf(PUBLIC).equals(source.getVisibility())) {
            throw new BaseException(HttpStatus.FORBIDDEN, "Only public projects can be remixed");
        }
        UserInfoDTO user = requireUser();
        BlockProjectPO remix = BlockProjectPO.builder()
                .ownerId(user.getUserId())
                .ownerName(resolveUserName(user))
                .title("Copy of " + source.getTitle())
                .description(source.getDescription())
                .workspaceJson(source.getWorkspaceJson())
                .stageJson(source.getStageJson())
                .thumbnailData(source.getThumbnailData())
                .visibility(PRIVATE)
                .sourceProjectId(source.getId())
                .remixCount(0)
                .viewCount(0)
                .deleted(NOT_DELETED)
                .build();
        projectMapper.insert(remix);
        projectMapper.updateById(BlockProjectPO.builder()
                .id(source.getId())
                .remixCount((source.getRemixCount() == null ? 0 : source.getRemixCount()) + 1)
                .build());
        return toView(remix, true);
    }

    private BlockProjectPO requireOwnedProject(Long projectId) {
        BlockProjectPO project = requireProject(projectId);
        if (!requireUser().getUserId().equals(project.getOwnerId())) {
            throw new BaseException(HttpStatus.FORBIDDEN, "Only the owner can edit this project");
        }
        return project;
    }

    private BlockProjectPO requireProject(Long projectId) {
        BlockProjectPO project = projectMapper.selectOne(baseQuery().eq(BlockProjectPO::getId, projectId));
        if (project == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "Project not found");
        }
        return project;
    }

    private LambdaQueryWrapper<BlockProjectPO> baseQuery() {
        return new LambdaQueryWrapper<BlockProjectPO>().eq(BlockProjectPO::getDeleted, NOT_DELETED);
    }

    private UserInfoDTO requireUser() {
        UserInfoDTO user = SecurityUtil.getLoginUser();
        if (user == null || user.getUserId() == null) {
            throw new BaseException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        return user;
    }

    private String resolveUserName(UserInfoDTO user) {
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
    }

    private String normalizeDescription(String description) {
        return description == null ? "" : description.trim();
    }

    private BlockProjectVO toView(BlockProjectPO project, boolean includeContent) {
        return BlockProjectVO.builder()
                .id(project.getId())
                .ownerId(project.getOwnerId())
                .ownerName(project.getOwnerName())
                .title(project.getTitle())
                .description(project.getDescription())
                .workspaceJson(includeContent ? project.getWorkspaceJson() : null)
                .stageJson(includeContent ? project.getStageJson() : null)
                .thumbnailData(project.getThumbnailData())
                .published(Integer.valueOf(PUBLIC).equals(project.getVisibility()))
                .sourceProjectId(project.getSourceProjectId())
                .remixCount(project.getRemixCount() == null ? 0 : project.getRemixCount())
                .viewCount(project.getViewCount() == null ? 0 : project.getViewCount())
                .publishedTime(project.getPublishedTime())
                .createTime(project.getCreateTime())
                .updateTime(project.getUpdateTime())
                .build();
    }
}
