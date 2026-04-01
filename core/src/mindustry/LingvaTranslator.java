package mindustry;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LingvaTranslator {

    private static final String BASE_URL = "https://lingva.ml/api/v1";

    /**
     * 非同步翻譯 function
     * @param text   要翻譯的文字
     * @param source 來源語言 (如 "en", "auto")
     * @param target 目標語言 (如 "zh-TW")
     * @return CompletableFuture<String> 翻譯結果
     */
    public static CompletableFuture<String> translateAsync(String text, String source, String target) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. URL 編碼處理
                // 重點修正：URLEncoder 會把空格變成 +，但在 API 的路徑中應為 %20
                String encodedText = URLEncoder.encode(text, "UTF-8").replace("+", "%20");
                String urlString = String.format("%s/%s/%s/%s", BASE_URL, source, target, encodedText);
                
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                // 2. 讀取回應
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    // 重點修正：明確指定使用 UTF-8 讀取，防止中文亂碼
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    // 3. 解析內容
                    return parseTranslation(response.toString());
                } else {
                    throw new RuntimeException("HTTP Error: " + responseCode);
                }
            } catch (Exception e) {
                throw new RuntimeException("翻譯請求失敗: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 解析 JSON 中的 translation 欄位並處理轉義字元
     */
    private static String parseTranslation(String json) {
        Pattern pattern = Pattern.compile("\"translation\":\"(.*?)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            String result = matcher.group(1);
            // 處理 JSON 返回的 Unicode 編碼 (如 \u4f60 -> 你)
            return unescapeJson(result);
        }
        return json;
    }

    /**
     * 簡單處理 JSON 轉義字元的方法
     * 處理包含 \\uXXXX, \n, \", \\ 等
     */
    private static String unescapeJson(String input) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch == '\\' && i + 1 < input.length()) {
                char next = input.charAt(i + 1);
                if (next == 'u' && i + 5 < input.length()) {
                    // 解析 \\uXXXX
                    String code = input.substring(i + 2, i + 6);
                    sb.append((char) Integer.parseInt(code, 16));
                    i += 5;
                } else {
                    switch (next) {
                        case 'n': sb.append('\n'); break;
                        case 't': sb.append('\t'); break;
                        case '\"': sb.append('\"'); break;
                        case '\\': sb.append('\\'); break;
                        default: sb.append(next);
                    }
                    i++;
                }
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}