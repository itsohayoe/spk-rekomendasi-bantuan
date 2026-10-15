import numpy as np
import pandas as pd
import json
import time
import logging
import io
from sqlalchemy import text

from database.db_connection import get_connection
from pipeline.load_data import (
    load_family_id_chunks,
    load_members_by_family_ids,
    load_household,
    get_province_list,
    get_member_feature_columns
)

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s"
)


def extract_columns_from_variables(variables):
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

    recurse(variables)
    return cols


def build_household_query(configs):
    all_cols = set()

    for cfg in configs:
        all_cols.update(extract_columns_from_variables(cfg["variables"]))

    base = {
        "id_keluarga",
        "kode_provinsi",
        "kode_kabupaten_kota",
        "kode_kecamatan",
        "kode_kelurahan_desa",
        "desil_nasional"
    }

    final_cols = sorted(base.union(all_cols))

    query = f"""
        SELECT {",".join(final_cols)}
        FROM dtsen.vw_input_keluarga
        WHERE kode_provinsi = :kode_provinsi
    """

    return query, final_cols


def read_sql_file(path):
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


def clean_dataframe(df):
    obj_cols = [
        c for c in df.select_dtypes(include="object").columns
        if c != "id_keluarga"
    ]

    if obj_cols:
        for col in obj_cols:
            if df[col].dtype == "object":
                df[col] = df[col].str.strip()

        df[obj_cols] = df[obj_cols].replace("", None)

    return df


def optimize_household_dtypes(df, configs):
    used_cols = set()

    for config in configs:
        used_cols.update(extract_columns_from_variables(config["variables"]))

    exclude_cols = {
        "id_keluarga",
        "kode_provinsi",
        "kode_kabupaten_kota",
        "kode_kecamatan",
        "kode_kelurahan_desa"
    }

    target_cols = [c for c in used_cols if c in df.columns and c not in exclude_cols]

    for col in target_cols:
        df[col] = pd.to_numeric(df[col], errors="coerce")

    if "desil_nasional" in df.columns:
        df["desil_nasional"] = pd.to_numeric(df["desil_nasional"], errors="coerce")

    return df


def optimize_member_dtypes(df, configs):
    used_cols = set()

    for config in configs:
        used_cols.update(extract_columns_from_variables(config["variables"]))

    used_cols.add("status_dalam_pekerjaan_utama")

    exclude_cols = {"id_keluarga"}

    target_cols = [c for c in used_cols if c in df.columns and c not in exclude_cols]

    for col in target_cols:
        df[col] = pd.to_numeric(df[col], errors="coerce")

    return df


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


def assert_unique_key(df, key_col, label):
    dup_mask = df[key_col].duplicated(keep=False)

    if dup_mask.any():
        dup_counts = (
            df.loc[dup_mask, key_col]
            .value_counts()
            .head(10)
            .to_dict()
        )
        raise ValueError(
            f"{label} punya duplicate {key_col}. Contoh: {dup_counts}"
        )


def create_member_feature_staging(engine, df_stage):
    cols = df_stage.columns.tolist()

    cols_sql = []
    for col in cols:
        if col == "id_keluarga":
            cols_sql.append(f"{col} VARCHAR PRIMARY KEY")
        else:
            cols_sql.append(f"{col} SMALLINT")

    sql = f"""
        DROP TABLE IF EXISTS dtsen.tmp_member_feature_stage;
        CREATE TABLE dtsen.tmp_member_feature_stage (
            {", ".join(cols_sql)}
        )
    """

    with engine.begin() as conn:
        conn.execute(text(sql))


def insert_member_stage(df_stage, engine):
    if df_stage.empty:
        return

    cols = df_stage.columns.tolist()

    copy_dataframe_to_postgres(
        df_stage,
        engine,
        "tmp_member_feature_stage",
        "dtsen",
        cols
    )


def load_member_rules():
    files = ["education.json", "health.json"]

    rules = []
    for file in files:
        with open(f"config/{file}") as f:
            rules.append(json.load(f))

    return rules


def load_household_rules():
    files = ["housing.json", "economy.json"]

    rules = []
    for file in files:
        with open(f"config/{file}") as f:
            rules.append(json.load(f))

    return rules


def apply_null_mask(result, series):
    result = result.astype("float")
    result[series.isna()] = None
    return result


