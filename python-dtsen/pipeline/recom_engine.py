import pandas as pd
import json
import logging
import io
import time
from sqlalchemy import text

from database.db_connection import get_connection
from pipeline.load_data import get_province_list


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s"
)


def copy_dataframe_to_postgres(
    df,
    engine,
    table_name,
    schema,
    column_order
):
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


def pretty_indicator_name(indicator_name):
    return indicator_name.replace("_", " ").title()


def normalize_indicator_name(name):
    return " ".join(
        str(name)
        .strip()
        .lower()
        .replace("_", " ")
        .split()
    )


def build_ahp_map(ahp_weights):
    ahp_map = {}

    for row in ahp_weights["weights"]:
        program = str(row["Program"]).strip()
        indikator = normalize_indicator_name(row["Indikator"])

        key = (program, indikator)

        if key in ahp_map:
            raise ValueError(
                f"Duplicate bobot AHP ditemukan untuk "
                f"program={program}, indikator={row['Indikator']}"
            )

        ahp_map[key] = row["Bobot"]

    return ahp_map


def validate_ahp_mapping(config, ahp_map):
    errors = []

    for program, cfg in config["programs"].items():
        indicator_cfg = cfg.get("indicators", [])

        for ind in indicator_cfg:
            nama = ind["nama"]

            key = (
                str(program).strip(),
                normalize_indicator_name(nama)
            )

            if key not in ahp_map:
                errors.append(
                    f"- {program}: {nama}"
                )

    if errors:
        raise ValueError(
            "Mapping bobot AHP tidak lengkap.\n"
            "Indikator berikut belum ditemukan di ahp_weights.json:\n"
            + "\n".join(errors)
        )


def validate_program_indicators(config, indicator_config):
    errors = []

    available_indicators = set(
        indicator_config["indicators"].keys()
    )

    for program, cfg in config["programs"].items():
        for ind in cfg.get("indicators", []):
            nama = ind["nama"]

            if nama not in available_indicators:
                errors.append(
                    f"- {program}: {nama}"
                )

    if errors:
        raise ValueError(
            "Indikator pada program.json tidak ditemukan "
            "di indicator.json:\n"
            + "\n".join(errors)
        )


def build_base(df_deprivasi, df_feature):
    status_wide = (
        df_deprivasi
        .pivot(
            index="id_keluarga",
            columns="indikator",
            values="status"
        )
        .add_prefix("status_")
        .reset_index()
    )

    nilai_wide = (
        df_deprivasi
        .pivot(
            index="id_keluarga",
            columns="indikator",
            values="nilai"
        )
        .add_prefix("nilai_")
        .reset_index()
    )

    complete_wide = (
        df_deprivasi
        .pivot(
            index="id_keluarga",
            columns="indikator",
            values="is_complete"
        )
        .add_prefix("is_complete_")
        .reset_index()
    )

    base = (
        status_wide
        .merge(
            nilai_wide,
            on="id_keluarga",
            how="outer"
        )
        .merge(
            complete_wide,
            on="id_keluarga",
            how="outer"
        )
        .merge(
            df_feature,
            on="id_keluarga",
            how="left"
        )
    )

    return base


def build_indikator_deprivasi(
    pivot,
    active_cols,
    indicator_labels,
    status_program
):
    active_matrix = pivot[active_cols].eq(1).copy()

    active_matrix.columns = [
        col.replace("aktif_", "")
        for col in active_cols
    ]

    result = pd.Series(
        index=pivot.index,
        dtype="object"
    )

    active_rows = status_program == 1

    if not active_rows.any():
        return result

    selected = active_matrix.loc[active_rows]

    labels = []

    for _, row in selected.iterrows():
        active_indicators = [
            indicator_labels.get(
                ind,
                pretty_indicator_name(ind)
            )
            for ind, is_active in row.items()
            if is_active
        ]

        if active_indicators:
            labels.append(
                ", ".join(active_indicators)
            )
        else:
            labels.append(None)

    result.loc[active_rows] = labels

    return result


