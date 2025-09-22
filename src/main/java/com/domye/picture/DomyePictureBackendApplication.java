package com.domye.picture;

import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@MapperScan("com.domye.picture.mapper")
@EnableAsync
@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication(exclude = {ShardingSphereAutoConfiguration.class})
public class DomyePictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DomyePictureBackendApplication.class, args);
    }

}
