CREATE TABLE IF NOT EXISTS edu_problem_subject (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(40) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_problem_subject_code (code)
);

CREATE TABLE IF NOT EXISTS edu_problem_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    subject_id BIGINT NOT NULL,
    name VARCHAR(60) NOT NULL,
    description VARCHAR(255) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    INDEX idx_problem_category_subject (subject_id)
);

CREATE TABLE IF NOT EXISTS edu_problem (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    intro VARCHAR(500) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    tags_json JSON NOT NULL,
    create_by BIGINT NULL,
    current_version INT NOT NULL DEFAULT 1,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_problem_category_status (category_id, status),
    INDEX idx_problem_title (title)
);

CREATE TABLE IF NOT EXISTS edu_problem_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    problem_id BIGINT NOT NULL,
    version INT NOT NULL,
    definition_json JSON NOT NULL,
    standard_answer TEXT NULL,
    answer_analysis TEXT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    published_at DATETIME NULL,
    UNIQUE KEY uk_problem_version (problem_id, version),
    INDEX idx_problem_version_published (problem_id, status, published_at)
);

CREATE TABLE IF NOT EXISTS edu_problem_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    problem_id BIGINT NOT NULL,
    object_key VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_problem_image_object_key (object_key),
    INDEX idx_problem_image_problem_sort (problem_id, sort_order)
);

UPDATE edu_problem
SET title = CASE title
    WHEN '抛物线动点三角形面积最值' THEN '二次函数动点三角形面积最值'
    WHEN '单弹簧平衡与弹性势能' THEN '弹簧测力计与摩擦力'
    WHEN '光滑斜面滑块加速度' THEN '斜面机械效率'
    WHEN '等质量小球弹性碰撞' THEN '小球碰撞中的运动状态改变'
    WHEN '理想动滑轮拉力' THEN '动滑轮提升重物的机械效率'
    WHEN '平面镜反射角' THEN '平面镜反射的角度关系'
    WHEN '磁场中正电荷受力方向' THEN '通电螺线管的磁极判断'
    WHEN '串联电路电流' THEN '串联电路的电流与电压'
    WHEN '一元二次方程求根' THEN '矩形花圃的边长'
    WHEN '平面两点距离与中点' THEN '一次函数图像与三角形面积'
    WHEN '空间两点距离' THEN '一次函数与反比例函数交点'
    WHEN '正方体体对角线' THEN '正方体熔铸体积守恒'
    WHEN '四个数据的平均数' THEN '平均数、中位数与众数'
    ELSE title
END
WHERE title IN ('抛物线动点三角形面积最值', '单弹簧平衡与弹性势能', '光滑斜面滑块加速度', '等质量小球弹性碰撞', '理想动滑轮拉力', '平面镜反射角', '磁场中正电荷受力方向', '串联电路电流', '一元二次方程求根', '平面两点距离与中点', '空间两点距离', '正方体体对角线', '四个数据的平均数');

INSERT INTO edu_problem_subject (code, name, sort_order, status)
SELECT 'MATH', '数学', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM edu_problem_subject WHERE code = 'MATH');

INSERT INTO edu_problem_category (subject_id, name, description, sort_order, status)
SELECT s.id, '二次函数', '二次函数图像、性质与最值', 1, 1
FROM edu_problem_subject s
WHERE s.code = 'MATH'
  AND NOT EXISTS (
      SELECT 1 FROM edu_problem_category c
      WHERE c.subject_id = s.id AND c.name = '二次函数'
  );

INSERT INTO edu_problem (category_id, title, intro, difficulty, tags_json, current_version, status)
SELECT c.id, '二次函数动点三角形面积最值', '用二次函数与三角形面积模型求最值。', '中等',
       JSON_ARRAY('面积最值', '二次函数', '垂线'), 1, 1
FROM edu_problem_category c
WHERE c.name = '二次函数'
  AND NOT EXISTS (SELECT 1 FROM edu_problem WHERE title = '二次函数动点三角形面积最值');

INSERT INTO edu_problem_version (problem_id, version, definition_json, status, published_at)
SELECT p.id, 1,
       '{"schemaVersion":"1.0","stem":"抛物线 y = -x² + 2x + 3 与 x 轴交于 A(-1, 0)、B(3, 0)，与 y 轴交于 C(0, 3)。P 是第一象限内抛物线上的动点，求 △PBC 面积的最大值。","facts":{"variables":[{"id":"t","label":"P 的横坐标 t","range":[0,3],"value":0.8}],"pointP":"P(t, -t² + 2t + 3)","baseLine":"BC: x + y - 3 = 0","areaFormula":"S(t) = -1.5t² + 4.5t","latex":{"pointP":"P(t, -t^2 + 2t + 3)","baseLine":"BC: x + y - 3 = 0","areaFormula":"S(t) = -1.5t^2 + 4.5t"},"answer":"当 t = 1.5 时，△PBC 的面积最大，最大值为 3.375。"},"solutionGraph":{"nodes":[{"id":"choose-base","dependsOn":[]},{"id":"derive-area","dependsOn":["choose-base"]},{"id":"maximize-area","dependsOn":["derive-area"]}]},"lessonGraph":{"nodes":[{"id":"orient","kind":"ORIENT","transitions":{"mastered":"observe"}},{"id":"observe","kind":"OBSERVE","transitions":{"mastered":"represent"}},{"id":"represent","kind":"REPRESENT","transitions":{"mastered":"model"}},{"id":"model","kind":"MODEL","misconceptions":["wrong-height"],"transitions":{"mastered":"derive","misconception:wrong-height":"model"}},{"id":"derive","kind":"DERIVE","transitions":{"mastered":"verify"}},{"id":"verify","kind":"VERIFY_TRANSFER","transitions":{}}]},"visualCatalog":["show-perpendicular-height","highlight-BC"],"evaluation":{"acceptedAnswers":["3.375","27/8","t=1.5,smax=3.375","t=3/2,smax=27/8"],"misconceptions":[{"id":"wrong-height","matches":["1.76","-t²+3t","-t^2+3t"],"message":"1.76 是面积公式的中间量，不是 P 到 BC 的垂直距离。"}]},"sceneDefinition":{"schemaVersion":"1.0","sceneType":"coordinate2d","variables":{"t":{"value":0.8,"min":0,"max":3}},"entities":["parabola","A","B","C","P","trianglePBC","baseLine","altitude","areaChart"]},"cards":[{"id":"orient","kind":"ORIENT","title":"看清固定点、动点与目标","task":"找出固定底边和动点的取值范围。","scene":{"t":0.8,"showAltitude":false,"highlight":[]},"explanations":{"basic":"B、C 是固定点，P 在抛物线上移动。目标是比较不同位置的 △PBC 面积。","sprint":"先把不变部分 BC 和变量点 P 分开：BC 固定，P 的横坐标可记为 t。","perfect":"由第一象限条件和抛物线与 x 轴的交点可知，0 < t < 3。后续的函数最值必须回到这个定义域检验。"}},{"id":"observe","kind":"OBSERVE","title":"拖动 P，观察面积变化","task":"拖动图中的 P，预测面积最大的位置。","scene":{"t":0.8,"showAltitude":false,"highlight":["triangle"]},"explanations":{"basic":"拖动 P 时，三角形会变大再变小，最大值应该出现在中间附近。","sprint":"BC 不变，面积变化只由 P 到直线 BC 的距离决定。拖动只是帮助我们形成猜想，结论仍要计算验证。","perfect":"面积函数呈开口向下的二次关系。观察到的峰值位置将与二次函数顶点对应，但几何高度必须先定义正确。"}},{"id":"represent","kind":"REPRESENT","title":"用变量表示动点","task":"写出 P 的坐标，并保留 t 的取值限制。","scene":{"t":1.1,"showAltitude":false,"highlight":["P"]},"explanations":{"basic":"令 P 的横坐标为 t，代入抛物线就得到 P(t, -t² + 2t + 3)。","sprint":"把几何对象坐标化：P 的横坐标是 t，纵坐标由函数值唯一决定。","perfect":"变量选择要让约束最少。用横坐标 t 可直接利用函数表达式，同时定义域由第一象限和交点边界共同限制。"}},{"id":"model","kind":"MODEL","title":"作出 P 到 BC 的垂线","task":"判断图中的 h 为什么必须垂直于 BC。","scene":{"t":1.5,"showAltitude":true,"highlight":["BC","altitude"]},"explanations":{"basic":"选 BC 作底时，高必须是 P 到 BC 所在直线的垂直距离，不能直接用竖直长度。","sprint":"BC 的方程是 x + y - 3 = 0。点到直线距离给出 h = (-t² + 3t) / √2。","perfect":"BC 是斜边，铅垂方向不与 BC 垂直。由点 P 到 x + y - 3 = 0 的距离，分子为 |t + (-t² + 2t + 3) - 3| = -t² + 3t。"}},{"id":"derive","kind":"DERIVE","title":"得到面积函数并求最值","task":"完成面积函数的配方或指出其顶点横坐标。","scene":{"t":1.5,"showAltitude":true,"highlight":["areaChart","P"]},"explanations":{"basic":"S(t) = -1.5t² + 4.5t 是开口向下的二次函数，顶点在 t = 1.5。","sprint":"配方得 S(t) = -1.5(t - 1.5)² + 3.375，所以最大面积是 3.375。","perfect":"二次项系数为负，顶点即全局最大值。t = -b/(2a) = 1.5 落在 0 < t < 3 内，因此最大值可取。"}},{"id":"verify","kind":"VERIFY_TRANSFER","title":"回代验证结论","task":"检查 t = 1.5 时 P 的位置和面积是否一致。","scene":{"t":1.5,"showAltitude":true,"highlight":["triangle","BC","altitude"]},"explanations":{"basic":"t = 1.5 在允许范围内，代回面积函数得到 3.375，结论成立。","sprint":"此时 P(1.5, 3.75)，垂线高和固定底 BC 对应的面积与函数值一致。","perfect":"完成三项验证：定义域满足、顶点位置可取、几何面积模型与二次函数数值相符。改变底边时，要重新选择对应的垂直高。"}}]}',
       1, NOW()
