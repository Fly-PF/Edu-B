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

/**
 * 系统用户表PO
 *
 * @author Fly
 * @since 2026-03-20 11:46
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "sys_user")
public class SysUserPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "username")
    private String username;

    @TableField(value = "password")
    private String password;

    @TableField(value = "real_name")
    private String realName;

    @TableField(value = "phone")
    private String phone;

    @TableField(value = "email")
    private String email;

    @TableField(value = "avatar")
    private String avatar;

    @TableField(value = "user_type")
    private Integer userType;

    @TableField(value = "grade")
    private String grade;

    @TableField(value = "school")
    private String school;

    @TableField(value = "status")
    private Integer status;

    @TableField(value = "last_login_time")
    private LocalDateTime lastLoginTime;

    @TableField(value = "last_login_ip")
    private String lastLoginIp;

    @TableField(value = "create_by")
    private Long createBy;

    @TableField(value = "update_by")
    private Long updateBy;

    @TableField(value = "create_time")
    private LocalDateTime createTime;

    @TableField(value = "update_time")
    private LocalDateTime updateTime;

    @TableField(value = "deleted")
    private Integer deleted;

    @TableField(value = "ext_json")
    private String extJson;
}
