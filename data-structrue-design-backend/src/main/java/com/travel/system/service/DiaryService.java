package com.travel.system.service;

import com.travel.system.model.Diary;
import com.travel.system.repository.DiarySearchRepository;
import com.travel.system.mapper.DiaryMapper;
import com.travel.system.search.DiaryDocument;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 日记服务类
 * <p>
 * 提供与旅游日记相关的业务逻辑，包括：
 * <ul>
 *     <li>日记列表查询（支持全文搜索）</li>
 *     <li>日记保存（双写 MySQL 和 Elasticsearch）</li>
 *     <li>全文搜索功能</li>
 * </ul>
 * </p>
 * <p>
 * 该服务实现了 MySQL 和 Elasticsearch 的双写策略：
 * <ul>
 *     <li>写入：同时写入 MySQL 和 ES，ES 失败不影响 MySQL 写入</li>
 *     <li>读取：优先使用 ES 进行全文搜索，ES 不可用时回退到 MySQL 模糊查询</li>
 * </ul>
 * </p>
 *
 * @author Travel System Team
 * @since 1.0.0
 * @see Diary 日记实体类
 * @see DiaryDocument Elasticsearch 日记文档类
 */
@Service
public class DiaryService {

    /**
     * 日记数据访问映射器（MyBatis）
     * 用于 MySQL 数据库的 CRUD 操作
     */
    private final DiaryMapper diaryRepository;

    /**
     * 日记搜索仓库（Elasticsearch）
     * 用于全文搜索，可为 null（当 ES 不可用时）
     */
    private final DiarySearchRepository diarySearchRepository;

    /**
     * 构造函数，注入依赖
     *
     * @param diaryRepository 日记数据访问映射器
     * @param diarySearchRepositoryProvider ES 搜索仓库提供者，使用 ObjectProvider 实现可选依赖
     */
    public DiaryService(DiaryMapper diaryRepository,
                        ObjectProvider<DiarySearchRepository> diarySearchRepositoryProvider) {
        this.diaryRepository = diaryRepository;
        this.diarySearchRepository = diarySearchRepositoryProvider.getIfAvailable();
    }

    /**
     * 查询日记列表
     * <p>
     * 搜索策略：
     * <ol>
     *     <li>无关键字时：返回全部日记（从 MySQL）</li>
     *     <li>有关键字且 ES 可用：使用 ES 进行全文搜索</li>
     *     <li>有关键字但 ES 不可用：回退到 MySQL 模糊查询（标题或内容包含关键字）</li>
     * </ol>
     * </p>
     *
     * @param keyword 可选的搜索关键字；若为 null 或空字符串，则返回全部日记
     * @return 符合条件的 Diary 列表
     */
    public List<Diary> list(String keyword) {
        // 无搜索关键字时返回全部
        if (keyword == null || keyword.isBlank()) {
            return diaryRepository.findAll();
        }

        // 优先使用 Elasticsearch 进行全文搜索
        if (diarySearchRepository != null) {
            try {
                List<DiaryDocument> docs = diarySearchRepository
                        .findByTitleContainingOrContentContaining(keyword, keyword);
                return docs.stream().map(this::toDiary).collect(Collectors.toList());
            } catch (Exception e) {
                // ES 搜索失败时回退到 MySQL
            }
        }

        // 使用 MySQL 进行模糊搜索
        return diaryRepository.findByTitleOrContentContainingIgnoreCase(keyword);
    }

    /**
     * 保存日记
     * <p>
     * 双写策略：
     * <ol>
     *     <li>首先保存到 MySQL 数据库</li>
     *     <li>如果 ES 可用，异步将日记转换为文档并保存到 ES</li>
     *     <li>ES 写入失败不影响主流程</li>
     * </ol>
     * </p>
     *
     * @param diary 要保存的日记对象
     * @return 保存后的日记对象（包含生成的 ID）
     */
    public Diary save(Diary diary) {
        // 保存到 MySQL
        Diary saved = diaryRepository.save(diary);
        
        // 同步到 Elasticsearch
        if (diarySearchRepository != null) {
            DiaryDocument doc = toDocument(saved);
            try {
                diarySearchRepository.save(doc);
            } catch (Exception ignored) {
                // ES 写入失败不影响主流程
            }
        }
        return saved;
    }

    /**
     * 全文搜索日记
     * <p>
     * 优先使用 Elasticsearch 进行全文搜索，
     * 如果 ES 不可用则回退到 MySQL 模糊查询
     * </p>
     *
     * @param keyword 搜索关键字
     * @return 匹配的日记文档列表
     */
    public List<DiaryDocument> fullTextSearch(String keyword) {
        if (diarySearchRepository != null) {
            return diarySearchRepository.findByTitleContainingOrContentContaining(keyword, keyword);
        }
        // ES 不可用时回退到 MySQL
        return diaryRepository.findByTitleOrContentContainingIgnoreCase(keyword).stream()
                .map(this::toDocument)
                .toList();
    }

    /**
     * DiaryDocument 转换为 Diary
     * <p>
     * 将 Elasticsearch 文档对象转换为实体对象
     * </p>
     *
     * @param doc ES 日记文档
     * @return 日记实体对象
     */
    private Diary toDiary(DiaryDocument doc) {
        Diary diary = new Diary();
        diary.setId(Long.valueOf(doc.getId()));
        diary.setTitle(doc.getTitle());
        diary.setContent(doc.getContent());
        return diary;
    }

    /**
     * Diary 转换为 DiaryDocument
     * <p>
     * 将实体对象转换为 Elasticsearch 文档对象
     * </p>
     *
     * @param diary 日记实体
     * @return ES 日记文档对象
     */
    private DiaryDocument toDocument(Diary diary) {
        DiaryDocument doc = new DiaryDocument();
        doc.setId(String.valueOf(diary.getId()));
        doc.setTitle(diary.getTitle());
        doc.setContent(diary.getContent());
        if (diary.getDestination() != null) {
            doc.setDestinationName(diary.getDestination().getName());
        }
        return doc;
    }
}