FROM edu_problem p
WHERE p.title = '二次函数动点三角形面积最值'
  AND NOT EXISTS (SELECT 1 FROM edu_problem_version v WHERE v.problem_id = p.id AND v.version = 1);

INSERT INTO edu_problem_subject (code, name, sort_order, status)
SELECT 'PHYSICS', '物理', 2, 1
WHERE NOT EXISTS (SELECT 1 FROM edu_problem_subject WHERE code = 'PHYSICS');

INSERT INTO edu_problem_category (subject_id, name, description, sort_order, status)
SELECT s.id, '力学', '受力、运动、功和简单机械', 1, 1
FROM edu_problem_subject s
WHERE s.code = 'PHYSICS'
  AND NOT EXISTS (
      SELECT 1 FROM edu_problem_category c
      WHERE c.subject_id = s.id AND c.name = '力学'
  );

INSERT INTO edu_problem (category_id, title, intro, difficulty, tags_json, current_version, status)
SELECT c.id, '弹簧测力计与摩擦力', '利用匀速运动时的平衡关系分析摩擦力。', '中等',
       JSON_ARRAY('胡克定律', '受力平衡', '弹性势能'), 1, 1
FROM edu_problem_category c
WHERE c.name = '力学'
  AND NOT EXISTS (SELECT 1 FROM edu_problem WHERE title = '弹簧测力计与摩擦力');

INSERT INTO edu_problem_version (problem_id, version, definition_json, status, published_at)
SELECT p.id, 1,
       '{"schemaVersion":"1.0","domain":"physics.mechanics","stem":"一轻质弹簧左端固定在光滑水平面上，右端连接物块。弹簧劲度系数 k = 20 N/m。用水平向右的外力使物块静止时，弹簧伸长 x = 0.40 m。求弹簧对物块的弹力大小和弹簧的弹性势能。","facts":{"variables":[{"id":"extension","label":"弹簧伸长量 x","range":[0,0.8],"value":0.4}],"constants":{"k":"20 N/m"},"forceFormula":"F = kx","energyFormula":"E_p = 1/2 kx^2","latex":{"force":"F = kx","energy":"E_p = \\frac{1}{2}kx^2","equilibrium":"F_{\\text{外}} = F_{\\text{弹}}"},"answer":"弹簧弹力大小为 8 N，弹性势能为 1.60 J。"},"solutionGraph":{"nodes":[{"id":"identify","dependsOn":[]},{"id":"represent","dependsOn":["identify"]},{"id":"derive","dependsOn":["represent"]}]},"lessonGraph":{"nodes":[{"id":"orient","kind":"ORIENT","transitions":{"mastered":"observe"}},{"id":"observe","kind":"OBSERVE","transitions":{"mastered":"represent"}},{"id":"represent","kind":"REPRESENT","transitions":{"mastered":"model"}},{"id":"model","kind":"MODEL","misconceptions":["half-force"],"transitions":{"mastered":"derive","misconception:half-force":"model"}},{"id":"derive","kind":"DERIVE","transitions":{"mastered":"verify"}},{"id":"verify","kind":"VERIFY_TRANSFER","transitions":{}}]},"visualCatalog":["show-spring-force","show-energy-chart"],"evaluation":{"acceptedAnswers":["8n1.6j","8n;1.6j","8N，1.60J"],"misconceptions":[{"id":"half-force","matches":["4n","1/2kx"],"message":"弹力大小由 F = kx 给出，二分之一只出现在弹性势能公式中。"},{"id":"double-energy","matches":["3.2j"],"message":"弹性势能应为 E_p = 1/2 kx^2，不能漏掉二分之一。"}]},"sceneDefinition":{"schemaVersion":"1.0","sceneType":"mechanics2d","chartType":"chart2d","variables":{"extension":{"value":0.4,"min":0,"max":0.8}},"entities":["wall","spring","mass","forceArrow","forceChart","energyChart"]},"cards":[{"id":"orient","kind":"ORIENT","title":"看清研究对象与已知量","task":"找出弹簧劲度系数、伸长量和求解目标。","scene":{"extension":0.4,"showForce":false,"showEnergyChart":false,"highlight":[]},"explanations":{"basic":"研究对象是物块和弹簧。已知 k = 20 N/m、x = 0.40 m，要分别求弹力和弹性势能。","sprint":"题目给出的是伸长量，不是弹簧总长度；胡克定律中的 x 必须是形变量。","perfect":"光滑水平面说明水平方向只需比较外力和弹力，物块静止意味着合力为零。"}},{"id":"observe","kind":"OBSERVE","title":"改变伸长量，观察弹力变化","task":"拖动物块，预测伸长量增大时弹力如何变化。","scene":{"extension":0.2,"showForce":false,"showEnergyChart":false,"highlight":["spring"]},"explanations":{"basic":"弹簧拉得越长，回复弹力越大。","sprint":"在弹性限度内，F 与 x 成正比，图像是一条过原点的直线。","perfect":"比例关系的斜率就是劲度系数 k，单位为 N/m。"}},{"id":"represent","kind":"REPRESENT","title":"用 x 表示形变量","task":"写出本题中 x、k 的数值与单位。","scene":{"extension":0.4,"showForce":false,"showEnergyChart":false,"highlight":["spring"]},"explanations":{"basic":"取弹簧伸长量 x = 0.40 m，劲度系数 k = 20 N/m。","sprint":"单位要统一为国际单位制，0.40 m 可以直接代入。","perfect":"这里讨论的是弹力大小；方向由弹簧形变决定，伸长时弹簧对物块的力向左。"}},{"id":"model","kind":"MODEL","title":"建立平衡与胡克定律","task":"说明静止时外力与弹力的大小关系。","scene":{"extension":0.4,"showForce":true,"showEnergyChart":false,"highlight":["forceArrow","spring"]},"explanations":{"basic":"物块静止，外力向右、弹簧弹力向左，两力大小相等。弹力大小 F = kx。","sprint":"代入 k = 20 N/m、x = 0.40 m，先得到 F = 20 × 0.40。","perfect":"平衡条件是矢量合力为零；本题只求大小，因此 F外 = F弹 = kx。"}},{"id":"derive","kind":"DERIVE","title":"计算弹力和弹性势能","task":"分别代入 F = kx 与 E_p = 1/2 kx²。","scene":{"extension":0.4,"showForce":true,"showEnergyChart":true,"highlight":["forceArrow","energyChart"]},"explanations":{"basic":"弹力 F = 20 × 0.40 = 8 N。弹性势能 E_p = 1/2 × 20 × 0.40² = 1.60 J。","sprint":"不要把 1/2 放进弹力公式；它来自弹簧从 0 拉到 x 时，力随位移线性增大。","perfect":"E_p 等于 F-x 图像下的三角形面积，即 1/2 × x × F，因此也得到 1/2 kx²。"}},{"id":"verify","kind":"VERIFY_TRANSFER","title":"检查方向、单位与数量级","task":"验证弹力方向、能量单位，并判断 x 加倍后的变化。","scene":{"extension":0.4,"showForce":true,"showEnergyChart":true,"highlight":["forceArrow","energyChart"]},"explanations":{"basic":"弹簧对物块的力向左，弹力单位是 N，能量单位是 J。","sprint":"若 x 加倍，弹力加倍；弹性势能与 x² 成正比，会变为原来的四倍。","perfect":"用平衡检验方向、用 F = kx 检验单位、用 E_p = 1/2 kx² 检验数量级，三项都一致。"}}]}',
       1, NOW()
FROM edu_problem p
WHERE p.title = '弹簧测力计与摩擦力'
  AND NOT EXISTS (SELECT 1 FROM edu_problem_version v WHERE v.problem_id = p.id AND v.version = 1);

INSERT INTO edu_problem_category (subject_id, name, description, sort_order, status)
SELECT s.id, c.name, c.description, c.sort_order, 1
FROM edu_problem_subject s
CROSS JOIN (
    SELECT '光学' AS name, '光的直线传播、反射与折射' AS description, 2 AS sort_order
    UNION ALL SELECT '电磁学', '电流的磁场与通电螺线管', 3
    UNION ALL SELECT '电学', '电路、电流、电压与电阻', 4
) c
WHERE s.code = 'PHYSICS'
  AND NOT EXISTS (SELECT 1 FROM edu_problem_category pc WHERE pc.subject_id = s.id AND pc.name = c.name);

INSERT INTO edu_problem (category_id, title, intro, difficulty, tags_json, current_version, status)
SELECT c.id, '斜面机械效率', '结合有用功、总功和额外功分析斜面。', '中等',
       JSON_ARRAY('斜面', '受力分析', '牛顿第二定律'), 1, 1
