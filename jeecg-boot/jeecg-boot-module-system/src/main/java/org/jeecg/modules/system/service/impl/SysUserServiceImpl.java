package org.jeecg.modules.system.service.impl;

import java.util.Date;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.constant.CacheConstant;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.system.api.ISysBaseAPI;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.system.vo.SysUserCacheInfo;
import org.jeecg.common.util.*;
import org.jeecg.modules.base.service.BaseCommonService;
import org.jeecg.modules.event.SendActivationMailEvent;
import org.jeecg.modules.system.constant.UserConstants;
import org.jeecg.modules.system.dto.UserRegisterDTO;
import org.jeecg.modules.system.entity.*;
import org.jeecg.modules.system.mapper.*;
import org.jeecg.modules.system.model.SysUserSysDepartModel;
import org.jeecg.modules.system.service.ISysUserService;
import org.jeecg.modules.system.util.CommonUtil;
import org.jeecg.modules.system.util.MailClient;
import org.jeecg.modules.system.util.SecurityUtil;
import org.jeecg.modules.system.vo.SysUserDepVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;


import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * ????????? ???????????????
 * </p>
 *
 * @Author: scott
 * @Date: 2018-12-20
 */
@Service
@Slf4j
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysPermissionMapper sysPermissionMapper;
    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;
    @Autowired
    private SysUserDepartMapper sysUserDepartMapper;
    @Autowired
    private ISysBaseAPI sysBaseAPI;
    @Autowired
    private SysDepartMapper sysDepartMapper;
    @Autowired
    private SysRoleMapper sysRoleMapper;
    @Autowired
    private SysDepartRoleUserMapper departRoleUserMapper;
    @Autowired
    private SysDepartRoleMapper sysDepartRoleMapper;
    @Resource
    private BaseCommonService baseCommonService;

    @Resource
    private MailClient mailClient;

    @Resource
    private RedisUtil redisUtil;

    /**
     * ??????
     */
    @Value("${community.path.domain}")
    private String domain;

    /**
     * ?????????
     */
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Resource
    private ApplicationContext applicationContext;


    @Override
    @CacheEvict(value = {CacheConstant.SYS_USERS_CACHE}, allEntries = true)
    public Result<?> resetPassword(String username, String oldpassword, String newpassword, String confirmpassword) {
        SysUser user = userMapper.getUserByName(username);
        String passwordEncode = PasswordUtil.encrypt(username, oldpassword, user.getSalt());
        if (!user.getPassword().equals(passwordEncode)) {
            return Result.error("?????????????????????!");
        }
        if (oConvertUtils.isEmpty(newpassword)) {
            return Result.error("????????????????????????!");
        }
        if (!newpassword.equals(confirmpassword)) {
            return Result.error("???????????????????????????!");
        }
        String password = PasswordUtil.encrypt(username, newpassword, user.getSalt());
        this.userMapper.update(new SysUser().setPassword(password), new LambdaQueryWrapper<SysUser>().eq(SysUser::getId, user.getId()));
        return Result.ok("??????????????????!");
    }

    @Override
    @CacheEvict(value = {CacheConstant.SYS_USERS_CACHE}, allEntries = true)
    public Result<?> changePassword(SysUser sysUser) {
        String salt = oConvertUtils.randomGen(8);
        sysUser.setSalt(salt);
        String password = sysUser.getPassword();
        String passwordEncode = PasswordUtil.encrypt(sysUser.getUsername(), password, salt);
        sysUser.setPassword(passwordEncode);
        this.userMapper.updateById(sysUser);
        return Result.ok("??????????????????!");
    }

    @Override
    @CacheEvict(value = {CacheConstant.SYS_USERS_CACHE}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(String userId) {
        //1.????????????
        this.removeById(userId);
        return false;
    }

    @Override
    @CacheEvict(value = {CacheConstant.SYS_USERS_CACHE}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBatchUsers(String userIds) {
        //1.????????????
        this.removeByIds(Arrays.asList(userIds.split(",")));
        return false;
    }

    @Override
    public SysUser getUserByName(String username) {
        return userMapper.getUserByName(username);
    }


    @Override
    @Transactional
    public void addUserWithRole(SysUser user, String roles) {
        this.save(user);
        if (oConvertUtils.isNotEmpty(roles)) {
            String[] arr = roles.split(",");
            for (String roleId : arr) {
                SysUserRole userRole = new SysUserRole(user.getId(), roleId);
                sysUserRoleMapper.insert(userRole);
            }
        }
    }

    @Override
    @CacheEvict(value = {CacheConstant.SYS_USERS_CACHE}, allEntries = true)
    @Transactional
    public void editUserWithRole(SysUser user, String roles) {
        this.updateById(user);
        //????????????
        sysUserRoleMapper.delete(new QueryWrapper<SysUserRole>().lambda().eq(SysUserRole::getUserId, user.getId()));
        if (oConvertUtils.isNotEmpty(roles)) {
            String[] arr = roles.split(",");
            for (String roleId : arr) {
                SysUserRole userRole = new SysUserRole(user.getId(), roleId);
                sysUserRoleMapper.insert(userRole);
            }
        }
    }


    @Override
    public List<String> getRole(String username) {
        return sysUserRoleMapper.getRoleByUserName(username);
    }

    /**
     * ???????????????????????????????????????
     *
     * @param username ?????????
     * @return ????????????
     */
    @Override
    public Set<String> getUserRolesSet(String username) {
        // ?????????????????????????????????
        List<String> roles = sysUserRoleMapper.getRoleByUserName(username);
        log.info("-------??????????????????????????????????????????Rules------username??? " + username + ",Roles size: " + (roles == null ? 0 : roles.size()));
        return new HashSet<>(roles);
    }

    /**
     * ???????????????????????????????????????
     *
     * @param username ?????????
     * @return ????????????
     */
    @Override
    public Set<String> getUserPermissionsSet(String username) {
        Set<String> permissionSet = new HashSet<>();
        List<SysPermission> permissionList = sysPermissionMapper.queryByUser(username);
        for (SysPermission po : permissionList) {
//			// TODO URL??????????????????
//			if (oConvertUtils.isNotEmpty(po.getUrl())) {
//				permissionSet.add(po.getUrl());
//			}
            if (oConvertUtils.isNotEmpty(po.getPerms())) {
                permissionSet.add(po.getPerms());
            }
        }
        log.info("-------??????????????????????????????????????????Perms------username??? " + username + ",Perms size: " + (permissionSet == null ? 0 : permissionSet.size()));
        return permissionSet;
    }

    @Override
    public SysUserCacheInfo getCacheUser(String username) {
        SysUserCacheInfo info = new SysUserCacheInfo();
        info.setOneDepart(true);
//		SysUser user = userMapper.getUserByName(username);
//		info.setSysUserCode(user.getUsername());
//		info.setSysUserName(user.getRealname());


        LoginUser user = sysBaseAPI.getUserByName(username);
        if (user != null) {
            info.setSysUserCode(user.getUsername());
            info.setSysUserName(user.getRealname());
            info.setSysOrgCode(user.getOrgCode());
        }

        //???????????????in??????
        List<SysDepart> list = sysDepartMapper.queryUserDeparts(user.getId());
        List<String> sysMultiOrgCode = new ArrayList<String>();
        if (list == null || list.size() == 0) {
            //?????????????????????
            //sysMultiOrgCode.add("0");
        } else if (list.size() == 1) {
            sysMultiOrgCode.add(list.get(0).getOrgCode());
        } else {
            info.setOneDepart(false);
            for (SysDepart dpt : list) {
                sysMultiOrgCode.add(dpt.getOrgCode());
            }
        }
        info.setSysMultiOrgCode(sysMultiOrgCode);

        return info;
    }

    // ????????????Id??????
    @Override
    public IPage<SysUser> getUserByDepId(Page<SysUser> page, String departId, String username) {
        return userMapper.getUserByDepId(page, departId, username);
    }

    @Override
    public IPage<SysUser> getUserByDepIds(Page<SysUser> page, List<String> departIds, String username) {
        return userMapper.getUserByDepIds(page, departIds, username);
    }

    @Override
    public Map<String, String> getDepNamesByUserIds(List<String> userIds) {
        List<SysUserDepVo> list = this.baseMapper.getDepNamesByUserIds(userIds);

        Map<String, String> res = new HashMap<String, String>();
        list.forEach(item -> {
                    if (res.get(item.getUserId()) == null) {
                        res.put(item.getUserId(), item.getDepartName());
                    } else {
                        res.put(item.getUserId(), res.get(item.getUserId()) + "," + item.getDepartName());
                    }
                }
        );
        return res;
    }

    @Override
    public IPage<SysUser> getUserByDepartIdAndQueryWrapper(Page<SysUser> page, String departId, QueryWrapper<SysUser> queryWrapper) {
        LambdaQueryWrapper<SysUser> lambdaQueryWrapper = queryWrapper.lambda();

        lambdaQueryWrapper.eq(SysUser::getDelFlag, CommonConstant.DEL_FLAG_0);
        lambdaQueryWrapper.inSql(SysUser::getId, "SELECT user_id FROM sys_user_depart WHERE dep_id = '" + departId + "'");

        return userMapper.selectPage(page, lambdaQueryWrapper);
    }

    @Override
    public IPage<SysUserSysDepartModel> queryUserByOrgCode(String orgCode, SysUser userParams, IPage page) {
        List<SysUserSysDepartModel> list = baseMapper.getUserByOrgCode(page, orgCode, userParams);
        Integer total = baseMapper.getUserByOrgCodeTotal(orgCode, userParams);

        IPage<SysUserSysDepartModel> result = new Page<>(page.getCurrent(), page.getSize(), total);
        result.setRecords(list);

        return result;
    }

    // ????????????Id??????
    @Override
    public IPage<SysUser> getUserByRoleId(Page<SysUser> page, String roleId, String username) {
        return userMapper.getUserByRoleId(page, roleId, username);
    }


    @Override
    @CacheEvict(value = {CacheConstant.SYS_USERS_CACHE}, key = "#username")
    public void updateUserDepart(String username, String orgCode) {
        baseMapper.updateUserDepart(username, orgCode);
    }


    @Override
    public SysUser getUserByPhone(String phone) {
        return userMapper.getUserByPhone(phone);
    }


    @Override
    public SysUser getUserByEmail(String email) {
        return userMapper.getUserByEmail(email);
    }

    @Override
    @Transactional
    public void addUserWithDepart(SysUser user, String selectedParts) {
//		this.save(user);  //?????????????????????????????????????????????
        if (oConvertUtils.isNotEmpty(selectedParts)) {
            String[] arr = selectedParts.split(",");
            for (String deaprtId : arr) {
                SysUserDepart userDeaprt = new SysUserDepart(user.getId(), deaprtId);
                sysUserDepartMapper.insert(userDeaprt);
            }
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {CacheConstant.SYS_USERS_CACHE}, allEntries = true)
    public void editUserWithDepart(SysUser user, String departs) {
        this.updateById(user);  //?????????????????????????????????????????????????????????????????????
        String[] arr = {};
        if (oConvertUtils.isNotEmpty(departs)) {
            arr = departs.split(",");
        }
        //?????????????????????
        List<SysUserDepart> userDepartList = sysUserDepartMapper.selectList(new QueryWrapper<SysUserDepart>().lambda().eq(SysUserDepart::getUserId, user.getId()));
        if (userDepartList != null && userDepartList.size() > 0) {
            for (SysUserDepart depart : userDepartList) {
                //???????????????????????????????????????????????????
                if (!Arrays.asList(arr).contains(depart.getDepId())) {
                    List<SysDepartRole> sysDepartRoleList = sysDepartRoleMapper.selectList(
                            new QueryWrapper<SysDepartRole>().lambda().eq(SysDepartRole::getDepartId, depart.getDepId()));
                    List<String> roleIds = sysDepartRoleList.stream().map(SysDepartRole::getId).collect(Collectors.toList());
                    if (roleIds != null && roleIds.size() > 0) {
                        departRoleUserMapper.delete(new QueryWrapper<SysDepartRoleUser>().lambda().eq(SysDepartRoleUser::getUserId, user.getId())
                                .in(SysDepartRoleUser::getDroleId, roleIds));
                    }
                }
            }
        }
        //????????????
        sysUserDepartMapper.delete(new QueryWrapper<SysUserDepart>().lambda().eq(SysUserDepart::getUserId, user.getId()));
        if (oConvertUtils.isNotEmpty(departs)) {
            for (String departId : arr) {
                SysUserDepart userDepart = new SysUserDepart(user.getId(), departId);
                sysUserDepartMapper.insert(userDepart);
            }
        }
    }


    /**
     * ????????????????????????
     *
     * @param sysUser
     * @return
     */
    @Override
    public Result<?> checkUserIsEffective(SysUser sysUser) {
        Result<?> result = new Result<Object>();
        //??????1????????????????????????????????????????????????
        if (sysUser == null) {
            result.error500("??????????????????????????????");
            baseCommonService.addLog("???????????????????????????????????????", CommonConstant.LOG_TYPE_1, null);
            return result;
        }
        //??????2????????????????????????????????????????????????
        //update-begin---author:??????   Date:20200601  for???if???????????????falsebug------------
        if (CommonConstant.DEL_FLAG_1.equals(sysUser.getDelFlag())) {
            //update-end---author:??????   Date:20200601  for???if???????????????falsebug------------
            baseCommonService.addLog("??????????????????????????????:" + sysUser.getUsername() + "????????????", CommonConstant.LOG_TYPE_1, null);
            result.error500("??????????????????");
            return result;
        }
        //??????3????????????????????????????????????????????????
        if (CommonConstant.USER_FREEZE.equals(sysUser.getStatus())) {
            baseCommonService.addLog("??????????????????????????????:" + sysUser.getUsername() + "????????????", CommonConstant.LOG_TYPE_1, null);
            result.error500("??????????????????");
            return result;
        }
        return result;
    }

    @Override
    public List<SysUser> queryLogicDeleted() {
        return this.queryLogicDeleted(null);
    }

    @Override
    public List<SysUser> queryLogicDeleted(LambdaQueryWrapper<SysUser> wrapper) {
        if (wrapper == null) {
            wrapper = new LambdaQueryWrapper<>();
        }
        wrapper.eq(SysUser::getDelFlag, CommonConstant.DEL_FLAG_1);
        return userMapper.selectLogicDeleted(wrapper);
    }

    @Override
    public boolean revertLogicDeleted(List<String> userIds, SysUser updateEntity) {
        String ids = String.format("'%s'", String.join("','", userIds));
        return userMapper.revertLogicDeleted(ids, updateEntity) > 0;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateNullPhoneEmail() {
        userMapper.updateNullByEmptyString("email");
        userMapper.updateNullByEmptyString("phone");
        return true;
    }


    @Override
    public List<SysUser> queryByDepIds(List<String> departIds, String username) {
        return userMapper.queryByDepIds(departIds, username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUser(SysUser user, String selectedRoles, String selectedDeparts) {
        //step.1 ????????????
        this.save(user);
        //step.2 ????????????
        if (oConvertUtils.isNotEmpty(selectedRoles)) {
            String[] arr = selectedRoles.split(",");
            for (String roleId : arr) {
                SysUserRole userRole = new SysUserRole(user.getId(), roleId);
                sysUserRoleMapper.insert(userRole);
            }
        }
        //step.3 ??????????????????
        if (oConvertUtils.isNotEmpty(selectedDeparts)) {
            String[] arr = selectedDeparts.split(",");
            for (String deaprtId : arr) {
                SysUserDepart userDeaprt = new SysUserDepart(user.getId(), deaprtId);
                sysUserDepartMapper.insert(userDeaprt);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {CacheConstant.SYS_USERS_CACHE}, allEntries = true)
    public void editUser(SysUser user, String roles, String departs) {
        //step.1 ????????????????????????
        this.updateById(user);
        //step.2 ????????????
        //?????????????????? ????????????
        sysUserRoleMapper.delete(new QueryWrapper<SysUserRole>().lambda().eq(SysUserRole::getUserId, user.getId()));
        if (oConvertUtils.isNotEmpty(roles)) {
            String[] arr = roles.split(",");
            for (String roleId : arr) {
                SysUserRole userRole = new SysUserRole(user.getId(), roleId);
                sysUserRoleMapper.insert(userRole);
            }
        }

        //step.3 ????????????
        String[] arr = {};
        if (oConvertUtils.isNotEmpty(departs)) {
            arr = departs.split(",");
        }
        //?????????????????????
        List<SysUserDepart> userDepartList = sysUserDepartMapper.selectList(new QueryWrapper<SysUserDepart>().lambda().eq(SysUserDepart::getUserId, user.getId()));
        if (userDepartList != null && userDepartList.size() > 0) {
            for (SysUserDepart depart : userDepartList) {
                //???????????????????????????????????????????????????
                if (!Arrays.asList(arr).contains(depart.getDepId())) {
                    List<SysDepartRole> sysDepartRoleList = sysDepartRoleMapper.selectList(
                            new QueryWrapper<SysDepartRole>().lambda().eq(SysDepartRole::getDepartId, depart.getDepId()));
                    List<String> roleIds = sysDepartRoleList.stream().map(SysDepartRole::getId).collect(Collectors.toList());
                    if (roleIds != null && roleIds.size() > 0) {
                        departRoleUserMapper.delete(new QueryWrapper<SysDepartRoleUser>().lambda().eq(SysDepartRoleUser::getUserId, user.getId())
                                .in(SysDepartRoleUser::getDroleId, roleIds));
                    }
                }
            }
        }
        //????????????
        sysUserDepartMapper.delete(new QueryWrapper<SysUserDepart>().lambda().eq(SysUserDepart::getUserId, user.getId()));
        if (oConvertUtils.isNotEmpty(departs)) {
            for (String departId : arr) {
                SysUserDepart userDepart = new SysUserDepart(user.getId(), departId);
                sysUserDepartMapper.insert(userDepart);
            }
        }
        //step.4 ????????????????????????
        // ??????????????????????????????????????? null
        userMapper.updateNullByEmptyString("email");
        userMapper.updateNullByEmptyString("phone");

    }

    @Override
    public List<String> userIdToUsername(Collection<String> userIdList) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(SysUser::getId, userIdList);
        List<SysUser> userList = super.list(queryWrapper);
        return userList.stream().map(SysUser::getUsername).collect(Collectors.toList());
    }

    //???????????????????????????
    @Override
    @Transactional
    public Result<JSONObject> emailRegister(UserRegisterDTO userRegisterDTO) {
        Result<JSONObject> result = new Result<>();
        //??????????????????
        checkRegisterInfo(userRegisterDTO, result);


        //??????????????????
        registerUser(userRegisterDTO);
        result.setMessage("???????????????");
        result.setSuccess(true);
        return result;
    }

    private void checkRegisterInfo(UserRegisterDTO userRegisterDTO, Result<JSONObject> result) {
        //??????????????????????????????
        checkUserName(userRegisterDTO, result);
        //???????????????????????????
        checkEmail(userRegisterDTO, result);
    }

    //????????????
    private void registerUser(UserRegisterDTO userRegisterDTO) {
        SysUser sysUser = null;
        try {
            //????????????
            sysUser = createUser(userRegisterDTO);
            saveActivationCodeInRedis(sysUser);
        }catch (Exception e) {
            throw e;
        }
        publishEvent(sysUser);

        //???????????????@async??????????????????????????????????????????????????????spring????????????????????????????????????????????????
        //????????????
//        sendMessage(sysUser);

    }

    @Override
    public Boolean studentRoleInsert(String userid) {
        Boolean aBoolean = userMapper.studentRoleInsert(userid);
        return aBoolean;
    }


    //????????????
    private void publishEvent(SysUser sysUser) {
        Map<String,Object> map = new HashMap<>();
        String url = domain + contextPath + "/sys/user/activation/" + sysUser.getId() + "/" + sysUser.getActivationCode();
//		String activationCode = sysUser.getActivationCode();
        String username = sysUser.getUsername();

        String content = "????????? " + username + " ????????????????????????????????????????????????????????????";
        String turn = "<br/>";
        String htmlUrl = "<a href=" + url + ">????????????</a>";

        map.put("email",sysUser.getEmail());
        map.put("subject","??????????????????");
        map.put("content",content + turn + htmlUrl);
        applicationContext.publishEvent(new SendActivationMailEvent(map));
    }




    //???????????? ????????????????????????????????????
    public Boolean publishEvent1(SysUser sysUser,String captcha) {
        Map<String,Object> map = new HashMap<>();

        String username = sysUser.getUsername();

        String content = "????????? " + username + " ???????????????????????????";
        String turn = "<br/>";

        map.put("email",sysUser.getEmail());
        map.put("subject","??????????????????");
        map.put("content",content + turn + captcha);
        applicationContext.publishEvent(new SendActivationMailEvent(map));
        return true;

    }

    private void saveActivationCodeInRedis(SysUser sysUser) {
        String activationCode = sysUser.getActivationCode();
        //???????????????
        redisUtil.set(sysUser.getId(), activationCode, 600);
    }

    /**
     * ??????????????????
     *
     * @param sysUser
     */
    protected void sendMessage(SysUser sysUser) {
        String url = domain + contextPath + "/sys/user/activation/" + sysUser.getId() + "/" + sysUser.getActivationCode();
//		String activationCode = sysUser.getActivationCode();
        String username = sysUser.getUsername();

        String content = "????????? " + username + " ????????????????????????????????????????????????????????????";
        String turn = "<br/>";
        String htmlUrl = "<a href=" + url + ">????????????</a>";

        mailClient.sendMail(sysUser.getEmail(), "??????????????????", content + turn + htmlUrl);
    }

    //??????????????????
    private SysUser createUser(UserRegisterDTO userRegisterDTO) {
        SysUser sysUser = new SysUser();
        sysUser.setEmail(userRegisterDTO.getEmail());
        sysUser.setUsername(userRegisterDTO.getUsername());
        String salt = oConvertUtils.randomGen(8);
        String passwordEncode = PasswordUtil.encrypt(userRegisterDTO.getUsername(), userRegisterDTO.getPassword(), salt);
        //??????5???????????????????????????
        sysUser.setSalt(salt);
        //??????????????????
        sysUser.setPassword(passwordEncode);
        //????????????????????????????????????
        sysUser.setActivationCode(IdUtil.fastUUID());
        //??????????????????  ????????????????????????????????????
        sysUser.setAvatar(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        //?????????????????????????????????
        sysUser.setStatus(2);
        //????????????????????????
        sysUser.setCreateBy(UserConstants.ADMIN);
        sysUser.setCreateTime(new Date());
        //?????????
        sysUser.setUserIdentity(0);
        baseMapper.insert(sysUser);
        return sysUser;
    }

    /**
     * ????????????
     *
     * @param userRegisterDTO
     * @param result
     */
    private void checkEmail(UserRegisterDTO userRegisterDTO, Result<JSONObject> result) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper();
        wrapper.eq("email", userRegisterDTO.getEmail());
        SysUser sysUser = baseMapper.selectOne(wrapper);
        if (Objects.nonNull(sysUser)) {
            result.setMessage("??????????????????");
            result.setSuccess(false);
        }
    }

    /**
     * ???????????????
     *
     * @param userRegisterDTO
     * @param result
     */
    private void checkUserName(UserRegisterDTO userRegisterDTO, Result<JSONObject> result) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper();
        wrapper.eq("username", userRegisterDTO.getUsername());
        List<SysUser> sysUserList = baseMapper.selectList(wrapper);
        if (CollectionUtil.isNotEmpty(sysUserList)) {
            result.setMessage("?????????????????????");
            result.setSuccess(false);
        }
    }

    @Override
    public int activation(String userId, String activationCode) {
        SysUser sysUser = new SysUser();
        checkActivation(userId);
        sysUser = checkUser(userId, sysUser);
        return activation(sysUser, activationCode);
    }

    private int activation(SysUser sysUser, String activationCode) {
        if (Objects.nonNull(sysUser.getStatus()) & sysUser.getStatus() == 1) {
            return UserConstants.ACTIVATION_REPEAT;
        }else if (sysUser.getActivationCode().equals(activationCode)) {
            baseMapper.activationEmail(sysUser);
//			baseMapper.update(sysUser,new UpdateWrapper<SysUser>()
//					.lambda()
//					.set(SysUser::getStatus, 1)
//					.set(SysUser::getDelFlag, 0)
//					.eq(SysUser::getId,sysUser.getId()) );
            baseMapper.activationEmail(sysUser);
            return UserConstants.ACTIVATION_SUCCESS;
        }
        return UserConstants.ACTIVATION_FAILURE;
    }

    private SysUser checkUser(String userId, SysUser sysUser) {
        sysUser = baseMapper.selectByUserId(userId);
        Objects.requireNonNull(sysUser, "??????????????????");
        return sysUser;
    }

    private void checkActivation(String userId) {
        Object redisActivationCode = redisUtil.get(userId);
        Objects.requireNonNull(redisActivationCode, "?????????????????????");
    }

    @Override
    public SysUser findByEmail(String email) {
        SysUser sysUser = baseMapper.findUserByEmail(email);
        return sysUser;
    }

    @Override
    public boolean removeLogicDeleted(List<String> userIds) {
        return false;
    }

}
