package com.tihu.backend.controller;

import com.tihu.backend.common.Result;
import com.tihu.backend.entity.User;
import com.tihu.backend.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;

    /**
     * 新增用户
     *
     * @param user
     * @return
     */
    @PostMapping
    public Result save(@RequestBody User user) {
        userService.save(user);
        return Result.success();
    }

    /**
     * 修改用户
     *
     * @param user
     * @return
     */
    @PutMapping
    Result update(@RequestBody User user) {
        userService.updateById(user);
        return Result.success();
    }

    /**
     * 查询单个用户
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result getOne(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    /**
     * 查询所有用户
     *
     * @return
     */
    @GetMapping
    public Result getList() {
        return Result.success(userService.list());
    }

    /**
     * 删除单个用户
     *
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success();
    }
}
