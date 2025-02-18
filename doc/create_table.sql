-- 创建库
create database if not exists easy_picture;
-- 用户表
create table if not exists user
(
    id            bigint auto_increment comment 'id' primary key,
    userAccount   varchar(256)                           not null comment '账号',
    userPassword  varchar(512)                           not null comment '密码',
    userName      varchar(256)                           null comment '用户昵称',
    userAvatar    varchar(1024)                          null comment '用户头像',
    userProfile   varchar(512)                           null comment '用户简介',
    userRole      varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime      datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime    datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime    datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint      default 0                 not null comment '是否删除',
    vipExpireTime datetime                               null comment '会员过期时间',
    vipCode       varchar(128)                           null comment '会员兑换码',
    vipNumber     bigint                                 null comment '会员编号',
    shareCode     varchar(20)  DEFAULT NULL COMMENT '分享码',
    inviteUser    bigint       DEFAULT NULL COMMENT '邀请用户 id',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;
ALTER TABLE user
    ADD COLUMN email VARCHAR(256) NULL COMMENT '用户邮箱';
CREATE UNIQUE INDEX  uk_email
    ON user (email);
CREATE INDEX idx_email
    ON user (email);
-- 图片表
create table if not exists picture
(
    id           bigint auto_increment comment 'id' primary key,
    url          varchar(512)                       not null comment '图片 url',
    name         varchar(128)                       not null comment '图片名称',
    introduction varchar(512)                       null comment '简介',
    category     varchar(64)                        null comment '分类',
    tags         varchar(512)                       null comment '标签（JSON 数组）',
    picSize      bigint                             null comment '图片体积',
    picWidth     int                                null comment '图片宽度',
    picHeight    int                                null comment '图片高度',
    picScale     double                             null comment '图片宽高比例',
    picFormat    varchar(32)                        null comment '图片格式',
    userId       bigint                             not null comment '创建用户 id',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime     datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    INDEX idx_name (name),                 -- 提升基于图片名称的查询性能
    INDEX idx_introduction (introduction), -- 用于模糊搜索图片简介
    INDEX idx_category (category),         -- 提升基于分类的查询性能
    INDEX idx_tags (tags),                 -- 提升基于标签的查询性能
    INDEX idx_userId (userId)              -- 提升基于用户 ID 的查询性能
) comment '图片' collate = utf8mb4_unicode_ci;

ALTER TABLE picture
    -- 添加新列
    ADD COLUMN reviewStatus  INT DEFAULT 0 NOT NULL COMMENT '审核状态：0-待审核; 1-通过; 2-拒绝',
    ADD COLUMN reviewMessage VARCHAR(512)  NULL COMMENT '审核信息',
    ADD COLUMN reviewerId    BIGINT        NULL COMMENT '审核人 ID',
    ADD COLUMN reviewTime    DATETIME      NULL COMMENT '审核时间';
-- 创建基于 reviewStatus 列的索引
CREATE INDEX idx_reviewStatus ON picture (reviewStatus);
ALTER TABLE picture
    ADD COLUMN originUrl VARCHAR(512) NULL COMMENT '原始url';
-- 添加新列
ALTER TABLE picture
    ADD COLUMN thumbnailUrl varchar(512) NULL COMMENT '缩略图 url';
ALTER TABLE picture
    ADD COLUMN spaceId bigint null comment '空间 id（为空表示公共空间）';
ALTER TABLE picture
    ADD COLUMN commentCount BIGINT NOT NULL DEFAULT 0 COMMENT '评论数',
    ADD COLUMN likeCount    BIGINT NOT NULL DEFAULT 0 COMMENT '点赞数',
    ADD COLUMN shareCount   BIGINT NOT NULL DEFAULT 0 COMMENT '分享数';
ALTER TABLE picture
    ADD COLUMN viewCount BIGINT NOT NULL DEFAULT 0 COMMENT '浏览量';
CREATE INDEX idx_viewCount
    ON picture (viewCount);

-- 标签表
CREATE TABLE IF NOT EXISTS `Tag`
(
    `id`         BIGINT AUTO_INCREMENT COMMENT '标签id' PRIMARY KEY,
    `tagName`    VARCHAR(256)                       NOT NULL COMMENT '标签名称',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `editTime`   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '编辑时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`   TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    UNIQUE KEY `uk_tagName` (`tagName`)
) COMMENT '标签' COLLATE = utf8mb4_unicode_ci;

-- 分类表
CREATE TABLE IF NOT EXISTS `Category`
(
    `id`           BIGINT AUTO_INCREMENT COMMENT '分类id' PRIMARY KEY,
    `categoryName` VARCHAR(256)                       NOT NULL COMMENT '分类名称',
    `createTime`   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `editTime`     DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '分类编辑时间',
    `updateTime`   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '分类更新时间',
    `isDelete`     TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    UNIQUE KEY `uk_categoryName` (`categoryName`)
) COMMENT '分类' COLLATE = utf8mb4_unicode_ci;

ALTER TABLE Category
    ADD COLUMN type tinyint default 0 not null comment '分类类型：0-图片分类 1-帖子分类';

-- 空间表
create table if not exists space
(
    id         bigint auto_increment comment 'id' primary key,
    spaceName  varchar(128)                       null comment '空间名称',
    spaceLevel int      default 0                 null comment '空间级别：0-普通版 1-专业版 2-旗舰版',
    maxSize    bigint   default 0                 null comment '空间图片的最大总大小',
    maxCount   bigint   default 0                 null comment '空间图片的最大数量',
    totalSize  bigint   default 0                 null comment '当前空间下图片的总大小',
    totalCount bigint   default 0                 null comment '当前空间下的图片数量',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime   datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    -- 索引设计
    index idx_userId (userId),        -- 提升基于用户的查询效率
    index idx_spaceName (spaceName),  -- 提升基于空间名称的查询效率
    index idx_spaceLevel (spaceLevel) -- 提升按空间级别查询的效率
) comment '空间' collate = utf8mb4_unicode_ci;
-- 创建索引
CREATE INDEX idx_spaceId ON picture (spaceId);
ALTER TABLE picture
    ADD COLUMN picColor varchar(16) null comment '图片主色调';

ALTER TABLE space
    ADD COLUMN spaceType int default 0 not null comment '空间类型：0-私有 1-团队';
CREATE INDEX idx_spaceType ON space (spaceType);


-- 点赞记录表
CREATE TABLE pictureLike
(
    id            bigint AUTO_INCREMENT PRIMARY KEY COMMENT '主键 ID',
    userId        bigint   NOT NULL COMMENT '用户 ID',
    pictureId     bigint   NOT NULL COMMENT '图片 ID',
    isLiked       BOOLEAN  NOT NULL COMMENT '用户是否点赞（true 表示点赞，false 表示取消点赞）',
    firstLikeTime datetime NOT NULL COMMENT '第一次点赞的时间',
    lastLikeTime  datetime NOT NULL COMMENT '最近一次点赞的时间'
) COMMENT '点赞表' COLLATE = utf8mb4_unicode_ci;

-- 联合索引
CREATE INDEX idx_userId_pictureId_isLiked ON pictureLike (userId, pictureId, isLiked);

ALTER TABLE pictureLike
    MODIFY COLUMN firstLikeTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
-- 评论记录表
CREATE TABLE comments
(
    commentId       bigint AUTO_INCREMENT
        PRIMARY KEY,
    userId          bigint                               NOT NULL,
    pictureId       bigint                               NOT NULL,
    content         text                                 NOT NULL,
    createTime      datetime   DEFAULT CURRENT_TIMESTAMP NULL,
    parentCommentId bigint     DEFAULT 0                 NULL COMMENT '0表示顶级',
    isDelete        tinyint(1) DEFAULT 0                 NULL,
    likeCount       bigint     DEFAULT 0                 NULL,
    dislikeCount    bigint     DEFAULT 0                 NULL
);

CREATE INDEX idx_pictureId
    ON comments (pictureId);
-- 留言板表
CREATE TABLE `message`
(
    `id`         bigint   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `content`    text     NOT NULL COMMENT '留言内容',
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`   tinyint(1)        DEFAULT 0 COMMENT '是否删除(0-未删除 1-已删除)',
    `ip`         varchar(50)       DEFAULT NULL COMMENT 'IP地址',
    PRIMARY KEY (`id`),
    KEY `idx_createTime` (`createTime`) -- 创建时间索引
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='留言板表';

-- 空间成员表
create table if not exists space_user
(
    id         bigint auto_increment comment 'id' primary key,
    spaceId    bigint                                 not null comment '空间 id',
    userId     bigint                                 not null comment '用户 id',
    spaceRole  varchar(128) default 'viewer'          null comment '空间角色：viewer/editor/admin',
    createTime datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    -- 索引设计
    UNIQUE KEY uk_spaceId_userId (spaceId, userId), -- 唯一索引，用户在一个空间中只能有一个角色
    INDEX idx_spaceId (spaceId),                    -- 提升按空间查询的性能
    INDEX idx_userId (userId)                       -- 提升按用户查询的性能
) comment '空间用户关联' collate = utf8mb4_unicode_ci;

-- 用户关注表
CREATE TABLE userfollows
(
    followId            bigint AUTO_INCREMENT
        PRIMARY KEY,
    followerId          bigint                             NOT NULL COMMENT '关注者的用户 ID',
    followingId         bigint                             NOT NULL COMMENT '被关注者的用户 ID',
    followStatus        tinyint                            NULL COMMENT '关注状态，0 表示取消关注，1 表示关注',
    isMutual            tinyint                            NULL COMMENT '是否为双向关注，0 表示单向，1 表示双向',
    lastInteractionTime datetime                           NULL COMMENT '最后交互时间',
    createTime          datetime DEFAULT CURRENT_TIMESTAMP NULL COMMENT '关注关系创建时间，默认为当前时间',
    editTime            datetime DEFAULT CURRENT_TIMESTAMP NULL COMMENT '关注关系编辑时间，默认为当前时间',
    updateTime          datetime DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '关注关系更新时间，更新时自动更新',
    isDelete            tinyint  DEFAULT 0                 NULL COMMENT '是否删除，0 表示未删除，1 表示已删除'
);

CREATE INDEX idx_followStatus
    ON userfollows (followStatus);

CREATE INDEX idx_followerId
    ON userfollows (followerId);

CREATE INDEX idx_followingId
    ON userfollows (followingId);

CREATE INDEX idx_isDelete
    ON userfollows (isDelete);


-- 用户签到记录表（使用bitmap存储）
CREATE TABLE IF NOT EXISTS user_sign_in_record
(
    id         BIGINT                             NOT NULL COMMENT 'id' PRIMARY KEY,
    userId     BIGINT                             NOT NULL COMMENT '用户id',
    year       INT                                NOT NULL COMMENT '年份',
    signInData BINARY(46)                         NOT NULL COMMENT '签到数据位图(366天/8≈46字节)',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete   TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    UNIQUE KEY uk_userId_year (userId, year) COMMENT '用户id和年份唯一索引'
) COMMENT '用户签到记录表' COLLATE = utf8mb4_unicode_ci;

-- 聊天消息表
CREATE TABLE chat_message
(
    id            bigint AUTO_INCREMENT COMMENT '主键'
        PRIMARY KEY,
    senderId      bigint                             NOT NULL COMMENT '发送者id',
    receiverId    bigint                             NULL COMMENT '接收者id',
    pictureId     bigint                             NULL COMMENT '图片id',
    content       text                               NOT NULL COMMENT '消息内容',
    type          tinyint  DEFAULT 1                 NOT NULL COMMENT '消息类型 1-私聊 2-图片聊天室',
    status        tinyint  DEFAULT 0                 NOT NULL COMMENT '状态 0-未读 1-已读',
    replyId       bigint                             NULL COMMENT '回复的消息id',
    rootId        bigint                             NULL COMMENT '会话根消息id',
    createTime    datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime    datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete      tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    spaceId       bigint                             NULL COMMENT '空间id',
    privateChatId bigint                             NULL COMMENT '私聊ID'
)
    COMMENT '聊天消息表';

CREATE INDEX idx_picture
    ON chat_message (pictureId);

CREATE INDEX idx_private_chat
    ON chat_message (privateChatId);

CREATE INDEX idx_reply
    ON chat_message (replyId);

CREATE INDEX idx_root
    ON chat_message (rootId);

CREATE INDEX idx_sender_receiver
    ON chat_message (senderId, receiverId);

CREATE INDEX idx_space
    ON chat_message (spaceId);

-- 帖子
CREATE TABLE post
(
    id            bigint AUTO_INCREMENT COMMENT '帖子ID'
        PRIMARY KEY,
    userId        bigint                             NOT NULL COMMENT '发帖用户ID',
    title         varchar(100)                       NOT NULL COMMENT '标题',
    content       text                               NOT NULL COMMENT '内容',
    category      varchar(50)                        NULL COMMENT '分类',
    tags          varchar(255)                       NULL COMMENT '标签JSON数组',
    viewCount     bigint   DEFAULT 0                 NULL COMMENT '浏览量',
    likeCount     bigint   DEFAULT 0                 NULL COMMENT '点赞数',
    commentCount  bigint   DEFAULT 0                 NULL COMMENT '评论数',
    status        tinyint  DEFAULT 0                 NULL COMMENT '状态 0-待审核 1-已发布 2-已拒绝',
    reviewMessage varchar(255)                       NULL COMMENT '审核信息',
    createTime    datetime DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    updateTime    datetime DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete      tinyint  DEFAULT 0                 NULL COMMENT '是否删除',
    shareCount    bigint   DEFAULT 0                 NULL COMMENT '分享数'
)
    COMMENT '论坛帖子表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_category
    ON post (category);

CREATE INDEX idx_shareCount
    ON post (shareCount);

CREATE INDEX idx_status
    ON post (status);

CREATE INDEX idx_userId
    ON post (userId);
-- 帖子附件表
CREATE TABLE post_attachment
(
    id         bigint AUTO_INCREMENT COMMENT '附件ID'
        PRIMARY KEY,
    postId     bigint                             NOT NULL COMMENT '帖子ID',
    type       tinyint                            NOT NULL COMMENT '类型 1-图片 2-文件',
    url        varchar(255)                       NOT NULL COMMENT '资源URL',
    name       varchar(100)                       NULL COMMENT '原始文件名',
    size       bigint                             NULL COMMENT '文件大小(字节)',
    position   int                                NULL COMMENT '在文章中的位置',
    marker     varchar(50)                        NULL COMMENT '在文章中的标识符',
    sort       int      DEFAULT 0                 NULL COMMENT '排序号',
    createTime datetime DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    updateTime datetime DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete   tinyint  DEFAULT 0                 NULL COMMENT '是否删除'
)
    COMMENT '帖子附件表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_position
    ON post_attachment (position);

CREATE INDEX idx_postId
    ON post_attachment (postId);

CREATE INDEX idx_type
    ON post_attachment (type);
-- 私聊表
CREATE TABLE private_chat
(
    id                    bigint AUTO_INCREMENT COMMENT '主键'
        PRIMARY KEY,
    userId                bigint                             NOT NULL COMMENT '用户id',
    targetUserId          bigint                             NOT NULL COMMENT '目标用户id',
    lastMessage           text                               NULL COMMENT '最后一条消息内容',
    lastMessageTime       datetime                           NULL COMMENT '最后一条消息时间',
    userUnreadCount       int      DEFAULT 0                 NULL COMMENT '用户未读消息数',
    targetUserUnreadCount int      DEFAULT 0                 NULL COMMENT '目标用户未读消息数',
    userChatName          varchar(50)                        NULL COMMENT '用户自定义的私聊名称',
    targetUserChatName    varchar(50)                        NULL COMMENT '目标用户自定义的私聊名称',
    chatType              tinyint  DEFAULT 0                 NOT NULL COMMENT '聊天类型：0-私信 1-好友(双向关注)',
    createTime            datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime            datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete              tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除'
)
    COMMENT '私聊表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_chat_type
    ON private_chat (chatType);

CREATE INDEX idx_user_target
    ON private_chat (userId, targetUserId);

-- 分享记录表
CREATE TABLE share_record
(
    id           bigint AUTO_INCREMENT COMMENT '分享ID'
        PRIMARY KEY,
    userId       bigint                               NOT NULL COMMENT '用户ID',
    targetId     bigint                               NOT NULL COMMENT '被分享内容的ID',
    targetType   tinyint                              NOT NULL COMMENT '内容类型：1-图片 2-帖子',
    targetUserId bigint                               NOT NULL COMMENT '被分享内容所属用户ID',
    isShared     tinyint(1) DEFAULT 1                 NOT NULL COMMENT '是否分享',
    shareTime    datetime   DEFAULT CURRENT_TIMESTAMP NULL COMMENT '分享时间',
    isRead       tinyint(1) DEFAULT 0                 NOT NULL COMMENT '是否已读（0-未读，1-已读）'
)
    COMMENT '分享记录表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_target
    ON share_record (targetId, targetType);

CREATE INDEX idx_targetUserId_isRead
    ON share_record (targetUserId, isRead);

CREATE INDEX idx_userId_isRead
    ON share_record (userId, isRead);

CREATE INDEX idx_userId_targetType
    ON share_record (userId, targetType);

-- 热门搜索记录表
CREATE TABLE hot_search
(
    id             bigint AUTO_INCREMENT COMMENT '主键'
        PRIMARY KEY,
    keyword        varchar(128)                       NOT NULL COMMENT '搜索关键词',
    type           varchar(32)                        NOT NULL COMMENT '搜索类型',
    count          bigint   DEFAULT 0                 NOT NULL COMMENT '搜索次数',
    lastUpdateTime datetime                           NOT NULL COMMENT '最后更新时间',
    createTime     datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime     datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete       tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    CONSTRAINT uk_type_keyword
        UNIQUE (type, keyword)
)
    COMMENT '热门搜索记录表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_lastUpdateTime
    ON hot_search (lastUpdateTime);

CREATE INDEX idx_type_count
    ON hot_search (type ASC, count DESC);
-- 通用点赞表
CREATE TABLE like_record
(
    id            bigint AUTO_INCREMENT COMMENT '主键 ID'
        PRIMARY KEY,
    userId        bigint                               NOT NULL COMMENT '用户 ID',
    targetId      bigint                               NOT NULL COMMENT '被点赞内容的ID',
    targetType    tinyint                              NOT NULL COMMENT '内容类型：1-图片 2-帖子 3-空间',
    targetUserId  bigint                               NOT NULL COMMENT '被点赞内容所属用户ID',
    isLiked       tinyint(1)                           NOT NULL COMMENT '是否点赞',
    firstLikeTime datetime   DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '第一次点赞时间',
    lastLikeTime  datetime                             NOT NULL COMMENT '最近一次点赞时间',
    isRead        tinyint(1) DEFAULT 0                 NOT NULL COMMENT '是否已读（0-未读，1-已读）',
    CONSTRAINT uk_user_target
        UNIQUE (userId, targetId, targetType)
)
    COMMENT '通用点赞表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_target
    ON like_record (targetId, targetType);

CREATE INDEX idx_targetUserId_isRead
    ON like_record (targetUserId, isRead);

CREATE INDEX idx_userId_isRead
    ON like_record (userId, isRead);

CREATE INDEX idx_userId_targetType
    ON like_record (userId, targetType);