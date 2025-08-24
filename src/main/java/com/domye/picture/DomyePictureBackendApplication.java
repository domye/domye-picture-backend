package com.domye.picture;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@MapperScan("com.domye.picture.mapper")
@SpringBootApplication
public class DomyePictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DomyePictureBackendApplication.class, args);
    }

}
