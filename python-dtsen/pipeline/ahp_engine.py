import json
import os

import numpy as np
import pandas as pd
from scipy.stats import rankdata, chi2


# ============================================================
# PATH & LOAD CONFIG
# ============================================================

BASE_DIR = os.path.dirname(
    os.path.dirname(os.path.abspath(__file__))
)

CONFIG_PATH = os.path.join(
    BASE_DIR,
    "config",
    "ahp.json"
)

with open(CONFIG_PATH, "r", encoding="utf-8") as f:
    config = json.load(f)


# ============================================================
# HELPER CONFIG
# ============================================================

def get_config(config, *keys, default=None):
    """
    Mengambil nilai nested dari config secara aman.
    """
    value = config

    try:
        for key in keys:
            value = value[key]
        return value
    except (KeyError, TypeError):
        return default


# ============================================================
# LOAD DATA EXCEL
# ============================================================

input_file = os.path.join(
    BASE_DIR,
    config["input"]["file"]
)

sheet_name = config["input"]["sheet"]

if not os.path.exists(input_file):
    raise FileNotFoundError(
        f"File input tidak ditemukan: {input_file}"
    )

data = pd.read_excel(
    input_file,
    sheet_name=sheet_name
)


# ============================================================
# NAMA KOLOM DARI CONFIG
# ============================================================

columns = config["input"]["columns"]

COL_PROGRAM = columns["program"]
COL_EXPERT = columns["expert"]
COL_A = columns["indicator_a"]
COL_RELATION = columns["relation"]
COL_B = columns["indicator_b"]
COL_SCORE = columns["score"]


# ============================================================
# VALIDASI KOLOM
# ============================================================

required_columns = [
    COL_PROGRAM,
    COL_EXPERT,
    COL_A,
    COL_RELATION,
    COL_B,
    COL_SCORE
]

missing_columns = [
    col
    for col in required_columns
    if col not in data.columns
]

if missing_columns:
    raise ValueError(
        "Kolom berikut tidak ditemukan di Excel: "
        + str(missing_columns)
    )


# ============================================================
# CLEAN DATA
# ============================================================

text_columns = [
    COL_PROGRAM,
    COL_EXPERT,
    COL_A,
    COL_RELATION,
    COL_B
]

for col in text_columns:
    data[col] = (
        data[col]
        .astype(str)
        .str.strip()
    )

data[COL_SCORE] = pd.to_numeric(
    data[COL_SCORE],
    errors="coerce"
)

if data[COL_SCORE].isna().any():
    bad_rows = data[
        data[COL_SCORE].isna()
    ].index.tolist()

    raise ValueError(
        "Ada nilai Skor yang kosong atau bukan angka. "
        f"Baris bermasalah: {bad_rows}"
    )


# ============================================================
# VALIDASI DATA DASAR
# ============================================================

for col in [
    COL_PROGRAM,
    COL_EXPERT,
    COL_A,
    COL_B
]:
    if data[col].eq("").any():
        raise ValueError(
            f"Kolom '{col}' memiliki nilai kosong."
        )

if data[COL_RELATION].eq("").any():
    raise ValueError(
        f"Kolom '{COL_RELATION}' memiliki nilai kosong."
    )


# ============================================================
# VALIDASI SKALA AHP
# ============================================================

scale_min = float(
    config["scale"]["min"]
)

scale_max = float(
    config["scale"]["max"]
)

invalid_scores = data[
    (data[COL_SCORE] < scale_min)
    |
    (data[COL_SCORE] > scale_max)
]

if len(invalid_scores) > 0:
    raise ValueError(
        f"Terdapat skor di luar rentang "
        f"{scale_min:g}-{scale_max:g}."
    )


# ============================================================
# VALIDASI RELASI
# ============================================================

relation_mapping = config["relation_symbols"]

supported_relations = set(
    relation_mapping.keys()
)

invalid_relations = data[
    ~data[COL_RELATION].isin(
        supported_relations
    )
]

if len(invalid_relations) > 0:
    invalid_values = sorted(
        invalid_relations[
            COL_RELATION
        ].unique()
    )

    raise ValueError(
        "Terdapat simbol relasi yang tidak "
        f"dikenal di ahp.json: {invalid_values}"
    )


