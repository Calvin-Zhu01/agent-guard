"""
数据库连接工具

@author zhuhx
"""

import os
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """应用配置"""

    db_host: str = "localhost"
    db_port: int = 3306
    db_name: str = "agent_guard"
    db_user: str = "root"
    db_password: str = "root"

    redis_host: str = "localhost"
    redis_port: int = 6379
    redis_password: str = ""

    class Config:
        env_file = ".env"


settings = Settings()


def get_database_url() -> str:
    """获取数据库连接 URL"""
    return (
        f"mysql+pymysql://{settings.db_user}:{settings.db_password}"
        f"@{settings.db_host}:{settings.db_port}/{settings.db_name}"
    )


def get_engine():
    """获取数据库引擎"""
    return create_engine(get_database_url(), pool_pre_ping=True)


def get_session():
    """获取数据库会话"""
    engine = get_engine()
    SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
    return SessionLocal()
