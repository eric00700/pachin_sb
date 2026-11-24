# PACHI AI v2.5 — 팁 사전(통상 시 + 연출 커스텀) + 오류 수정판

## v2.7
- GitHub에 올리면 APK를 자동 빌드해 Releases 페이지에 게시 (폰에서 바로 내려받기)

## v2.6
- 초보자용 설치 가이드(설치가이드_초보자용.md) 추가, GitHub 클라우드 APK 빌드 설정 추가
- 더 많은 Android Studio에서 열리도록 AGP 8.7.3 / compileSdk 35로 조정

## 새 기능
- v2.5: ▼演出カスタム(연출 커스텀) 탭 추가 — 通常時先役物落下カスタム(OFF/40%/90%/EX), 先読みカスタム 등, 용어 사전 확장
- 하단 4번째 탭 **팁 사전**: 에반게리온 17 「ゲーム性カスタマイズ ▼通常時」 메뉴 일본어·한국어 대조
  (先告知カスタム / 先読み / レバブルアップ / インパクトフラッシュアップ / エアーカスタム)
- **용어 사전 매칭**: 팁 번역 탭에서 OCR 결과에 사전 용어가 있으면 정확한 번역을 먼저 표시 (ML Kit 번역은 보조)
- 항목별로 ‘화면 문구 기준’ / ‘추정’ 표시

## 오류 수정
1. DownloadConditions import 경로 오류 수정 (com.google.mlkit.common.model)
2. @ExperimentalGetImage opt-in 추가
3. catch(_:Exception) 구문 제거 (Kotlin 버전별 컴파일 오류 방지)
4. INTERNET/ACCESS_NETWORK_STATE 권한 명시
5. 의존성 버전을 확인된 버전으로 조정 (CameraX 1.4.2, ML Kit OCR 16.0.1)
6. recognizer/translator/executor/카메라를 DisposableEffect로 정리 (누수 방지)
7. 번역: 같은 결과가 2회 연속일 때만 호출 + 캐시 + 오래된 응답 무시 + 실패 시 재시도
8. 빈 결과가 인식 텍스트를 덮어쓰던 깜빡임 제거
9. 분석 해상도 1080p, OCR 450ms 간격 제한
10. 전각 숫자 처리(NFKC), 전일 줄과 당일 줄 분리
11. 입력 폼 rememberSaveable (회전 시 유지)
12. 팁 모드에서는 숫자 자동 추출 중단
13. 순위 기준을 ‘1,000회전당 당첨 빈도’로 변경, 투입 금액은 선택 입력, 저장 시각 기록

## 실행
Android Studio(최신)에서 폴더를 열고 Gradle Sync → API 26+ 기기에서 실행.
Gradle 8.9 이상(Android Studio 2024.2.1 이상)이 필요합니다. 최초 1회 Wi-Fi에서 번역 모델이 내려받아집니다.
