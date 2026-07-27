package com.edu.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_face_compare_record")
public class AiFaceCompareRecordPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("user_name")
    private String userName;

    @TableField("profile_id")
    private Long profileId;

    @TableField("profile_image_url")
    private String profileImageUrl;

    @TableField("compare_image_url")
    private String compareImageUrl;

    @TableField("compare_image_object")
    private String compareImageObject;

    @TableField("score")
    private Double score;

    @TableField("threshold")
    private Double threshold;

    @TableField("matched")
    private Integer matched;

    @TableField("provider")
    private String provider;

    @TableField("request_id")
    private String requestId;

    @TableField("raw_result_json")
    private String rawResultJson;

    @TableField("create_by")
    private Long createBy;

    @TableField("update_by")
    private Long updateBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("deleted")
    private Integer deleted;
}