FROM edu_problem_category c
WHERE c.name = '力学'
  AND NOT EXISTS (SELECT 1 FROM edu_problem WHERE title = '斜面机械效率');

INSERT INTO edu_problem_version (problem_id, version, definition_json, standard_answer, answer_analysis, status, published_at)
SELECT p.id, 1,
       '{"schemaVersion":"2.0","stem":"质量为 2 kg 的滑块从倾角 30° 的光滑斜面由静止释放，取 g=10 m/s²。求滑块沿斜面向下的加速度大小。","facts":{"answer":"a = 5 m/s²"},"evaluation":{"acceptedAnswers":["5","5m/s2","5 m/s²"],"misconceptions":[{"id":"cosine-error","matches":["8.66","5√3"],"message":"沿斜面方向的重力分力为 mg sin θ。"}]},"sceneDefinition":{"sceneType":"physicsInclinedPlane","variables":{},"drawSteps":{"UNDERSTAND":["scene"]}}}',
       '$a=5 m/s²$。',
       '沿斜面方向只有重力分力 $mg sin 30°$。由牛顿第二定律，$ma=mg sin 30°$，所以 $a=g sin 30°=5 m/s²$。',
       1, NOW()
FROM edu_problem p
WHERE p.title = '斜面机械效率'
  AND NOT EXISTS (SELECT 1 FROM edu_problem_version v WHERE v.problem_id = p.id AND v.version = 1);

INSERT INTO edu_problem (category_id, title, intro, difficulty, tags_json, current_version, status)
SELECT c.id, '小球碰撞中的运动状态改变', '从碰撞现象归纳力改变物体运动状态。', '中等',
       JSON_ARRAY('碰撞', '动量守恒', '机械能守恒'), 1, 1
FROM edu_problem_category c
WHERE c.name = '力学'
  AND NOT EXISTS (SELECT 1 FROM edu_problem WHERE title = '小球碰撞中的运动状态改变');

INSERT INTO edu_problem_version (problem_id, version, definition_json, standard_answer, answer_analysis, status, published_at)
SELECT p.id, 1,
       '{"schemaVersion":"2.0","stem":"光滑水平面上，质量相等的小球 A 以 4 m/s 向右运动，与静止小球 B 发生一维完全弹性碰撞。求碰撞后两球的速度。","facts":{"answer":"A 停止，B 以 4 m/s 向右运动"},"evaluation":{"acceptedAnswers":["A=0,B=4","0,4","A停止B4m/s"],"misconceptions":[{"id":"same-speed-error","matches":["2,2"],"message":"等质量的一维完全弹性碰撞会交换速度。"}]},"sceneDefinition":{"sceneType":"physicsCollision","variables":{},"drawSteps":{"UNDERSTAND":["scene"]}}}',
       '碰撞后 A 的速度为 $0$，B 的速度为 $4 m/s$，方向向右。',
       '等质量小球发生一维完全弹性碰撞时会交换速度。B 原来静止，所以 A 碰后停止，B 继承 A 原来的速度 $4 m/s$ 向右。',
       1, NOW()
FROM edu_problem p
WHERE p.title = '小球碰撞中的运动状态改变'
  AND NOT EXISTS (SELECT 1 FROM edu_problem_version v WHERE v.problem_id = p.id AND v.version = 1);

INSERT INTO edu_problem (category_id, title, intro, difficulty, tags_json, current_version, status)
SELECT c.id, '动滑轮提升重物的机械效率', '用功和功率关系分析动滑轮。', '中等',
       JSON_ARRAY('动滑轮', '受力平衡', '机械简单装置'), 1, 1
FROM edu_problem_category c
WHERE c.name = '力学'
  AND NOT EXISTS (SELECT 1 FROM edu_problem WHERE title = '动滑轮提升重物的机械效率');

INSERT INTO edu_problem_version (problem_id, version, definition_json, standard_answer, answer_analysis, status, published_at)
SELECT p.id, 1,
       '{"schemaVersion":"2.0","stem":"用理想动滑轮匀速提升重力为 80 N 的物体，不计滑轮和绳重及摩擦。求绳端拉力 F。","facts":{"answer":"F = 40 N"},"evaluation":{"acceptedAnswers":["40","40n","40 N"],"misconceptions":[{"id":"fixed-pulley-error","matches":["80","80n"],"message":"动滑轮由两段绳子共同承担重力。"}]},"sceneDefinition":{"sceneType":"physicsPulley","variables":{},"drawSteps":{"UNDERSTAND":["scene"]}}}',
       '$F=40 N$。',
       '理想绳两端张力相等，动滑轮受到两段绳子的向上拉力，总上拉力为 $2F$。匀速时 $2F=80 N$，所以 $F=40 N$。',
       1, NOW()
FROM edu_problem p
WHERE p.title = '动滑轮提升重物的机械效率'
  AND NOT EXISTS (SELECT 1 FROM edu_problem_version v WHERE v.problem_id = p.id AND v.version = 1);

INSERT INTO edu_problem (category_id, title, intro, difficulty, tags_json, current_version, status)
SELECT c.id, '平面镜反射的角度关系', '辨析镜面夹角、法线夹角与反射角。', '中等',
       JSON_ARRAY('平面镜', '光的反射', '反射定律'), 1, 1
FROM edu_problem_category c
WHERE c.name = '光学'
  AND NOT EXISTS (SELECT 1 FROM edu_problem WHERE title = '平面镜反射的角度关系');

INSERT INTO edu_problem_version (problem_id, version, definition_json, standard_answer, answer_analysis, status, published_at)
SELECT p.id, 1,
       '{"schemaVersion":"2.0","stem":"一束光线射向平面镜，入射角为 35°。求反射角。","facts":{"answer":"反射角为 35°"},"evaluation":{"acceptedAnswers":["35","35°","35度"],"misconceptions":[{"id":"mirror-angle-error","matches":["55"],"message":"入射角和反射角都相对于法线测量。"}]},"sceneDefinition":{"sceneType":"physicsReflection","variables":{},"drawSteps":{"UNDERSTAND":["scene"]}}}',
       '反射角为 35°。',
       '反射定律指出：反射角等于入射角，且两角都以法线为基准。因此反射角为 35°。',
       1, NOW()
FROM edu_problem p
WHERE p.title = '平面镜反射的角度关系'
  AND NOT EXISTS (SELECT 1 FROM edu_problem_version v WHERE v.problem_id = p.id AND v.version = 1);

INSERT INTO edu_problem (category_id, title, intro, difficulty, tags_json, current_version, status)
SELECT c.id, '通电螺线管的磁极判断', '用安培定则判断通电螺线管的磁极。', '中等',
       JSON_ARRAY('磁场', '洛伦兹力', '左手定则'), 1, 1
FROM edu_problem_category c
WHERE c.name = '电磁学'
  AND NOT EXISTS (SELECT 1 FROM edu_problem WHERE title = '通电螺线管的磁极判断');

INSERT INTO edu_problem_version (problem_id, version, definition_json, standard_answer, answer_analysis, status, published_at)
SELECT p.id, 1,
       '{"schemaVersion":"2.0","stem":"正电荷以速度 $v$ 水平向右进入垂直纸面向里的匀强磁场。求其刚进入磁场时所受洛伦兹力方向。","facts":{"answer":"洛伦兹力方向竖直向上"},"evaluation":{"acceptedAnswers":["向上","竖直向上","上"],"misconceptions":[{"id":"negative-charge-error","matches":["向下","下"],"message":"题目中的电荷为正电荷，方向不需要反向。"}]},"sceneDefinition":{"sceneType":"physicsMagneticField","variables":{},"drawSteps":{"UNDERSTAND":["scene"]}}}',
       '洛伦兹力方向竖直向上。',
       '对正电荷使用左手定则，四指指向速度向右，磁感线垂直纸面向里，拇指所指即为受力方向，故力竖直向上。',
       1, NOW()
FROM edu_problem p
WHERE p.title = '通电螺线管的磁极判断'
  AND NOT EXISTS (SELECT 1 FROM edu_problem_version v WHERE v.problem_id = p.id AND v.version = 1);

INSERT INTO edu_problem (category_id, title, intro, difficulty, tags_json, current_version, status)
SELECT c.id, '串联电路的电流与电压', '综合运用串联电路规律和欧姆定律。', '中等',
       JSON_ARRAY('欧姆定律', '串联电路', '电流'), 1, 1
FROM edu_problem_category c
WHERE c.name = '电学'
  AND NOT EXISTS (SELECT 1 FROM edu_problem WHERE title = '串联电路的电流与电压');

INSERT INTO edu_problem_version (problem_id, version, definition_json, standard_answer, answer_analysis, status, published_at)
SELECT p.id, 1,
       '{"schemaVersion":"2.0","stem":"电源电压为 12 V，与阻值 6 Ω 的电阻和理想电流表串联。求电路中的电流。","facts":{"answer":"I = 2 A"},"evaluation":{"acceptedAnswers":["2","2a","2 A"],"misconceptions":[{"id":"inverse-error","matches":["72"],"message":"电流由 I=U/R 计算，不是 U×R。"}]},"sceneDefinition":{"sceneType":"physicsCircuit","variables":{},"drawSteps":{"UNDERSTAND":["scene"]}}}',
       '$I=2 A$。',
       '理想电流表电阻可忽略，电路总电阻为 $R=6 Ω$。由欧姆定律 $I=U/R=12/6=2 A$。',
       1, NOW()
