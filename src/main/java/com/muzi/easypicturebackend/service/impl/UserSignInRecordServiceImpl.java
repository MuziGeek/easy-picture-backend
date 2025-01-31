package com.muzi.easypicturebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzi.easypicturebackend.model.entity.UserSignInRecord;
import com.muzi.easypicturebackend.service.UserSignInRecordService;
import com.muzi.easypicturebackend.mapper.UserSignInRecordMapper;
import org.springframework.stereotype.Service;

/**
* @author 57242
* @description 针对表【user_sign_in_record(用户签到记录表)】的数据库操作Service实现
* @createDate 2025-01-31 13:21:25
*/
@Service
public class UserSignInRecordServiceImpl extends ServiceImpl<UserSignInRecordMapper, UserSignInRecord>
    implements UserSignInRecordService{

}




