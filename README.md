# Goclean with Java

[CSE2107-003] Final Exam Project

## 개발 환경

- Language: Java 21
- UI: Java Swing
- OS: Windows 11

## 구현 기능

| 기능           | 상태 |
|--------------|--|
| 서비스 관리       | ✅ |
| 프로세스 관리      | ✅ |
| 시작 프로그램 관리   | ✅ |
| 작업 스케줄러 관리   | - |
| 하드디스크 상태 점검  | - |
| 하드디스크 사용시간   | - |
| 시스템 정보       | ✅ |
| 동영상 파일 찾기    | ✅ |
| 컴퓨터 사용시간 체크  | ✅ |
| 블루스크린        | ✅ |
| 파일 강제 삭제     | ✅ |
| 개인정보 삭제      | ✅ |
| 프로그램 삭제      | - |
| 종료 타이머       | ✅ |
| DNS 변조 체크    | ✅ |
| CPU/그래픽카드 온도 | ❌ |

## 기능별 사용 명령어

<details>
<summary>서비스 관리</summary>

| 항목 | 명령어 |
|---|---|
| 서비스 목록 조회 | `Get-CimInstance Win32_Service \| Where-Object { $p = ($_.PathName -replace '"','').Trim(); ($p -notlike 'C:\Windows*') -and ($p -notlike '%SystemRoot%*') } \| Sort-Object DisplayName` |
| 서비스 실행 | `Start-Service -Name '서비스명'` |
| 서비스 중지 | `Stop-Service -Name '서비스명' -Force` |

- `C:\Windows` 및 `%SystemRoot%` 경로에 있는 윈도우 내장 서비스는 목록에서 제외
- 서비스 목록을 백그라운드(`SwingWorker`)에서 비동기로 로드
- 서비스명 또는 설명을 기준으로 실시간 검색 (`DocumentListener`)
- 서비스 실행/중지는 관리자 권한이 필요하며, 권한 없이 시도하면 경고 팝업 표시
- 실행/중지 완료 후 목록 자동 새로고침

</details>

<details>
<summary>프로세스 관리</summary>

| 항목 | 명령어 |
|---|---|
| CPU/메모리/프로세스 수 조회 | `(Get-CimInstance Win32_PerfFormattedData_PerfOS_Processor \| Where-Object {$_.Name -eq '_Total'}).PercentProcessorTime` / `Get-CimInstance Win32_OperatingSystem` / `(Get-Process).Count` |
| 프로세스 종료 | `Get-Process \| Where-Object { $_.ProcessName -notin @(화이트리스트) } \| ForEach-Object { Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue }` |

- CPU 사용률, 실제 메모리 사용률, 실행 중인 프로세스 수를 1초마다 갱신 (`javax.swing.Timer` + `SwingWorker`)
- "윈도우 프로세스 초기화" 버튼 클릭 시 확인 팝업 후, 기본 프로세스/백신/메신저를 제외한 모든 프로세스를 강제 종료
- 본인 프로세스(자바 앱)와 명령어를 실행 중인 `powershell` 자신은 항상 제외
- 종료 후 종료된 프로세스 수와 소요 시간을 로그에 표시
- "예외처리 방법" 버튼을 누르면 종료 대상에서 제외되는 기본/예외 프로세스 목록을 확인 가능

</details>

<details>
<summary>시작 프로그램 관리</summary>

| 항목 | 명령어 |
|---|---|
| 시작 프로그램 목록 조회 | `Get-CimInstance Win32_StartupCommand \| Sort-Object Name` |
| 시작 프로그램 삭제 (HKLM) | `Remove-ItemProperty -Path 'HKLM:\SOFTWARE\...\Run' -Name '프로그램명'` |
| 시작 프로그램 삭제 (HKCU) | `Remove-ItemProperty -Path 'HKCU:\SOFTWARE\...\Run' -Name '프로그램명'` |
| 폴더 열기 | `explorer.exe /select,"실행파일경로"` |

- 부팅 시 자동 실행되는 프로그램 목록을 백그라운드(`SwingWorker`)에서 비동기로 로드
- `Win32_StartupCommand`는 HKLM/HKCU/WOW6432Node 등 여러 위치를 동시에 조회하므로, 이름 기준으로 중복 제거(`HashSet`) 후 표시
- "열기" 버튼 클릭 시 해당 프로그램의 실행 파일이 있는 폴더를 탐색기로 열고 파일을 선택 상태로 표시 (`/select,` 옵션)
- `%windir%`, `%APPDATA%` 등 환경변수가 포함된 경로는 자동으로 실제 경로로 치환 후 탐색기 실행
- 체크박스로 항목 선택 후 삭제 시 레지스트리에서 해당 항목만 제거 (프로그램 자체는 삭제되지 않음)
- `Location` 값을 그대로 변환하여 정확한 레지스트리 경로(WOW6432Node 포함)로 삭제 처리
- 삭제 완료 후 목록 자동 갱신

