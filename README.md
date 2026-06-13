# Goclean with Java

[CSE2107-003] Final Exam Project

## 개발 환경

- Language: Java 21
- UI: Java Swing
- OS: Windows 11

## 구현 기능

| 기능 | 상태 |
|---|--|
| 서비스 관리 | - |
| 시작 프로그램 관리 | - |
| 작업 스케줄러 관리 | - |
| 하드디스크 상태 점검 | - |
| 하드디스크 사용시간 | - |
| 시스템 정보 | ✅ |
| 동영상 파일 찾기 | - |
| 컴퓨터 사용시간 체크 | ✅ |
| 블루스크린 | - |
| 파일 강제 삭제 | - |
| 개인정보 삭제 | ✅ |
| 프로그램 삭제 | - |
| 종료 타이머 | - |
| DNS 변조 체크 | - |
| DNS 변조 초기화 | - |
| CPU/그래픽카드 온도 | ❌ |

## 기능별 사용 명령어

<details>
<summary>시스템 정보</summary>

| 항목 | 명령어 |
|---|---|
| CPU | `(Get-WmiObject Win32_Processor).Name` |
| 그래픽카드 | `(Get-CimInstance Win32_VideoController).Name` |
| 사운드카드 | `(Get-WmiObject Win32_SoundDevice \| Select-Object -First 1).Name` |
| 윈도우 설치일자 | `(Get-WmiObject Win32_OperatingSystem).ConvertToDateTime(...).ToString('yyyy-MM-dd')` |
| 전체 메모리 | `(Get-WmiObject Win32_OperatingSystem).TotalVisibleMemorySize` |
| 사용가능 메모리 | `(Get-WmiObject Win32_OperatingSystem).FreePhysicalMemory` |
| 사설 IP | `ipconfig \| Select-String 'IPv4'` |
| 맥주소 | `(Get-NetAdapter \| Where-Object { $_.Status -eq 'Up' }).MacAddress` |
| 드라이브 목록 | `(Get-PSDrive -PSProvider FileSystem).Name` |
| 드라이브 용량 | `(Get-PSDrive 드라이브명).Used / .Free` |

</details>

<details>
<summary>컴퓨터 사용시간</summary>

| 항목 | 명령어 |
|---|---|
| 켜진/꺼진 시간 로그 | `Get-WinEvent -LogName System \| Where-Object { $_.Id -eq 6005 -or $_.Id -eq 6006 }` |

- `EventID 6005`: 시스템 시작 (켜진시간)
- `EventID 6006`: 시스템 종료 (꺼진시간)
- 최근 30일치만 조회, 내림차순 정렬
- 자정을 넘긴 세션은 날짜 기준으로 분할하여 집계

</details>

<details>
<summary>개인정보 삭제</summary>

| 항목 | 명령어 |
|---|---|
| 열어본 페이지 목록 삭제 | `Remove-Item -Path 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Explorer\TypedPaths\*' -Force` |
| 자동완성 패스워드 삭제 | `Remove-Item -Path 'HKCU:\Software\Microsoft\Internet Explorer\IntelliForms\Storage2' -Recurse -Force` |
| 폼 자동완성 정보 삭제 | `Remove-Item -Path 'HKCU:\Software\Microsoft\Internet Explorer\IntelliForms\Storage1' -Recurse -Force` |
| 워드패드 열기 목록 삭제 | `Remove-Item -Path 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Applets\Wordpad\Recent File List' -Recurse -Force` |
| URL 히스토리 삭제 | `Remove-Item -Path 'HKCU:\Software\Microsoft\Internet Explorer\TypedURLs(Time)' -Recurse -Force` |
| 실행 목록 삭제 | `Remove-Item -Path 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Explorer\RunMRU\*' -Force` |
| 최근 열어본 문서 목록 삭제 | `Remove-Item -Path "$env:APPDATA\Microsoft\Windows\Recent\*" -Recurse -Force` / `Remove-Item -Path 'HKCU:\...\Explorer\RecentDocs\*' -Recurse -Force` |
| 시스템 부팅 속도 최적화 | `Remove-Item -Path 'C:\Windows\Prefetch\*.pf' -Force` |
| 그림판 기록 삭제 | `Remove-Item -Path 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Applets\Paint\Recent File List' -Recurse -Force` |
| 오피스 문서 기록 삭제 | `Get-ChildItem -Path 'HKCU:\Software\Microsoft\Office' -Recurse \| Where-Object { $_.PSChildName -eq 'File MRU' -or $_.PSChildName -eq 'Place MRU' } \| ForEach-Object { Remove-Item $_.PSPath -Recurse -Force }` |

- 선택한 항목만 백그라운드(`SwingWorker`)에서 순차 삭제 후 로그에 결과 출력
- 모든 명령어에 `-ErrorAction SilentlyContinue` 적용 (해당 항목이 없어도 오류 없이 진행)

</details>

## 브랜치 구조

- `main`: 완성된 버전만
- `dev`: 일반 개발
- `feature/기능명`: 기능 단위 작업
