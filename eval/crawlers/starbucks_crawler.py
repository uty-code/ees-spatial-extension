from base_crawler import BaseCrawler

class StarbucksCrawler(BaseCrawler):
    def __init__(self):
        # brand_id = 1 (스타벅스)
        super().__init__(brand_id=1, brand_name="Starbucks")
        self.api_url = "https://www.starbucks.co.kr/store/getStore.do?r=G8ZJ520P64"
        
    def crawl(self):
        self.logger.info("Starting Starbucks Crawler...")
        
        # 스타벅스 코리아 매장 검색 API 파라미터 (전국 검색)
        payload = {
            'in_biz_cds': '0',
            'in_scodes': '0',
            'ins_lat': '37.5665',
            'ins_lng': '126.9780',
            'search_text': '',
            'p_sido_cd': '01', # 01: 서울, 전체를 돌려면 시도 코드 루프 필요 (여기서는 테스트로 서울만)
            'p_gugun_cd': '',
            'in_biz_cd': '',
            'set_date': ''
        }
        
        headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
            'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
            'X-Requested-With': 'XMLHttpRequest',
            'Referer': 'https://www.starbucks.co.kr/store/store_map.do'
        }
        
        # 시도(sido) 코드 리스트 (01: 서울 ~ 17: 세종)
        sido_codes = [f"{i:02d}" for i in range(1, 18)]
        
        all_stores = []
        
        for sido in sido_codes:
            self.logger.info(f"Fetching Sido Code: {sido}")
            payload['p_sido_cd'] = sido
            
            response = self.fetch_with_retry(self.api_url, method='POST', data=payload, headers=headers)
            data = response.json()
            
            store_list = data.get('list', [])
            for item in store_list:
                store_info = {
                    'branch_name': item.get('s_name', '').strip() + '점',
                    'address': item.get('addr', '').strip(),
                    'latitude': float(item.get('lat', 0.0)),
                    'longitude': float(item.get('lot', 0.0))
                }
                all_stores.append(store_info)
                
        self.logger.info(f"Total stores fetched: {len(all_stores)}")
        
        # DB 저장
        self.save_to_db(all_stores)

if __name__ == "__main__":
    crawler = StarbucksCrawler()
    crawler.crawl()
