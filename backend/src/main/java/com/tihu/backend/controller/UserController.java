package com.tihu.backend.controller;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
     * 用户分页
     *
     * @param pageNumber
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public Result getPage(@RequestParam(defaultValue = "1") Integer pageNumber,
                          @RequestParam(defaultValue = "10") Integer pageSize,
                          @RequestParam(defaultValue = "") String name) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (!name.isEmpty()) {
            queryWrapper.like(User::getName, name);
        }
        return Result.success(
                userService.page(new Page<>(pageNumber, pageSize), queryWrapper)
        );
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

    // 登录接口
    @RequestMapping("doLogin")
    public Result doLogin() {
        // 第1步，先登录上
        StpUtil.login(10001);
        // 第2步，获取 Token  相关参数
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        // 第3步，返回给前端
        return Result.success(tokenInfo);
    }


    @RequestMapping("isLogin")
    public String isLogin() {
        return "当前会话是否登录：" + StpUtil.isLogin();
    }
}
