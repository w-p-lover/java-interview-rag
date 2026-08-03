ALTER TABLE t_interview_answer_evaluation_log
    ADD COLUMN baseline_run_id CHAR(36) NULL COMMENT '固定评分基线运行标识' AFTER evaluation_mode,
    ADD COLUMN baseline_case_id VARCHAR(100) NULL COMMENT '固定评分样本标识' AFTER baseline_run_id,
    ADD KEY idx_interview_evaluation_baseline_run (
        evaluation_mode, baseline_run_id, baseline_case_id, created_at
    );
