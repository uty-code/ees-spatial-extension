import time
import logging
import requests
import pymssql
from typing import List, Dict, Any

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

class BaseCrawler:
    """
    모든 프랜차이즈 크롤러의 기본 뼈대가 되는 부모 클래스입니다.
    재시도 로직, DB 연결 및 적재 등 공통 기능을 제공합니다.
    """
    def __init__(self, brand_id: int, brand_name: str):
        self.brand_id = brand_id
        self.brand_name = brand_name
        self.logger = logging.getLogger(f"{self.brand_name}_Crawler")
        
        # TODO: 실제 환경에서는 환경 변수(os.environ)에서 가져와야 함 (보안)
        self.db_server = 'localhost'
        self.db_user = 'sa'
        self.db_password = 'LocalPassword123!'
        self.db_name = 'master'

    def fetch_with_retry(self, url: str, method: str = 'GET', data: dict = None, headers: dict = None, retries: int = 3) -> requests.Response:
        """네트워크 요청 시 실패하면 지수 백오프(Exponential Backoff) 방식으로 재시도합니다."""
        for attempt in range(retries):
            try:
                if method.upper() == 'POST':
                    response = requests.post(url, data=data, headers=headers, timeout=10)
                else:
                    response = requests.get(url, params=data, headers=headers, timeout=10)
                
                response.raise_for_status()
                return response
            except requests.exceptions.RequestException as e:
                self.logger.warning(f"Request failed ({attempt+1}/{retries}): {e}")
                if attempt < retries - 1:
                    time.sleep(2 ** attempt) # 1초, 2초, 4초 대기
                else:
                    self.logger.error("Max retries reached.")
                    raise

    def save_to_db(self, stores: List[Dict[str, Any]]):
        """수집된 매장 정보를 DB에 적재 (Upsert 방식 - 중복 방지)"""
        if not stores:
            self.logger.warning("No stores to save.")
            return

        try:
            # pymssql 커넥션 (autocommit=True로 설정하거나 명시적 commit 호출 필요)
            conn = pymssql.connect(self.db_server, self.db_user, self.db_password, self.db_name)
            cursor = conn.cursor()
            
            success_count = 0
            for store in stores:
                # Merge (Upsert) 쿼리 실행
                # UK_BRANCH (brand_id, branch_name, address) 기준으로 중복 체크
                query = """
                MERGE INTO branches_51 AS target
                USING (SELECT %d AS brand_id, %s AS branch_name, %s AS address, %s AS lat, %s AS lng) AS source
                ON (target.brand_id = source.brand_id AND target.branch_name = source.branch_name)
                WHEN MATCHED THEN 
                    UPDATE SET target.address = source.address, target.latitude = source.lat, target.longitude = source.lng, target.updated_at = GETDATE()
                WHEN NOT MATCHED THEN
                    INSERT (brand_id, branch_name, address, latitude, longitude)
                    VALUES (source.brand_id, source.branch_name, source.address, source.lat, source.lng);
                """
                try:
                    cursor.execute(query, (
                        self.brand_id, 
                        store['branch_name'], 
                        store['address'], 
                        store['latitude'], 
                        store['longitude']
                    ))
                    success_count += 1
                except Exception as db_err:
                    self.logger.error(f"DB Error on store {store['branch_name']}: {db_err}")
            
            conn.commit()
            self.logger.info(f"Successfully saved {success_count}/{len(stores)} stores to DB.")
        
        except Exception as e:
            self.logger.error(f"Failed to connect or save to DB: {e}")
        finally:
            if 'conn' in locals():
                conn.close()

    def crawl(self):
        """자식 클래스에서 구현해야 하는 메인 크롤링 메서드"""
        raise NotImplementedError("crawl() must be implemented in subclass")
