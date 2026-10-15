from pyexpat import features

import pandas as pd
import json
import logging
import io
import time
import re

from sqlalchemy import text
from database.db_connection import get_connection
from pipeline.load_data import get_province_list

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s"
)

def copy_dataframe_to_postgres(df, engine, table_name, schema, column_order):
    if df.empty:
        return

    df = df[column_order].copy()

    buffer = io.StringIO()
    df.to_csv(buffer, index=False)
    buffer.seek(0)

    raw_conn = engine.raw_connection()
    cursor = raw_conn.cursor()

    try:
        cursor.copy_expert(
            f"""
            COPY {schema}.{table_name}
            ({",".join(column_order)})
            FROM STDIN WITH CSV HEADER
            """,
            buffer
        )
        raw_conn.commit()
    finally:
        cursor.close()
        raw_conn.close()

def resolve_latest_feature(feature_name, available_columns):

    version_pattern = re.compile(
        rf"^{re.escape(feature_name)}_v(\d+)$"
    )

    candidates = []

    # Exact feature
    if feature_name in available_columns:
        candidates.append((0, feature_name))

    # Semua versi feature
    for column in available_columns:
        match = version_pattern.match(column)

        if match:
            version = int(match.group(1))
            candidates.append((version, column))

    if not candidates:
        raise ValueError(
            f"Feature '{feature_name}' tidak ditemukan di "
            f"dtsen.dataset_feature, termasuk versi *_vN."
        )

    # Ambil versi tertinggi
    candidates.sort(key=lambda x: x[0], reverse=True)

    return candidates[0][1]

def generate_deprivasi(df_feature, config):
    indicator_dict = config["indicators"]

    base_cols = [
        "id_keluarga",
        "kode_provinsi",
        "kode_kabupaten_kota",
        "kode_kecamatan",
        "kode_kelurahan_desa",
        "desil_nasional"
    ]

    results = []

    for indicator, cfg in indicator_dict.items():
        feature_groups = cfg["features"]

        features = [f for group in feature_groups for f in group]

        missing_features = [
            f for f in features
            if f not in df_feature.columns
        ]

        if missing_features:
            raise ValueError(
                f"Feature hasil resolving untuk indikator '{indicator}' "
                f"tidak ditemukan di data yang dimuat: {missing_features}"
            )

        valid_features = features

        use_cols = base_cols + valid_features
        subset = df_feature.loc[:, use_cols].copy()

        subset[valid_features] = subset[valid_features].replace(
            ["", "null", "None", "nan"], pd.NA
        )
        subset[valid_features] = subset[valid_features].apply(
            pd.to_numeric, errors="coerce"
        )

        if len(valid_features) == 1:
            col = valid_features[0]
            status = subset[col].copy()
            any_null = subset[col].isna()
        else:
            any_one = subset[valid_features].eq(1).any(axis=1)
            any_null = subset[valid_features].isna().any(axis=1)

            status = pd.Series(index=subset.index, dtype="float")
            status[any_one] = 1
            status[~any_one & any_null] = None
            status[~any_one & ~any_null] = 0

        if indicator in ["kondisi_hunian", "kepemilikan_aset"]:
            nilai = subset[valid_features].fillna(0).sum(axis=1) / len(valid_features)
            nilai = nilai.round(4)
        else:
            nilai = status

        nilai[status.isna()] = None

        is_complete = (~any_null).astype(int)
        is_complete[status.isna()] = 0

        temp = pd.DataFrame({
            "id_keluarga": subset["id_keluarga"],
            "kode_provinsi": subset["kode_provinsi"],
            "kode_kabupaten_kota": subset["kode_kabupaten_kota"],
            "kode_kecamatan": subset["kode_kecamatan"],
            "kode_kelurahan_desa": subset["kode_kelurahan_desa"],
            "desil_nasional": subset["desil_nasional"],
            "indikator": indicator,
            "status": status,
            "nilai": nilai,
            "is_complete": is_complete
        })

        temp = temp[temp["status"] != 0]
        results.append(temp)

    if not results:
        return pd.DataFrame()

    df_final = pd.concat(results, ignore_index=True)
    return df_final


def run_deprivation_engine():
    logging.info("Deprivation engine started")

    engine = get_connection()

    with engine.begin() as conn:
        conn.execute(text("TRUNCATE TABLE dtsen.data_deprivasi"))

    config = json.load(open("config/indicator.json"))

    # Ambil seluruh nama kolom aktual dari dataset_feature
    with engine.begin() as conn:
        feature_columns = pd.read_sql(
            text("""
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'dtsen'
                AND table_name = 'dataset_feature'
            """),
            conn
        )["column_name"].tolist()

    available_columns = set(feature_columns)

    # Resolve setiap feature config ke feature versi terbaru
    resolved_config = json.loads(json.dumps(config))

    for indicator, cfg in resolved_config["indicators"].items():
        resolved_groups = []

        for group in cfg["features"]:
            resolved_group = []

            for feature in group:
                resolved_feature = resolve_latest_feature(
                    feature,
                    available_columns
                )

                resolved_group.append(resolved_feature)

            resolved_groups.append(resolved_group)

        cfg["features"] = resolved_groups

    config = resolved_config

    all_features = set()

    for ind in config["indicators"].values():
        for group in ind["features"]:
            all_features.update(group)

    base_cols = [
        "id_keluarga",
        "kode_provinsi",
        "kode_kabupaten_kota",
        "kode_kecamatan",
        "kode_kelurahan_desa",
        "desil_nasional"
    ]

    cols = base_cols + list(all_features)

    for kode_provinsi in get_province_list():
        logging.info(f"Province {kode_provinsi}")

        query = text(f"""
            SELECT {",".join(cols)}
            FROM dtsen.dataset_feature
            WHERE kode_provinsi = :kode_provinsi
        """)

        t0 = time.time()
        df_feature = pd.read_sql(
            query,
            engine,
            params={"kode_provinsi": kode_provinsi}
        )
        logging.info(f"[deprivasi] load df_feature: {time.time() - t0:.2f}s | rows={len(df_feature)}")

        if df_feature.empty:
            continue

        t1 = time.time()
        df_deprivasi = generate_deprivasi(
            df_feature,
            config
        )
        logging.info(f"[deprivasi] generate_deprivasi: {time.time() - t1:.2f}s | rows={len(df_deprivasi)}")

        if df_deprivasi.empty:
            continue

        t2 = time.time()
        logging.info("Save to data_deprivasi")

        for col in ["status", "is_complete", "desil_nasional"]:
            if col in df_deprivasi.columns:
                df_deprivasi[col] = (
                    pd.to_numeric(df_deprivasi[col], errors="coerce")
                    .round()
                    .astype("Int64")
                )

        df_deprivasi = df_deprivasi[
            [
                "id_keluarga",
                "kode_provinsi",
                "kode_kabupaten_kota",
                "kode_kecamatan",
                "kode_kelurahan_desa",
                "desil_nasional",
                "indikator",
                "status",
                "nilai",
                "is_complete"
            ]
        ]

        copy_dataframe_to_postgres(
            df_deprivasi,
            engine,
            "data_deprivasi",
            "dtsen",
            [
                "id_keluarga",
                "kode_provinsi",
                "kode_kabupaten_kota",
                "kode_kecamatan",
                "kode_kelurahan_desa",
                "desil_nasional",
                "indikator",
                "status",
                "nilai",
                "is_complete"
            ]
        )

        logging.info(f"[deprivasi] save data_deprivasi: {time.time() - t2:.2f}s")

    logging.info("Deprivation engine finished")