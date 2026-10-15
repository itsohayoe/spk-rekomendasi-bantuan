import os
from sqlalchemy import create_engine
from dotenv import load_dotenv

load_dotenv()


def get_connection():
    required_keys = [
        "DB_HOST",
        "DB_PORT",
        "DB_NAME",
        "DB_USER",
        "DB_PASSWORD",
    ]

    missing_keys = [key for key in required_keys if not os.getenv(key)]

    if missing_keys:
        raise ValueError(
            f"Konfigurasi database belum lengkap di file .env: {missing_keys}"
        )

    db_host = os.getenv("DB_HOST")
    db_port = os.getenv("DB_PORT")
    db_name = os.getenv("DB_NAME")
    db_user = os.getenv("DB_USER")
    db_password = os.getenv("DB_PASSWORD")

    connection_url = (
        f"postgresql+psycopg2://{db_user}:{db_password}"
        f"@{db_host}:{db_port}/{db_name}"
    )

    engine = create_engine(connection_url)
    return engine