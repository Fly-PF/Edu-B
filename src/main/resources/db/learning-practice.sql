SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS edu_learning_practice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    practice_title VARCHAR(120) NOT NULL,
    practice_intro VARCHAR(500) NULL,
    total_score INT NOT NULL DEFAULT 100,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_learning_practice_course (course_id)
);

CREATE TABLE IF NOT EXISTS edu_learning_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    practice_id BIGINT NOT NULL,
    question_type VARCHAR(20) NOT NULL,
    question_content TEXT NOT NULL,
    options_json TEXT NULL,
    reference_answer TEXT NULL,
    answer_explanation TEXT NULL,
    question_score INT NOT NULL DEFAULT 10,
    sort_order INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_learning_question_practice (practice_id)
);

CREATE TABLE IF NOT EXISTS edu_learning_submission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    practice_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    student_name VARCHAR(80) NULL,
    answer_json TEXT NOT NULL,
    question_review_json TEXT NULL,
    auto_score INT NOT NULL DEFAULT 0,
    teacher_score INT NULL,
    teacher_feedback VARCHAR(1000) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    submit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    review_time DATETIME NULL,
    reviewer_id BIGINT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_learning_submission_student (practice_id, student_id),
    INDEX idx_learning_submission_practice (practice_id),
    INDEX idx_learning_submission_student (student_id)
);

INSERT INTO edu_learning_practice (course_id, practice_title, practice_intro, total_score, status)
SELECT 1, '人工智能基础与应用 - 认识 AI', '完成基础概念辨析和学习反思，提交后等待老师点评。', 100, 1
WHERE NOT EXISTS (SELECT 1 FROM edu_learning_practice WHERE course_id = 1);

INSERT INTO edu_learning_practice (course_id, practice_title, practice_intro, total_score, status)
SELECT 2, '校园 AI 项目设计练习', '围绕校园中的真实问题，设计一个负责任的 AI 小项目。', 100, 1
WHERE NOT EXISTS (SELECT 1 FROM edu_learning_practice WHERE course_id = 2);

INSERT INTO edu_learning_practice (course_id, practice_title, practice_intro, total_score, status)
SELECT 3, 'Python 与图像分类基础练习', '检查 Python 数据处理和图像分类流程的理解。', 100, 1
WHERE NOT EXISTS (SELECT 1 FROM edu_learning_practice WHERE course_id = 3);

INSERT INTO edu_learning_practice (course_id, practice_title, practice_intro, total_score, status)
SELECT 4, '机器学习实验入门练习', '根据实验流程完成选择与简答，并提交实验观察。', 100, 1
WHERE NOT EXISTS (SELECT 1 FROM edu_learning_practice WHERE course_id = 4);

INSERT INTO edu_learning_question (practice_id, question_type, question_content, options_json, reference_answer, answer_explanation, question_score, sort_order)
SELECT p.id, 'SINGLE', '下列哪一项最能说明人工智能在学习中的合理用途？', '["A. 直接代替学生完成全部作业","B. 帮助学生理解概念并给出学习建议","C. 不经核实地输出所有结论","D. 收集其他同学的隐私信息"]', 'B', 'AI 可以辅助理解和学习，但不能代替学生完成学习任务，也不能侵犯隐私。', 30, 1
FROM edu_learning_practice p WHERE p.course_id = 1
  AND NOT EXISTS (SELECT 1 FROM edu_learning_question q WHERE q.practice_id = p.id AND q.sort_order = 1);

INSERT INTO edu_learning_question (practice_id, question_type, question_content, options_json, reference_answer, answer_explanation, question_score, sort_order)
SELECT p.id, 'SHORT', '请用自己的话说明：使用 AI 学习工具时，为什么还需要核对回答来源？', NULL, '开放题', '可以从信息准确性、课程适配性和避免模型编造等角度说明。', 70, 2
FROM edu_learning_practice p WHERE p.course_id = 1
  AND NOT EXISTS (SELECT 1 FROM edu_learning_question q WHERE q.practice_id = p.id AND q.sort_order = 2);

INSERT INTO edu_learning_question (practice_id, question_type, question_content, options_json, reference_answer, answer_explanation, question_score, sort_order)
SELECT p.id, 'SINGLE', '设计校园 AI 项目前，最应该先明确什么？', '["A. 页面颜色","B. 要解决的真实问题和使用对象","C. 项目名称","D. 宣传海报"]', 'B', '先明确问题和用户，后续的数据、功能和评价方式才有依据。', 35, 1
FROM edu_learning_practice p WHERE p.course_id = 2
  AND NOT EXISTS (SELECT 1 FROM edu_learning_question q WHERE q.practice_id = p.id AND q.sort_order = 1);