FROM edu_problem p
WHERE p.title = '串联电路的电流与电压'
  AND NOT EXISTS (SELECT 1 FROM edu_problem_version v WHERE v.problem_id = p.id AND v.version = 1);

INSERT INTO edu_problem_category (subject_id, name, description, sort_order, status)
SELECT s.id, c.name, c.description, c.sort_order, 1
FROM edu_problem_subject s
CROSS JOIN (
    SELECT '代数基础' AS name, '方程、代数式与基础运算' AS description, 2 AS sort_order
    UNION ALL SELECT '平面解析几何', '平面直角坐标系与距离关系', 3
    UNION ALL SELECT '平面几何', '三角形、四边形与圆的性质', 4
    UNION ALL SELECT '空间几何', '空间图形、距离与体积', 5
    UNION ALL SELECT '统计与概率', '数据分析与随机事件概率', 6
) c
WHERE s.code = 'MATH'
  AND NOT EXISTS (SELECT 1 FROM edu_problem_category pc WHERE pc.subject_id = s.id AND pc.name = c.name);

INSERT INTO edu_problem (category_id, title, intro, difficulty, tags_json, create_by, current_version, status)
SELECT c.id, '矩形花圃的边长', '列一元二次方程解决实际问题。', '中等',
       JSON_ARRAY('一元二次方程', '因式分解'), 2, 1, 1
FROM edu_problem_category c
WHERE c.name = '代数基础'
  AND NOT EXISTS (SELECT 1 FROM edu_problem WHERE title = '矩形花圃的边长');

INSERT INTO edu_problem_version (problem_id, version, definition_json, standard_answer, answer_analysis, status, published_at)
SELECT p.id, 1,
       '{"schemaVersion":"2.0","stem":"解方程：$x² - 5x + 6 = 0$。","facts":{"answer":"x = 2 或 x = 3"},"evaluation":{"acceptedAnswers":["x=2或x=3","2,3","x=2,x=3"],"misconceptions":[{"id":"sign-error","matches":["-2","-3"],"message":"分解后的两个一次因式都应分别等于 0。"}]},"visualCatalog":[],"sceneDefinition":{"sceneType":"textOnly","variables":{}}}',
       'x = 2 或 x = 3。',
       '将方程左边因式分解：$x² - 5x + 6 = (x - 2)(x - 3)$。\n\n由零乘积定理，$(x - 2)(x - 3)=0$ 时，$x - 2=0$ 或 $x - 3=0$。\n\n因此 $x=2$ 或 $x=3$。把两个值分别代回原方程，等式都成立。',
       1, NOW()
FROM edu_problem p
WHERE p.title = '矩形花圃的边长'
  AND NOT EXISTS (SELECT 1 FROM edu_problem_version v WHERE v.problem_id = p.id AND v.version = 1);

INSERT INTO edu_problem (category_id, title, intro, difficulty, tags_json, create_by, current_version, status)
SELECT c.id, '一次函数图像与三角形面积', '利用一次函数图像与坐标轴围成图形。', '中等',
       JSON_ARRAY('两点距离', '中点坐标', '平面坐标系'), 2, 1, 1
FROM edu_problem_category c
WHERE c.name = '平面解析几何'
  AND NOT EXISTS (SELECT 1 FROM edu_problem WHERE title = '一次函数图像与三角形面积');

INSERT INTO edu_problem_version (problem_id, version, definition_json, standard_answer, answer_analysis, status, published_at)
SELECT p.id, 1,
       '{"schemaVersion":"2.0","stem":"在平面直角坐标系中，已知 $A(-2,1)$、$B(4,5)$。求线段 $AB$ 的长度，以及 $AB$ 中点 $M$ 的坐标。","facts":{"answer":"AB = 2√13，M(1,3)"},"evaluation":{"acceptedAnswers":["2√13,(1,3)","AB=2√13,M(1,3)"],"misconceptions":[{"id":"difference-error","matches":["√20","2√5"],"message":"横坐标差为 6，纵坐标差为 4。"}]},"visualCatalog":["draw-math-scene"],"sceneDefinition":{"sceneType":"coordinatePlane2d","variables":{},"entities":[{"id":"A","type":"point","x":-2,"y":1,"label":"A(-2,1)"},{"id":"B","type":"point","x":4,"y":5,"label":"B(4,5)"},{"id":"M","type":"point","x":1,"y":3,"label":"M(1,3)"},{"id":"AB","type":"segment","from":"A","to":"B"}],"drawSteps":{"UNDERSTAND":["A","B"],"MODEL":["AB","M"],"DERIVE":["AB","M"],"VERIFY":["A","B","M","AB"]}}}',
       '$AB = 2√13$，中点 $M(1,3)$。',
       '两点的横坐标差为 $4-(-2)=6$，纵坐标差为 $5-1=4$。\n\n由两点间距离公式，$AB=√(6²+4²)=√52=2√13$。\n\n中点坐标等于两端点对应坐标的平均数，所以 $M((−2+4)/2,(1+5)/2)=(1,3)$。中点 $M$ 位于线段 $AB$ 上，且到两端点距离相等。',
       1, NOW()
FROM edu_problem p
WHERE p.title = '一次函数图像与三角形面积'
  AND NOT EXISTS (SELECT 1 FROM edu_problem_version v WHERE v.problem_id = p.id AND v.version = 1);

INSERT INTO edu_problem (category_id, title, intro, difficulty, tags_json, create_by, current_version, status)
SELECT c.id, '一次函数与反比例函数交点', '联立函数关系求图像交点。', '中等',
       JSON_ARRAY('空间直角坐标系', '两点距离', 'XYZ坐标系'), 2, 1, 1
FROM edu_problem_category c
WHERE c.name = '空间几何'
  AND NOT EXISTS (SELECT 1 FROM edu_problem WHERE title = '一次函数与反比例函数交点');

INSERT INTO edu_problem_version (problem_id, version, definition_json, standard_answer, answer_analysis, status, published_at)
SELECT p.id, 1,
       '{"schemaVersion":"2.0","stem":"在空间直角坐标系中，已知 $A(1,-2,3)$、$B(4,2,6)$，求线段 $AB$ 的长度。","facts":{"answer":"AB = √34"},"evaluation":{"acceptedAnswers":["√34","AB=√34"],"misconceptions":[{"id":"omit-z","matches":["5","√25"],"message":"空间距离还要计入 z 坐标差。"}]},"visualCatalog":["draw-math-scene"],"sceneDefinition":{"sceneType":"coordinate3d","variables":{},"entities":[{"id":"A","type":"point","x":1,"y":-2,"z":3,"label":"A(1,-2,3)"},{"id":"B","type":"point","x":4,"y":2,"z":6,"label":"B(4,2,6)"},{"id":"AB","type":"segment","from":"A","to":"B"}],"drawSteps":{"UNDERSTAND":["A","B"],"MODEL":["AB"],"DERIVE":["AB"],"VERIFY":["A","B","AB"]}}}',
       '$AB = √34$。',
       '三组坐标的差分别为：$Δx=4-1=3$，$Δy=2-(-2)=4$，$Δz=6-3=3$。\n\n空间两点距离公式为 $AB=√(Δx²+Δy²+Δz²)$。\n\n代入得 $AB=√(3²+4²+3²)=√34$。三个坐标方向都参与计算，不能遗漏 z 坐标差。',
       1, NOW()
FROM edu_problem p
WHERE p.title = '一次函数与反比例函数交点'
  AND NOT EXISTS (SELECT 1 FROM edu_problem_version v WHERE v.problem_id = p.id AND v.version = 1);

INSERT INTO edu_problem (category_id, title, intro, difficulty, tags_json, create_by, current_version, status)
SELECT c.id, '等腰三角形面积', '通过作高把等腰三角形分成两个直角三角形。', '中等',
       JSON_ARRAY('等腰三角形', '勾股定理', '三角形面积'), 2, 1, 1
FROM edu_problem_category c
WHERE c.name = '平面几何'
  AND NOT EXISTS (SELECT 1 FROM edu_problem WHERE title = '等腰三角形面积');

INSERT INTO edu_problem_version (problem_id, version, definition_json, standard_answer, answer_analysis, status, published_at)
SELECT p.id, 1,
       '{"schemaVersion":"2.0","stem":"在等腰三角形 $ABC$ 中，$AB=10$，$AC=BC=13$。求三角形 $ABC$ 的面积。","facts":{"answer":"S△ABC = 60"},"evaluation":{"acceptedAnswers":["60","S=60"],"misconceptions":[{"id":"height-error","matches":["65","130"],"message":"面积公式中要使用对应底边的垂直高，并除以 2。"}]},"visualCatalog":["draw-math-scene"],"sceneDefinition":{"sceneType":"geometry2d","variables":{},"entities":[{"id":"A","type":"point","x":-5,"y":0,"label":"A"},{"id":"B","type":"point","x":5,"y":0,"label":"B"},{"id":"C","type":"point","x":0,"y":12,"label":"C"},{"id":"M","type":"point","x":0,"y":0,"label":"M"},{"id":"ABC","type":"polygon","points":["A","B","C"]},{"id":"CM","type":"segment","from":"C","to":"M"}],"drawSteps":{"UNDERSTAND":["A","B","C","ABC"],"MODEL":["M","CM"],"DERIVE":["ABC","CM"],"VERIFY":["A","B","C","M","ABC","CM"]}}}',
       '$S△ABC = 60$。',
       '作顶点 $C$ 到底边 $AB$ 的高 $CM$。等腰三角形的顶角平分线、底边中线和高重合，因此 $AM=BM=AB/2=5$。\n\n在直角三角形 $ACM$ 中，$AC=13$，$AM=5$。由勾股定理：$CM=√(13²-5²)=√144=12$。\n\n所以 $S△ABC=1/2×AB×CM=1/2×10×12=60$。',
       1, NOW()
