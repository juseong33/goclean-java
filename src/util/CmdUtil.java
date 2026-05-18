package util;

import java.io.*;
import java.util.*;

//Windows 명령어를 처리하는 유틸 (ProcessBuilder 이용)
public class CmdUtil {
    public static String run(String command) {
        StringBuilder sb = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            process.waitFor();
        } catch (Exception e) {
            sb.append("[오류] ").append(e.getMessage());
        }
        return sb.toString().trim();
    }

    // 명령어 실행 후 결과를 줄 단위 리스트로 반환 (빈 줄 제외)
    public static List<String> runLines(String command) {
        List<String> lines = new ArrayList<>();
        for (String line : run(command).split("\n")) {
            if (!line.trim().isEmpty()) {
                lines.add(line);
            }
        }
        return lines;
    }

    // 관리자 권한 여부 확인
    public static boolean isAdmin() {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "net session");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            new BufferedReader(new InputStreamReader(p.getInputStream())).lines().forEach(l -> {
            });
            p.waitFor();
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    // 바이트 -> 읽기 쉬운 단위로 변환 (예: 1.2 MB)
    public static String formatSize(long bytes) {
        if (bytes < 0) return "-";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}