</details>

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
<summary>동영상 파일 찾기</summary>

| 항목 | 명령어 |
|---|---|
| 폴더 열기 (파일 선택) | `explorer.exe /select,"파일경로"` |

- "찾을 파일" 콤보박스에서 동영상/음악(2MB 이상)/대용량(50MB 이상)/엑셀/파워포인트/Ms워드/한글/Pdf/Psd/Zip 중 선택
- "드라이브" 콤보박스에서 검색할 드라이브 선택 (전체 드라이브 선택 시 모든 드라이브 탐색)
- 선택한 조건에 맞는 파일을 백그라운드(`SwingWorker`)에서 디렉토리 재귀 탐색하며 찾는 즉시 테이블에 추가 (경로, 파일명, 폴더 열기, 크기, 만든 날짜)
- 접근 권한이 없는 폴더는 건너뜀
- 검색 중에는 로그에 "검색 중." → "검색 중.." → "검색 중..."처럼 점이 늘어나는 애니메이션 표시, 완료 시 발견 개수 표시
- "폴더 열기" 버튼을 누르면 해당 파일이 있는 폴더를 탐색기로 열고 파일을 선택 상태로 표시
- 테이블은 자체 스크롤만 사용하며, 한 화면 안에 모든 UI가 들어오도록 구성

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
<summary>블루스크린</summary>

| 항목 | 명령어 |
|---|---|
| 블루스크린(BSOD) 기록 조회 | `Get-WinEvent -FilterHashtable @{LogName='System'; ProviderName='Microsoft-Windows-WER-SystemErrorReporting'; Id=1001; StartTime=$start}` |

- 최근 3개월치 블루스크린 발생 기록을 내림차순으로 조회
- 이벤트 메시지에서 정규식(`0x[0-9a-fA-F]{8}`)으로 버그 체크 코드를 추출
- 추출한 코드를 미리 정의된 표(버그 체크 문자열, 추정 원인)와 매칭하여 테이블에 표시 (발생 시간, 버그 체크 문자열, 버그 코드, 원인(추정))
- 매칭되는 코드가 없으면 "알 수 없음"으로 표시
- 하단에 블루스크린 발생 횟수 표시

</details>

<details>
<summary>파일 강제 삭제</summary>

| 항목 | 명령어 |
|---|---|
| 파일 강제 삭제 | `Remove-Item -LiteralPath '경로' -Force -Recurse -ErrorAction SilentlyContinue` |

- 파일 탐색기(`JFileChooser`)에서 다중 선택한 파일을 목록에 추가
- 체크된 항목만 백그라운드(`SwingWorker`)에서 순차 강제 삭제
- 삭제 후 파일 존재 여부를 다시 확인하여 성공/실패를 로그에 출력, 성공한 항목은 목록에서 제거

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

<details>
<summary>종료 타이머</summary>

| 항목 | 명령어 |
|---|---|
| 컴퓨터 종료 | `shutdown /s /t 0` |

- 현재시간을 1초마다 갱신하여 표시
- "시간선택" 드롭다운에서 5분후/10분후/30분후/1시간후를 선택하면 현재시간 기준으로 시간이 자동 계산되어 스피너에 반영, 직접 시간을 지정할 수도 있음
- 지정한 시간이 되면 "메모글 띄우기" 또는 "컴퓨터 종료하기" 중 선택한 동작을 수행
- 지정 시간이 현재시간보다 이전이면 다음날 같은 시간으로 예약
- 1초마다 현재시간과 목표시간을 비교하는 방식(`javax.swing.Timer`)으로 동작

</details>

<details>
<summary>DNS 변조 체크</summary>

| 항목 | 명령어 |
|---|---|
| 기본/보조 DNS 조회 | `(Get-DnsClientServerAddress -AddressFamily IPv4 \| Where-Object { $_.ServerAddresses.Count -gt 0 } \| Select-Object -First 1).ServerAddresses` |
| 주요 사이트 IP 조회 (현재 DNS) | `(Resolve-DnsName 도메인 -Type A -ErrorAction SilentlyContinue).IPAddress` |
| 주요 사이트 IP 조회 (공용 DNS) | `(Resolve-DnsName 도메인 -Type A -Server 8.8.8.8 -ErrorAction SilentlyContinue).IPAddress` |

- 국민은행(`www.kbstar.com`), 우리은행(`www.wooribank.com`)의 IP를 현재 DNS와 공용 DNS(8.8.8.8) 양쪽에서 조회
- 두 결과에 겹치는 IP가 하나라도 있으면 정상연결, 없으면 변조 의심, 조회 실패 시 연결실패로 표시
- 접속 환경에 따라 정상이어도 변조 의심으로 표시될 수 있음을 화면에 안내 문구로 표시

</details>

## 브랜치 구조

- `main`: 완성된 버전만
- `dev`: 일반 개발
- `feature/기능명`: 기능 단위 작업