INSERT INTO edu_learning_question (practice_id, question_type, question_content, options_json, reference_answer, answer_explanation, question_score, sort_order)
SELECT p.id, 'SHORT', '请选择一个校园场景，写出你想用 AI 帮助解决的问题，并说明它可能带来的价值。', NULL, '开放题', '可从学习、校园服务、环保、文化传承等场景展开，答案需具体、可实施。', 65, 2
FROM edu_learning_practice p WHERE p.course_id = 2
  AND NOT EXISTS (SELECT 1 FROM edu_learning_question q WHERE q.practice_id = p.id AND q.sort_order = 2);

INSERT INTO edu_learning_question (practice_id, question_type, question_content, options_json, reference_answer, answer_explanation, question_score, sort_order)
SELECT p.id, 'SINGLE', '在图像分类任务中，训练数据最重要的特点是？', '["A. 全部来自同一种图片","B. 标签清晰且样本有代表性","C. 图片越大越好","D. 不需要检查错误标签"]', 'B', '高质量、带正确标签且有代表性的数据，是训练可靠模型的基础。', 40, 1
FROM edu_learning_practice p WHERE p.course_id = 3
  AND NOT EXISTS (SELECT 1 FROM edu_learning_question q WHERE q.practice_id = p.id AND q.sort_order = 1);

INSERT INTO edu_learning_question (practice_id, question_type, question_content, options_json, reference_answer, answer_explanation, question_score, sort_order)
SELECT p.id, 'SHORT', '请写出数据处理流程中至少两个需要检查的环节，并说明原因。', NULL, '开放题', '可回答缺失值、重复值、标签错误、数据格式和数据划分等。', 60, 2
FROM edu_learning_practice p WHERE p.course_id = 3
  AND NOT EXISTS (SELECT 1 FROM edu_learning_question q WHERE q.practice_id = p.id AND q.sort_order = 2);

INSERT INTO edu_learning_question (practice_id, question_type, question_content, options_json, reference_answer, answer_explanation, question_score, sort_order)
SELECT p.id, 'SINGLE', '完成机器学习实验后，下面哪一步最适合用来判断模型是否可靠？', '["A. 只看训练集是否全部答对","B. 用未参与训练的数据进行评估","C. 只比较代码行数","D. 让模型自己给自己打分"]', 'B', '应使用独立的验证或测试数据评估模型的泛化能力。', 40, 1
FROM edu_learning_practice p WHERE p.course_id = 4
  AND NOT EXISTS (SELECT 1 FROM edu_learning_question q WHERE q.practice_id = p.id AND q.sort_order = 1);

INSERT INTO edu_learning_question (practice_id, question_type, question_content, options_json, reference_answer, answer_explanation, question_score, sort_order)
SELECT p.id, 'SHORT', '请描述一次机器学习实验的基本流程，并指出你最想进一步验证的一项内容。', NULL, '开放题', '可包含数据准备、训练、评估和分析改进等步骤。', 60, 2
FROM edu_learning_practice p WHERE p.course_id = 4
  AND NOT EXISTS (SELECT 1 FROM edu_learning_question q WHERE q.practice_id = p.id AND q.sort_order = 2);

-- Correct previously imported rows as well as seed new databases.
UPDATE edu_learning_practice
SET practice_title = '人工智能基础与应用 - 认识 AI',
    practice_intro = '完成基础概念辨析和学习反思，提交后等待老师点评。',
    total_score = 100,
    status = 1
WHERE course_id = 1;

UPDATE edu_learning_practice
SET practice_title = '校园 AI 项目设计练习',
    practice_intro = '围绕校园中的真实问题，设计一个负责任的 AI 小项目。',
    total_score = 100,
    status = 1
WHERE course_id = 2;

UPDATE edu_learning_practice
SET practice_title = 'Python 与图像分类基础练习',
    practice_intro = '检查 Python 数据处理和图像分类流程的理解。',
    total_score = 100,
    status = 1
WHERE course_id = 3;

UPDATE edu_learning_practice
SET practice_title = '机器学习实验入门练习',
    practice_intro = '根据实验流程完成选择与简答，并提交实验观察。',
    total_score = 100,
    status = 1
