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
 * @date: 2022年02月28日 15:25
 */

@Data //自动生成getset方法
@Accessors(chain = true)
@TableName(value = "community")//指定表名
public class Community implements Serializable {
    /*
    id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    /*
    teacher_no
     */
    private String teacherno;

    /*
    create_time
     */
    private Date createTime;
    /*
    update_by
     */
    private String updateBy;
    /*
    update_time
     */
    private Date updateTime;
    /*
    community_code
     */
    private String communitycode;
    /*
    community_name
     */
    private String communityname;
    /*
    community_total
     */
    private Integer communitytotal;
    /*
    community_profile
     */
    private Blob community_profile;
    /*
    img
     */
    private String img;
    /*
    tag
     */
    private String tag;
    /*
    activity_total
     */
    private Integer activitytotal;
    /*
    community_leader
     */
    private String communityleader;


}
