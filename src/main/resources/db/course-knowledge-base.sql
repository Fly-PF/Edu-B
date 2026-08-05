-- Create one course-scoped knowledge base for existing courses.
-- This script is idempotent and does not create a mapping table.
INSERT INTO rag_knowledge_base (
    user_id,
    kb_name,
    kb_cover,
    description,
    kb_type,
    course_id,
    is_public,
    status,
    create_time,
    update_time,
    deleted
)
SELECT
    c.teacher_id,
    CONCAT(c.course_name, '课程知识库'),
    CONCAT('course-kb://', c.id),
    c.intro,
    2,
    c.id,
    0,
    1,
    NOW(),
    NOW(),
    0
FROM edu_course c
WHERE c.deleted = 0
  AND c.teacher_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM rag_knowledge_base kb
      WHERE kb.course_id = c.id
        AND kb.deleted = 0
  );
