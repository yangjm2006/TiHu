package com.tihu.backend.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tihu.backend.common.ApiException;
import com.tihu.backend.common.Constants;
import com.tihu.backend.entity.BookList;
import com.tihu.backend.entity.Comment;
import com.tihu.backend.entity.User;
import com.tihu.backend.mapper.UserMapper;
import com.tihu.backend.service.UserService;
import com.tihu.backend.service.BookListService;
import com.tihu.backend.service.CommentService;
import com.tihu.backend.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 用户Service实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[a-zA-Z])[a-zA-Z0-9]{6,12}$");

    @Autowired(required = false)
    private CommentService commentService;

    @Autowired(required = false)
    private BookListService bookListService;

    @Autowired(required = false)
    private FollowService followService;

    /**
     * 用户注册
     */
    @Override
    public User register(String username, String password, String inviteCode) throws Exception {
        // 验证用户名
        if (!StringUtils.hasText(username)) {
            throw new ApiException(400, "用户名不能为空");
        }
        username = username.trim();
        if (username.length() < Constants.USERNAME_MIN_LENGTH || username.length() > Constants.USERNAME_MAX_LENGTH) {
            throw new ApiException(400, "用户名长度必须在2-10之间");
        }
        
        // 检查用户名唯一性
        User existing = this.getUserByUsername(username);
        if (existing != null) {
            throw new ApiException(409, "用户名已存在");
        }
        
        // 验证密码
        if (!StringUtils.hasText(password)) {
            throw new ApiException(400, "密码不能为空");
        }
        password = password.trim();
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new ApiException(400, "密码必须为6-12位，包含数字和英文字符");
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password));
        
        // 判断是否注册为管理员
        if (Constants.ADMIN_INVITE_CODE.equals(inviteCode)) {
            user.setRole(Constants.ROLE_ADMIN);
        } else {
            user.setRole(Constants.ROLE_USER);
        }
        
        user.setStatus(0); // 正常状态
        user.setIsDeleted(0);
        
        this.save(user);
        return user;
    }

    /**
     * 用户登录
     */
    @Override
    public User login(String username, String password) throws Exception {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new ApiException(400, "用户名或密码不能为空");
        }
        username = username.trim();
        password = password.trim();

        User user = this.getUserByUsername(username);
        if (user == null) {
            throw new ApiException(401, "用户名或密码错误");
        }
        
        // 检查用户是否被封禁
        if (user.getStatus() == 1) {
            if (user.getBanExpireTime() != null && LocalDateTime.now().isBefore(user.getBanExpireTime())) {
                throw new ApiException(403, "该用户已被封禁");
            } else if (user.getBanExpireTime() != null) {
                // 解封
                user.setStatus(0);
                user.setBanExpireTime(null);
                this.updateById(user);
            }
        }
        
        // 验证密码
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new ApiException(401, "用户名或密码错误");
        }
        
        // 使用Sa-Token登录
        StpUtil.login(user.getId());

        return user;
    }

    /**
     * 按用户名查询用户
     */
    @Override
    public User getUserByUsername(String username) {
        return this.baseMapper.selectByUsername(username);
    }

    /**
     * 修改密码
     */
    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) throws Exception {
        User user = this.getById(userId);
        if (user == null) {
            throw new ApiException(404, "用户不存在");
        }

        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw new ApiException(400, "旧密码和新密码不能为空");
        }
        oldPassword = oldPassword.trim();
        newPassword = newPassword.trim();

        // 验证旧密码
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new ApiException(401, "旧密码错误");
        }
        
        // 验证新密码格式
        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new ApiException(400, "新密码必须为6-12位，包含数字和英文字符");
        }
        
        user.setPassword(BCrypt.hashpw(newPassword));
        this.updateById(user);
    }

    /**
     * 修改用户名
     */
    @Override
    public void updateUsername(Long userId, String newUsername) throws Exception {
        if (!StringUtils.hasText(newUsername)) {
            throw new ApiException(400, "用户名不能为空");
        }
        newUsername = newUsername.trim();
        if (newUsername.length() < Constants.USERNAME_MIN_LENGTH || newUsername.length() > Constants.USERNAME_MAX_LENGTH) {
            throw new ApiException(400, "用户名长度必须在2-10之间");
        }
        
        User existing = this.getUserByUsername(newUsername);
        if (existing != null && !existing.getId().equals(userId)) {
            throw new ApiException(409, "用户名已存在");
        }
        
        User user = this.getById(userId);
        if (user == null) {
            throw new ApiException(404, "用户不存在");
        }
        
        user.setUsername(newUsername);
        this.updateById(user);
    }

    /**
     * 封禁用户
     */
    @Override
    public void banUser(Long userId, Long banDurationSeconds) {
        User user = this.getById(userId);
        if (user == null) {
            throw new ApiException(404, "用户不存在");
        }
        
        user.setStatus(1);
        user.setBanExpireTime(LocalDateTime.now().plusSeconds(banDurationSeconds));
        this.updateById(user);
    }

    /**
     * 解封用户
     */
    @Override
    public void unbanUser(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new ApiException(404, "用户不存在");
        }
        
        user.setStatus(0);
        user.setBanExpireTime(null);
        this.updateById(user);
    }

    /**
     * 获取用户主页信息（包含评论、书单、关注等）
     */
    @Override
    public Object getUserProfile(String username) {
        User user = this.getUserByUsername(username);
        if (user == null) {
            return null;
        }

        // 构建返回对象
        java.util.Map<String, Object> profile = new java.util.HashMap<>();
        profile.put("userInfo", user);

        // 获取该用户的评论列表（时间倒序）
        java.util.List<?> comments = new java.util.ArrayList<>();
        if (commentService != null) {
            try {
                // 获取用户评论
                LambdaQueryWrapper<Comment> commentQueryWrapper = new LambdaQueryWrapper<>();
                commentQueryWrapper.eq(Comment::getUserId, user.getId())
                        .eq(Comment::getIsDeleted, 0)
                        .orderByDesc(Comment::getCreateTime);
                comments = commentService.list(commentQueryWrapper);
            } catch (Exception e) {
                // 忽略异常，使用空列表
            }
        }
        profile.put("comments", comments);

        // 获取该用户的公开书单列表
        java.util.List<?> bookLists = new java.util.ArrayList<>();
        if (bookListService != null) {
            try {
                LambdaQueryWrapper<BookList> bookListQueryWrapper = new LambdaQueryWrapper<>();
                bookListQueryWrapper.eq(BookList::getUserId, user.getId());
                bookLists = bookListService.list(bookListQueryWrapper);
            } catch (Exception e) {
                // 忽略异常，使用空列表
            }
        }
        profile.put("bookLists", bookLists);

        // 获取关注数和粉丝数
        long followingCount = 0L;
        long followerCount = 0L;
        if (followService != null) {
            followingCount = followService.getFolloweeCount(user.getId());
            followerCount = followService.getFollowerCount(user.getId());
        }
        profile.put("followingCount", followingCount);
        profile.put("followerCount", followerCount);

        // 检查当前登录用户是否已关注
        boolean followedByCurrentUser = false;
        try {
            Long currentUserId = Long.parseLong(StpUtil.getLoginId().toString());
            if (followService != null && !currentUserId.equals(user.getId())) {
                followedByCurrentUser = followService.isFollowing(currentUserId, user.getId());
            }
        } catch (Exception e) {
            // 未登录，默认false
        }
        profile.put("followedByCurrentUser", followedByCurrentUser);

        return profile;
    }

    /**
     * 获取管理员封禁列表
     */
    @Override
    public Object getBanList() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, 1)
               .eq(User::getIsDeleted, 0)
               .isNotNull(User::getBanExpireTime);

        return this.list(wrapper);
    }
}