def jenks_breaks(values, k):
    values = sorted(
        set(
            float(v)
            for v in values
            if pd.notna(v)
        )
    )

    if len(values) < k:
        logging.warning(
            f"Jumlah nilai unik ({len(values)}) lebih kecil "
            f"dari jumlah kelas k={k}. "
            f"Jumlah kelas disesuaikan menjadi {len(values)}."
        )
        k = len(values)

    n = len(values)

    lower_class_limits = [
        [0] * (k + 1)
        for _ in range(n + 1)
    ]

    variance_combinations = [
        [float("inf")] * (k + 1)
        for _ in range(n + 1)
    ]

    for i in range(1, k + 1):
        lower_class_limits[1][i] = 1
        variance_combinations[1][i] = 0

    for l in range(2, n + 1):
        variance = 0.0
        sum_values = 0.0
        sum_squares = 0.0
        w = 0

        for m in range(1, l + 1):
            i3 = l - m + 1
            value = values[i3 - 1]

            w += 1
            sum_values += value
            sum_squares += value * value

            variance = sum_squares - (
                sum_values * sum_values / w
            )

            if i3 != 1:
                for j in range(2, k + 1):
                    candidate = (
                        variance_combinations[i3 - 1][j - 1]
                        + variance
                    )

                    if candidate < variance_combinations[l][j]:
                        variance_combinations[l][j] = candidate
                        lower_class_limits[l][j] = i3

        variance_combinations[l][1] = variance
        lower_class_limits[l][1] = 1

    breaks = [0] * (k + 1)
    breaks[k] = values[-1]

    count = n

    for j in range(k, 1, -1):
        idx = lower_class_limits[count][j]
        breaks[j - 1] = values[idx - 1]
        count = idx - 1

    breaks[0] = values[0]

    return breaks


def assign_priority(scores, breaks):
    priority = pd.Series(
        pd.NA,
        index=scores.index,
        dtype="Int64"
    )

    total_classes = len(breaks) - 1

    for i in range(1, len(breaks)):
        lower = breaks[i - 1]
        upper = breaks[i]

        if i == 1:
            mask = (
                (scores >= lower) &
                (scores <= upper)
            )
        else:
            mask = (
                (scores > lower) &
                (scores <= upper)
            )

        priority.loc[mask] = (
            total_classes - i + 1
        )

    return priority