FROM edu_problem p
WHERE p.title = '等腰三角形面积'
  AND NOT EXISTS (SELECT 1 FROM edu_problem_version v WHERE v.problem_id = p.id AND v.version = 1);

INSERT INTO edu_problem (category_id, title, intro, difficulty, tags_json, create_by, current_version, status)
SELECT c.id, '正方体熔铸体积守恒', '运用体积守恒解决立体几何问题。', '中等',
       JSON_ARRAY('正方体', '体对角线', '空间勾股定理'), 2, 1, 1
FROM edu_problem_category c
WHERE c.name = '空间几何'
  AND NOT EXISTS (SELECT 1 FROM edu_problem WHERE title = '正方体熔铸体积守恒');

INSERT INTO edu_problem_version (problem_id, version, definition_json, standard_answer, answer_analysis, status, published_at)
SELECT p.id, 1,
       '{"schemaVersion":"2.0","stem":"正方体 $ABCD-A₁B₁C₁D₁$ 的棱长为 $2$，求体对角线 $AC₁$ 的长度。","facts":{"answer":"AC₁ = 2√3"},"evaluation":{"acceptedAnswers":["2√3","AC1=2√3"],"misconceptions":[{"id":"face-diagonal","matches":["2√2"],"message":"2√2 是正方体的面对角线，不是体对角线。"}]},"visualCatalog":["draw-math-scene"],"sceneDefinition":{"sceneType":"geometry3d","variables":{},"entities":[{"id":"A","type":"point","x":-1,"y":-1,"z":-1},{"id":"B","type":"point","x":1,"y":-1,"z":-1},{"id":"C","type":"point","x":1,"y":1,"z":-1},{"id":"D","type":"point","x":-1,"y":1,"z":-1},{"id":"A1","type":"point","x":-1,"y":-1,"z":1},{"id":"B1","type":"point","x":1,"y":-1,"z":1},{"id":"C1","type":"point","x":1,"y":1,"z":1},{"id":"D1","type":"point","x":-1,"y":1,"z":1},{"id":"ABCD","type":"polygon","points":["A","B","C","D"]},{"id":"A1B1C1D1","type":"polygon","points":["A1","B1","C1","D1"]},{"id":"AC1","type":"segment","from":"A","to":"C1"}],"drawSteps":{"UNDERSTAND":["A","B","C","D","A1","B1","C1","D1"],"MODEL":["ABCD","A1B1C1D1"],"DERIVE":["AC1"],"VERIFY":["ABCD","A1B1C1D1","AC1"]}}}',
       '$AC₁ = 2√3$。',
       '先看底面正方形 $ABCD$，其面对角线 $AC=√(2²+2²)=2√2$。\n\n线段 $AC$ 与竖直棱 $CC₁$ 垂直，且 $CC₁=2$，所以三角形 $ACC₁$ 是直角三角形。\n\n由勾股定理，$AC₁=√(AC²+CC₁²)=√((2√2)²+2²)=√12=2√3$。',
       1, NOW()
FROM edu_problem p
WHERE p.title = '正方体熔铸体积守恒'
  AND NOT EXISTS (SELECT 1 FROM edu_problem_version v WHERE v.problem_id = p.id AND v.version = 1);

INSERT INTO edu_problem (category_id, title, intro, difficulty, tags_json, create_by, current_version, status)
SELECT c.id, '平均数、中位数与众数', '综合分析一组数据的集中趋势。', '中等',
       JSON_ARRAY('平均数', '统计图', '数据分析'), 2, 1, 1
FROM edu_problem_category c
WHERE c.name = '统计与概率'
  AND NOT EXISTS (SELECT 1 FROM edu_problem WHERE title = '平均数、中位数与众数');

INSERT INTO edu_problem_version (problem_id, version, definition_json, standard_answer, answer_analysis, status, published_at)
SELECT p.id, 1,
       '{"schemaVersion":"2.0","stem":"某小组四名同学的成绩分别为 $10、12、16、12$ 分。求这四个数据的平均数。","facts":{"answer":"平均数为 12.5"},"evaluation":{"acceptedAnswers":["12.5","25/2"],"misconceptions":[{"id":"divide-error","matches":["50","13"],"message":"平均数等于总和除以数据个数。"}]},"visualCatalog":["draw-math-scene"],"sceneDefinition":{"sceneType":"statistics2d","variables":{},"entities":[{"id":"score1","type":"bar","label":"第1人","value":10},{"id":"score2","type":"bar","label":"第2人","value":12},{"id":"score3","type":"bar","label":"第3人","value":16},{"id":"score4","type":"bar","label":"第4人","value":12}],"drawSteps":{"UNDERSTAND":["score1","score2","score3","score4"],"MODEL":["score1","score2","score3","score4"],"DERIVE":["score1","score2","score3","score4"],"VERIFY":["score1","score2","score3","score4"]}}}',
       '平均数为 $12.5$。',
       '先求四个数据的总和：$10+12+16+12=50$。\n\n数据个数为 $4$，因此平均数为 $50÷4=12.5$。\n\n平均数 12.5 介于最小值 10 和最大值 16 之间，符合平均数的数量级。',
       1, NOW()
FROM edu_problem p
WHERE p.title = '平均数、中位数与众数'
  AND NOT EXISTS (SELECT 1 FROM edu_problem_version v WHERE v.problem_id = p.id AND v.version = 1);

INSERT INTO edu_problem (category_id, title, intro, difficulty, tags_json, create_by, current_version, status)
SELECT c.id, '不放回摸球概率', '用概率树状图计算连续不放回摸球的联合概率。', '中等',
       JSON_ARRAY('概率树', '不放回', '联合概率'), 2, 1, 1
FROM edu_problem_category c
WHERE c.name = '统计与概率'
  AND NOT EXISTS (SELECT 1 FROM edu_problem WHERE title = '不放回摸球概率');

INSERT INTO edu_problem_version (problem_id, version, definition_json, standard_answer, answer_analysis, status, published_at)
SELECT p.id, 1,
       '{"schemaVersion":"2.0","stem":"一个盒子中有 3 个红球和 2 个蓝球，除颜色外完全相同。连续随机摸取 2 个球，每次摸取后不放回。求两次都摸到红球的概率。","facts":{"answer":"P(两次都摸到红球) = 3/10"},"evaluation":{"acceptedAnswers":["3/10","0.3"],"misconceptions":[{"id":"replacement-error","matches":["9/25"],"message":"第一次摸出后不放回，第二次的球总数和红球数都会改变。"}]},"visualCatalog":["draw-math-scene"],"sceneDefinition":{"sceneType":"probabilityTree","variables":{},"entities":[{"id":"start","type":"node","x":80,"y":210,"label":"开始"},{"id":"red1","type":"node","x":300,"y":110,"label":"第一次红"},{"id":"blue1","type":"node","x":300,"y":310,"label":"第一次蓝"},{"id":"redred","type":"node","x":570,"y":60,"label":"红红"},{"id":"redblue","type":"node","x":570,"y":160,"label":"红蓝"},{"id":"bluered","type":"node","x":570,"y":260,"label":"蓝红"},{"id":"blueblue","type":"node","x":570,"y":360,"label":"蓝蓝"},{"id":"start-red","type":"branch","from":"start","to":"red1","label":"3/5"},{"id":"start-blue","type":"branch","from":"start","to":"blue1","label":"2/5"},{"id":"red-red","type":"branch","from":"red1","to":"redred","label":"2/4"},{"id":"red-blue","type":"branch","from":"red1","to":"redblue","label":"2/4"},{"id":"blue-red","type":"branch","from":"blue1","to":"bluered","label":"3/4"},{"id":"blue-blue","type":"branch","from":"blue1","to":"blueblue","label":"1/4"}],"drawSteps":{"UNDERSTAND":["start","red1","blue1"],"MODEL":["start-red","start-blue","red-red","red-blue","blue-red","blue-blue"],"DERIVE":["redred","start-red","red-red"],"VERIFY":["start","red1","blue1","redred","redblue","bluered","blueblue"]}}}',
       '$P(两次都摸到红球)=3/10$。',
       '第一次从 5 个球中摸到红球的概率是 $3/5$。若第一次已经摸到红球，盒中剩下 2 个红球和 2 个蓝球，共 4 个球。\n\n此时第二次仍摸到红球的条件概率是 $2/4$。\n\n沿概率树的“红-红”分支相乘：$3/5×2/4=6/20=3/10$。由于是不放回摸取，第二次不能仍按 $3/5$ 计算。',
       1, NOW()
FROM edu_problem p
WHERE p.title = '不放回摸球概率'
  AND NOT EXISTS (SELECT 1 FROM edu_problem_version v WHERE v.problem_id = p.id AND v.version = 1);

