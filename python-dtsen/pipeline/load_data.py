import pandas as pd
from sqlalchemy import text
from database.db_connection import get_connection


def get_province_list():
    engine = get_connection()

    query = """
        SELECT DISTINCT kode_provinsi
        FROM dtsen.vw_input_keluarga
        WHERE kode_provinsi IS NOT NULL
        ORDER BY kode_provinsi
    """

    df = pd.read_sql(text(query), engine)
    return df["kode_provinsi"].astype(str).tolist()


def get_member_feature_columns(configs):
    cols = set()

    def recurse(obj):
        if isinstance(obj, dict):
            for k, v in obj.items():
                if k == "column":
                    if isinstance(v, list):
                        cols.update(v)
                    else:
                        cols.add(v)
                else:
                    recurse(v)

    for cfg in configs:
        recurse(cfg["variables"])

    cols.discard("id_keluarga")
    cols.discard("status_dalam_pekerjaan_utama")

    return sorted(cols)


def load_family_id_chunks(kode_provinsi, chunksize=10000):
    engine = get_connection()

    query = """
        SELECT id_keluarga
        FROM dtsen.vw_input_keluarga
        WHERE kode_provinsi = :kode_provinsi
        ORDER BY id_keluarga
    """

    with engine.connect() as conn:
        df_ids = pd.read_sql(
            text(query),
            conn,
            params={"kode_provinsi": kode_provinsi}
        )

    ids = df_ids["id_keluarga"].tolist()

    for i in range(0, len(ids), chunksize):
        yield ids[i:i + chunksize]


def load_members_by_family_ids(family_ids, feature_cols):
    if not family_ids:
        return pd.DataFrame()

    engine = get_connection()

    params = {"family_ids": family_ids}

    select_cols = [
        "a.id_keluarga",
        "a.status_dalam_pekerjaan_utama"
    ] + [f"a.{col}" for col in feature_cols]

    query = f"""
        SELECT {", ".join(select_cols)}
        FROM dtsen.vw_input_anggota_keluarga a
        WHERE a.id_keluarga = ANY(:family_ids)
        ORDER BY a.id_keluarga
    """

    with engine.connect() as conn:
        df = pd.read_sql(
            text(query),
            conn,
            params=params
        )

    return df


def load_household(query, params=None):
    engine = get_connection()

    with engine.connect() as conn:
        df = pd.read_sql(
            text(query),
            conn,
            params=params or {}
        )

    return df