# ============================================================
# VALIDASI RELASI + SKOR
# ============================================================

def validate_relation_score(relation, score):
    """
    Aturan:
    > dan < membutuhkan skor pada skala AHP.
    = harus menggunakan skor 1.
    """

    relation = relation.strip()
    score = float(score)

    direction = relation_mapping[relation]

    if direction == "EQUAL":
        if not np.isclose(score, 1.0):
            raise ValueError(
                f"Relasi '=' harus memiliki Skor = 1, "
                f"tetapi ditemukan {score}."
            )


# ============================================================
# FUNGSI MEMBENTUK NILAI PAIRWISE
# ============================================================

def get_pairwise_value(relation, score):

    relation = relation.strip()
    score = float(score)

    if relation not in relation_mapping:
        raise ValueError(
            f"Relasi '{relation}' tidak ditemukan "
            "di ahp.json."
        )

    direction = relation_mapping[relation]

    if direction == "A_B":
        return score

    elif direction == "B_A":
        return 1.0 / score

    elif direction == "EQUAL":
        return 1.0

    else:
        raise ValueError(
            f"Mapping relasi '{direction}' tidak dikenal."
        )


# ============================================================
# RI TABLE
# ============================================================

ri_table = get_config(
    config,
    "consistency",
    "random_index",
    default=None
)

if ri_table is None:
    raise ValueError(
        "Konfigurasi Random Index belum ditemukan "
        "di ahp.json. Tambahkan "
        "'consistency.random_index'."
    )

# JSON menyimpan key sebagai string.
RI_TABLE = {
    int(k): float(v)
    for k, v in ri_table.items()
}


# ============================================================
# HITUNG PRIORITY VECTOR
# ============================================================

def calculate_priority(matrix):

    eigenvalues, eigenvectors = np.linalg.eig(
        matrix
    )

    # Ambil eigenvalue terbesar berdasarkan
    # bagian real.
    index = np.argmax(
        eigenvalues.real
    )

    lambda_max = float(
        eigenvalues[index].real
    )

    vector = eigenvectors[
        :,
        index
    ].real

    # Eigenvector bisa keluar dengan tanda
    # negatif tergantung library.
    vector = np.abs(vector)

    total = vector.sum()

    if np.isclose(total, 0):
        raise ValueError(
            "Gagal menghitung priority vector."
        )

    weights = vector / total

    return weights, lambda_max


# ============================================================
# HITUNG CI DAN CR
# ============================================================

def calculate_consistency(
    matrix,
    lambda_max
):

    n = matrix.shape[0]

    # Untuk n <= 2, CR tidak bermakna
    # untuk evaluasi konsistensi AHP.
    if n <= 2:
        return 0.0, 0.0

    CI = (
        lambda_max - n
    ) / (
        n - 1
    )

    if n not in RI_TABLE:
        raise ValueError(
            f"Random Index untuk n={n} belum tersedia "
            "di ahp.json."
        )

    RI = RI_TABLE[n]

    if np.isclose(RI, 0):
        CR = 0.0
    else:
        CR = CI / RI

    return float(CI), float(CR)


# ============================================================
# VALIDASI PAIRWISE
# ============================================================

def canonical_pair(indicator_a, indicator_b):
    """
    Membuat identitas pasangan yang tidak bergantung
    pada urutan.

    A-B dan B-A dianggap pasangan yang sama.
    """

    return tuple(
        sorted(
            [
                indicator_a,
                indicator_b
            ]
        )
    )


