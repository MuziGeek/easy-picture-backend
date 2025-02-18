package com.muzi.easypicturebackend.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.ShearCaptcha;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzi.easypicturebackend.constant.CommonValue;
import com.muzi.easypicturebackend.constant.CrawlerConstant;
import com.muzi.easypicturebackend.constant.RedisConstant;
import com.muzi.easypicturebackend.constant.UserConstant;
import com.muzi.easypicturebackend.esdao.EsPictureDao;
import com.muzi.easypicturebackend.esdao.EsPostDao;
import com.muzi.easypicturebackend.esdao.EsUserDao;
import com.muzi.easypicturebackend.exception.BusinessException;
import com.muzi.easypicturebackend.exception.ErrorCode;
import com.muzi.easypicturebackend.manager.CrawlerManager;
import com.muzi.easypicturebackend.manager.FileManager;
import com.muzi.easypicturebackend.manager.auth.StpKit;
import com.muzi.easypicturebackend.mapper.UserMapper;
import com.muzi.easypicturebackend.mapper.UserSignInRecordMapper;
import com.muzi.easypicturebackend.model.dto.file.UploadPictureResult;
import com.muzi.easypicturebackend.model.dto.user.UserModifyPassWord;
import com.muzi.easypicturebackend.model.dto.user.UserQueryRequest;
import com.muzi.easypicturebackend.model.entity.*;
import com.muzi.easypicturebackend.model.entity.es.EsUser;
import com.muzi.easypicturebackend.model.enums.UserRoleEnum;
import com.muzi.easypicturebackend.model.vo.LoginUserVO;
import com.muzi.easypicturebackend.model.vo.UserVO;
import com.muzi.easypicturebackend.service.PictureService;
import com.muzi.easypicturebackend.service.PostAttachmentService;
import com.muzi.easypicturebackend.service.PostService;
import com.muzi.easypicturebackend.service.UserService;
import com.muzi.easypicturebackend.utils.EmailSenderUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author 57242
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2024-12-10 22:43:05
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private UserMapper userMapper;
    @Resource
    private FileManager fileManager;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserSignInRecordMapper userSignInRecordMapper;

    @Resource
    private EsUserDao esUserDao;

    @Resource
    private EmailSenderUtil emailSenderUtil;

    @Resource
    @Lazy
    private CrawlerManager crawlerManager;

    @Resource
    @Lazy
    private PictureService pictureService;

    @Resource
    @Lazy
    private PostService postService;

    @Resource
    @Lazy
    private PostAttachmentService postAttachmentService;

    @Resource
    private EsPictureDao esPictureDao;

    @Resource
    private EsPostDao esPostDao;

    /**
     * 用户注册
     *
     * @param email       邮箱
     * @param userPassword 用户密码
     * @param checkPassword 校验密码
     * @param code         验证码
     * @return 用户注册成功后的ID
     */
    @Override
    public long userRegister(String email, String userPassword, String checkPassword, String code) {
        // 1. 校验
        if (StrUtil.hasBlank(email, userPassword, checkPassword, code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (!email.matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        // 校验验证码
        String verifyCodeKey = String.format("email:code:verify:register:%s", email);
        String correctCode = stringRedisTemplate.opsForValue().get(verifyCodeKey);
        if (correctCode == null || !correctCode.equals(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误或已过期");
        }

        synchronized (email.intern()) {
            // 检查邮箱是否已被注册
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("email", email);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱已被注册");
            }

            // 检查账号是否已被使用
            String userAccount = email.substring(0, email.indexOf("@")); // 使用邮箱前缀作为账号
            queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                // 如果账号已存在，则在后面加上随机数
                userAccount = userAccount + RandomUtil.randomNumbers(4);
            }

            // 2. 加密
            String encryptPassword = DigestUtil.md5Hex(CommonValue.DEFAULT_SALT + userPassword);
            // 3. 插入数据
            User user = new User();
            user.setUserAccount(userAccount);
            user.setEmail(email);
            user.setUserPassword(encryptPassword);
            user.setUserName(userAccount); // 使用账号作为默认用户名
            user.setUserRole(UserRoleEnum.USER.getRoleName());
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            // 删除验证码
            stringRedisTemplate.delete(verifyCodeKey);
            return user.getId();
        }
    }

    /**
     * 用户登录
     *
     * @param accountOrEmail 账号或邮箱
     * @param userPassword  用户密码
     * @param request      HttpServletRequest对象
     * @return 登录后的用户信息视图对象（LoginUserVO）
     */
    @Override
    public LoginUserVO userLogin(String accountOrEmail, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StrUtil.hasBlank(accountOrEmail, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码格式错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtil.md5Hex(CommonValue.DEFAULT_SALT + userPassword);
        // 3. 查询用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userPassword", encryptPassword)
                    .and(wrapper -> wrapper.eq("userAccount", accountOrEmail)
                                           .or()
                                           .eq("email", accountOrEmail));
        User user = this.getOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, accountOrEmail cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }

        // 4. 记录用户登录态
        // 先设置 Sa-Token 登录态，这样可以确保 Session 中有正确的权限信息
        StpKit.SPACE.login(user.getId());
        // 在 Sa-Token Session 中存入完整的用户信息
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);
        // 在 Spring Session 中也存入用户信息（为了兼容旧代码）
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);

        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 获取加密后的密码
     *
     * @param userPassword 用户密码
     * @return 加密后的密码
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        // 加盐，混淆密码
        return SecureUtil.md5(CommonValue.DEFAULT_SALT + userPassword);
    }

    // 以下是其他未修改的方法，省略了详细代码，可根据实际情况继续完善或优化

    @Override
    public User getLoginUser(HttpServletRequest request) {
        try {
            // 优先从 Sa-Token 中获取登录信息
            if (StpKit.SPACE.isLogin()) {
                User user = (User) StpKit.SPACE.getSession().get(UserConstant.USER_LOGIN_STATE);
                if (user != null) {
                    return user;
                }
            }

            // 如果 Sa-Token 中没有，尝试从 Spring Session 中获取（兼容旧代码）
            Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
            User currentUser = (User) userObj;
            if (currentUser == null || currentUser.getId() == null) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
            }

            // 从数据库中查询最新的用户信息
            Long userId = currentUser.getId();
            currentUser = this.getById(userId);
            if (currentUser == null) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
            }

            // 更新 Sa-Token 中的用户信息
            StpKit.SPACE.login(userId);
            StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, currentUser);

            return currentUser;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
    }

    /**
     * 获取脱敏类的用户信息
     *
     * @param user 用户
     * @return 脱敏后的用户信息
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public User isLogin(HttpServletRequest request) {
        // 判断是否已经登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        // 从数据库中查询（追求性能的话可以注释，直接返回上述结果）
        Long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            return null;
        }
        return currentUser;
    }

    /**
     * 获得脱敏后的用户信息
     *
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 获取脱敏后的用户列表
     *
     * @param userList
     * @return
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream()
                       .map(this::getUserVO)
                       .collect(Collectors.toList());
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 判断是否已经登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除 Spring Session 登录态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        // 移除 Sa-Token 登录态
        if (StpKit.SPACE.isLogin()) {
            StpKit.SPACE.logout();
        }
        return true;
    }

    /**
     * 异步删除用户相关数据
     */
    @Async("asyncExecutor")
    public void asyncDeleteUserData(Long userId) {
        try {
            // 1. 删除用户发布的图片
            QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
            pictureQueryWrapper.eq("userId", userId);
            List<Picture> pictureList = pictureService.list(pictureQueryWrapper);
            if (!pictureList.isEmpty()) {
                // 删除数据库记录
                pictureService.remove(pictureQueryWrapper);
                // 删除ES中的图片记录
                List<Long> pictureIds = pictureList.stream()
                                                   .map(Picture::getId)
                                                   .collect(Collectors.toList());
                esPictureDao.deleteAllById(pictureIds);
            }

            // 2. 删除用户发布的帖子
            QueryWrapper<Post> postQueryWrapper = new QueryWrapper<>();
            postQueryWrapper.eq("userId", userId);
            List<Post> postList = postService.list(postQueryWrapper);
            if (!postList.isEmpty()) {
                // 删除帖子附件
                List<Long> postIds = postList.stream()
                                             .map(Post::getId)
                                             .collect(Collectors.toList());
                QueryWrapper<PostAttachment> attachmentQueryWrapper = new QueryWrapper<>();
                attachmentQueryWrapper.in("postId", postIds);
                postAttachmentService.remove(attachmentQueryWrapper);
                // 删除帖子
                postService.remove(postQueryWrapper);
                // 删除ES中的帖子记录
                esPostDao.deleteAllById(postIds);
            }

            // 3. 删除用户数据
            this.removeById(userId);
            // 删除ES中的用户记录
            esUserDao.deleteById(userId);

            // 4. 清理相关缓存
            String userKey = String.format("user:ban:%d", userId);
            stringRedisTemplate.delete(userKey);

            log.info("用户相关数据删除完成, userId={}", userId);
        } catch (Exception e) {
            log.error("删除用户相关数据失败, userId={}", userId, e);
            // 这里不抛出异常，因为是异步操作，主流程已经完成
        }
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public boolean changePassword(UserModifyPassWord userModifyPassWord, HttpServletRequest request) {
        if(StrUtil.hasBlank(userModifyPassWord.getOldPassword(), userModifyPassWord.getNewPassword(), userModifyPassWord.getCheckPassword())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        if(!userModifyPassWord.getNewPassword().equals(userModifyPassWord.getCheckPassword())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        if(userModifyPassWord.getNewPassword().length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码长度不能小于8位");
        }
        //查询是否有这个用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", userModifyPassWord.getId());
        String encryptPassword = getEncryptPassword(userModifyPassWord.getOldPassword());
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        if(user == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "原密码错误");
        }

        user.setUserPassword(getEncryptPassword(userModifyPassWord.getNewPassword()));
        // 更新MySQL
        boolean result = userMapper.updateById(user) > 0;
        if (result) {
            // 更新ES
            EsUser esUser = new EsUser();
            BeanUtil.copyProperties(user, esUser);
            esUserDao.save(esUser);
        }
        return result;
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getRoleName().equals(user.getUserRole());
    }

    @Override
    public String updateUserAvatar(MultipartFile multipartFile, Long id, HttpServletRequest request) {
        //判断用户是否存在
        User user = userMapper.selectById(id);
        if(user == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        //判断用户是否登录
        User loginUser = getLoginUser(request);
        if(loginUser == null || !loginUser.getId().equals(id)){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }
        //判断文件是否为空
        if(multipartFile == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }
        //判断文件类型
        // 上传图片，得到图片信息
        // 按照用户 id 划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);
        //更新用户头像
        user.setUserAvatar(uploadPictureResult.getUrl());
        // 更新MySQL
        boolean result = userMapper.updateById(user) > 0;
        if (result) {
            // 更新ES
            EsUser esUser = new EsUser();
            BeanUtil.copyProperties(user, esUser);
            esUserDao.save(esUser);
        }
        return uploadPictureResult.getUrl();
    }

    @Override
    public Map<String, String> getCaptcha() {
        // 仅包含数字的字符集
        String characters = "0123456789";
        // 生成 4 位数字验证码
        RandomGenerator randomGenerator = new RandomGenerator(characters, 4);
        // 定义图片的显示大小，并创建验证码对象
        ShearCaptcha shearCaptcha = CaptchaUtil.createShearCaptcha(320, 100, 4, 4);
        shearCaptcha.setGenerator(randomGenerator);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        shearCaptcha.write(outputStream);
        byte[] captchaBytes = outputStream.toByteArray();
        String base64Captcha = Base64.getEncoder().encodeToString(captchaBytes);
        String captchaCode = shearCaptcha.getCode();

        // 使用 Hutool 的 MD5 加密
        String encryptedCaptcha = DigestUtil.md5Hex(captchaCode);

        // 将加密后的验证码和 Base64 编码的图片存储到 Redis 中，设置过期时间为 5 分钟（300 秒）
        stringRedisTemplate.opsForValue().set("captcha:" + encryptedCaptcha, captchaCode, 300, TimeUnit.SECONDS);

        Map<String, String> data = new HashMap<>();
        data.put("base64Captcha", base64Captcha);
        data.put("encryptedCaptcha", encryptedCaptcha);
        return data;
    }

    /**
     * 添加用户签到记录
     * @param userId 用户 id
     * @return 当前用户是否已签到成功
     */
    @Override
    public boolean addUserSignIn(long userId) {
        LocalDate date = LocalDate.now();
        int currentYear = date.getYear();
        String redisKey = RedisConstant.getUserSignInRedisKey(currentYear, userId);

        // 获取 Redis 的 BitMap
        RBitSet signInBitSet = redissonClient.getBitSet(redisKey);
        int dayOfYear = date.getDayOfYear();

        // 查询当天有没有签到
        if (!signInBitSet.get(dayOfYear)) {
            // 如果当前未签到，则设置Redis
            signInBitSet.set(dayOfYear, true);

            // 设置 Redis 键的过期时间到当年最后一天
            LocalDate endOfYear = LocalDate.of(currentYear, 12, 31);
            Duration timeUntilEndOfYear = Duration.between(
                    LocalDateTime.now(),
                    endOfYear.atTime(23, 59, 59)
            );
            redissonClient.getBucket(redisKey).expire(timeUntilEndOfYear);
        }

        return true;
    }

    /**
     * 获取用户某个年份的签到记录
     *
     * @param userId 用户 id
     * @param year   年份（为空表示当前年份）
     * @return 签到记录映射
     */
    @Override
    public List<Integer> getUserSignInRecord(long userId, Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }

        int currentYear = LocalDate.now().getYear();
        List<Integer> signInDays = new ArrayList<>();

        if (year != currentYear) {
            // 非当年数据直接从MySQL查询
            QueryWrapper<UserSignInRecord> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userId", userId)
                        .eq("year", year);

            UserSignInRecord record = userSignInRecordMapper.selectOne(queryWrapper);
            if (record != null && record.getSignInData() != null) {
                byte[] signInData = record.getSignInData();
                // 解析bitmap数据
                for (int day = 1; day <= 366; day++) {
                    int byteIndex = (day - 1) / 8;
                    int bitIndex = (day - 1) % 8;
                    if ((signInData[byteIndex] & (1 << bitIndex)) != 0) {
                        signInDays.add(day);
                    }
                }
            }
            return signInDays;
        }

        // 当年数据从Redis获取
        String redisKey = RedisConstant.getUserSignInRedisKey(year, userId);
        RBitSet signInBitSet = redissonClient.getBitSet(redisKey);

        // 如果Redis中没有数据，从MySQL加载
        if (!signInBitSet.isExists()) {
            QueryWrapper<UserSignInRecord> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userId", userId)
                        .eq("year", year);

            UserSignInRecord record = userSignInRecordMapper.selectOne(queryWrapper);
            if (record != null && record.getSignInData() != null) {
                byte[] signInData = record.getSignInData();
                // 将MySQL中的bitmap数据加载到Redis
                for (int day = 1; day <= 366; day++) {
                    int byteIndex = (day - 1) / 8;
                    int bitIndex = (day - 1) % 8;
                    if ((signInData[byteIndex] & (1 << bitIndex)) != 0) {
                        signInBitSet.set(day, true);
                    }
                }

                // 设置过期时间到年底
                LocalDate endOfYear = LocalDate.of(year, 12, 31);
                Duration timeUntilEndOfYear = Duration.between(
                        LocalDateTime.now(),
                        endOfYear.atTime(23, 59, 59)
                );
                redissonClient.getBucket(redisKey).expire(timeUntilEndOfYear);
            }
        }

        // 从Redis的bitmap中获取签到记录
        BitSet bitSet = signInBitSet.asBitSet();
        int index = bitSet.nextSetBit(0);
        while (index >= 0) {
            signInDays.add(index);
            index = bitSet.nextSetBit(index + 1);
        }

        return signInDays;
    }

    @Override
    public boolean validateCaptcha(String userInputCaptcha, String serververifycode) {
        if (userInputCaptcha!= null && serververifycode!= null) {
            // 使用Hutool对用户输入的验证码进行MD5加密
            String encryptedVerifycode = DigestUtil.md5Hex(userInputCaptcha);
            if(encryptedVerifycode.equals(serververifycode)){
                return true;
            }
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
    }

    /**
     * 校验用户相关输入参数的合法性
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码（注册时需要，登录时传null）
     * @param isRegister    是否为注册操作
     */
    private void validateUserInputParams(String userAccount, String userPassword, String checkPassword, boolean isRegister) {
        if (StrUtil.hasBlank(userAccount, userPassword) || (isRegister && StrUtil.hasBlank(checkPassword))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号长度不能小于4位");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码长度不能小于8位");
        }
        if (isRegister &&!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
    }

    /**
     * 根据注册信息构建User对象
     *
     * @param userAccount    用户账户
     * @param encryptPassword 加密后的密码
     * @return 构建好的User对象
     */
    private User buildUserForRegistration(String userAccount, String encryptPassword) {
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName(userAccount);
        user.setUserRole(UserRoleEnum.USER.getRoleName());
        return user;
    }

    @Override
    public boolean removeById(Serializable id) {
        // 从MySQL删除
        boolean result = super.removeById(id);
        if (result) {
            // 从ES删除
            esUserDao.deleteById((Long) id);
        }
        return result;
    }

    @Override
    public boolean removeByIds(Collection<?> idList) {
        // 从MySQL批量删除
        boolean result = super.removeByIds(idList);
        if (result) {
            // 从ES批量删除
            idList.forEach(id -> esUserDao.deleteById((Long) id));
        }
        return result;
    }

    @Override
    public boolean updateById(User entity) {
        // 更新MySQL
        boolean result = super.updateById(entity);
        if (result) {
            // 更新ES
            // 获取完整的用户信息
            User updatedUser = this.getById(entity.getId());
            // 转换为ES实体
            EsUser esUser = new EsUser();
            BeanUtil.copyProperties(updatedUser, esUser);
            esUserDao.save(esUser);
        }
        return result;
    }

    @Override
    public void sendEmailCode(String email, String type, HttpServletRequest request) {
        if (StrUtil.hasBlank(email, type)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        // 检测高频操作
        crawlerManager.detectFrequentRequest(request);

        // 获取客户端IP
        String clientIp = request.getRemoteAddr();
        String ipKey = String.format("email:code:ip:%s", clientIp);
        String emailKey = String.format("email:code:email:%s", email);

        // 检查IP是否频繁请求验证码
        String ipCount = stringRedisTemplate.opsForValue().get(ipKey);
        if (ipCount != null && Integer.parseInt(ipCount) >= 5) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "请求验证码过于频繁，请稍后再试");
        }

        // 检查邮箱是否频繁请求验证码
        String emailCount = stringRedisTemplate.opsForValue().get(emailKey);
        if (emailCount != null && Integer.parseInt(emailCount) >= 3) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "该邮箱请求验证码过于频繁，请稍后再试");
        }

        // 生成验证码
        String code = RandomUtil.randomNumbers(6);

        // 发送验证码
        try {
            emailSenderUtil.sendEmail(email, code);
        } catch (Exception e) {
            log.error("发送邮件失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "发送验证码失败");
        }

        // 记录IP和邮箱的请求次数，设置1小时过期
        stringRedisTemplate.opsForValue().increment(ipKey, 1);
        stringRedisTemplate.expire(ipKey, 1, TimeUnit.HOURS);

        stringRedisTemplate.opsForValue().increment(emailKey, 1);
        stringRedisTemplate.expire(emailKey, 1, TimeUnit.HOURS);

        // 将验证码存入Redis，设置5分钟过期
        String verifyCodeKey = String.format("email:code:verify:%s:%s", type, email);
        stringRedisTemplate.opsForValue().set(verifyCodeKey, code, 5, TimeUnit.MINUTES);
    }

    @Override
    public boolean changeEmail(String newEmail, String code, HttpServletRequest request) {
        // 1. 校验参数
        if (StrUtil.hasBlank(newEmail, code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (!newEmail.matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式错误");
        }

        // 2. 校验验证码
        String verifyCodeKey = String.format("email:code:verify:changeEmail:%s", newEmail);
        String correctCode = stringRedisTemplate.opsForValue().get(verifyCodeKey);
        if (correctCode == null || !correctCode.equals(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误或已过期");
        }

        // 3. 获取当前登录用户
        User loginUser = getLoginUser(request);

        synchronized (newEmail.intern()) {
            // 4. 检查新邮箱是否已被使用
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("email", newEmail);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该邮箱已被使用");
            }

            // 5. 更新邮箱
            User user = new User();
            user.setId(loginUser.getId());
            user.setEmail(newEmail);
            boolean result = this.updateById(user);
            if (!result) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "修改邮箱失败");
            }

            // 6. 删除验证码
            stringRedisTemplate.delete(verifyCodeKey);
            return true;
        }
    }

    @Override
    public boolean resetPassword(String email, String newPassword, String checkPassword, String code) {
        // 1. 校验参数
        if (StrUtil.hasBlank(email, newPassword, checkPassword, code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        // 2. 校验邮箱格式
        if (!email.matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式错误");
        }

        // 3. 校验密码
        if (newPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能小于8位");
        }
        if (!newPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        // 4. 校验验证码
        String verifyCodeKey = String.format("email:code:verify:resetPassword:%s", email);
        String correctCode = stringRedisTemplate.opsForValue().get(verifyCodeKey);
        if (correctCode == null || !correctCode.equals(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误或已过期");
        }

        // 5. 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);
        User user = this.getOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 6. 更新密码
        String encryptPassword = getEncryptPassword(newPassword);
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setUserPassword(encryptPassword);
        boolean result = this.updateById(updateUser);

        if (result) {
            // 7. 删除验证码
            stringRedisTemplate.delete(verifyCodeKey);

        }

        return result;
    }

    @Override
    public boolean banOrUnbanUser(Long userId, Boolean isUnban, User admin) {
        // 1. 校验参数
        if (userId == null || userId <= 0 || isUnban == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2. 校验管理员权限
        if (!UserConstant.ADMIN_ROLE.equals(admin.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "非管理员不能执行此操作");
        }

        // 3. 获取目标用户信息
        User targetUser = this.getById(userId);
        if (targetUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 4. 检查当前状态是否需要变更
        boolean isBanned = CrawlerConstant.BAN_ROLE.equals(targetUser.getUserRole());
        if (isUnban == isBanned) {
            // 5. 更新用户角色
            User updateUser = new User();
            updateUser.setId(userId);
            updateUser.setUserRole(isUnban ? UserConstant.DEFAULT_ROLE : CrawlerConstant.BAN_ROLE);
            updateUser.setUpdateTime(new Date());
            boolean result = this.updateById(updateUser);

            if (result) {
                // 6. 记录操作日志
                log.info("管理员[{}]{}用户[{}]",
                        admin.getUserAccount(),
                        isUnban ? "解封" : "封禁",
                        targetUser.getUserAccount());

                // 7. 处理Redis缓存
                String banKey = String.format("user:ban:%d", userId);
                if (isUnban) {
                    stringRedisTemplate.delete(banKey);
                } else {
                    stringRedisTemplate.opsForValue().set(banKey, "1");
                }

                // 8. 更新ES中的用户信息
                try {
                    Optional<EsUser> esUserOpt = esUserDao.findById(userId);
                    if (esUserOpt.isPresent()) {
                        EsUser esUser = esUserOpt.get();
                        esUser.setUserRole(isUnban ? UserConstant.DEFAULT_ROLE : CrawlerConstant.BAN_ROLE);
                        esUserDao.save(esUser);
                    }
                } catch (Exception e) {
                    log.error("更新ES用户信息失败", e);
                }
            }

            return result;
        } else {
            // 状态已经是目标状态
            String operation = isUnban ? "解封" : "封禁";
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    String.format("该用户当前%s不需要%s", isUnban ? "未被封禁" : "已被封禁", operation));
        }
    }
}




