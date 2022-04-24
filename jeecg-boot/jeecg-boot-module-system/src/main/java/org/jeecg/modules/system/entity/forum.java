package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.sql.Blob;
import java.util.Date;

/**
 * @Description: TODO
 * @author: scott
 * @date: 2022年03月07日 22:30
 */
@Data //自动生成getset方法
@Accessors(chain = true)
@TableName(value = "forum")//指定表名
public class forum implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String createby;

    private String forumcode;

    private Blob content;

    private String img;

    private Date createTime;

    private Date updateTime;

    private String tag;

    private String processing_state;


}