def evaluate_condition(df, cond):
    column = cond["column"]

    if column not in df.columns:
        return pd.Series([None] * len(df), index=df.index, dtype="float")

    series = df[column]

    if "between" in cond:
        low, high = cond["between"]
        return apply_null_mask(series.between(low, high), series)

    elif "in" in cond:
        return apply_null_mask(series.isin(cond["in"]), series)

    elif "not_in" in cond:
        return apply_null_mask(~series.isin(cond["not_in"]), series)

    elif "gt" in cond:
        return apply_null_mask(series > cond["gt"], series)

    elif "gte" in cond:
        return apply_null_mask(series >= cond["gte"], series)

    elif "lt" in cond:
        return apply_null_mask(series < cond["lt"], series)

    elif "lte" in cond:
        return apply_null_mask(series <= cond["lte"], series)

    return pd.Series([None] * len(df), index=df.index, dtype="float")


def parse_rule(rule, variables, df):
    parts = rule.split(".")

    if len(parts) == 2:
        var, key = parts
        cfg = variables[var]

        column = cfg["column"]
        values = cfg[key]

        if "operation" in cfg and cfg["operation"] == "divide":
            col1, col2 = column

            if col1 not in df.columns or col2 not in df.columns:
                return pd.Series(False, index=df.index)

            result = df[col1] / df[col2]

            if isinstance(values, dict):
                cond_dict = {"column": "tmp", **values}
            else:
                cond_dict = {"column": "tmp", "in": values}

            return evaluate_condition(
                result.to_frame(name="tmp"),
                cond_dict
            )

        if isinstance(column, list):
            masks = []

            for col in column:
                if col not in df.columns:
                    continue

                if isinstance(values, dict):
                    cond_dict = {"column": col, **values}
                else:
                    cond_dict = {"column": col, "in": values}

                masks.append(
                    evaluate_condition(df, cond_dict)
                )

            if masks:
                mask_df = pd.concat(masks, axis=1)
                return mask_df.eq(1).any(axis=1)

            return pd.Series(False, index=df.index)

        if column not in df.columns:
            return pd.Series(False, index=df.index)

        if isinstance(values, dict):
            cond_dict = {"column": column, **values}
        else:
            cond_dict = {"column": column, "in": values}

        return evaluate_condition(df, cond_dict)

    elif len(parts) == 3:
        var, subvar, key = parts

        cfg = variables[var][subvar]
        column = cfg["column"]
        values = cfg[key]

        if column not in df.columns:
            return pd.Series(False, index=df.index)

        if isinstance(values, dict):
            cond_dict = {"column": column, **values}
        else:
            cond_dict = {"column": column, "in": values}

        return evaluate_condition(df, cond_dict)

    else:
        raise Exception(f"Rule tidak valid: {rule}")


def combine_and_masks(cond_masks, index):
    group_df = pd.concat(cond_masks, axis=1)

    all_one = group_df.eq(1).all(axis=1)
    any_zero = group_df.eq(0).any(axis=1)
    any_null = group_df.isna().any(axis=1)

    group_result = pd.Series(index=index, dtype="float")
    group_result[all_one] = 1
    group_result[~all_one & any_zero] = 0
    group_result[~all_one & ~any_zero & any_null] = None

    return group_result


def combine_or_masks(all_masks, index):
    combined = pd.concat(all_masks, axis=1)

    any_one = combined.eq(1).any(axis=1)
    any_null = combined.isna().any(axis=1)

    feature = pd.Series(index=index, dtype="float")
    feature[any_one] = 1
    feature[~any_one & any_null] = None
    feature[~any_one & ~any_null] = 0

    return feature


