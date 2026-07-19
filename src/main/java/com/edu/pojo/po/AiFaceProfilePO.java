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
@TableName("ai_face_profile")
public class AiFaceProfilePO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("user_name")
    private String userName;

    @TableField("face_image_object")
    private String faceImageObject;

    @TableField("face_image_url")
    private String faceImageUrl;

    @TableField("face_image_name")
    private String faceImageName;

    @TableField("provider")
    private String provider;

    @TableField("face_model_version")
    private String faceModelVersion;

    @TableField("compare_count")
    private Integer compareCount;

    @TableField("last_compare_score")
    private Double lastCompareScore;

    @TableField("last_compare_time")
    private LocalDateTime lastCompareTime;

    @TableField("status")
    private Integer status;

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

    @TableField("ext_json")
    private String extJson;
}