def validate_pairwise_structure(
    program,
    program_data,
    indicators,
    experts
):

    n = len(indicators)

    expected_pair_count = (
        n * (n - 1)
    ) // 2

    errors = []

    for expert in experts:

        expert_data = program_data[
            program_data[COL_EXPERT] == expert
        ]

        pair_counts = {}

        for row_number, row in expert_data.iterrows():

            indicator_a = row[COL_A]
            indicator_b = row[COL_B]

            if indicator_a == indicator_b:
                errors.append(
                    f"Program '{program}', Ahli '{expert}': "
                    f"indikator dibandingkan dengan dirinya sendiri "
                    f"({indicator_a}) pada baris Excel {row_number + 2}."
                )
                continue

            pair = canonical_pair(
                indicator_a,
                indicator_b
            )

            pair_counts[pair] = (
                pair_counts.get(pair, 0) + 1
            )

        duplicate_pairs = [
            pair
            for pair, count in pair_counts.items()
            if count > 1
        ]

        if duplicate_pairs:

            errors.append(
                f"Program '{program}', Ahli '{expert}': "
                f"terdapat pasangan indikator duplikat: "
                f"{duplicate_pairs}"
            )

        actual_pair_count = len(pair_counts)

        if actual_pair_count != expected_pair_count:

            errors.append(
                f"Program '{program}', Ahli '{expert}': "
                f"jumlah pasangan indikator tidak lengkap. "
                f"Expected={expected_pair_count}, "
                f"Actual={actual_pair_count}."
            )

    if errors:

        error_message = (
            "\n\n".join(
                errors
            )
        )

        raise ValueError(
            "Validasi struktur pairwise gagal:\n\n"
            + error_message
        )


# ============================================================
# HITUNG KENDALL'S W
# ============================================================

def calculate_kendall_w(
    expert_weights,
    expert_names
):
    """
    Menghitung Kendall's W berdasarkan ranking indikator
    yang dihasilkan dari priority vector masing-masing ahli.

    expert_weights:
        list of tuples:
        (expert_name, weights)

    Returns:
        W, chi_square, p_value
    """

    if len(expert_weights) < 2:
        return 0.0, 0.0, 1.0

    rankings = []

    for expert_name, weights in expert_weights:

        weights = np.asarray(
            weights,
            dtype=float
        )

        # rankdata:
        # nilai terbesar = ranking terbesar.
        #
        # method='average' menangani ties.
        ranks = rankdata(
            weights,
            method="average"
        )

        rankings.append(ranks)

    rankings = np.asarray(
        rankings,
        dtype=float
    )

    m = rankings.shape[0]  # jumlah ahli
    n = rankings.shape[1]  # jumlah indikator

    if n < 2:
        return 0.0, 0.0, 1.0

    rank_sums = rankings.sum(
        axis=0
    )

    mean_rank_sum = (
        m * (n + 1)
    ) / 2.0

    S = np.sum(
        (
            rank_sums
            - mean_rank_sum
        ) ** 2
    )

    # ========================================================
    # TIE CORRECTION
    # ========================================================

    tie_sum = 0.0

    for row in rankings:

        _, counts = np.unique(
            row,
            return_counts=True
        )

        tie_groups = counts[
            counts > 1
        ]

        for t in tie_groups:
            tie_sum += (
                t ** 3 - t
            )

    denominator = (
        m ** 2
        * (n ** 3 - n)
        - m * tie_sum
    )

    if np.isclose(denominator, 0):
        W = 0.0
    else:
        W = (
            12.0 * S
        ) / denominator

    # Batasi efek floating point.
    W = float(
        np.clip(
            W,
            0.0,
            1.0
        )
    )

    # Approximation chi-square:
    # chi2 = m * (n - 1) * W
    chi_square = (
        m
        * (n - 1)
        * W
    )

    df = n - 1

    p_value = float(
        chi2.sf(
            chi_square,
            df
        )
    )

    return (
        W,
        float(chi_square),
        p_value
    )


# ============================================================
# INTERPRETASI KENDALL W
# ============================================================

def get_agreement_status(
    W,
    p_value,
    alpha
):
    """
    Kategori deskriptif agreement.

    Ini hanya label deskriptif untuk sistem,
    bukan pengujian substantif baru.
    """

    if p_value <= alpha:

        significant = True

    else:

        significant = False

    if W < 0.20:
        level = "Slight"
    elif W < 0.40:
        level = "Fair"
    elif W < 0.60:
        level = "Moderate"
    elif W < 0.80:
        level = "Substantial"
    else:
        level = "Almost perfect"

    return {
        "Level": level,
        "Signifikan": significant
    }


# ============================================================
# PROSES SATU PROGRAM
# ============================================================