def generate_features(df, configs):
    result = pd.DataFrame(index=df.index)
    result["id_keluarga"] = df["id_keluarga"]

    for config in configs:
        variables = config["variables"]
        features = config["features"]

        unique_rules = set()

        for feature_name, rule in features.items():
            groups = rule["conditions"]

            for group in groups:
                is_sequential = (
                    isinstance(group, list)
                    and all(isinstance(x, list) for x in group)
                )

                if is_sequential:
                    unique_rules.add(group[0][0])
                    unique_rules.add(group[1][0])
                else:
                    for cond in group:
                        unique_rules.add(cond)

        rule_cache = {}
        for rule_text in unique_rules:
            rule_cache[rule_text] = parse_rule(rule_text, variables, df)

        for feature_name, rule in features.items():
            groups = rule["conditions"]
            all_masks = []

            for group in groups:
                is_sequential = (
                    isinstance(group, list)
                    and all(isinstance(x, list) for x in group)
                )

                if is_sequential:
                    first_mask = rule_cache[group[0][0]]
                    second_mask = rule_cache[group[1][0]]

                    group_result = pd.Series(index=df.index, dtype="float")

                    group_result[first_mask == 0] = 0
                    group_result[first_mask.isna()] = None

                    mask_lolos = first_mask == 1

                    group_result[(mask_lolos) & (second_mask == 1)] = 1
                    group_result[(mask_lolos) & (second_mask == 0)] = 0
                    group_result[(mask_lolos) & (second_mask.isna())] = None

                else:
                    cond_masks = [rule_cache[cond] for cond in group]

                    arr = np.column_stack([
                        mask.to_numpy(dtype="float64", na_value=np.nan)
                        for mask in cond_masks
                    ])

                    all_one = np.all(arr == 1, axis=1)
                    any_zero = np.any(arr == 0, axis=1)
                    any_null = np.isnan(arr).any(axis=1)

                    group_result = pd.Series(index=df.index, dtype="float")

                    group_result[all_one] = 1
                    group_result[~all_one & any_zero] = 0
                    group_result[~all_one & ~any_zero & any_null] = None

                all_masks.append(group_result)

                arr = np.column_stack([
                    mask.to_numpy(dtype="float64", na_value=np.nan)
                    for mask in all_masks
                ])

                any_one = np.any(arr == 1, axis=1)
                any_null = np.isnan(arr).any(axis=1)

                feature = pd.Series(index=df.index, dtype="float")
                feature[any_one] = 1
                feature[~any_one & any_null] = None
                feature[~any_one & ~any_null] = 0

                result[feature_name] = feature

    return result


def aggregate_member_feature_chunk(df_member_feature):
    feature_cols = [c for c in df_member_feature.columns if c != "id_keluarga"]

    if not feature_cols:
        return df_member_feature[["id_keluarga"]].drop_duplicates().copy()

    grouped = df_member_feature.groupby(
        "id_keluarga",
        dropna=False,
        sort=False
    )

    group_sizes = grouped.size()
    max_vals = grouped[feature_cols].max(min_count=1)
    non_null_counts = grouped[feature_cols].count()
    any_null = non_null_counts.lt(group_sizes, axis=0)

    agg_df = max_vals.astype("float")

    agg_df = agg_df.mask(max_vals.ne(1) & any_null, other=np.nan)
    agg_df = agg_df.fillna(np.nan)

    zero_mask = max_vals.ne(1) & ~any_null
    agg_df = agg_df.mask(zero_mask, other=0)

    agg_df = agg_df.reset_index()

    return agg_df


