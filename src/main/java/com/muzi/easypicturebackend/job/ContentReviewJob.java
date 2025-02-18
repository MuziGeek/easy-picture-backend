package com.muzi.easypicturebackend.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.muzi.easypicturebackend.model.entity.Picture;
import com.muzi.easypicturebackend.model.entity.Post;
import com.muzi.easypicturebackend.model.entity.User;
import com.muzi.easypicturebackend.service.PictureService;
import com.muzi.easypicturebackend.service.PostService;
import com.muzi.easypicturebackend.service.UserService;
import com.muzi.easypicturebackend.utils.EmailSenderUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class ContentReviewJob {

    @Resource
    private PictureService pictureService;

    @Resource
    private PostService postService;

    @Resource
    private UserService userService;

    @Resource
    private EmailSenderUtil emailSenderUtil;

    @Value("${spring.mail.admin}")
    private String adminEmail;

    private Date lastCheckTime = null;

    /**
     * 每天7点检查昨天的数据
     */
    @Scheduled(cron = "0 0 7 * * ?")
    public void dailyCheck() {
        log.info("开始执行每日审核检查");

        // 获取昨天的时间范围
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date startTime = calendar.getTime();

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date endTime = calendar.getTime();

        // 检查并发送邮件
        checkAndSendEmail(startTime, endTime, true);

        // 更新最后检查时间
        lastCheckTime = new Date();
    }

    /**
     * 在7-22点之间，每小时检查最近一小时的数据
     */
    @Scheduled(cron = "0 0 7-22 * * ?")
    public void regularCheck() {
        log.info("开始执行定时审核检查");

        // 获取最近一小时的时间范围
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, -1);
        Date startTime = calendar.getTime();
        Date endTime = new Date();

        checkAndSendEmail(startTime, endTime, false);
        lastCheckTime = new Date();
    }

    private void checkAndSendEmail(Date startTime, Date endTime, boolean isDaily) {
        try {
            // 查询未审核的公共图库图片（spaceId为null或0）
            QueryWrapper<Picture> pictureWrapper = new QueryWrapper<Picture>()
                    .eq("reviewStatus", 0)
                    .eq("isDelete", 0)
                    .between("createTime", startTime, endTime)
                    .and(wrapper -> wrapper
                            .isNull("spaceId")
                            .or()
                            .eq("spaceId", 0)
                    );

            // 查询未审核的帖子
            QueryWrapper<Post> postWrapper = new QueryWrapper<Post>()
                    .eq("status", 0)
                    .eq("isDelete", 0)
                    .between("createTime", startTime, endTime);

            List<Picture> pictures = pictureService.list(pictureWrapper);
            List<Post> posts = postService.list(postWrapper);

            // 如果有未审核的内容，发送邮件
            if (!pictures.isEmpty() || !posts.isEmpty()) {
                String emailContent = generateEmailContent(pictures, posts, isDaily);
                emailSenderUtil.sendReviewEmail(adminEmail, emailContent);
                log.info("已发送审核通知邮件，未审核公共图片：{}张，未审核帖子：{}条",
                        pictures.size(), posts.size());
            }
        } catch (Exception e) {
            log.error("审核检查任务执行失败", e);
        }
    }

    private String generateEmailContent(List<Picture> pictures, List<Post> posts, boolean isDaily)
            throws IOException {
        // 读取HTML模板
        String template = readHtmlTemplate();
        StringBuilder tableContent = new StringBuilder();
        int totalCount = pictures.size() + posts.size();

        // 生成图片行
        for (Picture picture : pictures) {
            User user = userService.getById(picture.getUserId());
            tableContent.append(String.format("<tr>" +
                            "<td>%d</td>" +
                            "<td>%s</td>" +
                            "<td>%d</td>" +
                            "<td>%s</td>" +
                            "<td><span class='type picture'>图片</span></td>" +
                            "<td>%s</td>" +
                            "</tr>",
                    user.getId(),
                    user.getEmail(),
                    picture.getId(),
                    picture.getName(),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(picture.getCreateTime())
            ));
        }

        // 生成帖子行
        for (Post post : posts) {
            User user = userService.getById(post.getUserId());
            tableContent.append(String.format("<tr>" +
                            "<td>%d</td>" +
                            "<td>%s</td>" +
                            "<td>%d</td>" +
                            "<td>%s</td>" +
                            "<td><span class='type post'>帖子</span></td>" +
                            "<td>%s</td>" +
                            "</tr>",
                    user.getId(),
                    user.getEmail(),
                    post.getId(),
                    post.getTitle(),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(post.getCreateTime())
            ));
        }

        // 替换模板中的占位符
        return template
                .replace(":count", String.valueOf(totalCount))
                .replace(":table_content", tableContent.toString());
    }

    private String readHtmlTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource("html/review_notification.html");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }
}
