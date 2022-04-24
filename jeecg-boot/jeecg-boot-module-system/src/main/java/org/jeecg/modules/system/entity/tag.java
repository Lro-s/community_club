package org.jeecg.modules.system.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @Description: TODO
 * @author: scott
 * @date: 2022年03月09日 16:09
 */
@Data //自动生成getset方法
@Accessors(chain = true)
public class tag implements Serializable {


    private String tag;

    private Integer shuliang;
}