def process_program(
    program,
    program_data
):

    # --------------------------------------------------------
    # AMBIL SEMUA INDIKATOR
    # --------------------------------------------------------

    indicators = sorted(
        set(
            program_data[COL_A].tolist()
            +
            program_data[COL_B].tolist()
        )
    )

    n = len(indicators)

    if n < 2:

        raise ValueError(
            f"Program '{program}' memiliki "
            f"kurang dari 2 indikator."
        )

    indicator_index = {
        indicator: i
        for i, indicator
        in enumerate(indicators)
    }

    # --------------------------------------------------------
    # DAFTAR AHLI
    # --------------------------------------------------------

    experts = sorted(
        program_data[
            COL_EXPERT
        ].unique()
    )

    if len(experts) < 2:

        raise ValueError(
            f"Program '{program}' hanya memiliki "
            f"{len(experts)} ahli. "
            "Minimal 2 ahli diperlukan untuk "
            "menghitung Kendall's W."
        )

    # --------------------------------------------------------
    # VALIDASI STRUKTUR
    # --------------------------------------------------------

    validate_pairwise_structure(
        program,
        program_data,
        indicators,
        experts
    )

    # --------------------------------------------------------
    # VALIDASI SETIAP BARIS
    # --------------------------------------------------------

    for _, row in program_data.iterrows():

        validate_relation_score(
            row[COL_RELATION],
            row[COL_SCORE]
        )

    # --------------------------------------------------------
    # MATRICES AHLI
    # --------------------------------------------------------

    expert_matrices = []

    expert_weights = []

    for expert in experts:

        expert_data = program_data[
            program_data[COL_EXPERT] == expert
        ]

        matrix = np.ones(
            (n, n),
            dtype=float
        )

        # ----------------------------------------------------
        # BENTUK MATRIKS AHLI
        # ----------------------------------------------------

        for _, row in expert_data.iterrows():

            indicator_a = row[COL_A]
            indicator_b = row[COL_B]

            relation = row[
                COL_RELATION
            ]

            score = row[
                COL_SCORE
            ]

            i = indicator_index[
                indicator_a
            ]

            j = indicator_index[
                indicator_b
            ]

            value = get_pairwise_value(
                relation,
                score
            )

            matrix[i, j] = value

            matrix[j, i] = (
                1.0 / value
            )

        # ----------------------------------------------------
        # VALIDASI MATRIKS
        # ----------------------------------------------------

        if not np.allclose(
            matrix * matrix.T,
            1.0
        ):
            raise ValueError(
                f"Matriks pairwise Program '{program}', "
                f"Ahli '{expert}' tidak reciprocal."
            )

        # ----------------------------------------------------
        # PRIORITY VECTOR MASING-MASING AHLI
        # ----------------------------------------------------

        individual_weights, _ = (
            calculate_priority(
                matrix
            )
        )

        expert_matrices.append(
            matrix
        )

        expert_weights.append(
            (
                expert,
                individual_weights
            )
        )

    # ========================================================
    # KENDALL'S W
    # ========================================================

    kendall_w, kendall_chi_square, kendall_p = (
        calculate_kendall_w(
            expert_weights,
            experts
        )
    )

    alpha = float(
        get_config(
            config,
            "agreement",
            "alpha",
            default=0.05
        )
    )

    agreement_status = get_agreement_status(
        kendall_w,
        kendall_p,
        alpha
    )

    # ========================================================
    # AGREGASI GEOMETRIC MEAN
    # ========================================================

    matrices = np.stack(
        expert_matrices
    )

    aggregated_matrix = np.exp(
        np.mean(
            np.log(matrices),
            axis=0
        )
    )

    # ========================================================
    # HITUNG BOBOT AGREGAT
    # ========================================================

    weights, lambda_max = (
        calculate_priority(
            aggregated_matrix
        )
    )

    # ========================================================
    # HITUNG CI / CR
    # ========================================================

    CI, CR = calculate_consistency(
        aggregated_matrix,
        lambda_max
    )

    threshold = float(
        config[
            "consistency"
        ][
            "threshold"
        ]
    )

    # --------------------------------------------------------
    # STATUS CR
    # --------------------------------------------------------

    if n == 2:

        status_cr = (
            "CR tidak dievaluasi "
            "(2 indikator)"
        )

    elif CR <= threshold:

        status_cr = "Konsisten"

    else:

        status_cr = "Tidak konsisten"

    # ========================================================
    # HASIL BOBOT
    # ========================================================

    weight_results = []

    for indicator, weight in zip(
        indicators,
        weights
    ):

        weight_results.append({

            "Program": program,

            "Indikator": indicator,

            # Tetap NUMERIC.
            # Tidak diubah menjadi string.
            "Bobot": float(weight)

        })

    # ========================================================
    # SUMMARY
    # ========================================================

    summary = {

        "Program": program,

        "Jumlah_Indikator": int(n),

        "Jumlah_Ahli": int(len(experts)),

        "Lambda_Max": float(
            lambda_max
        ),

        "CI": float(CI),

        "CR": float(CR),

        "Status_CR": status_cr,

        "Kendall_W": float(
            kendall_w
        ),

        "Kendall_Chi_Square": float(
            kendall_chi_square
        ),

        "Kendall_P_Value": float(
            kendall_p
        ),

        "Agreement_Level": (
            agreement_status[
                "Level"
            ]
        ),

        "Agreement_Signifikan": (
            agreement_status[
                "Signifikan"
            ]
        )

    }

    return (
        summary,
        weight_results
    )