WHERE course_id = 4;

UPDATE edu_learning_question
SET question_type = 'SINGLE',
    question_content = '下列哪一项最能说明人工智能在学习中的合理用途？',
    options_json = '["A. 直接代替学生完成全部作业","B. 帮助学生理解概念并给出学习建议","C. 不经核实地输出所有结论","D. 收集其他同学的隐私信息"]',
    reference_answer = 'B',
    answer_explanation = 'AI 可以辅助理解和学习，但不能代替学生完成学习任务，也不能侵犯隐私。',
    question_score = 30
WHERE practice_id IN (SELECT id FROM edu_learning_practice WHERE course_id = 1) AND sort_order = 1;

UPDATE edu_learning_question
SET question_type = 'SHORT',
    question_content = '请用自己的话说明：使用 AI 学习工具时，为什么还需要核对回答来源？',
    options_json = NULL,
    reference_answer = '开放题',
    answer_explanation = '可以从信息准确性、课程适配性和避免模型编造等角度说明。',
    question_score = 70
WHERE practice_id IN (SELECT id FROM edu_learning_practice WHERE course_id = 1) AND sort_order = 2;

UPDATE edu_learning_question
SET question_type = 'SINGLE',
    question_content = '设计校园 AI 项目前，最应该先明确什么？',
    options_json = '["A. 页面颜色","B. 要解决的真实问题和使用对象","C. 项目名称","D. 宣传海报"]',
    reference_answer = 'B',
    answer_explanation = '先明确问题和用户，后续的数据、功能和评价方式才有依据。',
    question_score = 35
WHERE practice_id IN (SELECT id FROM edu_learning_practice WHERE course_id = 2) AND sort_order = 1;

UPDATE edu_learning_question
SET question_type = 'SHORT',
    question_content = '请选择一个校园场景，写出你想用 AI 帮助解决的问题，并说明它可能带来的价值。',
    options_json = NULL,
    reference_answer = '开放题',
    answer_explanation = '可从学习、校园服务、环保、文化传承等场景展开，答案需具体、可实施。',
    question_score = 65
WHERE practice_id IN (SELECT id FROM edu_learning_practice WHERE course_id = 2) AND sort_order = 2;

UPDATE edu_learning_question
SET question_type = 'SINGLE',
    question_content = '在图像分类任务中，训练数据最重要的特点是？',
    options_json = '["A. 全部来自同一种图片","B. 标签清晰且样本有代表性","C. 图片越大越好","D. 不需要检查错误标签"]',
    reference_answer = 'B',
    answer_explanation = '高质量、带正确标签且有代表性的数据，是训练可靠模型的基础。',
    question_score = 40
WHERE practice_id IN (SELECT id FROM edu_learning_practice WHERE course_id = 3) AND sort_order = 1;

UPDATE edu_learning_question
SET question_type = 'SHORT',
    question_content = '请写出数据处理流程中至少两个需要检查的环节，并说明原因。',
    options_json = NULL,
    reference_answer = '开放题',
    answer_explanation = '可回答缺失值、重复值、标签错误、数据格式和数据划分等。',
    question_score = 60
WHERE practice_id IN (SELECT id FROM edu_learning_practice WHERE course_id = 3) AND sort_order = 2;

UPDATE edu_learning_question
SET question_type = 'SINGLE',
    question_content = '完成机器学习实验后，下面哪一步最适合用来判断模型是否可靠？',
    options_json = '["A. 只看训练集是否全部答对","B. 用未参与训练的数据进行评估","C. 只比较代码行数","D. 让模型自己给自己打分"]',
    reference_answer = 'B',
    answer_explanation = '应使用独立的验证或测试数据评估模型的泛化能力。',
    question_score = 40
WHERE practice_id IN (SELECT id FROM edu_learning_practice WHERE course_id = 4) AND sort_order = 1;

UPDATE edu_learning_question
SET question_type = 'SHORT',
    question_content = '请描述一次机器学习实验的基本流程，并指出你最想进一步验证的一项内容。',
    options_json = NULL,
    reference_answer = '开放题',
    answer_explanation = '可包含数据准备、训练、评估和分析改进等步骤。',
    question_score = 60
WHERE practice_id IN (SELECT id FROM edu_learning_practice WHERE course_id = 4) AND sort_order = 2;