-- 将内置讲题统一校准为初中中档难度；按固定内置标题更新，不影响教师或学生创建的其他题目。
UPDATE edu_problem
SET intro = CASE title
    WHEN '二次函数动点三角形面积最值' THEN '用二次函数与三角形面积模型求最值。'
    WHEN '弹簧测力计与摩擦力' THEN '利用匀速运动时的平衡关系分析摩擦力。'
    WHEN '斜面机械效率' THEN '结合有用功、总功和额外功分析斜面。'
    WHEN '小球碰撞中的运动状态改变' THEN '从碰撞现象归纳力改变物体运动状态。'
    WHEN '动滑轮提升重物的机械效率' THEN '用有用功和总功计算动滑轮的机械效率。'
    WHEN '平面镜反射的角度关系' THEN '辨析镜面夹角、法线夹角与反射角。'
    WHEN '通电螺线管的磁极判断' THEN '用安培定则判断通电螺线管的磁极。'
    WHEN '串联电路的电流与电压' THEN '综合运用串联电路规律和欧姆定律。'
    WHEN '矩形花圃的边长' THEN '列一元二次方程解决实际问题。'
    WHEN '一次函数图像与三角形面积' THEN '利用一次函数图像与坐标轴围成图形。'
    WHEN '一次函数与反比例函数交点' THEN '联立函数关系求图像交点。'
    WHEN '正方体熔铸体积守恒' THEN '运用体积守恒解决立体几何问题。'
    WHEN '平均数、中位数与众数' THEN '综合分析一组数据的集中趋势。'
    ELSE intro
END,
difficulty = CASE WHEN title IN ('二次函数动点三角形面积最值', '弹簧测力计与摩擦力', '斜面机械效率', '小球碰撞中的运动状态改变', '动滑轮提升重物的机械效率', '平面镜反射的角度关系', '通电螺线管的磁极判断', '串联电路的电流与电压', '矩形花圃的边长', '一次函数图像与三角形面积', '一次函数与反比例函数交点', '正方体熔铸体积守恒', '平均数、中位数与众数') THEN '中等' ELSE difficulty END,
tags_json = CASE title
    WHEN '弹簧测力计与摩擦力' THEN JSON_ARRAY('弹簧测力计', '平衡力', '滑动摩擦力')
    WHEN '斜面机械效率' THEN JSON_ARRAY('斜面', '机械效率', '功')
    WHEN '小球碰撞中的运动状态改变' THEN JSON_ARRAY('力的作用效果', '相互作用力')
    WHEN '动滑轮提升重物的机械效率' THEN JSON_ARRAY('动滑轮', '机械效率', '额外功')
    WHEN '通电螺线管的磁极判断' THEN JSON_ARRAY('通电螺线管', '安培定则', '磁极')
    ELSE tags_json
END
WHERE title IN ('二次函数动点三角形面积最值', '弹簧测力计与摩擦力', '斜面机械效率', '小球碰撞中的运动状态改变', '动滑轮提升重物的机械效率', '平面镜反射的角度关系', '通电螺线管的磁极判断', '串联电路的电流与电压', '矩形花圃的边长', '一次函数图像与三角形面积', '一次函数与反比例函数交点', '正方体熔铸体积守恒', '平均数、中位数与众数');

UPDATE edu_problem_version v JOIN edu_problem p ON p.id = v.problem_id
SET v.definition_json = CAST('{"schemaVersion":"2.0","stem":"在二次函数 $y=-x²+2x+3$ 的图像上有动点 P。图像与 x 轴交于 A(-1,0)、B(3,0)。求 △PAB 面积的最大值。","facts":{"answer":"当 t=1 时，△PAB 的面积最大，最大值为 8"},"evaluation":{"acceptedAnswers":["8","t=1,s=8"],"misconceptions":[{"id":"height-error","matches":["16"],"message":"三角形面积要乘二分之一。"}]},"visualCatalog":["draw-math-scene"],"sceneDefinition":{"sceneType":"coordinate2d","variables":{"a":{"value":-1,"min":-10,"max":-0.1},"b":{"value":2,"min":-20,"max":20},"c":{"value":3,"min":-20,"max":20},"rootLeft":{"value":-1,"min":-20,"max":20},"rootRight":{"value":3,"min":-20,"max":20},"t":{"value":0.8,"min":-1,"max":3}},"entities":["parabola","A","B","P","trianglePAB","baseLine","altitude","areaChart"]}}' AS JSON),
    v.standard_answer = '当 $t=1$ 时，$△PAB$ 的面积最大，最大值为 $8$。',
    v.answer_analysis = '线段 $AB$ 在 $x$ 轴上，长度为 $3-(-1)=4$。点 $P$ 到 $x$ 轴的高为 $-t²+2t+3$。\n\n所以 $S_{△PAB}=1/2×4×(-t²+2t+3)=-2(t-1)²+8$。\n\n因为平方项的系数为负，当 $t=1$ 时面积最大，最大值为 $8$。'
WHERE p.title = '二次函数动点三角形面积最值' AND v.version = 1;

UPDATE edu_problem_version v JOIN edu_problem p ON p.id = v.problem_id
SET v.definition_json = CAST('{"schemaVersion":"2.0","stem":"在水平桌面上，用弹簧测力计以 $3.2 N$ 的拉力拉木块做匀速直线运动。撤去拉力后，木块仍向前滑行一段距离才停下。求匀速时木块受到的滑动摩擦力大小，并说明撤去拉力后木块为什么会停下。","facts":{"answer":"滑动摩擦力为 3.2 N；撤去拉力后摩擦力阻碍运动，使木块速度减小直至停下"},"evaluation":{"acceptedAnswers":["3.2N","3.2 N"],"misconceptions":[{"id":"inertia-error","matches":["立刻停下"],"message":"撤去拉力后木块因惯性仍会运动，摩擦力使其逐渐减速。"}]},"visualCatalog":["show-spring-force"],"sceneDefinition":{"sceneType":"mechanics2d","variables":{"force":{"value":3.2,"min":0,"max":100},"blockPosition":{"value":0,"min":0,"max":1}}}}' AS JSON),
    v.standard_answer = '滑动摩擦力为 $3.2 N$；撤去拉力后，摩擦力使木块逐渐减速直至停下。',
    v.answer_analysis = '木块做匀速直线运动，水平方向受力平衡，因此拉力与滑动摩擦力大小相等，摩擦力为 $3.2 N$。\n\n撤去拉力后，木块由于惯性仍保持原来的运动状态；此时摩擦力方向与运动方向相反，使木块速度逐渐减小，最后停下。'
WHERE p.title = '弹簧测力计与摩擦力' AND v.version = 1;

UPDATE edu_problem_version v JOIN edu_problem p ON p.id = v.problem_id
SET v.definition_json = CAST('{"schemaVersion":"2.0","stem":"用沿斜面向上的拉力 $F=45 N$，把重为 $120 N$ 的物体沿斜面匀速拉高 $h=1.5 m$，拉力移动的距离为 $s=5 m$。求斜面的机械效率和克服摩擦所做的额外功。","facts":{"answer":"机械效率为 80%，额外功为 45 J"},"evaluation":{"acceptedAnswers":["80%,45J","80%,45 J","80％,45J"],"misconceptions":[{"id":"height-distance-error","matches":["33.3%"],"message":"有用功用 Gh 计算，总功用 Fs 计算。"}]},"sceneDefinition":{"sceneType":"physicsInclinedPlane","variables":{}}}' AS JSON),
    v.standard_answer = '斜面的机械效率为 $80%$，额外功为 $45 J$。',
    v.answer_analysis = '有用功为 $W有=Gh=120×1.5=180 J$。总功为 $W总=Fs=45×5=225 J$。\n\n机械效率 $η=W有/W总=180/225=80%$。额外功为 $W额=W总-W有=225-180=45 J$。'
WHERE p.title = '斜面机械效率' AND v.version = 1;

UPDATE edu_problem_version v JOIN edu_problem p ON p.id = v.problem_id
SET v.definition_json = CAST('{"schemaVersion":"2.0","stem":"水平桌面上，滚动的小球 A 撞到静止的小球 B 后，A 的速度变小，B 由静止开始向右运动。根据这一现象，说明力的作用效果；碰撞过程中 A 对 B 的力与 B 对 A 的力大小有什么关系？","facts":{"answer":"力可以改变物体的运动状态；两力大小相等、方向相反"},"evaluation":{"acceptedAnswers":["运动状态,大小相等方向相反","大小相等方向相反"],"misconceptions":[{"id":"one-way-force","matches":["A只对B有力"],"message":"物体间力的作用是相互的。"}]},"sceneDefinition":{"sceneType":"physicsCollision","variables":{}}}' AS JSON),
    v.standard_answer = '力可以改变物体的运动状态；两力大小相等、方向相反。',
    v.answer_analysis = '碰后 A 的速度变小、B 从静止开始运动，说明力可以改变物体的运动状态。\n\n碰撞时 A 对 B 有力的作用，同时 B 也对 A 有力的作用。这两个力大小相等、方向相反，作用在不同物体上。'
WHERE p.title = '小球碰撞中的运动状态改变' AND v.version = 1;

