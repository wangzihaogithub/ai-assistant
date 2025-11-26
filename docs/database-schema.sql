/*
 Navicat Premium Data Transfer

 Source Server Type    : MySQL

 Target Server Type    : MySQL
 Target Server Version : 80042
 File Encoding         : 65001

 Date: 07/09/2025 21:40:59
*/

-- 表结构关系-在线文档：https://zihaoapi.cn/static/doc/ai.html

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_assistant
-- ----------------------------
DROP TABLE IF EXISTS `ai_assistant`;
CREATE TABLE `ai_assistant` (
                                `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '智能体ID，不可变。保证唯一',
                                `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '智能体名称',
                                `system_prompt_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '系统提示词',
                                `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '智能体描述',
                                `hello_message` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '智能体打招呼语',
                                `logo_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '智能体的头像',
                                `kn_prompt_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '知识库提示词',
                                `status_enum` int NOT NULL COMMENT '智能体状态：1发布 0未发布',
                                `sorted` int NOT NULL COMMENT '智能体排列顺序，从小到大',
                                `ai_tool_ids` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '这个智能体能使用的工具',
                                `max_memory_tokens` int NOT NULL COMMENT '记忆字数的上限',
                                `max_memory_rounds` int NOT NULL COMMENT '最多记忆几轮对话',
                                `create_time` datetime NOT NULL COMMENT '智能体创建时间',
                                `create_uid` int NOT NULL COMMENT '智能体创建人',
                                `update_time` datetime NOT NULL COMMENT '智能体更新时间',
                                `update_uid` int NOT NULL COMMENT '智能体更新人',
                                `temperature` double NOT NULL COMMENT '丰富度，0.1至1之间，最大越丰富',
                                `chat_api_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '聊天大模型的密码',
                                `chat_base_url` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '聊天大模型的url',
                                `chat_model_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '聊天大模型的名称',
                                `ai_jsonschema_ids` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '使用哪些jsonschema ',
                                `mstate_prompt_text` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '记忆状态提示词',
                                `max_completion_tokens` int NOT NULL COMMENT '指令生成的回答中包含的最大token数。例如，如果设置为100，那么模型生成的回答中token数不会超过100个',
                                PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通用型智能体';

-- ----------------------------
-- Table structure for ai_assistant_fewshot
-- ----------------------------
DROP TABLE IF EXISTS `ai_assistant_fewshot`;
CREATE TABLE `ai_assistant_fewshot` (
                                        `id` int NOT NULL AUTO_INCREMENT,
                                        `ai_assistant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '智能体ID',
                                        `message_index` int NOT NULL COMMENT '消息下标',
                                        `message_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息内容',
                                        `message_type_enum` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息类型枚举: 参考MessageTypeEnum枚举\n取值范围【User，Ai】',
                                        PRIMARY KEY (`id`) USING BTREE,
                                        KEY `ai_assistant_id` (`ai_assistant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='少样本学习';

-- ----------------------------
-- Table structure for ai_assistant_kn
-- ----------------------------
DROP TABLE IF EXISTS `ai_assistant_kn`;
CREATE TABLE `ai_assistant_kn` (
                                   `id` int NOT NULL AUTO_INCREMENT,
                                   `assistant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '智能体ID',
                                   `vector_field_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '搜索字段',
                                   `min_score` bigint NOT NULL COMMENT '最小匹配度',
                                   `kn_limit` int NOT NULL COMMENT '最多召回几个',
                                   `kn_index_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '知识库数据存储es',
                                   `kn_type_enum` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '知识库类型枚举（qa=问答，majorjob=专业，job=岗位）',
                                   `kn_top1_score` bigint DEFAULT NULL COMMENT '知识库如果超过这个分数，直接用这1个',
                                   `embedding_api_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '向量大模型的密码',
                                   `embedding_base_url` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '向量大模型的url',
                                   `embedding_model_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '向量大模型的名称',
                                   `embedding_dimensions` int NOT NULL COMMENT '向量纬度，参考供应商文档，一般都是1024个纬度',
                                   `embedding_max_request_size` int NOT NULL COMMENT '向量每次最大提交条数',
                                   `kn_query_min_char_length` int NOT NULL COMMENT '用户的提问最少几个字才用知识库',
                                   `knn_factor` int NOT NULL COMMENT 'knn搜索k的系数',
                                   PRIMARY KEY (`id`) USING BTREE,
                                   UNIQUE KEY `uniq_ai_assistant_type` (`assistant_id`,`kn_type_enum`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=60 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='智能体知识库';

-- ----------------------------
-- Table structure for ai_assistant_mstate
-- ----------------------------
DROP TABLE IF EXISTS `ai_assistant_mstate`;
CREATE TABLE `ai_assistant_mstate` (
                                       `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
                                       `assistant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '智能体ID，不可变。保证唯一',
                                       `state_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '系统提示词',
                                       `prompt_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '字段提示词',
                                       `create_time` datetime NOT NULL COMMENT '创建时间',
                                       `update_time` datetime NOT NULL,
                                       PRIMARY KEY (`id`) USING BTREE,
                                       KEY `ai_assistant_id` (`assistant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=84 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='智能体记忆状态字段的定义与提示词\n';

-- ----------------------------
-- Table structure for ai_chat
-- ----------------------------
DROP TABLE IF EXISTS `ai_chat`;
CREATE TABLE `ai_chat` (
                           `id` int NOT NULL AUTO_INCREMENT,
                           `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '聊天名称',
                           `create_time` datetime NOT NULL COMMENT '创建时间',
                           `update_time` datetime NOT NULL COMMENT '更新时间',
                           `create_uid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '创建人',
                           `create_uid_int` int UNSIGNED DEFAULT NULL COMMENT '创建人-数字类型',
                           `ai_memory_id` int NOT NULL COMMENT '记忆ID',
                           `delete_time` datetime DEFAULT NULL COMMENT '重新回答时间',
                           `assistant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '智能体ID',
                           `uid_type` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户类型\n类型等于表名\nstudent= 学生\nsys_user=员工',
                           `last_chat_time` datetime NOT NULL COMMENT '最后一次聊天时间',
                           `last_websearch_flag` bit(1) NOT NULL COMMENT '最后一次是否联网',
                           `chat_source_enum` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '聊天来源枚举（pc=pc端创建的，wxmini=微信小程序）',
                           PRIMARY KEY (`id`) USING BTREE,
                           KEY `memory_id` (`ai_memory_id`) USING BTREE,
                           KEY `create_uid` (`create_uid`,`uid_type`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5229 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='聊天';

-- ----------------------------
-- Table structure for ai_chat_abort
-- ----------------------------
DROP TABLE IF EXISTS `ai_chat_abort`;
CREATE TABLE `ai_chat_abort` (
                                 `id` int NOT NULL AUTO_INCREMENT,
                                 `create_time` datetime NOT NULL COMMENT '终止时间',
                                 `before_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '终止时的文本',
                                 `ai_memory_id` int NOT NULL COMMENT '记忆ID',
                                 `ai_chat_id` int NOT NULL COMMENT '聊天ID',
                                 `user_query_trace_number` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户提问追踪ID',
                                 `message_index` int DEFAULT NULL COMMENT '第几个消息',
                                 `root_again_user_query_trace_number` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '重新回答用户问题聊天追踪号，根问题',
                                 PRIMARY KEY (`id`) USING BTREE,
                                 KEY `ai_memory_id` (`ai_memory_id`) USING BTREE,
                                 KEY `ai_chat_id` (`ai_chat_id`) USING BTREE,
                                 KEY `user_query_trace_number` (`user_query_trace_number`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1328 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户点击终止生成';

-- ----------------------------
-- Table structure for ai_chat_classify
-- ----------------------------
DROP TABLE IF EXISTS `ai_chat_classify`;
CREATE TABLE `ai_chat_classify` (
                                    `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `ai_chat_id` int NOT NULL COMMENT '聊天ID',
                                    `user_query_trace_number` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户问题追踪ID',
                                    `classify_id` int NOT NULL COMMENT '分类id（ai_question_classify表主键）',
                                    `classify_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类名称（ai_question_classify表name）',
                                    `classify_group_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类分组（ai_question_classify表group_code）',
                                    `classify_group_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类分组（ai_question_classify表group_name）',
                                    `create_time` datetime NOT NULL COMMENT '创建时间',
                                    `question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户问题',
                                    `ai_question_classify_assistant_id` int DEFAULT NULL COMMENT '分配AI',
                                    `action_enums` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '控制的动作 AiQuestionClassifyActionEnum',
                                    PRIMARY KEY (`id`) USING BTREE,
                                    KEY `ai_chat_id` (`ai_chat_id`) USING BTREE,
                                    KEY `user_query_trace_number` (`user_query_trace_number`) USING BTREE,
                                    KEY `classify_id` (`classify_id`) USING BTREE,
                                    KEY `create_time` (`create_time`,`classify_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6458 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='聊天分类';

-- ----------------------------
-- Table structure for ai_chat_history
-- ----------------------------
DROP TABLE IF EXISTS `ai_chat_history`;
CREATE TABLE `ai_chat_history` (
                                   `id` int NOT NULL AUTO_INCREMENT,
                                   `ai_chat_id` int NOT NULL COMMENT '聊天ID',
                                   `create_time` datetime NOT NULL COMMENT '创建时间',
                                   `message_type_enum` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息类型\nuser=用户\nai=ai\n',
                                   `message_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息内容',
                                   `message_index` int NOT NULL COMMENT '消息下标',
                                   `text_char_length` int NOT NULL COMMENT '内容长度',
                                   `user_chat_history_id` int DEFAULT NULL COMMENT '用户问题聊天ID',
                                   `delete_time` datetime DEFAULT NULL COMMENT '重新提问会触发删除',
                                   `user_query_trace_number` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户问题追踪ID',
                                   `user_query_flag` bit(1) NOT NULL COMMENT '是否用户问题',
                                   `again_user_query_trace_number` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '重新回答的哪个问题',
                                   `stage_enum` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据是哪个阶段生产出来的，取值范围【Request,Response】',
                                   `root_user_query_trace_number` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '重新回答用户问题聊天追踪号，根问题',
                                   `websearch_flag` bit(1) NOT NULL COMMENT '是否联网',
                                   `start_time` datetime NOT NULL COMMENT '开始时间',
                                   PRIMARY KEY (`id`) USING BTREE,
                                   KEY `create_time` (`create_time`) USING BTREE,
                                   KEY `user_chat_history_id` (`user_chat_history_id`) USING BTREE,
                                   KEY `ai_chat_id` (`ai_chat_id`,`create_time`) USING BTREE,
                                   KEY `user_query_trace_number` (`user_query_trace_number`,`user_query_flag`) USING BTREE,
                                   KEY `ai_chat_id_2` (`ai_chat_id`,`user_query_flag`) USING BTREE,
                                   KEY `again_user_query_trace_number` (`again_user_query_trace_number`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=26015 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='聊天记录';

-- ----------------------------
-- Table structure for ai_chat_reasoning
-- ----------------------------
DROP TABLE IF EXISTS `ai_chat_reasoning`;
CREATE TABLE `ai_chat_reasoning` (
                                     `id` int NOT NULL AUTO_INCREMENT,
                                     `ai_chat_id` int NOT NULL COMMENT '聊天ID',
                                     `question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '问题',
                                     `need_splitting_flag` bit(1) NOT NULL COMMENT '是否需要问题拆分',
                                     `user_chat_history_id` int NOT NULL COMMENT '用户提问的消息ID',
                                     `create_time` datetime NOT NULL COMMENT '创建时间',
                                     `user_query_trace_number` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户回答追踪ID',
                                     PRIMARY KEY (`id`) USING BTREE,
                                     KEY `ai_chat_id` (`ai_chat_id`) USING BTREE,
                                     KEY `user_chat_history_id` (`user_chat_history_id`,`need_splitting_flag`) USING BTREE,
                                     KEY `user_query_trace_number` (`user_query_trace_number`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1465 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='思考问题';

-- ----------------------------
-- Table structure for ai_chat_reasoning_plan
-- ----------------------------
DROP TABLE IF EXISTS `ai_chat_reasoning_plan`;
CREATE TABLE `ai_chat_reasoning_plan` (
                                          `id` int NOT NULL AUTO_INCREMENT,
                                          `ai_chat_reasoning_id` int NOT NULL COMMENT '思考ID',
                                          `ai_chat_id` int NOT NULL COMMENT '聊天ID',
                                          `user_chat_history_id` int NOT NULL COMMENT '用户提问的消息ID',
                                          `task` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务',
                                          `plan_index` int NOT NULL COMMENT '第几个计划下标',
                                          `resolved_flag` bit(1) NOT NULL COMMENT '此字段用于标识这个任务是否被解决',
                                          `fail_message` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '此字段用于你向用户解释，没有解决的原因',
                                          `answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '如果已被解决，这就是解决的最终答案',
                                          `ai_question` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '如果你有不明白或需要向用户确认的问题，可以通过此字段向用户提问',
                                          `websearch_keyword` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '如果任务未被解决，可以在此字段上返回一些搜索关键词，以助于使用搜索引擎',
                                          PRIMARY KEY (`id`) USING BTREE,
                                          KEY `ai_chat_id` (`ai_chat_id`) USING BTREE,
                                          KEY `ai_chat_reasoning_id` (`ai_chat_reasoning_id`) USING BTREE,
                                          KEY `user_chat_history_id` (`user_chat_history_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2379 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='思考问题得出的计划';

-- ----------------------------
-- Table structure for ai_chat_websearch
-- ----------------------------
DROP TABLE IF EXISTS `ai_chat_websearch`;
CREATE TABLE `ai_chat_websearch` (
                                     `id` int NOT NULL AUTO_INCREMENT,
                                     `ai_chat_id` int NOT NULL COMMENT '聊天ID',
                                     `user_chat_history_id` int NOT NULL COMMENT '用户提问的消息ID',
                                     `source_enum` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '触发来源',
                                     `question` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '联网的问题',
                                     `create_time` datetime NOT NULL COMMENT '创建时间',
                                     `provider_name` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '供应商',
                                     `search_time_ms` int NOT NULL COMMENT '查询耗时',
                                     `search_proxy` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '搜索代理',
                                     `user_query_trace_number` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户回答追踪ID',
                                     PRIMARY KEY (`id`) USING BTREE,
                                     KEY `ai_chat_id` (`ai_chat_id`) USING BTREE,
                                     KEY `user_chat_history_id` (`user_chat_history_id`) USING BTREE,
                                     KEY `user_query_trace_number` (`user_query_trace_number`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=16574 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='联网';

-- ----------------------------
-- Table structure for ai_chat_websearch_result
-- ----------------------------
DROP TABLE IF EXISTS `ai_chat_websearch_result`;
CREATE TABLE `ai_chat_websearch_result` (
                                            `id` int NOT NULL AUTO_INCREMENT,
                                            `ai_chat_id` int NOT NULL COMMENT '聊天ID',
                                            `user_chat_history_id` int NOT NULL COMMENT '用户提问的消息ID',
                                            `ai_chat_websearch_id` int NOT NULL COMMENT '联网搜索id',
                                            `page_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '网页url',
                                            `page_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '网页标题',
                                            `page_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '网页时间',
                                            `page_source` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '网页来源',
                                            `page_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '网页正文',
                                            `url_read_time_cost` int NOT NULL COMMENT '读取内容耗时',
                                            `url_read_proxy` varchar(35) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '读取内容的代理',
                                            PRIMARY KEY (`id`) USING BTREE,
                                            KEY `ai_chat_id` (`ai_chat_id`) USING BTREE,
                                            KEY `user_chat_history_id` (`user_chat_history_id`) USING BTREE,
                                            KEY `ai_chat_websearch_id` (`ai_chat_websearch_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=78314 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='联网结果';

-- ----------------------------
-- Table structure for ai_embedding
-- ----------------------------
DROP TABLE IF EXISTS `ai_embedding`;
CREATE TABLE `ai_embedding` (
                                `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
                                `keyword` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '关键词',
                                `vector` json NOT NULL COMMENT '向量',
                                `md5` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '关键词的md5',
                                `create_time` datetime NOT NULL COMMENT '创建时间',
                                `model_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模型名称',
                                `dimensions` int NOT NULL COMMENT '向量纬度',
                                PRIMARY KEY (`id`) USING BTREE,
                                UNIQUE KEY `uniq_md5` (`md5`,`model_name`,`dimensions`) USING BTREE,
                                KEY `keyword` (`keyword`(64)) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=163269 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='AI向量缓存';

-- ----------------------------
-- Table structure for ai_jsonschema
-- ----------------------------
DROP TABLE IF EXISTS `ai_jsonschema`;
CREATE TABLE `ai_jsonschema` (
                                 `id` int NOT NULL AUTO_INCREMENT COMMENT '智能体ID，不可变。保证唯一',
                                 `api_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '聊天大模型的密码',
                                 `base_url` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '聊天大模型的url',
                                 `model_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '聊天大模型的名称',
                                 `json_schema_enum` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '接口枚举',
                                 `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '备注',
                                 `system_prompt_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '系统提示词',
                                 `user_prompt_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户提示词',
                                 `kn_prompt_text` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '知识库提示词',
                                 `enable_flag` bit(1) NOT NULL COMMENT '是否开启',
                                 `response_format` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '@JsonProperty("text")\nTEXT,\n@JsonProperty("json_object")\nJSON_OBJECT,\n@JsonProperty("json_schema")\nJSON_SCHEMA\n',
                                 `ai_tool_ids` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工具ID',
                                 `max_completion_tokens` int NOT NULL COMMENT '指令生成的回答中包含的最大token数。例如，如果设置为100，那么模型生成的回答中token数不会超过100个',
                                 `temperature` double NOT NULL COMMENT '丰富度，0.1至1之间，最大越丰富',
                                 `top_p` double NOT NULL COMMENT '随机度，0.1至1之间，最大越随机\n',
                                 `timeout_ms` int NOT NULL COMMENT '接口请求大模型超时时间（毫秒）',
                                 PRIMARY KEY (`id`) USING BTREE,
                                 KEY `json_schema_enum` (`json_schema_enum`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='任务型智能体';

-- ----------------------------
-- Table structure for ai_memory
-- ----------------------------
DROP TABLE IF EXISTS `ai_memory`;
CREATE TABLE `ai_memory` (
                             `id` int NOT NULL AUTO_INCREMENT,
                             `create_time` datetime NOT NULL COMMENT '创建时间',
                             `update_time` datetime NOT NULL COMMENT '更新时间',
                             `user_token_count` int NOT NULL COMMENT '用户的token数量',
                             `ai_token_count` int NOT NULL COMMENT 'AI回复的token数量',
                             `knowledge_token_count` int NOT NULL DEFAULT '0',
                             `user_char_length` int NOT NULL COMMENT '用户使用的文本长度',
                             `ai_char_length` int NOT NULL COMMENT 'ai使用的文本长度',
                             `knowledge_char_length` int NOT NULL DEFAULT '0',
                             PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5229 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='记忆';

-- ----------------------------
-- Table structure for ai_memory_error
-- ----------------------------
DROP TABLE IF EXISTS `ai_memory_error`;
CREATE TABLE `ai_memory_error` (
                                   `id` int NOT NULL AUTO_INCREMENT,
                                   `memory_id` int NOT NULL COMMENT '记忆ID',
                                   `error_class_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '错误的java的Clas类',
                                   `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '错误消息',
                                   `base_message_index` int NOT NULL COMMENT '基础消息下标',
                                   `add_message_count` int NOT NULL COMMENT '本次回复增加的下标',
                                   `generate_count` int NOT NULL COMMENT '本次AI生成次数',
                                   `create_time` datetime NOT NULL COMMENT '创建时间',
                                   `session_time` datetime NOT NULL COMMENT '本次提问的创建时间',
                                   `user_query_trace_number` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '本次提问的问题追踪ID',
                                   `message_count` int NOT NULL COMMENT '消息下标',
                                   `error_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '错误类型',
                                   `message_text` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '显示的信息',
                                   `attachment_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '附加数据',
                                   `root_again_user_query_trace_number` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '原始问题',
                                   `ai_chat_id` int DEFAULT NULL COMMENT '聊天ID',
                                   PRIMARY KEY (`id`) USING BTREE,
                                   KEY `memory_id` (`memory_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=721 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Ai错误';

-- ----------------------------
-- Table structure for ai_memory_message
-- ----------------------------
DROP TABLE IF EXISTS `ai_memory_message`;
CREATE TABLE `ai_memory_message` (
                                     `id` int NOT NULL AUTO_INCREMENT,
                                     `ai_memory_id` int NOT NULL COMMENT '记忆ID',
                                     `message_index` int NOT NULL COMMENT '消息下标',
                                     `message_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息内容',
                                     `message_type_enum` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息类型枚举: 参考MessageTypeEnum枚举',
                                     `use_knowledge_flag` bit(1) NOT NULL DEFAULT b'0',
                                     `use_tool_flag` bit(1) NOT NULL COMMENT '是否使用工具',
                                     `user_query_flag` bit(1) NOT NULL COMMENT '是否是用户提的问题',
                                     `create_time` datetime NOT NULL COMMENT '创建时间',
                                     `commit_time` datetime NOT NULL COMMENT '持久化时间',
                                     `reply_tool_request_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '回复的是哪个工具ID',
                                     `reply_tool_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '回复的是哪个工具名称',
                                     `token_count` int NOT NULL COMMENT 'token数量',
                                     `char_length` int NOT NULL COMMENT '内容字符串长度',
                                     `user_token_count` int NOT NULL COMMENT '用户token数量',
                                     `user_char_length` int NOT NULL COMMENT '用户内容字符串长度',
                                     `ai_token_count` int NOT NULL COMMENT 'ai的token数量',
                                     `ai_char_length` int NOT NULL COMMENT 'ai的内容长度',
                                     `knowledge_token_count` int NOT NULL DEFAULT '0',
                                     `knowledge_char_length` int NOT NULL DEFAULT '0',
                                     `delete_time` datetime DEFAULT NULL COMMENT '重新回答',
                                     `user_query_trace_number` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户问题追踪ID',
                                     `again_user_query_trace_number` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '重新回答的哪个问题',
                                     `stage_enum` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据是哪个阶段生产出来的，取值范围【Request,Response】',
                                     `root_user_query_trace_number` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '重新回答用户问题聊天追踪号，根问题',
                                     `open_ai_request_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '供应商的请求id，用于向供应商反馈错误问题',
                                     `websearch_flag` bit(1) DEFAULT NULL COMMENT '是否联网',
                                     `start_time` datetime DEFAULT NULL COMMENT '开始时间',
                                     `first_token_time` datetime DEFAULT NULL COMMENT 'AI吐第一个字的时间',
                                     PRIMARY KEY (`id`) USING BTREE,
                                     KEY `ai_memory_id` (`ai_memory_id`,`message_index`) USING BTREE,
                                     KEY `user_query_trace_number` (`user_query_trace_number`,`user_query_flag`) USING BTREE,
                                     KEY `again_user_query_trace_number` (`again_user_query_trace_number`) USING BTREE,
                                     KEY `create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=26060 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='记忆的消息';

-- ----------------------------
-- Table structure for ai_memory_message_kn
-- ----------------------------
DROP TABLE IF EXISTS `ai_memory_message_kn`;
CREATE TABLE `ai_memory_message_kn` (
                                        `id` int NOT NULL AUTO_INCREMENT,
                                        `ai_memory_message_id` int NOT NULL COMMENT '记忆的消息ID',
                                        `kn_id` int NOT NULL COMMENT '知识库id',
                                        `kn_question_text` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '问题',
                                        `kn_answer_text` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '回答',
                                        `kn_score` bigint NOT NULL COMMENT '匹配度，百分之，乘以100后的',
                                        `kn_index_updated_time` datetime NOT NULL COMMENT '知识库更新时间',
                                        `kn_index_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '知识库类型：就是es的索引名称',
                                        `ai_memory_id` int NOT NULL COMMENT '记忆ID',
                                        PRIMARY KEY (`id`) USING BTREE,
                                        KEY `ai_memory_message_id` (`ai_memory_message_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1289 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库查询结果';

-- ----------------------------
-- Table structure for ai_memory_message_metadata
-- ----------------------------
DROP TABLE IF EXISTS `ai_memory_message_metadata`;
CREATE TABLE `ai_memory_message_metadata` (
                                              `id` int NOT NULL AUTO_INCREMENT,
                                              `ai_memory_message_id` int NOT NULL COMMENT '记忆消息ID',
                                              `meta_index` int NOT NULL COMMENT '第几个AiMessageString',
                                              `meta_key` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '通过AiMessageString放置的数据key',
                                              `meta_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '通过AiMessageString放置的数据value',
                                              `ai_memory_id` int NOT NULL COMMENT '记忆ID',
                                              `user_query_trace_number` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户问题追踪ID',
                                              `again_user_query_trace_number` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '重新回答的哪个问题',
                                              `root_user_query_trace_number` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '重新回答用户问题聊天追踪号，根问题',
                                              PRIMARY KEY (`id`) USING BTREE,
                                              KEY `ai_memory_message_id` (`ai_memory_message_id`) USING BTREE,
                                              KEY `ai_memory_id` (`ai_memory_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=411 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通过AiMessageString放置的元数据，用于给业务做判断\n';

-- ----------------------------
-- Table structure for ai_memory_message_tool
-- ----------------------------
DROP TABLE IF EXISTS `ai_memory_message_tool`;
CREATE TABLE `ai_memory_message_tool` (
                                          `id` int NOT NULL AUTO_INCREMENT,
                                          `ai_memory_message_id` int NOT NULL COMMENT '记忆消息ID',
                                          `tool_request_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工具的请求ID',
                                          `tool_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工具名称',
                                          `tool_arguments` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工具参数',
                                          `ai_memory_id` int NOT NULL COMMENT '记忆ID',
                                          PRIMARY KEY (`id`) USING BTREE,
                                          KEY `ai_memory_message_id` (`ai_memory_message_id`) USING BTREE,
                                          KEY `ai_memory_id` (`ai_memory_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3397 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='记忆消息中借助了工具';

-- ----------------------------
-- Table structure for ai_memory_mstate
-- ----------------------------
DROP TABLE IF EXISTS `ai_memory_mstate`;
CREATE TABLE `ai_memory_mstate` (
                                    `id` int NOT NULL AUTO_INCREMENT,
                                    `ai_memory_id` int NOT NULL COMMENT '记忆ID',
                                    `state_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '状态的key',
                                    `state_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '状态数据',
                                    `user_ai_memory_message_id` int NOT NULL COMMENT '用户的记忆消息ID',
                                    `create_time` datetime NOT NULL COMMENT '创建时间',
                                    `user_message_index` int NOT NULL COMMENT '消息下标',
                                    `user_query_trace_number` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户问题追踪ID',
                                    `known_flag` bit(1) NOT NULL COMMENT '是否是已知的状态',
                                    PRIMARY KEY (`id`) USING BTREE,
                                    KEY `ai_memory_id` (`ai_memory_id`,`state_key`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=80007 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='长期记忆片段与记忆变量\n';

-- ----------------------------
-- Table structure for ai_memory_search
-- ----------------------------
DROP TABLE IF EXISTS `ai_memory_search`;
CREATE TABLE `ai_memory_search` (
                                    `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `ai_memory_id` int NOT NULL COMMENT '记忆ID',
                                    `index_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '查询索引对象',
                                    `request_body` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '查询条件',
                                    `response_doc_count` int NOT NULL COMMENT '返回文档数量',
                                    `error_message` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '\0' COMMENT '错误内容',
                                    `user_query_trace_number` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '本次提问的问题追踪ID',
                                    `ai_chat_id` int NOT NULL COMMENT '聊天ID',
                                    `create_time` datetime NOT NULL COMMENT '创建时间',
                                    `search_start_time` datetime NOT NULL COMMENT 'RAG开始时间',
                                    `search_end_time` datetime NOT NULL COMMENT 'RAG结束时间',
                                    `search_cost_ms` int NOT NULL COMMENT 'RAG耗时',
                                    PRIMARY KEY (`id`) USING BTREE,
                                    KEY `ai_chat_id` (`ai_chat_id`) USING BTREE,
                                    KEY `create_time` (`create_time`) USING BTREE,
                                    KEY `ai_memory_id` (`ai_memory_id`) USING BTREE,
                                    KEY `user_query_trace_number` (`user_query_trace_number`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=68 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='记忆的RAG记录';

-- ----------------------------
-- Table structure for ai_memory_search_doc
-- ----------------------------
DROP TABLE IF EXISTS `ai_memory_search_doc`;
CREATE TABLE `ai_memory_search_doc` (
                                        `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
                                        `ai_memory_search_id` int NOT NULL COMMENT 'rag ID',
                                        `doc_id_string` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文档ID文本类型',
                                        `doc_id_int` int DEFAULT NULL COMMENT '文档ID数字类型',
                                        `doc_score` int DEFAULT NULL COMMENT '返回文档匹配度',
                                        PRIMARY KEY (`id`) USING BTREE,
                                        KEY `ai_memory_rag_id` (`ai_memory_search_id`) USING BTREE,
                                        KEY `doc_id_int` (`doc_id_int`) USING BTREE,
                                        KEY `doc_id_string` (`doc_id_string`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=98 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='记忆的RAG记录文档';

-- ----------------------------
-- Table structure for ai_question_classify
-- ----------------------------
DROP TABLE IF EXISTS `ai_question_classify`;
CREATE TABLE `ai_question_classify` (
                                        `id` int NOT NULL AUTO_INCREMENT,
                                        `classify_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '问题类型名称（二级）',
                                        `group_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '问题类型名称（一级）',
                                        `group_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '问题类型代码（一级）',
                                        `action_enums` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '控制的动作,多个用逗号分隔\nqa= 问答库\njdlw = 简单联网\ndclw = 多层联网\nlwdd = 联网兜底\nwtcj = 问题拆解\nwfhd = 无法回答\n\n enum AiQuestionClassifyActionEnum {\n    qa("qa", "问答库"),\n    jdlw("jdlw", "简单联网"),\nwtcj("wtcj", "问题拆解"),\n    dclw("dclw", "多层联网"),\n    lwdd("lwdd", "联网兜底");\n    wfhd("wfhd", “”无法回答);\n\n\n',
                                        `example_text` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '示例文案',
                                        `enable_flag` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否开启，1=开启',
                                        `read_timeout_second` int NOT NULL COMMENT '读超时秒数',
                                        `ai_question_classify_assistant_id` int DEFAULT NULL COMMENT '聊天大模型',
                                        `ai_assistant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '智能体',
                                        PRIMARY KEY (`id`) USING BTREE,
                                        UNIQUE KEY `uniq_classify_name` (`classify_name`,`ai_assistant_id`) USING BTREE,
                                        KEY `ai_assistant_id` (`ai_assistant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=54 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='问题分类';

-- ----------------------------
-- Table structure for ai_question_classify_assistant
-- ----------------------------
DROP TABLE IF EXISTS `ai_question_classify_assistant`;
CREATE TABLE `ai_question_classify_assistant` (
                                                  `id` int NOT NULL AUTO_INCREMENT COMMENT '智能体ID，不可变。保证唯一',
                                                  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '智能体名称',
                                                  `system_prompt_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '系统提示词',
                                                  `ai_tool_ids` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '这个智能体能使用的工具',
                                                  `max_memory_tokens` int NOT NULL COMMENT '记忆字数的上限',
                                                  `max_memory_rounds` int NOT NULL COMMENT '最多记忆几轮对话',
                                                  `temperature` double NOT NULL COMMENT '丰富度，0.1至1之间，最大越丰富',
                                                  `chat_api_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '聊天大模型的密码',
                                                  `chat_base_url` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '聊天大模型的url',
                                                  `chat_model_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '聊天大模型的名称',
                                                  `ai_jsonschema_ids` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '使用哪些jsonschema ',
                                                  `max_completion_tokens` int NOT NULL COMMENT '指令生成的回答中包含的最大token数。例如，如果设置为100，那么模型生成的回答中token数不会超过100个',
                                                  `kn_prompt_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '知识库提示词',
                                                  `mstate_prompt_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '记忆状态提示词',
                                                  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='专家智能体（经过问题分类后使用的）';

-- ----------------------------
-- Table structure for ai_question_top
-- ----------------------------
DROP TABLE IF EXISTS `ai_question_top`;
CREATE TABLE `ai_question_top` (
                                   `id` int NOT NULL AUTO_INCREMENT,
                                   `question_text` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '问题',
                                   `sorted` int NOT NULL COMMENT '排序',
                                   `create_time` datetime NOT NULL COMMENT '创建时间',
                                   `update_time` datetime NOT NULL COMMENT '更新时间',
                                   `icon_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '问题icon',
                                   `public_flag` bit(1) NOT NULL COMMENT '是否全部人都能看',
                                   PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='热门问题';

-- ----------------------------
-- Table structure for ai_question_top_assistant
-- ----------------------------
DROP TABLE IF EXISTS `ai_question_top_assistant`;
CREATE TABLE `ai_question_top_assistant` (
                                             `id` int NOT NULL AUTO_INCREMENT,
                                             `ai_question_top_id` int NOT NULL COMMENT '问题ID',
                                             `assistant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '智能体',
                                             PRIMARY KEY (`id`) USING BTREE,
                                             KEY `ai_question_top_id` (`ai_question_top_id`,`assistant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='热门问题和智能体权限关联';

-- ----------------------------
-- Table structure for ai_tool
-- ----------------------------
DROP TABLE IF EXISTS `ai_tool`;
CREATE TABLE `ai_tool` (
                           `id` int NOT NULL AUTO_INCREMENT,
                           `tool_function_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '函数名称，给AI和让用户看的名称（需要全局唯一）\n',
                           `tool_function_enum` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '函数枚举（就是研发实现的工具类对象的方法名）\n',
                           `tool_enum` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工具枚举（就是研发实现的工具类在spring中的beanName）\n',
                           `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '备注',
                           `tool_function_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '函数提示词（给AI用的）\n',
                           `create_time` datetime NOT NULL COMMENT '创建时间',
                           `update_time` datetime NOT NULL COMMENT '更新时间',
                           `create_uid` int NOT NULL COMMENT '创建人',
                           `update_uid` int NOT NULL COMMENT '修改人',
                           PRIMARY KEY (`id`) USING BTREE,
                           UNIQUE KEY `uniq_tool_function_name` (`tool_function_name`) USING BTREE,
                           KEY `uniq_tool_enum_function_enum` (`tool_enum`,`tool_function_enum`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI工具提示词';

-- ----------------------------
-- Table structure for ai_tool_parameter
-- ----------------------------
DROP TABLE IF EXISTS `ai_tool_parameter`;
CREATE TABLE `ai_tool_parameter` (
                                     `id` int NOT NULL AUTO_INCREMENT,
                                     `ai_tool_id` int NOT NULL COMMENT 'ai_tool的ID',
                                     `parameter_enum` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '参数枚举（就是研发实现的工具类对象的方法名）\n     * UNIQUE INDEX `uniq_tool_parameter_enum`(`ai_tool_id`, `parameter_enum`) USING BTREE',
                                     `parameter_description` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '参数提示词（给AI用的）',
                                     `required_flag` bit(1) NOT NULL COMMENT '是否必填',
                                     `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称，AI不用',
                                     `enable_flag` bit(1) NOT NULL COMMENT '是否开启',
                                     `default_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '默认值',
                                     PRIMARY KEY (`id`) USING BTREE,
                                     UNIQUE KEY `uniq_tool_parameter_enum` (`ai_tool_id`,`parameter_enum`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=80 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI工具参数的提示词';

-- ----------------------------
-- Table structure for ai_variables
-- ----------------------------
DROP TABLE IF EXISTS `ai_variables`;
CREATE TABLE `ai_variables` (
                                `id` int NOT NULL AUTO_INCREMENT,
                                `var_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '变量key',
                                `var_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '变量数据',
                                `create_time` datetime NOT NULL COMMENT '创建时间',
                                `update_time` datetime NOT NULL COMMENT '更新时间',
                                `enable_flag` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
                                PRIMARY KEY (`id`) USING BTREE,
                                UNIQUE KEY `var_key` (`var_key`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Ai提示词全局变量';

-- ----------------------------
-- Table structure for kn_question
-- ----------------------------
DROP TABLE IF EXISTS `kn_question`;
CREATE TABLE `kn_question` (
                               `id` int NOT NULL AUTO_INCREMENT,
                               `question` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                               `answer` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                               `qa_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                               `qa_level` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                               PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=556 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='问答知识库';

-- ----------------------------
-- Table structure for kn_setting_websearch_blacklist
-- ----------------------------
DROP TABLE IF EXISTS `kn_setting_websearch_blacklist`;
CREATE TABLE `kn_setting_websearch_blacklist` (
                                                  `id` int NOT NULL AUTO_INCREMENT,
                                                  `question` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '黑名单问题',
                                                  `similarity` bigint NOT NULL COMMENT '相似度,0~100',
                                                  `sorted` int NOT NULL COMMENT '排序，从小到大，越容易命中的放前面',
                                                  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Ai联网结果黑名单';

SET FOREIGN_KEY_CHECKS = 1;