def run_preprocessing_province(kode_provinsi):
    engine = get_connection()

    member_rules = load_member_rules()
    household_rules = load_household_rules()

    member_feature_cols_raw = get_member_feature_columns(member_rules)
    household_query, _ = build_household_query(household_rules)

    logging.info("Generate member features")

    stage_created = False
    stage_cols = None

    for i, family_ids in enumerate(
        load_family_id_chunks(kode_provinsi, chunksize=10000),
        start=1
    ):
        chunk_start = time.time()

        t0 = time.time()
        anggota_chunk = load_members_by_family_ids(
            family_ids,
            member_feature_cols_raw
        )
        logging.info(f"[member chunk {i}] load: {time.time() - t0:.2f}s")

        if anggota_chunk.empty:
            logging.info(f"[member chunk {i}] empty, skip")
            continue

        t1 = time.time()
        anggota_chunk = clean_dataframe(anggota_chunk)
        anggota_chunk = optimize_member_dtypes(anggota_chunk, member_rules)
        logging.info(f"[member chunk {i}] clean + optimize dtype: {time.time() - t1:.2f}s")

        t2 = time.time()
        anggota_chunk["is_asn"] = (
            anggota_chunk["status_dalam_pekerjaan_utama"] == 5
        ).astype("int8")
        logging.info(f"[member chunk {i}] is_asn: {time.time() - t2:.2f}s")

        t3 = time.time()
        anggota_feature = generate_features(
            anggota_chunk,
            member_rules
        )
        logging.info(f"[member chunk {i}] generate_features: {time.time() - t3:.2f}s")

        t4 = time.time()
        anggota_feature["ada_asn"] = anggota_chunk["is_asn"].to_numpy()
        agg_chunk = aggregate_member_feature_chunk(anggota_feature)
        logging.info(f"[member chunk {i}] aggregate_chunk: {time.time() - t4:.2f}s")

        assert_unique_key(
            agg_chunk,
            "id_keluarga",
            f"agg_chunk province {kode_provinsi} chunk {i}"
        )

        if stage_cols is None:
            stage_cols = ["id_keluarga"] + sorted(
                [c for c in agg_chunk.columns if c != "id_keluarga"]
            )

        agg_chunk = agg_chunk.reindex(columns=stage_cols)

        for col in stage_cols:
            if col != "id_keluarga":
                agg_chunk[col] = (
                    pd.to_numeric(agg_chunk[col], errors="coerce")
                    .round()
                    .astype("Int64")
                )

        if not stage_created:
            create_member_feature_staging(engine, agg_chunk)
            stage_created = True

        t5 = time.time()
        insert_member_stage(agg_chunk, engine)
        logging.info(f"[member chunk {i}] save_stage: {time.time() - t5:.2f}s")

        logging.info(f"[member chunk {i}] total: {time.time() - chunk_start:.2f}s")

    logging.info("Generate household features")

    t6 = time.time()
    keluarga = load_household(
        household_query,
        params={"kode_provinsi": kode_provinsi}
    )
    keluarga = clean_dataframe(keluarga)
    keluarga = optimize_household_dtypes(keluarga, household_rules)
    logging.info(f"[household] load + clean + optimize dtype: {time.time() - t6:.2f}s")

    t7 = time.time()
    keluarga_feature = generate_features(
        keluarga,
        household_rules
    )
    logging.info(f"[household] generate_features: {time.time() - t7:.2f}s")

    assert_unique_key(keluarga, "id_keluarga", f"keluarga province {kode_provinsi}")
    assert_unique_key(keluarga_feature, "id_keluarga", f"keluarga_feature province {kode_provinsi}")

    if stage_created:
        df_member_agg = pd.read_sql(
            text("""
                SELECT *
                FROM dtsen.tmp_member_feature_stage
                ORDER BY id_keluarga
            """),
            engine
        )
    else:
        df_member_agg = pd.DataFrame(columns=["id_keluarga"])

    assert_unique_key(df_member_agg, "id_keluarga", f"member_stage province {kode_provinsi}")

    logging.info("Build dataset_feature")

    base_cols = [
        "id_keluarga",
        "kode_provinsi",
        "kode_kabupaten_kota",
        "kode_kecamatan",
        "kode_kelurahan_desa",
        "desil_nasional"
    ]

    keluarga_base = keluarga[base_cols].copy()

    member_feature_cols = [
        c for c in df_member_agg.columns
        if c != "id_keluarga"
    ]

    household_feature_cols = [
        c for c in keluarga_feature.columns
        if c != "id_keluarga"
    ]

    t8 = time.time()
    df_final = (
        keluarga_base
        .merge(df_member_agg, on="id_keluarga", how="left", validate="one_to_one")
        .merge(keluarga_feature, on="id_keluarga", how="left", suffixes=("", "_hh"), validate="one_to_one")
    )
    logging.info(f"[build] merge final: {time.time() - t8:.2f}s")

    hh_dup_cols = [c for c in df_final.columns if c.endswith("_hh")]
    if hh_dup_cols:
        df_final = df_final.drop(columns=hh_dup_cols)

    ordered_cols = (
        base_cols
        + member_feature_cols
        + [c for c in household_feature_cols if c not in member_feature_cols]
    )

    for col in ordered_cols:
        if col not in df_final.columns:
            df_final[col] = None

    df_final = df_final[ordered_cols]

    assert_unique_key(df_final, "id_keluarga", f"df_final province {kode_provinsi}")

    smallint_cols = ["desil_nasional"] + member_feature_cols + household_feature_cols

    for col in smallint_cols:
        if col in df_final.columns:
            df_final[col] = (
                pd.to_numeric(df_final[col], errors="coerce")
                .round()
                .astype("Int64")
            )

    copy_dataframe_to_postgres(
        df_final,
        engine,
        "dataset_feature",
        "dtsen",
        ordered_cols
    )

    if stage_created:
        with engine.begin() as conn:
            conn.execute(text("DROP TABLE IF EXISTS dtsen.tmp_member_feature_stage"))


def run_preprocessing():
    total_start = time.time()
    logging.info("Preprocessing started")

    engine = get_connection()

    with engine.begin() as conn:
        conn.execute(text("TRUNCATE TABLE dtsen.dataset_feature CASCADE"))

    provinces = get_province_list()

    for kode_provinsi in provinces:
        logging.info(f"=== Province {kode_provinsi} ===")
        run_preprocessing_province(kode_provinsi)

    logging.info(f"Preprocessing finished in {time.time() - total_start:.2f}s")