UPDATE edu_problem_version v JOIN edu_problem p ON p.id = v.problem_id
SET v.definition_json = CAST('{"schemaVersion":"2.0","stem":"用动滑轮把重为 $240 N$ 的物体匀速提升 $2 m$，绳端拉力为 $100 N$，拉力移动距离为 $6 m$。求动滑轮的机械效率和额外功。","facts":{"answer":"机械效率为 80%，额外功为 120 J"},"evaluation":{"acceptedAnswers":["80%,120J","80%,120 J","80％,120J"],"misconceptions":[{"id":"ignore-extra-work","matches":["100%"],"message":"动滑轮存在摩擦和自重时，额外功不能忽略。"}]},"sceneDefinition":{"sceneType":"physicsPulley","variables":{}}}' AS JSON),
    v.standard_answer = '动滑轮的机械效率为 $80%$，额外功为 $120 J$。',
    v.answer_analysis = '有用功为 $W有=Gh=240×2=480 J$，总功为 $W总=Fs=100×6=600 J$。\n\n机械效率 $η=480/600=80%$，额外功为 $600-480=120 J$。'
WHERE p.title = '动滑轮提升重物的机械效率' AND v.version = 1;

UPDATE edu_problem_version v JOIN edu_problem p ON p.id = v.problem_id
SET v.definition_json = CAST('{"schemaVersion":"2.0","stem":"一束光线射向平面镜时，入射光线与镜面的夹角为 $30°$。求入射角、反射角以及入射光线与反射光线的夹角。","facts":{"answer":"入射角为 60°，反射角为 60°，两光线夹角为 120°"},"evaluation":{"acceptedAnswers":["60,60,120","60°，60°，120°"],"misconceptions":[{"id":"mirror-angle-error","matches":["30,30"],"message":"入射角和反射角都以法线为基准，不是以镜面为基准。"}]},"sceneDefinition":{"sceneType":"physicsReflection","variables":{}}}' AS JSON),
    v.standard_answer = '入射角为 $60°$，反射角为 $60°$，两光线夹角为 $120°$。',
    v.answer_analysis = '镜面与法线垂直，所以入射角为 $90°-30°=60°$。\n\n根据反射定律，反射角等于入射角，也为 $60°$。入射光线与反射光线分居法线两侧，它们的夹角为 $60°+60°=120°$。'
WHERE p.title = '平面镜反射的角度关系' AND v.version = 1;

UPDATE edu_problem_version v JOIN edu_problem p ON p.id = v.problem_id
SET v.definition_json = CAST('{"schemaVersion":"2.0","stem":"从通电螺线管的右端观察，线圈中电流方向为逆时针。判断螺线管的右端和左端分别是什么磁极。","facts":{"answer":"右端为 N 极，左端为 S 极"},"evaluation":{"acceptedAnswers":["右N左S","右端N左端S","N,S"],"misconceptions":[{"id":"clockwise-error","matches":["右S左N"],"message":"右手四指沿电流方向弯曲时，大拇指所指的一端是 N 极。"}]},"sceneDefinition":{"sceneType":"physicsMagneticField","variables":{}}}' AS JSON),
    v.standard_answer = '右端为 $N$ 极，左端为 $S$ 极。',
    v.answer_analysis = '用右手握住螺线管，使四指弯向与电流方向一致。\n\n大拇指所指的一端为螺线管的 $N$ 极。由右端观察电流为逆时针时，大拇指指向右端，所以右端为 $N$ 极，左端为 $S$ 极。'
WHERE p.title = '通电螺线管的磁极判断' AND v.version = 1;

UPDATE edu_problem_version v JOIN edu_problem p ON p.id = v.problem_id
SET v.definition_json = CAST('{"schemaVersion":"2.0","stem":"电源电压为 $12 V$，电阻 $R₁=4 Ω$、$R₂=8 Ω$ 串联在电路中。求电路电流以及两个电阻两端的电压。","facts":{"answer":"I=1 A，U₁=4 V，U₂=8 V"},"evaluation":{"acceptedAnswers":["1A,4V,8V","I=1,U1=4,U2=8"],"misconceptions":[{"id":"parallel-error","matches":["3A"],"message":"串联电路总电阻等于各电阻之和。"}]},"sceneDefinition":{"sceneType":"physicsCircuit","variables":{}}}' AS JSON),
    v.standard_answer = '$I=1 A$，$U₁=4 V$，$U₂=8 V$。',
    v.answer_analysis = '串联电路的总电阻为 $R=R₁+R₂=4+8=12 Ω$。\n\n由欧姆定律，$I=U/R=12/12=1 A$。所以 $U₁=IR₁=1×4=4 V$，$U₂=IR₂=1×8=8 V$，且 $U₁+U₂=12 V$。'
WHERE p.title = '串联电路的电流与电压' AND v.version = 1;

UPDATE edu_problem_version v JOIN edu_problem p ON p.id = v.problem_id
SET v.definition_json = CAST('{"schemaVersion":"2.0","stem":"一个矩形花圃的长比宽多 $3 m$，面积为 $108 m²$。求花圃的长和宽。","facts":{"answer":"长为 12 m，宽为 9 m"},"evaluation":{"acceptedAnswers":["长12宽9","12,9","12m,9m"],"misconceptions":[{"id":"negative-root","matches":["长-9宽-12"],"message":"长度必须取正值。"}]},"visualCatalog":[],"sceneDefinition":{"sceneType":"textOnly","variables":{}}}' AS JSON),
    v.standard_answer = '花圃长为 $12 m$，宽为 $9 m$。',
    v.answer_analysis = '设花圃宽为 $x m$，则长为 $(x+3)m$。由面积条件得 $x(x+3)=108$，即 $x²+3x-108=0$。\n\n因式分解得 $(x-9)(x+12)=0$，所以 $x=9$ 或 $x=-12$。长度不能为负，故宽为 $9 m$，长为 $12 m$。'
WHERE p.title = '矩形花圃的边长' AND v.version = 1;

UPDATE edu_problem_version v JOIN edu_problem p ON p.id = v.problem_id
SET v.definition_json = CAST('{"schemaVersion":"2.0","stem":"直线 $y=-x+6$ 与 $x$ 轴、$y$ 轴分别交于 A、B 两点。求 $△OAB$ 的面积。","facts":{"answer":"S△OAB=18"},"evaluation":{"acceptedAnswers":["18","S=18"],"misconceptions":[{"id":"intercept-error","matches":["36"],"message":"三角形面积要乘以二分之一。"}]},"visualCatalog":["draw-math-scene"],"sceneDefinition":{"sceneType":"coordinatePlane2d","variables":{},"entities":[{"id":"O","type":"point","x":0,"y":0,"label":"O"},{"id":"A","type":"point","x":6,"y":0,"label":"A(6,0)"},{"id":"B","type":"point","x":0,"y":6,"label":"B(0,6)"},{"id":"OA","type":"segment","from":"O","to":"A"},{"id":"OB","type":"segment","from":"O","to":"B"},{"id":"AB","type":"segment","from":"A","to":"B"}]}}' AS JSON),
    v.standard_answer = '$S_{△OAB}=18$。',
    v.answer_analysis = '令 $y=0$，得 $x=6$，所以 $A(6,0)$；令 $x=0$，得 $y=6$，所以 $B(0,6)$。\n\n$OA=6$、$OB=6$，且两边互相垂直。因此 $S_{△OAB}=1/2×6×6=18$。'
WHERE p.title = '一次函数图像与三角形面积' AND v.version = 1;

UPDATE edu_problem_version v JOIN edu_problem p ON p.id = v.problem_id
SET v.definition_json = CAST('{"schemaVersion":"2.0","stem":"已知一次函数 $y=x+1$ 与反比例函数 $y=6/x$ 的图像交于 A、B 两点。求 A、B 两点的坐标。","facts":{"answer":"A(2,3)，B(-3,-2)"},"evaluation":{"acceptedAnswers":["(2,3),(-3,-2)","A(2,3)B(-3,-2)"],"misconceptions":[{"id":"one-solution-error","matches":["(2,3)"],"message":"一元二次方程有两个实数解时，两个交点都要保留。"}]},"visualCatalog":["draw-math-scene"],"sceneDefinition":{"sceneType":"coordinatePlane2d","variables":{},"entities":[{"id":"A","type":"point","x":2,"y":3,"label":"A(2,3)"},{"id":"B","type":"point","x":-3,"y":-2,"label":"B(-3,-2)"},{"id":"line","type":"line","from":"A","to":"B"}]}}' AS JSON),
    v.standard_answer = '$A(2,3)$，$B(-3,-2)$。',
    v.answer_analysis = '交点同时满足两个函数关系，因此 $x+1=6/x$。\n\n两边乘以 $x$，得 $x²+x-6=0$，即 $(x+3)(x-2)=0$。\n\n当 $x=2$ 时 $y=3$；当 $x=-3$ 时 $y=-2$，所以两个交点为 $A(2,3)$、$B(-3,-2)$。'
WHERE p.title = '一次函数与反比例函数交点' AND v.version = 1;

