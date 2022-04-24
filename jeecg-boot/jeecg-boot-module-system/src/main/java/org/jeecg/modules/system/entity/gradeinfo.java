package org.jeecg.modules.system.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @Description: TODO
 * @author: scott
 * @date: 2022年03月12日 23:00
 */
@Data //自动生成getset方法
@Accessors(chain = true)
public class gradeinfo implements Serializable {

    private String sex;

    private String grade;

    private Integer shuliang;
}
