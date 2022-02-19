package org.jeecg.modules.system.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/**
 * @Author xiaomai
 * @Description
 * @Date 2022/2/14 16:44
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRegisterDTO {

    /**
     * 邮箱地址
     */
    @NotNull(message = "邮箱不能为空！")
    private String email;

    /**
     * 用户名
     */
    @NotNull(message = "用户名不能为空！")
    private String username;

    /**
     * 密码
     */
    @NotNull(message = "密码不能为空")
    private String password;

    /**
     * 盐值
     */
    @JsonIgnore
    private String salt;

    /**
     * 验证码
     */
    private String activationCode;

}
