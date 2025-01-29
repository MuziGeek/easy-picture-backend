package com.muzi.easypicturebackend;

import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = {ShardingSphereAutoConfiguration.class})
@MapperScan("com.muzi.easypicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)//提供对当前代理对象的使用
@EnableAsync
public class EasyPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasyPictureBackendApplication.class, args);
    }

}