def generate_recommendation(
    base_df,
    config,
    ahp_map
):
    programs = config["programs"]
    results = []

    for program, cfg in programs.items():
        prog_start = time.time()

        indicator_cfg = cfg["indicators"]

        indicator_list = [
            i["nama"]
            for i in indicator_cfg
        ]

        indicator_labels = {
            i["nama"]: i.get(
                "label",
                pretty_indicator_name(i["nama"])
            )
            for i in indicator_cfg
        }

        needed_cols = {
            "id_keluarga",
            "kode_provinsi",
            "kode_kabupaten_kota",
            "kode_kecamatan",
            "kode_kelurahan_desa",
            "desil_nasional",
            "ada_asn"
        }

        for ind in indicator_list:
            needed_cols.add(
                f"status_{ind}"
            )

            needed_cols.add(
                f"nilai_{ind}"
            )

            needed_cols.add(
                f"is_complete_{ind}"
            )

        for ind in indicator_cfg:
            if "syarat" in ind:
                needed_cols.update(
                    ind["syarat"].keys()
                )

        if "syarat_program" in cfg:
            needed_cols.update(
                cfg["syarat_program"].keys()
            )

        existing_cols = [
            c
            for c in needed_cols
            if c in base_df.columns
        ]

        pivot = base_df[existing_cols].copy()

        for ind in indicator_list:
            status_col = f"status_{ind}"
            nilai_col = f"nilai_{ind}"
            complete_col = f"is_complete_{ind}"

            if status_col not in pivot.columns:
                pivot[status_col] = 0

            if nilai_col not in pivot.columns:
                pivot[nilai_col] = 0

            if complete_col not in pivot.columns:
                pivot[complete_col] = 1

        aktif_cols = []

        for ind in indicator_cfg:
            nama = ind["nama"]
            aktif_col = f"aktif_{nama}"

            pivot[aktif_col] = pivot[
                f"status_{nama}"
            ]

            if "syarat" in ind:
                for key, val in ind["syarat"].items():
                    mask = (
                        pivot[key].isna() |
                        (pivot[key] != val)
                    )

                    pivot.loc[
                        mask,
                        aktif_col
                    ] = 0

            aktif_cols.append(
                aktif_col
            )

        pivot["eligible"] = 1

        if "syarat_program" in cfg:
            for key, val in cfg[
                "syarat_program"
            ].items():

                pivot.loc[
                    pivot[key].notna() &
                    (pivot[key] != val),
                    "eligible"
                ] = 0

                pivot.loc[
                    pivot[key].isna(),
                    "eligible"
                ] = None

        status_cols = [
            f"status_{i}"
            for i in indicator_list
        ]

        any_active = (
            pivot[aktif_cols]
            .eq(1)
            .any(axis=1)
        )

        any_null = (
            pivot[status_cols]
            .isna()
            .any(axis=1)
        )

        status_program = pd.Series(
            index=pivot.index,
            dtype="float"
        )

        status_program[
            (pivot["eligible"] == 1) &
            any_active
        ] = 1

        status_program[
            (pivot["eligible"] == 1) &
            ~any_active &
            any_null
        ] = None

        status_program[
            (pivot["eligible"] == 1) &
            ~any_active &
            ~any_null
        ] = 0

        status_program[
            pivot["eligible"] == 0
        ] = 0

        status_program[
            pivot["eligible"].isna()
        ] = None

        bobot_map = {}

        for ind in indicator_cfg:
            nama = ind["nama"]

            key = (
                str(program).strip(),
                normalize_indicator_name(nama)
            )

            bobot_map[nama] = ahp_map[key]

        nilai_cols = [
            f"nilai_{i}"
            for i in indicator_list
        ]

        nilai_matrix = (
            pivot[nilai_cols]
            .fillna(0)
        )

        nilai_matrix.columns = indicator_list

        bobot_series = pd.Series(
            bobot_map
        )

        S = (
            nilai_matrix *
            bobot_series
        ).sum(axis=1)

        skor = S

        skor = skor.where(
            status_program == 1
        )

        skor[
            (status_program == 1) &
            (skor.isna())
        ] = 0

        asesmen = pd.Series(
            index=pivot.index,
            dtype="float"
        )

        asesmen[
            (pivot["desil_nasional"] >= 1) &
            (pivot["desil_nasional"] <= 5)
        ] = 0

        asesmen[
            (pivot["desil_nasional"] >= 6) &
            (pivot["desil_nasional"] <= 10)
        ] = 1

        asesmen[
            status_program.isna()
        ] = None

        mask_asn = (
            pivot["ada_asn"] == 1
        )

        status_program[
            mask_asn
        ] = 0

        skor[
            mask_asn
        ] = 0

        complete_cols = [
            f"is_complete_{i}"
            for i in indicator_list
        ]

        is_complete = (
            pivot[complete_cols]
            .min(axis=1)
        )

        is_complete[
            status_program.isna()
        ] = 0

        indikator_deprivasi = (
            build_indikator_deprivasi(
                pivot=pivot,
                active_cols=aktif_cols,
                indicator_labels=indicator_labels,
                status_program=status_program
            )
        )

        temp = pd.DataFrame({
            "id_keluarga":
                pivot["id_keluarga"],

            "kode_provinsi":
                pivot["kode_provinsi"],

            "kode_kabupaten_kota":
                pivot["kode_kabupaten_kota"],

            "kode_kecamatan":
                pivot["kode_kecamatan"],

            "kode_kelurahan_desa":
                pivot["kode_kelurahan_desa"],

            "desil_nasional":
                pivot["desil_nasional"],

            "program":
                program,

            "status":
                status_program,

            "skor":
                skor,

            "asesmen":
                asesmen,

            "is_complete":
                is_complete,

            "indikator_deprivasi":
                indikator_deprivasi
        })

        temp = temp[
            temp["status"] != 0
        ]

        results.append(temp)

        logging.info(
            f"[rekom:{program}] done in "
            f"{time.time() - prog_start:.2f}s | "
            f"rows={len(temp)}"
        )

    if not results:
        return pd.DataFrame()

    return pd.concat(
        results,
        ignore_index=True
    )