# ============================================================
# PROSES SEMUA PROGRAM
# ============================================================

all_summary = []

all_weights = []

programs = sorted(
    data[
        COL_PROGRAM
    ].unique()
)

# Dibaca sekali dari config agar juga tersedia untuk metadata output.
threshold = float(
    config[
        "consistency"
    ][
        "threshold"
    ]
)

print()
print("=" * 100, flush=True)
print("MEMULAI PROSES AHP", flush=True)
print("=" * 100, flush=True)
print(
    f"Total program yang diproses: {len(programs)}",
    flush=True
)

for program_number, program in enumerate(programs, start=1):

    print(
        f"\n[{program_number}/{len(programs)}] Memproses program: {program} ...",
        flush=True
    )

    program_data = data[
        data[
            COL_PROGRAM
        ] == program
    ].copy()

    summary, weights = (
        process_program(
            program,
            program_data
        )
    )

    all_summary.append(
        summary
    )

    all_weights.extend(
        weights
    )

    print(
        f"[{program_number}/{len(programs)}] {program} selesai.",
        flush=True
    )


# ============================================================
# DATAFRAME HASIL
# ============================================================

print()
print("Menyusun hasil AHP ...", flush=True)

df_summary = pd.DataFrame(
    all_summary
)

df_weights = pd.DataFrame(
    all_weights
)

print("Hasil AHP berhasil disusun.", flush=True)


# ============================================================
# TAMPILKAN HASIL
# ============================================================

pd.set_option(
    "display.max_columns",
    None
)

pd.set_option(
    "display.width",
    250
)

pd.set_option(
    "display.max_rows",
    None
)

print()
print("=" * 100)
print("HASIL KONSISTENSI DAN AGREEMENT AHP")
print("=" * 100)

print(
    df_summary.to_string(
        index=False
    )
)

print()
print("=" * 100)
print("HASIL BOBOT AHP")
print("=" * 100)

print(
    df_weights.to_string(
        index=False
    )
)


# ============================================================
# SIMPAN HASIL KE JSON
# ============================================================

print()
print("Menyimpan hasil AHP ke JSON ...", flush=True)

output_file = os.path.join(
    BASE_DIR,
    config["output"]["file"]
)

output_dir = os.path.dirname(
    output_file
)

os.makedirs(
    output_dir,
    exist_ok=True
)


# ============================================================
# METADATA OUTPUT
# ============================================================

output = {

    "metadata": {

        "method": "AHP",

        "aggregation": config[
            "aggregation"
        ][
            "method"
        ],

        "priority": config[
            "priority"
        ][
            "method"
        ],

        "agreement": config[
            "agreement"
        ][
            "method"
        ],

        "consistency_threshold": threshold

    },

    "summary":
        df_summary.to_dict(
            orient="records"
        ),

    "weights":
        df_weights.to_dict(
            orient="records"
        )

}


# ============================================================
# WRITE JSON
# ============================================================

with open(
    output_file,
    "w",
    encoding="utf-8"
) as f:

    json.dump(
        output,
        f,
        ensure_ascii=False,
        indent=2
    )


# ============================================================
# SELESAI
# ============================================================

print()
print("=" * 100)
print(
    f"Hasil disimpan ke: {output_file}"
)
print("=" * 100)