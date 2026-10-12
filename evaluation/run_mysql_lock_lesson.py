#!/usr/bin/env python3
"""在独立开发库实验表演示 RR/RC、当前读和锁等待，不调用模型。"""

from __future__ import annotations

import argparse
import json
import os
import time
import uuid
from datetime import datetime, timezone
from pathlib import Path

import pymysql


ROOT = Path(__file__).resolve().parents[1]
TABLE = "t_learning_mysql_lock_lab"
DDL = f"""CREATE TABLE IF NOT EXISTS {TABLE} (
    run_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    item_id INT NOT NULL,
    balance INT NOT NULL,
    PRIMARY KEY (run_id, item_id)
) ENGINE=InnoDB COMMENT='仅供开发库双连接锁学习实验'"""


def configuration(env_file: Path | None) -> dict:
    values = dict(os.environ)
    if env_file:
        for line in env_file.read_text(encoding="utf-8-sig").splitlines():
            if "=" in line and not line.lstrip().startswith("#"):
                key, value = line.split("=", 1)
                values.setdefault(key.strip(), value.strip().strip("\"").strip("'"))
    return {
        "host": values["BLOG_DB_HOST"],
        "port": int(values.get("BLOG_DB_PORT", "3306")),
        "user": values["BLOG_DB_USERNAME"],
        "password": values["BLOG_DB_PASSWORD"],
        "database": "java_interview_rag",
        "charset": "utf8mb4",
        "autocommit": False,
        "connect_timeout": 8,
        "read_timeout": 8,
        "write_timeout": 8,
        "cursorclass": pymysql.cursors.DictCursor,
    }


def execute(connection, sql: str, args: tuple = ()) -> list[dict]:
    with connection.cursor() as cursor:
        cursor.execute(sql, args)
        return cursor.fetchall() if cursor.description else []


def start(connection, isolation: str) -> None:
    connection.rollback()
    execute(connection, "SET SESSION TRANSACTION ISOLATION LEVEL " + isolation)
    connection.begin()


def run(config: dict) -> dict:
    run_id = str(uuid.uuid4())
    a = pymysql.connect(**config)
    try:
        b = pymysql.connect(**config)
        try:
            target = execute(a, "SELECT DATABASE() db, VERSION() serverVersion")[0]
            if target["db"] != "java_interview_rag" or not target["serverVersion"].startswith("5.7."):
                raise RuntimeError("本课程只在 java_interview_rag 开发库的 MySQL 5.7 上运行")
            execute(a, DDL)
            engine = execute(a, "SELECT ENGINE FROM information_schema.tables "
                             "WHERE table_schema=DATABASE() AND table_name=%s", (TABLE,))[0]
            if engine["ENGINE"] != "InnoDB":
                raise RuntimeError("实验表必须使用 InnoDB")
            for connection in (a, b):
                execute(connection, "SET SESSION innodb_lock_wait_timeout=1")
            for item_id in (10, 20, 30):
                execute(a, f"INSERT INTO {TABLE} VALUES (%s,%s,100)", (run_id, item_id))
            a.commit()
            where = " WHERE run_id=%s AND item_id=20"
            select = f"SELECT balance FROM {TABLE}" + where
            update = f"UPDATE {TABLE} SET balance=balance+1" + where
            insert = f"INSERT INTO {TABLE} VALUES (%s,15,100)"
            plans = {
                "uniqueLookup": execute(a, "EXPLAIN " + select, (run_id,)),
                "unindexedPredicate": execute(a, f"EXPLAIN SELECT * FROM {TABLE} WHERE balance=100"),
            }

            # A 先建立快照，B 提交新值，A 普通读保持旧值、当前读看到新值。
            start(a, "REPEATABLE READ")
            first = execute(a, select, (run_id,))[0]["balance"]
            start(b, "REPEATABLE READ")
            execute(b, update, (run_id,))
            b.commit()
            repeated = execute(a, select, (run_id,))[0]["balance"]
            current = execute(a, select + " FOR UPDATE", (run_id,))[0]["balance"]
            if (first, repeated, current) != (100, 100, 101):
                raise RuntimeError("RR 快照读/当前读结果偏离课程预期")
            a.rollback()
            scenarios = [{"name": "rr_snapshot_vs_current", "first": first,
                          "repeated": repeated, "current": current, "passed": True}]

            range_lock = f"SELECT item_id FROM {TABLE} WHERE run_id=%s AND item_id>=10 AND item_id<30 FOR UPDATE"
            cases = [
                ("rr_existing_unique", "REPEATABLE READ", select + " FOR UPDATE", insert, False),
                ("rr_range_insert", "REPEATABLE READ", range_lock, insert, True),
                ("rc_range_insert", "READ COMMITTED", range_lock, insert, False),
                ("rr_same_row_update", "REPEATABLE READ", select + " FOR UPDATE", update, True),
            ]
            for name, isolation, lock_sql, contender_sql, expected_wait in cases:
                start(a, isolation)
                start(b, isolation)
                execute(a, lock_sql, (run_id,))
                started = time.monotonic()
                error_code = None
                try:
                    execute(b, contender_sql, (run_id,))
                except pymysql.err.OperationalError as error:
                    if error.args[0] != 1205:
                        raise
                    error_code = error.args[0]
                elapsed = round(time.monotonic() - started, 3)
                # 1205 不等于整个事务已经回滚；显式回滚 B，再释放 A。
                b.rollback()
                a.rollback()
                if (error_code == 1205) != expected_wait:
                    raise RuntimeError(f"{name}: 锁行为偏离课程预期，错误码={error_code}")
                # 仅实验 SQL 复核释放锁后的恢复，不重试业务写入或模型请求。
                b.begin()
                execute(b, contender_sql, (run_id,))
                b.rollback()
                scenarios.append({"name": name, "isolation": isolation,
                                  "holderSql": lock_sql, "contenderSql": contender_sql,
                                  "errorCode": error_code, "elapsedSeconds": elapsed,
                                  "afterReleaseSucceeded": True, "passed": True})
                print(f"{name}: {'等待超时 1205' if error_code else '立即完成'}；释放后可执行", flush=True)
            rows = execute(a, f"SELECT item_id,balance FROM {TABLE} WHERE run_id=%s ORDER BY item_id", (run_id,))
            if rows != [{"item_id": 10, "balance": 100}, {"item_id": 20, "balance": 101},
                        {"item_id": 30, "balance": 100}]:
                raise RuntimeError("实验临时写入未完整回滚")
            return {"runAt": datetime.now(timezone.utc).isoformat(), "runId": run_id,
                    "target": target, "table": TABLE, "plans": plans, "scenarios": scenarios,
                    "retainedRows": rows, "businessTablesWritten": False,
                    "note": "保留本轮三行教学数据；未清理其他运行。EXPLAIN 是估计，不是性能或精确锁区间证明。"}
        finally:
            b.rollback()
            b.close()
    finally:
        a.rollback()
        a.close()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--env-file", type=Path, help="只读读取既有 BLOG_DB_* 配置；也可用环境变量")
    parser.add_argument("--output", type=Path, default=ROOT / "output/mysql-lock-lesson.json")
    args = parser.parse_args()
    output = args.output.resolve()
    if not output.is_relative_to(ROOT):
        parser.error("结果只允许写入当前项目")
    result = run(configuration(args.env_file))
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"5 个实验完成，证据保存至 {output}")


if __name__ == "__main__":
    main()