def run_recommendation_engine():
    total_start = time.time()

    logging.info(
        "Recommendation engine started"
    )

    engine = get_connection()

    # ============================================================
    # 1. HAPUS DATA REKOMENDASI LAMA
    # ============================================================

    with engine.begin() as conn:
        conn.execute(text("""
            TRUNCATE TABLE dtsen.data_rekomendasi
        """))

    # ============================================================
    # 2. LOAD CONFIG
    # ============================================================

    with open(
        "config/program.json",
        "r",
        encoding="utf-8"
    ) as f:
        config = json.load(f)

    with open(
        "config/ahp_weights.json",
        "r",
        encoding="utf-8"
    ) as f:
        ahp_weights = json.load(f)

    with open(
        "config/indicator.json",
        "r",
        encoding="utf-8"
    ) as f:
        indicator_config = json.load(f)

    # ============================================================
    # 3. BUILD + VALIDATE AHP
    # ============================================================

    ahp_map = build_ahp_map(
        ahp_weights
    )

    validate_program_indicators(
        config,
        indicator_config
    )

    validate_ahp_mapping(
        config,
        ahp_map
    )

    weighting_method = (
        config
        .get("weighting", {})
        .get("method", "ahp")
    )

    if weighting_method != "ahp":
        raise ValueError(
            f"Recommendation Engine wajib menggunakan "
            f"weighting AHP, tetapi konfigurasi menggunakan: "
            f"{weighting_method}"
        )

    logging.info(
        "AHP mapping validation passed"
    )

    # ============================================================
    # 4. TAMPUNG SEMUA HASIL PROVINSI
    # ============================================================

    all_rekomendasi = []

    # ============================================================
    # 5. PROSES PER PROVINSI
    # ============================================================

    for kode_provinsi in get_province_list():

        logging.info(
            f"Province {kode_provinsi}"
        )

        # --------------------------------------------------------
        # LOAD DATA DEPRIVASI
        # --------------------------------------------------------

        t0 = time.time()

        df_deprivasi = pd.read_sql(
            text("""
                SELECT
                    id_keluarga,
                    kode_provinsi,
                    kode_kabupaten_kota,
                    kode_kecamatan,
                    kode_kelurahan_desa,
                    desil_nasional,
                    indikator,
                    status,
                    nilai,
                    is_complete
                FROM dtsen.data_deprivasi
                WHERE kode_provinsi = :kode_provinsi
            """),
            engine,
            params={
                "kode_provinsi": kode_provinsi
            }
        )

        logging.info(
            f"[rekom] load df_deprivasi: "
            f"{time.time() - t0:.2f}s | "
            f"rows={len(df_deprivasi)}"
        )

        # --------------------------------------------------------
        # LOAD DATA FEATURE
        # --------------------------------------------------------

        t1 = time.time()

        df_feature = pd.read_sql(
            text("""
                SELECT
                    id_keluarga,
                    kode_provinsi,
                    kode_kabupaten_kota,
                    kode_kecamatan,
                    kode_kelurahan_desa,
                    desil_nasional,
                    ada_asn,
                    ada_anak_sekolah,
                    rumah_bukan_milik_sendiri
                FROM dtsen.dataset_feature
                WHERE kode_provinsi = :kode_provinsi
            """),
            engine,
            params={
                "kode_provinsi": kode_provinsi
            }
        )

        logging.info(
            f"[rekom] load df_feature: "
            f"{time.time() - t1:.2f}s | "
            f"rows={len(df_feature)}"
        )

        # --------------------------------------------------------
        # CEK DUPLICATE
        # --------------------------------------------------------

        dup_depriv = (
            df_deprivasi
            .duplicated(
                subset=[
                    "id_keluarga",
                    "indikator"
                ]
            )
            .any()
        )

        if dup_depriv:
            raise ValueError(
                "Ada duplicate pasangan "
                "id_keluarga + indikator "
                "di data_deprivasi"
            )

        # --------------------------------------------------------
        # KALAU PROVINSI TIDAK PUNYA DATA, LEWATI
        # --------------------------------------------------------

        if (
            df_deprivasi.empty or
            df_feature.empty
        ):
            logging.info(
                f"[rekom] Province {kode_provinsi} "
                f"tidak memiliki data yang dapat diproses."
            )
            continue

        # --------------------------------------------------------
        # BUILD BASE
        # --------------------------------------------------------

        t2 = time.time()

        base_df = build_base(
            df_deprivasi,
            df_feature
        )

        logging.info(
            f"[rekom] build_base: "
            f"{time.time() - t2:.2f}s | "
            f"rows={len(base_df)}"
        )

        # --------------------------------------------------------
        # GENERATE RECOMMENDATION
        # --------------------------------------------------------

        t3 = time.time()

        df_rekom = generate_recommendation(
            base_df,
            config,
            ahp_map
        )

        logging.info(
            f"[rekom] generate_recommendation: "
            f"{time.time() - t3:.2f}s | "
            f"rows={len(df_rekom)}"
        )

        # --------------------------------------------------------
        # SIMPAN HASIL PROVINSI KE MEMORY
        # --------------------------------------------------------

        if df_rekom.empty:
            logging.info(
                f"[rekom] Province {kode_provinsi} "
                f"tidak menghasilkan rekomendasi."
            )
            continue

        all_rekomendasi.append(
            df_rekom
        )

    # ============================================================
    # 6. CEK APAKAH ADA HASIL SAMA SEKALI
    # ============================================================

    if not all_rekomendasi:
        logging.info(
            "Tidak ada data rekomendasi yang dihasilkan "
            "dari seluruh provinsi."
        )

        logging.info(
            f"Recommendation engine finished in "
            f"{time.time() - total_start:.2f}s | "
            f"inserted=0"
        )

        return

    # ============================================================
    # 7. GABUNG SEMUA HASIL PROVINSI
    # ============================================================

    df_rekom_all = pd.concat(
        all_rekomendasi,
        ignore_index=True
    )

    logging.info(
        f"[rekom] total skor nasional terkumpul: "
        f"{len(df_rekom_all)} rows"
    )

    # ============================================================
    # 8. AMBIL JUMLAH KELAS PRIORITAS
    # ============================================================

    prioritas_config = config.get(
        "prioritas",
        {}
    )

    k = int(
        prioritas_config.get(
            "k",
            5
        )
    )

    if k < 2:
        raise ValueError(
            f"Jumlah kelas prioritas k harus >= 2, "
            f"tetapi ditemukan {k}"
        )

    # ============================================================
    # 9. HITUNG JENKS NASIONAL PER PROGRAM
    # ============================================================

    priority_breaks = {}

    for program in config["programs"]:

        program_scores = (
            df_rekom_all.loc[
                (
                    df_rekom_all["program"]
                    == program
                ) &
                (
                    df_rekom_all["status"]
                    == 1
                ),
                "skor"
            ]
            .dropna()
        )

        if program_scores.empty:
            logging.warning(
                f"[prioritas:{program}] "
                f"tidak ada skor valid."
            )
            continue

        breaks = jenks_breaks(
            program_scores.tolist(),
            k
        )

        priority_breaks[
            program
        ] = breaks

        logging.info(
            f"[prioritas:{program}] "
            f"k={k} | breaks={breaks}"
        )

    # ============================================================
    # 10. BUAT KOLOM PRIORITAS
    # ============================================================

    df_rekom_all["prioritas"] = pd.Series(
        pd.NA,
        index=df_rekom_all.index,
        dtype="Int64"
    )

    # ============================================================
    # 11. ASSIGN PRIORITAS
    # ============================================================

    for program, breaks in priority_breaks.items():

        mask = (
            (
                df_rekom_all["program"]
                == program
            ) &
            (
                df_rekom_all["status"]
                == 1
            )
        )

        df_rekom_all.loc[
            mask,
            "prioritas"
        ] = assign_priority(
            df_rekom_all.loc[
                mask,
                "skor"
            ],
            breaks
        ).values

    # ============================================================
    # 12. NORMALISASI TIPE DATA
    # ============================================================

    for col in [
        "status",
        "prioritas",
        "asesmen",
        "is_complete",
        "desil_nasional"
    ]:
        if col in df_rekom_all.columns:
            df_rekom_all[col] = (
                pd.to_numeric(
                    df_rekom_all[col],
                    errors="coerce"
                )
                .round()
                .astype("Int64")
            )

    # ============================================================
    # 13. SIMPAN SEMUA HASIL SEKALIGUS
    # ============================================================

    t4 = time.time()

    copy_dataframe_to_postgres(
        df_rekom_all,
        engine,
        "data_rekomendasi",
        "dtsen",
        [
            "id_keluarga",
            "kode_provinsi",
            "kode_kabupaten_kota",
            "kode_kecamatan",
            "kode_kelurahan_desa",
            "desil_nasional",
            "program",
            "status",
            "skor",
            "prioritas",
            "asesmen",
            "is_complete",
            "indikator_deprivasi"
        ]
    )

    logging.info(
        f"[rekom] save data_rekomendasi: "
        f"{time.time() - t4:.2f}s"
    )

    # ============================================================
    # 14. LOGGING SELESAI — JANGAN DIHAPUS
    # ============================================================

    logging.info(
        f"Recommendation engine finished in "
        f"{time.time() - total_start:.2f}s | "
        f"inserted={len(df_rekom_all)}"
    )