UPDATE edu_problem_version v JOIN edu_problem p ON p.id = v.problem_id
SET v.definition_json = CAST('{"schemaVersion":"2.0","stem":"把一个棱长为 $6 cm$ 的正方体蜡块熔化后，全部铸成棱长为 $2 cm$ 的小正方体蜡块。可以铸成多少个？","facts":{"answer":"可以铸成 27 个"},"evaluation":{"acceptedAnswers":["27","27个"],"misconceptions":[{"id":"length-ratio-error","matches":["3"],"message":"熔铸前后体积守恒，数量等于体积之比。"}]},"visualCatalog":["draw-math-scene"],"sceneDefinition":{"sceneType":"geometry3d","variables":{},"entities":[{"id":"A","type":"point","x":-1,"y":-1,"z":-1},{"id":"B","type":"point","x":1,"y":-1,"z":-1},{"id":"C","type":"point","x":1,"y":1,"z":-1},{"id":"D","type":"point","x":-1,"y":1,"z":-1},{"id":"A1","type":"point","x":-1,"y":-1,"z":1},{"id":"B1","type":"point","x":1,"y":-1,"z":1},{"id":"C1","type":"point","x":1,"y":1,"z":1},{"id":"D1","type":"point","x":-1,"y":1,"z":1},{"id":"ABCD","type":"polygon","points":["A","B","C","D"]},{"id":"A1B1C1D1","type":"polygon","points":["A1","B1","C1","D1"]}]}}' AS JSON),
    v.standard_answer = '可以铸成 $27$ 个小正方体蜡块。',
    v.answer_analysis = '熔化后蜡的体积不变。大正方体体积为 $6³=216 cm³$，每个小正方体体积为 $2³=8 cm³$。\n\n小正方体数量为 $216÷8=27$。'
WHERE p.title = '正方体熔铸体积守恒' AND v.version = 1;

UPDATE edu_problem_version v JOIN edu_problem p ON p.id = v.problem_id
SET v.definition_json = CAST('{"schemaVersion":"2.0","stem":"某小组 8 名同学一分钟跳绳次数分别为 $15、12、18、15、14、17、13、16$。求这组数据的平均数、中位数和众数。","facts":{"answer":"平均数为 15，中位数为 15，众数为 15"},"evaluation":{"acceptedAnswers":["15,15,15","平均15中位15众数15"],"misconceptions":[{"id":"unsorted-median","matches":["14.5"],"message":"求中位数前需要先把数据按大小规律排列。"}]},"visualCatalog":["draw-math-scene"],"sceneDefinition":{"sceneType":"statistics2d","variables":{},"entities":[{"id":"score1","type":"bar","label":"12","value":12},{"id":"score2","type":"bar","label":"13","value":13},{"id":"score3","type":"bar","label":"14","value":14},{"id":"score4","type":"bar","label":"15","value":15},{"id":"score5","type":"bar","label":"15","value":15},{"id":"score6","type":"bar","label":"16","value":16},{"id":"score7","type":"bar","label":"17","value":17},{"id":"score8","type":"bar","label":"18","value":18}]}}' AS JSON),
    v.standard_answer = '平均数为 $15$，中位数为 $15$，众数为 $15$。',
    v.answer_analysis = '把数据从小到大排列为 $12、13、14、15、15、16、17、18$。总和为 $120$，所以平均数为 $120÷8=15$。\n\n8 个数据的中位数是第 4、5 个数据的平均数，即 $(15+15)÷2=15$。出现次数最多的是 $15$，所以众数也是 $15$。'
WHERE p.title = '平均数、中位数与众数' AND v.version = 1;

-- 为画图讲题补充与题干一致的专用场景数据；仅影响下列内置题。
UPDATE edu_problem_version v JOIN edu_problem p ON p.id = v.problem_id
SET v.definition_json = JSON_SET(v.definition_json, '$.sceneDefinition', CAST('{"sceneType":"physicsInclinedPlane","variables":{"inclinePosition":{"value":0.5,"min":0,"max":1},"force":{"value":45,"min":0,"max":500},"weight":{"value":120,"min":0,"max":1000},"height":{"value":1.5,"min":0,"max":100},"distance":{"value":5,"min":0,"max":100}}}' AS JSON))
WHERE p.title = '斜面机械效率' AND v.version = 1;

UPDATE edu_problem_version v JOIN edu_problem p ON p.id = v.problem_id
SET v.definition_json = JSON_SET(v.definition_json, '$.sceneDefinition', CAST('{"sceneType":"physicsCollision","variables":{"mass1":{"value":1,"min":0.1,"max":20},"mass2":{"value":1,"min":0.1,"max":20},"speed1":{"value":4,"min":-20,"max":20},"speed2":{"value":0,"min":-20,"max":20},"restitution":{"value":1,"min":0,"max":1}}}' AS JSON))
WHERE p.title = '小球碰撞中的运动状态改变' AND v.version = 1;

UPDATE edu_problem_version v JOIN edu_problem p ON p.id = v.problem_id
SET v.definition_json = JSON_SET(v.definition_json, '$.sceneDefinition', CAST('{"sceneType":"physicsPulley","variables":{"ropeEnd":{"value":0,"min":0,"max":1},"force":{"value":100,"min":0,"max":1000},"weight":{"value":240,"min":0,"max":1000},"height":{"value":2,"min":0,"max":100},"distance":{"value":6,"min":0,"max":100}}}' AS JSON))
WHERE p.title = '动滑轮提升重物的机械效率' AND v.version = 1;

UPDATE edu_problem_version v JOIN edu_problem p ON p.id = v.problem_id
SET v.definition_json = JSON_SET(v.definition_json, '$.sceneDefinition', CAST('{"sceneType":"physicsReflection","variables":{"incidentAngle":{"value":60,"min":15,"max":75}}}' AS JSON))
WHERE p.title = '平面镜反射的角度关系' AND v.version = 1;

UPDATE edu_problem_version v JOIN edu_problem p ON p.id = v.problem_id
SET v.definition_json = JSON_SET(v.definition_json, '$.sceneDefinition', CAST('{"sceneType":"coordinatePlane2d","variables":{},"entities":[{"id":"lineGraph","type":"function","kind":"linear","slope":-1,"intercept":6,"domain":[-1,7],"label":"y=-x+6","labelX":3.8,"labelY":2.6,"color":"#7566bb"},{"id":"O","type":"point","x":0,"y":0,"label":"O"},{"id":"A","type":"point","x":6,"y":0,"label":"A(6,0)"},{"id":"B","type":"point","x":0,"y":6,"label":"B(0,6)"},{"id":"OA","type":"segment","from":"O","to":"A"},{"id":"OB","type":"segment","from":"O","to":"B"},{"id":"AB","type":"segment","from":"A","to":"B"},{"id":"triangleOAB","type":"polygon","points":["O","A","B"]}],"drawSteps":{"UNDERSTAND":["lineGraph"],"MODEL":["O","A","B","OA","OB","AB","triangleOAB"],"DERIVE":["triangleOAB"],"VERIFY":["lineGraph","O","A","B","OA","OB","AB","triangleOAB"]}}' AS JSON))
WHERE p.title = '一次函数图像与三角形面积' AND v.version = 1;

UPDATE edu_problem_version v JOIN edu_problem p ON p.id = v.problem_id
SET v.definition_json = JSON_SET(v.definition_json, '$.sceneDefinition', CAST('{"sceneType":"coordinatePlane2d","variables":{},"entities":[{"id":"linearGraph","type":"function","kind":"linear","slope":1,"intercept":1,"domain":[-5,5],"label":"y=x+1","labelX":1.8,"labelY":4.3,"color":"#7566bb"},{"id":"inverseGraph","type":"function","kind":"inverse","coefficient":6,"domain":[-5,5],"label":"y=6/x","labelX":-4.7,"labelY":-1.7,"color":"#52aab4"},{"id":"A","type":"point","x":2,"y":3,"label":"A(2,3)"},{"id":"B","type":"point","x":-3,"y":-2,"label":"B(-3,-2)"}],"drawSteps":{"UNDERSTAND":["linearGraph","inverseGraph"],"MODEL":["A","B"],"DERIVE":["linearGraph","inverseGraph","A","B"],"VERIFY":["linearGraph","inverseGraph","A","B"]}}' AS JSON))
WHERE p.title = '一次函数与反比例函数交点' AND v.version = 1;

UPDATE edu_problem_version v JOIN edu_problem p ON p.id = v.problem_id
SET v.definition_json = JSON_SET(v.definition_json, '$.sceneDefinition', CAST('{"sceneType":"geometry3d","visualModel":"cubeVolume","variables":{},"entities":[]}' AS JSON))
WHERE p.title = '正方体熔铸体积守恒' AND v.version = 1;

UPDATE edu_problem_version v JOIN edu_problem p ON p.id = v.problem_id
SET v.definition_json = JSON_SET(v.definition_json, '$.visualCatalog', JSON_ARRAY('draw-math-scene'), '$.sceneDefinition', CAST('{"sceneType":"geometry2d","variables":{},"entities":[{"id":"A","type":"point","x":0,"y":0,"label":"A"},{"id":"B","type":"point","x":12,"y":0,"label":"B"},{"id":"C","type":"point","x":12,"y":9,"label":"C"},{"id":"D","type":"point","x":0,"y":9,"label":"D"},{"id":"lengthLabel","type":"point","x":6,"y":-1,"label":"长 = 12 m"},{"id":"widthLabel","type":"point","x":13,"y":4.5,"label":"宽 = 9 m"},{"id":"areaLabel","type":"point","x":6,"y":4.5,"label":"面积 = 108 m²"},{"id":"flowerbed","type":"polygon","points":["A","B","C","D"]}],"drawSteps":{"UNDERSTAND":["A","B","C","D","flowerbed"],"MODEL":["lengthLabel","widthLabel","areaLabel"],"DERIVE":["flowerbed","lengthLabel","widthLabel","areaLabel"],"VERIFY":["A","B","C","D","flowerbed","lengthLabel","widthLabel","areaLabel"]}}' AS JSON))
WHERE p.title = '矩形花圃的边长' AND v.version = 1;
