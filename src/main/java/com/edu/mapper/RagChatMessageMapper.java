package com.edu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edu.pojo.po.RagChatMessagePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RagChatMessageMapper extends BaseMapper<RagChatMessagePO> {
    @Select("""
            SELECT EXISTS(
                SELECT 1 FROM rag_chat_message
                WHERE deleted = 0
                  AND role = 'user'
                  AND JSON_CONTAINS(JSON_EXTRACT(metadata, '$.qaImg'), JSON_OBJECT('fileUrl', #{objectName}))
            )
            """)
    boolean existsActiveQaImage(@Param("objectName") String objectName);
}
