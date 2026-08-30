package com.edu.service.impl;

import com.edu.exception.BaseException;
import com.edu.common.PageResult;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.dto.gov.GovKnowledgeNodeCreateRequest;
import com.edu.pojo.dto.gov.GovKnowledgeCompareSaveRequest;
import com.edu.pojo.dto.gov.GovKnowledgeAnnotationSaveRequest;
import com.edu.pojo.dto.gov.GovKnowledgeNoteSaveRequest;
import com.edu.pojo.dto.gov.GovKnowledgeNodeUpdateRequest;
import com.edu.pojo.dto.gov.GovKnowledgeProgressUpdateRequest;
import com.edu.pojo.enums.gov.GovKnowledgeNodeType;
import com.edu.pojo.enums.gov.GovKnowledgeProgressStatus;
import com.edu.pojo.enums.gov.GovKnowledgeSubject;
import com.edu.pojo.po.gov.GovKnowledgeComparePO;
import com.edu.pojo.po.gov.GovKnowledgeAnnotationPO;
import com.edu.pojo.po.gov.GovKnowledgeFavoritePO;
import com.edu.pojo.po.gov.GovKnowledgeNodePO;
import com.edu.pojo.po.gov.GovKnowledgeProgressPO;
import com.edu.pojo.po.gov.GovKnowledgeNotePO;
import com.edu.pojo.vo.gov.GovKnowledgeCompareVO;
import com.edu.pojo.vo.gov.GovKnowledgeAnnotationVO;
import com.edu.pojo.vo.gov.GovKnowledgeFavoriteVO;
import com.edu.pojo.vo.gov.GovKnowledgeFavoriteItemVO;
import com.edu.pojo.vo.gov.GovKnowledgeNodeVO;
import com.edu.pojo.vo.gov.GovKnowledgeNoteItemVO;
import com.edu.pojo.vo.gov.GovKnowledgeNoteVO;
import com.edu.pojo.vo.gov.GovKnowledgeProgressVO;
import com.edu.repository.GovKnowledgeCompareRepository;
import com.edu.repository.GovKnowledgeFavoriteRepository;
import com.edu.repository.GovKnowledgeAnnotationRepository;
import com.edu.repository.GovKnowledgeNodeRepository;
import com.edu.repository.GovKnowledgeProgressRepository;
import com.edu.repository.GovKnowledgeNoteRepository;
import com.edu.service.GovKnowledgeService;
import com.edu.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GovKnowledgeServiceImpl implements GovKnowledgeService {
    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN", "SUPERADMIN");
    private static final String STATUS_TODO = GovKnowledgeProgressStatus.TODO.name();
    private static final String STATUS_LEARNING = GovKnowledgeProgressStatus.LEARNING.name();
    private static final String STATUS_DONE = GovKnowledgeProgressStatus.DONE.name();

    private final GovKnowledgeNodeRepository nodeRepository;
    private final GovKnowledgeProgressRepository progressRepository;
    private final GovKnowledgeCompareRepository compareRepository;
    private final GovKnowledgeFavoriteRepository favoriteRepository;
    private final GovKnowledgeAnnotationRepository annotationRepository;
    private final GovKnowledgeNoteRepository noteRepository;

    @Override
    public List<GovKnowledgeNodeVO> listKnowledgeTree(String subject, String keyword) {
        String normalizedSubject = resolveSubject(subject);
        List<GovKnowledgeNodePO> nodes = nodeRepository.selectVisibleNodesBySubject(normalizedSubject);
        if (nodes.isEmpty()) {
            return List.of();
        }
        Set<Long> visibleIds = filterVisibleNodeIds(nodes, keyword);
        Map<Long, List<GovKnowledgeNodePO>> childrenByParent = groupChildrenByParent(nodes, visibleIds);
        Map<Long, GovKnowledgeProgressPO> progressMap = loadProgressMap(nodes, visibleIds);
        return childrenByParent.getOrDefault(0L, List.of()).stream()
                .map(node -> buildNodeVo(node, childrenByParent, progressMap, visibleIds, false))
                .toList();
    }

    @Override
    public List<GovKnowledgeNodeVO> listAdminKnowledgeTree(String subject, String keyword) {
        requireAdmin();
        String normalizedSubject = resolveSubject(subject);
        List<GovKnowledgeNodePO> nodes = nodeRepository.selectExistingNodesBySubject(normalizedSubject);
        if (nodes.isEmpty()) {
            return List.of();
        }
        Set<Long> visibleIds = filterVisibleNodeIds(nodes, keyword);
        Map<Long, List<GovKnowledgeNodePO>> childrenByParent = groupChildrenByParent(nodes, visibleIds);
        return childrenByParent.getOrDefault(0L, List.of()).stream()
                .map(node -> buildNodeVo(node, childrenByParent, Map.of(), visibleIds, false))
                .toList();
    }

    @Override
    public GovKnowledgeNodeVO getKnowledgeNode(Long nodeId) {
        GovKnowledgeNodePO node = requireVisibleNode(nodeId);
        List<GovKnowledgeNodePO> nodes = nodeRepository.selectVisibleNodesBySubject(node.getSubject());
        Map<Long, List<GovKnowledgeNodePO>> childrenByParent = groupChildrenByParent(nodes, null);
        Set<Long> visibleIds = new LinkedHashSet<>();
        visibleIds.add(node.getId());
        collectDescendantIds(node.getId(), childrenByParent, visibleIds);
        return buildNodeVo(node, childrenByParent, loadProgressMap(nodes, visibleIds), visibleIds, true);
    }

    @Override
    public GovKnowledgeNodeVO getAdminKnowledgeNode(Long nodeId) {
        GovKnowledgeNodePO node = requireNodeById(nodeId);
        List<GovKnowledgeNodePO> nodes = nodeRepository.selectExistingNodesBySubject(node.getSubject());
        Set<Long> allIds = nodes.stream().map(GovKnowledgeNodePO::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, List<GovKnowledgeNodePO>> childrenByParent = groupChildrenByParent(nodes, allIds);
        return buildNodeVo(node, childrenByParent, Map.of(), allIds, true);
    }

    @Override
    public GovKnowledgeProgressVO getKnowledgeProgress(Long knowledgeId) {
        UserInfoDTO user = currentUser();
        GovKnowledgeNodePO node = requireVisibleNode(knowledgeId);
        if (!GovKnowledgeNodeType.POINT.name().equals(node.getNodeType())) {
            List<GovKnowledgeNodePO> nodes = nodeRepository.selectVisibleNodesBySubject(node.getSubject());
            Set<Long> visibleIds = nodes.stream().map(GovKnowledgeNodePO::getId).collect(Collectors.toCollection(LinkedHashSet::new));
            Map<Long, List<GovKnowledgeNodePO>> childrenByParent = groupChildrenByParent(nodes, visibleIds);
            GovKnowledgeNodeVO chapterVo = buildNodeVo(node, childrenByParent, loadProgressMap(nodes, visibleIds), visibleIds, false);
            return buildProgressVo(user.getUserId(), knowledgeId, chapterVo.getProgressStatus(), chapterVo.getCompletedAt());
        }
        GovKnowledgeProgressPO progress = progressRepository.selectProgress(user.getUserId(), knowledgeId);
        return buildProgressVo(user.getUserId(), knowledgeId, progress == null ? STATUS_TODO : progress.getStatus(),
                progress == null ? null : progress.getCompletedAt());
    }

    @Override
    @Transactional
    public GovKnowledgeProgressVO updateKnowledgeProgress(Long knowledgeId, GovKnowledgeProgressUpdateRequest request) {
        UserInfoDTO user = currentUser();
        GovKnowledgeNodePO node = requireVisibleNode(knowledgeId);
        if (!GovKnowledgeNodeType.POINT.name().equals(node.getNodeType())) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "只有知识点可以更新学习进度");
        }
        String status = normalizeProgressStatus(request.getStatus());
        LocalDateTime now = LocalDateTime.now();
        GovKnowledgeProgressPO progress = progressRepository.selectProgress(user.getUserId(), knowledgeId);
        if (progress == null) {
            progress = GovKnowledgeProgressPO.builder()
                    .userId(user.getUserId())
                    .knowledgeId(knowledgeId)
                    .status(status)
                    .completedAt(STATUS_DONE.equals(status) ? now : null)
                    .createTime(now)
                    .updateTime(now)
                    .deleted(0)
                    .build();
            progressRepository.insertProgress(progress);
        } else {
            progress.setStatus(status);
            progress.setCompletedAt(STATUS_DONE.equals(status) ? now : null);
            progress.setUpdateTime(now);
            progressRepository.updateProgress(progress);
        }
        return buildProgressVo(user.getUserId(), knowledgeId, status, progress.getCompletedAt());
    }

    @Override
    public List<GovKnowledgeCompareVO> listKnowledgeCompare(Long knowledgeId) {
        GovKnowledgeNodePO node = requireVisibleNode(knowledgeId);
        List<Long> targetIds = resolveCompareKnowledgeIds(node);
        if (targetIds.isEmpty()) {
            return List.of();
        }
        return compareRepository.selectVisibleCompareByKnowledgeIds(targetIds).stream()
                .map(this::buildCompareVo)
                .toList();
    }

    @Override
    public List<GovKnowledgeCompareVO> listAdminKnowledgeCompare(Long knowledgeId) {
        requireAdmin();
        GovKnowledgeNodePO node = requireNodeById(knowledgeId);
        List<Long> targetIds = resolveCompareKnowledgeIds(node);
        if (targetIds.isEmpty()) {
            return List.of();
        }
        List<GovKnowledgeComparePO> items = compareRepository.selectCompareByKnowledgeIds(targetIds);
        return items.stream()
                .map(this::buildCompareVo)
                .toList();
    }

    @Override
    @Transactional
    public GovKnowledgeCompareVO saveKnowledgeCompare(Long knowledgeId, GovKnowledgeCompareSaveRequest request) {
        UserInfoDTO user = requireAdmin();
        GovKnowledgeNodePO node = requireNodeById(knowledgeId);
        if (!GovKnowledgeNodeType.POINT.name().equals(node.getNodeType())) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "请先选择知识点");
        }
        validateCompareRequest(request);
        LocalDateTime now = LocalDateTime.now();
        GovKnowledgeComparePO compare = GovKnowledgeComparePO.builder()
                .knowledgeId(node.getId())
                .title(normalizeRequiredText(request.getTitle(), "辨析标题不能为空", 200))
                .contentMd(normalizeRequiredText(request.getContentMd(), "辨析内容不能为空", 50000))
                .sortOrder(request.getSortOrder() == null ? nextCompareSortOrder(node.getId()) : request.getSortOrder())
                .status(request.getStatus() == null ? 1 : request.getStatus())
                .createBy(user.getUserId())
                .updateBy(user.getUserId())
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .build();
        compareRepository.insertCompare(compare);
        return buildCompareVo(compare);
    }

    @Override
    @Transactional
    public GovKnowledgeCompareVO updateKnowledgeCompare(Long compareId, GovKnowledgeCompareSaveRequest request) {
        UserInfoDTO user = requireAdmin();
        if (compareId == null || compareId <= 0) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "辨析ID不能为空");
        }
        GovKnowledgeComparePO compare = compareRepository.selectCompareById(compareId);
        if (compare == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "辨析不存在");
        }
        validateCompareRequest(request);
        compare.setTitle(normalizeRequiredText(request.getTitle(), "辨析标题不能为空", 200));
        compare.setContentMd(normalizeRequiredText(request.getContentMd(), "辨析内容不能为空", 50000));
        compare.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        compare.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        compare.setUpdateBy(user.getUserId());
        compare.setUpdateTime(LocalDateTime.now());
        compareRepository.updateCompare(compare);
        return buildCompareVo(compare);
    }

    @Override
    @Transactional
    public void deleteKnowledgeCompare(Long compareId) {
        requireAdmin();
        if (compareId == null || compareId <= 0) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "辨析ID不能为空");
        }
        compareRepository.logicalDeleteCompare(compareId);
    }

    @Override
    public GovKnowledgeFavoriteVO getKnowledgeFavorite(Long knowledgeId) {
        UserInfoDTO user = currentUser();
        requireVisibleNode(knowledgeId);
        boolean favorited = favoriteRepository.selectFavorite(user.getUserId(), knowledgeId) != null;
        return GovKnowledgeFavoriteVO.builder()
                .knowledgeId(knowledgeId)
                .favorited(favorited)
                .build();
    }

    @Override
    @Transactional
    public GovKnowledgeFavoriteVO collectKnowledge(Long knowledgeId) {
        UserInfoDTO user = currentUser();
        requireVisibleNode(knowledgeId);
        LocalDateTime now = LocalDateTime.now();
        GovKnowledgeFavoritePO favorite = favoriteRepository.selectRecord(user.getUserId(), knowledgeId);
        if (favorite == null) {
            favorite = GovKnowledgeFavoritePO.builder()
                    .userId(user.getUserId())
                    .knowledgeId(knowledgeId)
                    .createTime(now)
                    .updateTime(now)
                    .deleted(0)
                    .build();
            favoriteRepository.insertFavorite(favorite);
        } else {
            favorite.setDeleted(0);
            favorite.setUpdateTime(now);
            favoriteRepository.updateFavorite(favorite);
        }
        return GovKnowledgeFavoriteVO.builder()
                .knowledgeId(knowledgeId)
                .favorited(true)
                .build();
    }

    @Override
    @Transactional
    public GovKnowledgeFavoriteVO cancelKnowledgeFavorite(Long knowledgeId) {
        UserInfoDTO user = currentUser();
        requireVisibleNode(knowledgeId);
        favoriteRepository.logicalDeleteFavorite(user.getUserId(), knowledgeId);
        return GovKnowledgeFavoriteVO.builder()
                .knowledgeId(knowledgeId)
                .favorited(false)
                .build();
    }

    @Override
    public GovKnowledgeNoteVO getKnowledgeNote(Long knowledgeId) {
        UserInfoDTO user = currentUser();
        requireVisibleNode(knowledgeId);
        GovKnowledgeNotePO note = noteRepository.selectNote(user.getUserId(), knowledgeId);
        return GovKnowledgeNoteVO.builder()
                .knowledgeId(knowledgeId)
                .content(note == null ? "" : note.getNoteContent())
                .updatedAt(note == null ? null : note.getUpdateTime())
                .build();
    }

    @Override
    @Transactional
    public GovKnowledgeNoteVO saveKnowledgeNote(Long knowledgeId, GovKnowledgeNoteSaveRequest request) {
        UserInfoDTO user = currentUser();
        requireVisibleNode(knowledgeId);
        String content = request == null ? "" : normalizeOptionalText(request.getContent(), 5000);
        if (!StringUtils.hasText(content)) {
            noteRepository.logicalDeleteNote(user.getUserId(), knowledgeId);
            return GovKnowledgeNoteVO.builder()
                    .knowledgeId(knowledgeId)
                    .content("")
                    .updatedAt(null)
                    .build();
        }
        LocalDateTime now = LocalDateTime.now();
        GovKnowledgeNotePO note = noteRepository.selectRecord(user.getUserId(), knowledgeId);
        if (note == null) {
            note = GovKnowledgeNotePO.builder()
                    .userId(user.getUserId())
                    .knowledgeId(knowledgeId)
                    .noteContent(content)
                    .createTime(now)
                    .updateTime(now)
                    .deleted(0)
                    .build();
            noteRepository.insertNote(note);
        } else {
            note.setNoteContent(content);
            note.setDeleted(0);
            note.setUpdateTime(now);
            noteRepository.updateNote(note);
        }
        return GovKnowledgeNoteVO.builder()
                .knowledgeId(knowledgeId)
                .content(content)
                .updatedAt(now)
                .build();
    }

    @Override
    public List<GovKnowledgeAnnotationVO> listKnowledgeAnnotations(Long knowledgeId) {
        UserInfoDTO user = currentUser();
        GovKnowledgeNodePO node = requireVisibleNode(knowledgeId);
        List<GovKnowledgeAnnotationPO> annotations = annotationRepository.selectAnnotationsByUserAndKnowledge(user.getUserId(), knowledgeId);
        if (annotations.isEmpty()) {
            return List.of();
        }
        Map<Long, GovKnowledgeNodePO> nodeMap = Map.of(node.getId(), node);
        GovKnowledgeProgressPO progress = progressRepository.selectProgress(user.getUserId(), knowledgeId);
        return annotations.stream()
                .map(annotation -> buildAnnotationVo(annotation, nodeMap.get(annotation.getKnowledgeId()), progress))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional
    public GovKnowledgeAnnotationVO saveKnowledgeAnnotation(Long knowledgeId, GovKnowledgeAnnotationSaveRequest request) {
        UserInfoDTO user = currentUser();
        GovKnowledgeNodePO node = requireVisibleNode(knowledgeId);
        validateAnnotationRequest(request);
        LocalDateTime now = LocalDateTime.now();
        GovKnowledgeAnnotationPO annotation = GovKnowledgeAnnotationPO.builder()
                .userId(user.getUserId())
                .knowledgeId(knowledgeId)
                .sectionKey(normalizeRequiredText(request.getSectionKey(), "段落标识不能为空", 100))
                .sectionTitle(normalizeRequiredText(request.getSectionTitle(), "段落标题不能为空", 200))
                .startOffset(request.getStartOffset())
                .endOffset(request.getEndOffset())
                .selectedText(normalizeRequiredText(request.getSelectedText(), "选中文本不能为空", 2000))
                .noteContent(normalizeRequiredText(request.getNoteContent(), "标注内容不能为空", 5000))
                .color(normalizeAnnotationColor(request.getColor()))
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .build();
        annotationRepository.insertAnnotation(annotation);
        return buildAnnotationVo(annotation, node, progressRepository.selectProgress(user.getUserId(), knowledgeId));
    }

    @Override
    @Transactional
    public GovKnowledgeAnnotationVO updateKnowledgeAnnotation(Long annotationId, GovKnowledgeAnnotationSaveRequest request) {
        UserInfoDTO user = currentUser();
        if (annotationId == null || annotationId <= 0) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "标注ID不能为空");
        }
        GovKnowledgeAnnotationPO annotation = annotationRepository.selectAnnotation(user.getUserId(), annotationId);
        if (annotation == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "标注不存在");
        }
        validateAnnotationRequest(request);
        annotation.setSectionKey(normalizeRequiredText(request.getSectionKey(), "段落标识不能为空", 100));
        annotation.setSectionTitle(normalizeRequiredText(request.getSectionTitle(), "段落标题不能为空", 200));
        annotation.setStartOffset(request.getStartOffset());
        annotation.setEndOffset(request.getEndOffset());
        annotation.setSelectedText(normalizeRequiredText(request.getSelectedText(), "选中文本不能为空", 2000));
        annotation.setNoteContent(normalizeRequiredText(request.getNoteContent(), "标注内容不能为空", 5000));
        annotation.setColor(normalizeAnnotationColor(request.getColor()));
        annotation.setUpdateTime(LocalDateTime.now());
        annotationRepository.updateAnnotation(annotation);
        GovKnowledgeNodePO node = requireVisibleNode(annotation.getKnowledgeId());
        return buildAnnotationVo(annotation, node, progressRepository.selectProgress(user.getUserId(), annotation.getKnowledgeId()));
    }

    @Override
    @Transactional
    public void deleteKnowledgeAnnotation(Long annotationId) {
        UserInfoDTO user = currentUser();
        if (annotationId == null || annotationId <= 0) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "标注ID不能为空");
        }
        annotationRepository.logicalDeleteAnnotation(user.getUserId(), annotationId);
    }

    @Override
    public PageResult<GovKnowledgeFavoriteItemVO> pageMyFavoriteKnowledge(Integer pageNum, Integer pageSize, String keyword) {
        UserInfoDTO user = currentUser();
        PageBounds pageBounds = resolvePageBounds(pageNum, pageSize);
        String normalizedKeyword = normalizeSearchKeyword(keyword);
        List<GovKnowledgeFavoritePO> favorites = favoriteRepository.selectFavoritesByUser(user.getUserId());
        if (favorites.isEmpty()) {
            return PageResult.of(0, pageBounds.pageQuery(), List.of());
        }
        List<Long> knowledgeIds = favorites.stream().map(GovKnowledgeFavoritePO::getKnowledgeId).toList();
        Map<Long, GovKnowledgeNodePO> nodeMap = nodeRepository.selectNodesByIds(knowledgeIds).stream()
                .collect(Collectors.toMap(GovKnowledgeNodePO::getId, item -> item));
        Map<Long, GovKnowledgeProgressPO> progressMap = progressRepository.selectProgressByKnowledgeIds(user.getUserId(), knowledgeIds)
                .stream()
                .collect(Collectors.toMap(GovKnowledgeProgressPO::getKnowledgeId, item -> item));
        List<GovKnowledgeFavoriteItemVO> items = favorites.stream()
                .map(favorite -> buildFavoriteItem(favorite, nodeMap.get(favorite.getKnowledgeId()), progressMap.get(favorite.getKnowledgeId())))
                .filter(Objects::nonNull)
                .filter(item -> matchesFavoriteKeyword(item, normalizedKeyword))
                .toList();
        return pageFavoriteItems(items, pageBounds);
    }

    @Override
    public PageResult<GovKnowledgeNoteItemVO> pageMyKnowledgeNotes(Integer pageNum, Integer pageSize, String keyword) {
        UserInfoDTO user = currentUser();
        PageBounds pageBounds = resolvePageBounds(pageNum, pageSize);
        String normalizedKeyword = normalizeSearchKeyword(keyword);
        List<GovKnowledgeNotePO> notes = noteRepository.selectNotesByUser(user.getUserId());
        if (notes.isEmpty()) {
            return PageResult.of(0, pageBounds.pageQuery(), List.of());
        }
        List<Long> knowledgeIds = notes.stream().map(GovKnowledgeNotePO::getKnowledgeId).toList();
        Map<Long, GovKnowledgeNodePO> nodeMap = nodeRepository.selectNodesByIds(knowledgeIds).stream()
                .collect(Collectors.toMap(GovKnowledgeNodePO::getId, item -> item));
        Map<Long, GovKnowledgeProgressPO> progressMap = progressRepository.selectProgressByKnowledgeIds(user.getUserId(), knowledgeIds)
                .stream()
                .collect(Collectors.toMap(GovKnowledgeProgressPO::getKnowledgeId, item -> item));
        List<GovKnowledgeNoteItemVO> items = notes.stream()
                .map(note -> buildNoteItem(note, nodeMap.get(note.getKnowledgeId()), progressMap.get(note.getKnowledgeId())))
                .filter(Objects::nonNull)
                .filter(item -> matchesNoteKeyword(item, normalizedKeyword))
                .toList();
        return pageNoteItems(items, pageBounds);
    }

    @Override
    public PageResult<GovKnowledgeAnnotationVO> pageMyKnowledgeAnnotations(Integer pageNum, Integer pageSize, String keyword) {
        UserInfoDTO user = currentUser();
        PageBounds pageBounds = resolvePageBounds(pageNum, pageSize);
        String normalizedKeyword = normalizeSearchKeyword(keyword);
        List<GovKnowledgeAnnotationPO> annotations = annotationRepository.selectAnnotationsByUser(user.getUserId());
        if (annotations.isEmpty()) {
            return PageResult.of(0, pageBounds.pageQuery(), List.of());
        }
        List<Long> knowledgeIds = annotations.stream().map(GovKnowledgeAnnotationPO::getKnowledgeId).distinct().toList();
        Map<Long, GovKnowledgeNodePO> nodeMap = nodeRepository.selectNodesByIds(knowledgeIds).stream()
                .collect(Collectors.toMap(GovKnowledgeNodePO::getId, item -> item));
        Map<Long, GovKnowledgeProgressPO> progressMap = progressRepository.selectProgressByKnowledgeIds(user.getUserId(), knowledgeIds)
                .stream()
                .collect(Collectors.toMap(GovKnowledgeProgressPO::getKnowledgeId, item -> item));
        List<GovKnowledgeAnnotationVO> items = annotations.stream()
                .map(annotation -> buildAnnotationVo(annotation, nodeMap.get(annotation.getKnowledgeId()), progressMap.get(annotation.getKnowledgeId())))
                .filter(Objects::nonNull)
                .filter(item -> matchesAnnotationKeyword(item, normalizedKeyword))
                .toList();
        return PageResult.of(items.size(), pageBounds.pageQuery(), slicePage(items, pageBounds.pageNum(), pageBounds.pageSize()));
    }

    @Override
    @Transactional
    public GovKnowledgeNodeVO createKnowledgeNode(GovKnowledgeNodeCreateRequest request) {
        UserInfoDTO user = requireAdmin();
        String subject = resolveSubject(request.getSubject());
        String nodeType = resolveNodeType(request.getNodeType());
        Long parentId = normalizeParentId(request.getParentId());
        String title = normalizeRequiredText(request.getTitle(), "标题不能为空", 200);
        String contentMd = normalizeOptionalText(request.getContentMd(), 50000);
        Integer sortOrder = request.getSortOrder() == null ? nextSortOrder(subject, parentId) : request.getSortOrder();
        Integer status = request.getStatus() == null ? 1 : request.getStatus();
        validateNodeShape(nodeType, parentId, contentMd, subject);

        LocalDateTime now = LocalDateTime.now();
        GovKnowledgeNodePO node = GovKnowledgeNodePO.builder()
                .subject(subject)
                .parentId(parentId)
                .nodeType(nodeType)
                .title(title)
                .contentMd(contentMd)
                .sortOrder(sortOrder)
                .status(status)
                .createBy(user.getUserId())
                .updateBy(user.getUserId())
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .build();
        nodeRepository.insertNode(node);
        return buildAdminNodeView(node.getId());
    }

    @Override
    @Transactional
    public GovKnowledgeNodeVO updateKnowledgeNode(Long nodeId, GovKnowledgeNodeUpdateRequest request) {
        UserInfoDTO user = requireAdmin();
        GovKnowledgeNodePO node = requireNodeById(nodeId);
        List<GovKnowledgeNodePO> currentNodes = nodeRepository.selectExistingNodesBySubject(node.getSubject());
        Map<Long, List<GovKnowledgeNodePO>> currentChildrenByParent = groupChildrenByParent(currentNodes, null);
        boolean hasChildren = !currentChildrenByParent.getOrDefault(nodeId, List.of()).isEmpty();
        if (request.getSubject() != null) {
            String newSubject = resolveSubject(request.getSubject());
            if (!Objects.equals(newSubject, node.getSubject()) && hasChildren) {
                throw new BaseException(HttpStatus.BAD_REQUEST, "已有子节点时不能修改科目");
            }
            node.setSubject(newSubject);
        }
        if (request.getNodeType() != null) {
            String newNodeType = resolveNodeType(request.getNodeType());
            if (GovKnowledgeNodeType.POINT.name().equals(newNodeType) && hasChildren) {
                throw new BaseException(HttpStatus.BAD_REQUEST, "已有子节点时不能改为知识点");
            }
            node.setNodeType(newNodeType);
        }
        if (request.getParentId() != null) {
            node.setParentId(normalizeParentId(request.getParentId()));
        }
        if (request.getTitle() != null) {
            node.setTitle(normalizeRequiredText(request.getTitle(), "标题不能为空", 200));
        }
        if (request.getContentMd() != null) {
            node.setContentMd(normalizeOptionalText(request.getContentMd(), 50000));
        }
        if (request.getSortOrder() != null) {
            node.setSortOrder(request.getSortOrder());
        }
        if (request.getStatus() != null) {
            node.setStatus(request.getStatus());
        }
        validateNodeShape(node.getNodeType(), node.getParentId(), node.getContentMd(), node.getSubject());
        node.setUpdateBy(user.getUserId());
        node.setUpdateTime(LocalDateTime.now());
        nodeRepository.updateNode(node);
        return buildAdminNodeView(nodeId);
    }

    @Override
    @Transactional
    public void deleteKnowledgeNode(Long nodeId) {
        requireAdmin();
        GovKnowledgeNodePO node = requireNodeById(nodeId);
        List<GovKnowledgeNodePO> allNodes = nodeRepository.selectExistingNodesBySubject(node.getSubject());
        Map<Long, List<GovKnowledgeNodePO>> childrenByParent = groupChildrenByParent(allNodes, null);
        List<Long> deleteIds = new ArrayList<>();
        collectDescendantIds(node.getId(), childrenByParent, deleteIds);
        deleteIds.add(node.getId());
        List<Long> distinctIds = deleteIds.stream().distinct().toList();
        nodeRepository.logicalDeleteNodes(distinctIds);
        List<Long> knowledgeIds = allNodes.stream()
                .filter(item -> distinctIds.contains(item.getId()))
                .filter(item -> GovKnowledgeNodeType.POINT.name().equals(item.getNodeType()))
                .map(GovKnowledgeNodePO::getId)
                .toList();
        progressRepository.logicalDeleteProgressByKnowledgeIds(knowledgeIds);
    }

    private Map<Long, GovKnowledgeProgressPO> loadProgressMap(List<GovKnowledgeNodePO> nodes, Set<Long> visibleIds) {
        List<Long> pointIds = nodes.stream()
                .filter(node -> GovKnowledgeNodeType.POINT.name().equals(node.getNodeType()))
                .filter(node -> visibleIds == null || visibleIds.contains(node.getId()))
                .map(GovKnowledgeNodePO::getId)
                .toList();
        if (pointIds.isEmpty()) {
            return Map.of();
        }
        UserInfoDTO user = currentUser();
        return progressRepository.selectProgressByKnowledgeIds(user.getUserId(), pointIds).stream()
                .collect(Collectors.toMap(GovKnowledgeProgressPO::getKnowledgeId, item -> item));
    }

    private Set<Long> filterVisibleNodeIds(List<GovKnowledgeNodePO> nodes, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return nodes.stream().map(GovKnowledgeNodePO::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        Map<Long, GovKnowledgeNodePO> nodeMap = nodes.stream()
                .collect(Collectors.toMap(GovKnowledgeNodePO::getId, item -> item));
        Map<Long, List<GovKnowledgeNodePO>> childrenByParent = groupChildrenByParent(nodes, null);
        Set<Long> matchedIds = nodes.stream()
                .filter(node -> containsKeyword(node, normalizedKeyword))
                .map(GovKnowledgeNodePO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> visibleIds = new LinkedHashSet<>(matchedIds);
        for (Long matchedId : matchedIds) {
            includeAncestors(matchedId, nodeMap, visibleIds);
            includeDescendants(matchedId, childrenByParent, visibleIds);
        }
        return visibleIds;
    }

    private void includeAncestors(Long nodeId, Map<Long, GovKnowledgeNodePO> nodeMap, Set<Long> visibleIds) {
        GovKnowledgeNodePO current = nodeMap.get(nodeId);
        while (current != null && current.getParentId() != null && current.getParentId() > 0) {
            Long parentId = current.getParentId();
            if (!visibleIds.add(parentId)) {
                // already included, continue climbing
            }
            current = nodeMap.get(parentId);
        }
    }

    private void includeDescendants(Long nodeId, Map<Long, List<GovKnowledgeNodePO>> childrenByParent, Set<Long> visibleIds) {
        List<GovKnowledgeNodePO> children = childrenByParent.getOrDefault(nodeId, List.of());
        for (GovKnowledgeNodePO child : children) {
            if (visibleIds.add(child.getId())) {
                includeDescendants(child.getId(), childrenByParent, visibleIds);
            }
        }
    }

    private boolean containsKeyword(GovKnowledgeNodePO node, String normalizedKeyword) {
        return containsText(node.getTitle(), normalizedKeyword)
                || containsText(node.getContentMd(), normalizedKeyword)
                || containsText(node.getSubject(), normalizedKeyword)
                || containsText(node.getNodeType(), normalizedKeyword);
    }

    private boolean containsText(String value, String normalizedKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

    private void collectDescendantIds(Long nodeId, Map<Long, List<GovKnowledgeNodePO>> childrenByParent, Collection<Long> target) {
        for (GovKnowledgeNodePO child : childrenByParent.getOrDefault(nodeId, List.of())) {
            target.add(child.getId());
            collectDescendantIds(child.getId(), childrenByParent, target);
        }
    }

    private PageResult<GovKnowledgeFavoriteItemVO> pageFavoriteItems(List<GovKnowledgeFavoriteItemVO> items, PageBounds pageBounds) {
        List<GovKnowledgeFavoriteItemVO> pageItems = slicePage(items, pageBounds.pageNum(), pageBounds.pageSize());
        return PageResult.of(items.size(), pageBounds.pageQuery(), pageItems);
    }

    private PageResult<GovKnowledgeNoteItemVO> pageNoteItems(List<GovKnowledgeNoteItemVO> items, PageBounds pageBounds) {
        List<GovKnowledgeNoteItemVO> pageItems = slicePage(items, pageBounds.pageNum(), pageBounds.pageSize());
        return PageResult.of(items.size(), pageBounds.pageQuery(), pageItems);
    }

    private <T> List<T> slicePage(List<T> items, int pageNum, int pageSize) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        int fromIndex = Math.max(0, (pageNum - 1) * pageSize);
        if (fromIndex >= items.size()) {
            return List.of();
        }
        int toIndex = Math.min(items.size(), fromIndex + pageSize);
        return items.subList(fromIndex, toIndex);
    }

    private GovKnowledgeFavoriteItemVO buildFavoriteItem(GovKnowledgeFavoritePO favorite,
                                                         GovKnowledgeNodePO node,
                                                         GovKnowledgeProgressPO progress) {
        if (favorite == null || node == null) {
            return null;
        }
        return GovKnowledgeFavoriteItemVO.builder()
                .favoriteId(favorite.getId())
                .knowledgeId(node.getId())
                .subject(node.getSubject())
                .nodeType(node.getNodeType())
                .title(node.getTitle())
                .contentPreview(buildPreview(node.getContentMd(), 140))
                .progressStatus(progress == null ? STATUS_TODO : progress.getStatus())
                .favoritedAt(favorite.getUpdateTime())
                .build();
    }

    private GovKnowledgeNoteItemVO buildNoteItem(GovKnowledgeNotePO note,
                                                 GovKnowledgeNodePO node,
                                                 GovKnowledgeProgressPO progress) {
        if (note == null || node == null) {
            return null;
        }
        String content = note.getNoteContent();
        return GovKnowledgeNoteItemVO.builder()
                .noteId(note.getId())
                .knowledgeId(node.getId())
                .subject(node.getSubject())
                .nodeType(node.getNodeType())
                .title(node.getTitle())
                .noteContent(content)
                .notePreview(buildPreview(content, 180))
                .progressStatus(progress == null ? STATUS_TODO : progress.getStatus())
                .updatedAt(note.getUpdateTime())
                .build();
    }

    private GovKnowledgeAnnotationVO buildAnnotationVo(GovKnowledgeAnnotationPO annotation,
                                                       GovKnowledgeNodePO node,
                                                       GovKnowledgeProgressPO progress) {
        if (annotation == null || node == null) {
            return null;
        }
        String selectedText = annotation.getSelectedText();
        String noteContent = annotation.getNoteContent();
        return GovKnowledgeAnnotationVO.builder()
                .annotationId(annotation.getId())
                .knowledgeId(node.getId())
                .subject(node.getSubject())
                .nodeType(node.getNodeType())
                .title(node.getTitle())
                .sectionKey(annotation.getSectionKey())
                .sectionTitle(annotation.getSectionTitle())
                .startOffset(annotation.getStartOffset())
                .endOffset(annotation.getEndOffset())
                .selectedText(selectedText)
                .selectedPreview(buildPreview(selectedText, 120))
                .noteContent(noteContent)
                .notePreview(buildPreview(noteContent, 180))
                .color(annotation.getColor())
                .progressStatus(progress == null ? STATUS_TODO : progress.getStatus())
                .createdAt(annotation.getCreateTime())
                .updatedAt(annotation.getUpdateTime())
                .build();
    }

    private boolean matchesFavoriteKeyword(GovKnowledgeFavoriteItemVO item, String normalizedKeyword) {
        return !StringUtils.hasText(normalizedKeyword)
                || containsText(item.getTitle(), normalizedKeyword)
                || containsText(item.getSubject(), normalizedKeyword)
                || containsText(item.getContentPreview(), normalizedKeyword);
    }

    private boolean matchesNoteKeyword(GovKnowledgeNoteItemVO item, String normalizedKeyword) {
        return !StringUtils.hasText(normalizedKeyword)
                || containsText(item.getTitle(), normalizedKeyword)
                || containsText(item.getSubject(), normalizedKeyword)
                || containsText(item.getNoteContent(), normalizedKeyword)
                || containsText(item.getNotePreview(), normalizedKeyword);
    }

    private boolean matchesAnnotationKeyword(GovKnowledgeAnnotationVO item, String normalizedKeyword) {
        return !StringUtils.hasText(normalizedKeyword)
                || containsText(item.getTitle(), normalizedKeyword)
                || containsText(item.getSubject(), normalizedKeyword)
                || containsText(item.getSectionTitle(), normalizedKeyword)
                || containsText(item.getSelectedText(), normalizedKeyword)
                || containsText(item.getSelectedPreview(), normalizedKeyword)
                || containsText(item.getNoteContent(), normalizedKeyword)
                || containsText(item.getNotePreview(), normalizedKeyword);
    }

    private String buildPreview(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private String normalizeSearchKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : "";
    }

    private void validateAnnotationRequest(GovKnowledgeAnnotationSaveRequest request) {
        if (request == null) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "请选择正文内容");
        }
        Integer startOffset = request.getStartOffset();
        Integer endOffset = request.getEndOffset();
        if (startOffset == null || startOffset < 0 || endOffset == null || endOffset <= startOffset) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "请选择正确的正文内容");
        }
    }

    private String normalizeAnnotationColor(String color) {
        if (!StringUtils.hasText(color)) {
            return "lavender";
        }
        String normalized = color.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "mint", "teal", "peach", "lavender" -> normalized;
            default -> "lavender";
        };
    }

    private PageBounds resolvePageBounds(Integer pageNum, Integer pageSize) {
        int normalizedPageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int normalizedPageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        return new PageBounds(normalizedPageNum, normalizedPageSize);
    }

    private List<Long> resolveCompareKnowledgeIds(GovKnowledgeNodePO node) {
        if (GovKnowledgeNodeType.POINT.name().equals(node.getNodeType())) {
            return List.of(node.getId());
        }
        List<GovKnowledgeNodePO> nodes = nodeRepository.selectVisibleNodesBySubject(node.getSubject());
        Map<Long, List<GovKnowledgeNodePO>> childrenByParent = groupChildrenByParent(nodes, null);
        List<Long> targetIds = new ArrayList<>();
        collectPointDescendantIds(node.getId(), childrenByParent, targetIds);
        return targetIds.stream().distinct().toList();
    }

    private void collectPointDescendantIds(Long nodeId, Map<Long, List<GovKnowledgeNodePO>> childrenByParent, Collection<Long> target) {
        for (GovKnowledgeNodePO child : childrenByParent.getOrDefault(nodeId, List.of())) {
            if (GovKnowledgeNodeType.POINT.name().equals(child.getNodeType())) {
                target.add(child.getId());
            }
            collectPointDescendantIds(child.getId(), childrenByParent, target);
        }
    }

    private Map<Long, List<GovKnowledgeNodePO>> groupChildrenByParent(List<GovKnowledgeNodePO> nodes, Set<Long> visibleIds) {
        return nodes.stream()
                .filter(node -> visibleIds == null || visibleIds.contains(node.getId()))
                .collect(Collectors.groupingBy(
                        node -> node.getParentId() == null ? 0L : node.getParentId(),
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(Collectors.toList(), list -> list.stream()
                                .sorted(Comparator
                                        .comparing((GovKnowledgeNodePO node) -> defaultInt(node.getSortOrder()))
                                        .thenComparing(GovKnowledgeNodePO::getId))
                                .toList())));
    }

    private GovKnowledgeNodeVO buildNodeVo(GovKnowledgeNodePO node,
                                           Map<Long, List<GovKnowledgeNodePO>> childrenByParent,
                                           Map<Long, GovKnowledgeProgressPO> progressMap,
                                           Set<Long> visibleIds,
                                           boolean includeContent) {
        List<GovKnowledgeNodePO> children = childrenByParent.getOrDefault(node.getId(), List.of()).stream()
                .filter(child -> visibleIds == null || visibleIds.contains(child.getId()))
                .toList();
        List<GovKnowledgeNodeVO> childVos = children.stream()
                .map(child -> buildNodeVo(child, childrenByParent, progressMap, visibleIds, false))
                .toList();
        boolean isPoint = GovKnowledgeNodeType.POINT.name().equals(node.getNodeType());
        String progressStatus = isPoint
                ? progressMap.getOrDefault(node.getId(), null) == null ? STATUS_TODO : progressMap.get(node.getId()).getStatus()
                : aggregateProgress(node, childVos);
        LocalDateTime completedAt = isPoint
                ? progressMap.getOrDefault(node.getId(), null) == null ? null : progressMap.get(node.getId()).getCompletedAt()
                : aggregateCompletedAt(childVos);
        return GovKnowledgeNodeVO.builder()
                .id(node.getId())
                .subject(node.getSubject())
                .parentId(node.getParentId())
                .nodeType(node.getNodeType())
                .title(node.getTitle())
                .contentMd(includeContent ? node.getContentMd() : null)
                .sortOrder(defaultInt(node.getSortOrder()))
                .status(defaultInt(node.getStatus()))
                .progressStatus(progressStatus)
                .completedAt(completedAt)
                .hasChildren(!childVos.isEmpty())
                .children(childVos)
                .build();
    }

    private String aggregateProgress(GovKnowledgeNodePO node, List<GovKnowledgeNodeVO> childVos) {
        if (childVos.isEmpty()) {
            return STATUS_TODO;
        }
        boolean allDone = childVos.stream().allMatch(child -> STATUS_DONE.equals(child.getProgressStatus()));
        if (allDone) {
            return STATUS_DONE;
        }
        boolean anyLearningOrDone = childVos.stream().anyMatch(child ->
                STATUS_LEARNING.equals(child.getProgressStatus()) || STATUS_DONE.equals(child.getProgressStatus()));
        return anyLearningOrDone ? STATUS_LEARNING : STATUS_TODO;
    }

    private LocalDateTime aggregateCompletedAt(List<GovKnowledgeNodeVO> childVos) {
        return childVos.stream()
                .map(GovKnowledgeNodeVO::getCompletedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private GovKnowledgeProgressVO buildProgressVo(Long userId, Long knowledgeId, String status) {
        return buildProgressVo(userId, knowledgeId, status, null);
    }

    private GovKnowledgeProgressVO buildProgressVo(Long userId, Long knowledgeId, String status, LocalDateTime completedAt) {
        return GovKnowledgeProgressVO.builder()
                .userId(userId)
                .knowledgeId(knowledgeId)
                .status(status)
                .completedAt(completedAt)
                .build();
    }

    private String normalizeSubject(String subject) {
        return GovKnowledgeSubject.resolve(subject).getLabel();
    }

    private String resolveSubject(String subject) {
        return normalizeSubject(subject);
    }

    private String resolveNodeType(String nodeType) {
        if (!StringUtils.hasText(nodeType)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "节点类型不能为空");
        }
        String normalized = nodeType.trim().toUpperCase(Locale.ROOT);
        try {
            return GovKnowledgeNodeType.valueOf(normalized).name();
        } catch (IllegalArgumentException ex) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "节点类型不正确");
        }
    }

    private String normalizeProgressStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "学习状态不能为空");
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!STATUS_TODO.equals(normalized) && !STATUS_LEARNING.equals(normalized) && !STATUS_DONE.equals(normalized)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "学习状态不正确");
        }
        return normalized;
    }

    private void validateNodeShape(String nodeType, Long parentId, String contentMd, String subject) {
        if (GovKnowledgeNodeType.CHAPTER.name().equals(nodeType)) {
            if (parentId != null && parentId > 0) {
                throw new BaseException(HttpStatus.BAD_REQUEST, "章节只能作为根节点");
            }
            return;
        }
        if (GovKnowledgeNodeType.POINT.name().equals(nodeType)) {
            if (parentId == null || parentId <= 0) {
                throw new BaseException(HttpStatus.BAD_REQUEST, "知识点必须挂在章节下");
            }
            GovKnowledgeNodePO parent = requireNodeById(parentId);
            if (!Objects.equals(parent.getSubject(), subject)) {
                throw new BaseException(HttpStatus.BAD_REQUEST, "知识点必须属于同一科目");
            }
            if (!GovKnowledgeNodeType.CHAPTER.name().equals(parent.getNodeType())) {
                throw new BaseException(HttpStatus.BAD_REQUEST, "知识点只能挂在章节下");
            }
            if (!Objects.equals(parent.getStatus(), 1)) {
                throw new BaseException(HttpStatus.BAD_REQUEST, "知识点必须挂在已启用的章节下");
            }
            if (!StringUtils.hasText(contentMd)) {
                throw new BaseException(HttpStatus.BAD_REQUEST, "知识点正文不能为空");
            }
            return;
        }
        throw new BaseException(HttpStatus.BAD_REQUEST, "节点类型不正确");
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null ? 0L : Math.max(0L, parentId);
    }

    private String normalizeRequiredText(String value, String errorMessage, int maxLength) {
        if (!StringUtils.hasText(value)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, errorMessage);
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new BaseException(HttpStatus.BAD_REQUEST, errorMessage);
        }
        return normalized;
    }

    private String normalizeOptionalText(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "内容长度超出限制");
        }
        return normalized;
    }

    private GovKnowledgeNodePO requireNodeById(Long nodeId) {
        if (nodeId == null || nodeId <= 0) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "节点ID不能为空");
        }
        GovKnowledgeNodePO node = nodeRepository.selectNodeById(nodeId);
        if (node == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "节点不存在");
        }
        return node;
    }

    private GovKnowledgeNodePO requireVisibleNode(Long nodeId) {
        if (nodeId == null || nodeId <= 0) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "节点ID不能为空");
        }
        GovKnowledgeNodePO node = nodeRepository.selectVisibleNodeById(nodeId);
        if (node == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "节点不存在");
        }
        return node;
    }

    private UserInfoDTO currentUser() {
        UserInfoDTO user = SecurityUtil.getLoginUser();
        if (user == null || user.getUserId() == null) {
            throw new BaseException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return user;
    }

    private UserInfoDTO requireAdmin() {
        UserInfoDTO user = currentUser();
        if (user.getRoleCode() == null || !ADMIN_ROLES.contains(user.getRoleCode())) {
            throw new BaseException(HttpStatus.FORBIDDEN, "仅管理员可以维护知识库");
        }
        return user;
    }

    private int nextSortOrder(String subject, Long parentId) {
        return nodeRepository.selectExistingNodesBySubject(subject).stream()
                .filter(node -> Objects.equals(defaultParent(node.getParentId()), defaultParent(parentId)))
                .map(GovKnowledgeNodePO::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(-1) + 1;
    }

    private GovKnowledgeNodeVO buildAdminNodeView(Long nodeId) {
        GovKnowledgeNodePO node = requireNodeById(nodeId);
        List<GovKnowledgeNodePO> nodes = nodeRepository.selectExistingNodesBySubject(node.getSubject());
        Set<Long> allIds = nodes.stream().map(GovKnowledgeNodePO::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, List<GovKnowledgeNodePO>> childrenByParent = groupChildrenByParent(nodes, allIds);
        return buildNodeVo(node, childrenByParent, Map.of(), allIds, true);
    }

    private GovKnowledgeCompareVO buildCompareVo(GovKnowledgeComparePO compare) {
        if (compare == null) {
            return null;
        }
        return GovKnowledgeCompareVO.builder()
                .id(compare.getId())
                .knowledgeId(compare.getKnowledgeId())
                .title(compare.getTitle())
                .contentMd(compare.getContentMd())
                .sortOrder(defaultInt(compare.getSortOrder()))
                .status(defaultInt(compare.getStatus()))
                .createdAt(compare.getCreateTime())
                .updatedAt(compare.getUpdateTime())
                .build();
    }

    private void validateCompareRequest(GovKnowledgeCompareSaveRequest request) {
        if (request == null) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "辨析内容不能为空");
        }
    }

    private int nextCompareSortOrder(Long knowledgeId) {
        return compareRepository.selectCompareByKnowledgeId(knowledgeId).stream()
                .map(GovKnowledgeComparePO::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(-1) + 1;
    }

    private long defaultParent(Long parentId) {
        return parentId == null ? 0L : parentId;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private record PageBounds(int pageNum, int pageSize) {
        private com.edu.common.PageQuery pageQuery() {
            return new com.edu.common.PageQuery(pageNum, pageSize);
        }
    }
}
