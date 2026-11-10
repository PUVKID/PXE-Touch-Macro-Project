# 자체 오프라인 명령 모델 연구 소스

한국어 명령을 구조 토큰과 COPY 위치로 예측하는 무작위 초기화 소형 encoder-decoder Transformer의 학습·평가·codec 코드입니다. 사전학습 가중치·외부 tokenizer·유료 추론 API를 연결하지 않습니다. 별도 시범 행동 정책 및 OCR 엔진과 구분합니다.

문자 어휘와 UTF-8 byte fallback, 인용 리터럴을 가역적으로 분리하는540 입력 codec, 고정72 출력 구조 토큰을 사용합니다. 의미 구조와 슬롯은 신경망이 선택하며 인용문 안 명령을 재해석하지 않습니다. 규칙이 한국어 동사를 AST로 대신 변환하는 방식은 사용하지 않습니다. 모델은192차원·4heads·encoder2/decoder2·FFN768이며540 구성의 실제 파라미터는2,651,976개입니다.

고정32행과 기계적 개명32행을 대상으로 한 작은 과적합 시험은 각각32/32 AST 일치였습니다. 개명 짝은 같은 codec 입력을 쓰므로 미본 문법 성능을 뜻하지 않습니다. 후속 v10 개발 관측은11/64로 품질이 부족했고 v11 새 초기·훈련은 보류했습니다. 이 코드를 범용 한국어 이해·99% 성능·실행 가능한 최종 언어 모델로 표시하지 않습니다. 학습된 명령 모델과 데이터는 공개 사본 및 앱 자산에 포함하지 않습니다.

`batching.py`는 teacher targets, `model.py`는 구조/start/end 손실, `evaluate.py`는 greedy 생성·원문 복원·분리 지표를 다룹니다. `train.py`는 source·자료·codec·checkpoint 결합과 배치별 인코딩·주기 snapshot·시간 상한을 기록합니다. `export.py`의 Torch/ORT 정합 도구는 최종 모델 납품 승인과 구분합니다. `requirements.lock`은 정확한 패키지 버전·공식 wheel URL·hash를 기록하며 패키지 자체를 재배포하지 않습니다.

[계약](CONTRACT.md) · [전체 동작 스키마](schema.py) · [체크포인트 결합](codec_contract.py) · [순수 codec](literal_codec.py)
