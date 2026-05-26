package com.tihu.backend;

import com.tihu.backend.entity.User;
import com.tihu.backend.mapper.UserMapper;
import com.tihu.backend.service.UserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class BackendApplicationTests {

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserService userService;

    @Test
    void contextLoads() {
        System.out.println(("----- selectAll method test ------"));
        List<User> userList = userMapper.selectList(null);
        org.junit.jupiter.api.Assertions.assertNotNull(userList);
        userList.forEach(System.out::println);
    }

    @Test
    void contextLoad2(){
        List<User> userList = userService.list();
        userList.forEach(System.out::println);
    }
}
