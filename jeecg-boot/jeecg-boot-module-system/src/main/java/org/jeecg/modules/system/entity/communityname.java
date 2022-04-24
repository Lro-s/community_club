package org.jeecg.modules.system.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @Description: TODO
 * @author: scott
 * @date: 2022年04月21日 2:28
 */
@Data //自动生成getset方法
@Accessors(chain = true)
public class communityname implements Serializable {

    private String usercode;
    private String communityname;
}
