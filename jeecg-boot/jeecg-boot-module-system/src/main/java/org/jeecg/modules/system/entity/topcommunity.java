package org.jeecg.modules.system.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @Description: TODO
 * @author: scott
 * @date: 2022年04月21日 2:09
 */
@Data //自动生成getset方法
@Accessors(chain = true)
public class topcommunity implements Serializable {

    private String communityname;

    private Integer total;
}
