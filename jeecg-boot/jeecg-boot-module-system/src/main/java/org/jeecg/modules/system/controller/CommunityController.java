package org.jeecg.modules.system.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.sun.org.apache.xpath.internal.operations.Bool;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.entity.*;
import org.jeecg.modules.system.service.CommunityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.sql.SQLException;
import java.util.List;

/**
 * @Description: TODO
 * @author: scott
 * @date: 2022年02月28日 15:35
 */

@Slf4j
@RestController
@RequestMapping("/community")
public class CommunityController {


    @Resource
    public CommunityService communityService;

    /*
    找社团信息的
     */
    @RequestMapping(value = "/findcommunityinfo", method = RequestMethod.GET)
    public Result<List<Community>> findinfo() {
        Result<List<Community>> result = new Result<List<Community>>();
        List<Community> communityList = communityService.findAllinfo();
        if (CollectionUtil.isEmpty(communityList)) {
            result.error500("未找到对应实体");
        } else {
            result.setResult(communityList);
            result.setSuccess(true);
        }
        return result;
    }

    /*
    找博客的标签数的
     */

    @RequestMapping(value = "/findtaginfo", method = RequestMethod.GET)
    public Result<List<tag>> findtaginfo() {
        Result<List<tag>> result = new Result<List<tag>>();
        List<tag> tagList = communityService.findtagInfo();
        if (CollectionUtil.isEmpty(tagList)) {
            result.error500("未找到对应实体");
        } else {
            result.setResult(tagList);
            result.setSuccess(true);
        }
        return result;
    }

    @RequestMapping(value = "/findgradeinfo", method = RequestMethod.GET)
    public Result<List<gradeinfo>> findgradeinfo() {
        Result<List<gradeinfo>> result = new Result<List<gradeinfo>>();
        List<gradeinfo> gradeList = communityService.findgradeInfo();
        if (CollectionUtil.isEmpty(gradeList)) {
            result.error500("未找到对应实体");
        } else {
            result.setResult(gradeList);
            result.setSuccess(true);
        }
        return result;
    }

    /**
    通过用户名去找是否有申请加入社团
     */

    @RequestMapping(value = "/findusernameinapplication", method = RequestMethod.POST)
    public Result<List<communityname>> findusernameinapplication(@RequestParam("usercode") String usercode) {
        System.out.println("usercode" + usercode);
        Result<List<communityname>> result = new Result<List<communityname>>();
        List<communityname> communitynameList = communityService.findusernameinapplication(usercode);
        if (CollectionUtil.isEmpty(communitynameList)) {
            result.setSuccess(false);
        } else {
            result.setResult(communitynameList);
            result.setSuccess(true);
        }
        return result;
    }

    @RequestMapping(value = "/topcommunityname", method = RequestMethod.GET)
    public Result<List<topcommunity>> topcommunityname() {
        Result<List<topcommunity>> result = new Result<List<topcommunity>>();
        List<topcommunity> topcommunityList = communityService.topcommunityname();
        if (CollectionUtil.isEmpty(topcommunityList)) {
            result.error500("未找到对应实体");
        } else {
            result.setResult(topcommunityList);
            result.setSuccess(true);
        }
        return result;
    }

    @RequestMapping(value = "/findcommunitynamefromsysuser", method = RequestMethod.POST)
    public String findcommunitynamefromsysuser(@RequestParam("usercode") String usercode){
        String community_name = communityService.findcommunitynamefromsysuser(usercode);
        return community_name;
    }




}
