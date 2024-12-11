package com.muzi.easypicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum UserRoleEnum {
    USER("用户", "user"),
    VIP("VIP用户", "vip"),
    ADMIN("管理员", "admin");

    private final String role;
    private final String roleName;

    UserRoleEnum(String role, String roleName) {
        this.role = role;
        this.roleName = roleName;
    }

    public static UserRoleEnum getEnumByRoleName(String roleName) {
        if (ObjUtil.isEmpty(roleName)) {
            return null;
        }
        for (UserRoleEnum value : UserRoleEnum.values()) {
            if (value.getRoleName().equals(roleName)) {
                return value;
            }
        }
        return null;
    }


}
