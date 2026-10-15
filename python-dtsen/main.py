import logging
import time

from pipeline.preprocessing import run_preprocessing
from pipeline.depriv_engine import run_deprivation_engine
from pipeline.recom_engine import run_recommendation_engine


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s"
)


if __name__ == "__main__":
    t0 = time.time()
    run_preprocessing()
    print(f"Preprocessing: {time.time()-t0:.2f} detik")

    t1 = time.time()
    run_deprivation_engine()
    print(f"Deprivation engine: {time.time()-t1:.2f} detik")

    t2 = time.time()
    run_recommendation_engine()
    print(f"Recommendation engine: {time.time()-t2:.2f} detik")