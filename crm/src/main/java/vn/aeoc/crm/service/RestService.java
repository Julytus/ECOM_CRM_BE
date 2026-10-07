package vn.aeoc.crm.service;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.stream.Collectors;

@Service
public class RestService {
    private RestTemplate restTemplate;

    // Định dạng đầu ra chuẩn
    private static final DateTimeFormatter OUTPUT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    // Các định dạng đầu vào có thể có
    private static final DateTimeFormatter[] INPUT_FORMATTERS = {
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,        // "2025-03-10T12:00+07:00"
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"),    // "2025-03-10 12:00:00.0"
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")   // "2025-03-10 12:00:00.123"
    };

    public RestService() {
        restTemplate = new RestTemplate();
        restTemplate.getMessageConverters()
                .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
    }

    /**
     * Gọi api chung
     *
     * @param url            Đường dẫn API
     * @param method         POST | GET | PUT | DELETE | ...
     * @param authentication
     * @param httpHeaders
     * @param jsonObject     Dữ liệu cần truyền đi dưới dạng JSON
     * @return
     */
    public String callApiFromPartner(String url, String method,
                                     String authentication, String defaultJwt,
                                     String httpHeaders, JSONObject jsonObject) {
        System.out.println("from callApiFromPartner");
        String requestJson = "";
        JSONObject standardizedJson = standardizeDateTimes(jsonObject);
        if (jsonObject != null) {
            requestJson = standardizedJson.toString();
            System.out.println(requestJson);
        }
        System.out.println(jsonObject);

        //Hỗ trợ url tương đối => Lúc đó lấy theo mặc định
        String fullUrl = getFullUrl(url);
        HttpHeaders headers = new HttpHeaders();

        //1. Có thiết lập cơ chế bảo mật
        if (!StringUtils.isBlank(authentication)) {
            JSONObject jsonAuthentication = new JSONObject(authentication);
            String type = jsonAuthentication.getString("type");
            if (type.startsWith("JWT")) {
                String jwt = jsonAuthentication.getJSONObject("config").getString("token");
                headers.set("Authorization", String.format("Bearer %s", jwt));
            }
        } else {
            //Không truyền thì lấy mặc định của hệ thống hiện hữu
            if (!StringUtils.isBlank(defaultJwt)) {
                headers.set("Authorization", String.format("Bearer %s", defaultJwt));
            }
        }

        if (!StringUtils.isBlank(httpHeaders)) {
            JSONArray jsonHeaders = new JSONArray(httpHeaders);
            for (int i = 0; i < jsonHeaders.length(); i++) {
                JSONObject jsonHeader = jsonHeaders.getJSONObject(i);

                // Access data inside each JSONObject
                String key = jsonHeader.getString("key");
                String value = jsonHeader.getString("value");

                if (!StringUtils.isBlank(key)) {
                    headers.set(key, value);
                }
            }
        }

        headers.setContentType(MediaType.APPLICATION_JSON);

        // 3. Kiểm tra phương thức và xử lý logic tương ứng
        HttpEntity<String> entity = new HttpEntity<>(requestJson, headers); // Sử dụng cho mọi phương thức
        ResponseEntity<String> response;
        String curlCommand;

        switch (method.toUpperCase()) {
            case "POST":
                System.out.println("Request URL: " + fullUrl);
                System.out.println("Request Headers: " + headers);

                curlCommand = generateCurl(fullUrl, method.toUpperCase(), entity);
                System.out.println("CURL Command:\n" + curlCommand);

                response = restTemplate.exchange(fullUrl, HttpMethod.POST, entity, String.class);
                break;
            case "GET":
                // Tạo query string từ jsonObject
                UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(fullUrl);

                // Thêm các tham số từ jsonObject vào URL nếu jsonObject không rỗng
                if (standardizedJson != null) {
                    standardizedJson.keySet().forEach(key -> {
                        Object valueObj = standardizedJson.get(key);  // lấy ra object gốc
                        if (valueObj != null) {
                            builder.queryParam(key, valueObj.toString());  // ép về string an toàn
                        }
                    });

                }

                // Xây dựng URL hoàn chỉnh với query string
                String finalUrl = builder.toUriString();
                System.out.println("Final GET URL: " + finalUrl);
                curlCommand = generateCurl(finalUrl, method.toUpperCase(), entity);
                System.out.println("CURL Command:\n" + curlCommand);
                // Gửi request GET với URL đã bao gồm query string
                response = restTemplate.exchange(finalUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);
                break;

            case "PUT":
                response = restTemplate.exchange(fullUrl, HttpMethod.PUT, entity, String.class);
                break;

            case "DELETE":
                response = restTemplate.exchange(fullUrl, HttpMethod.DELETE, entity, String.class); // Body có thể được dùng nếu cần
                break;

            case "PATCH":
                response = restTemplate.exchange(fullUrl, HttpMethod.PATCH, entity, String.class);
                break;

            default:
                throw new UnsupportedOperationException("Unsupported method: " + method);
        }

        System.out.println(method.toUpperCase() + " Response: " + response.getBody() + " " + response.toString());
        System.out.printf("url =>" + fullUrl);
        return response.getBody();
    }

    public static String generateCurl(String url, String method, HttpEntity<String> entity) {
        StringBuilder curl = new StringBuilder("curl -X " + method + " '" + url + "'");

        // Thêm headers
        HttpHeaders headers = entity.getHeaders();
        for (String headerName : headers.keySet()) {
            String headerValue = headers.get(headerName).stream().collect(Collectors.joining("; "));
            curl.append(" -H '").append(headerName).append(": ").append(headerValue).append("'");
        }

        // Thêm body nếu có
        String body = entity.getBody();
        if (body != null && !body.isEmpty()) {
            curl.append(" -d '").append(body.replace("'", "\\'")).append("'");
        }

        return curl.toString();
    }

    public static JSONObject standardizeDateTimes(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        // Tạo bản sao để không thay đổi object gốc
        JSONObject result = new JSONObject(jsonObject.toString());
        processJSONObject(result);
        return result;
    }

    private static void processJSONArray(JSONArray jsonArray) {
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                Object value = jsonArray.get(i);

                if (value instanceof String) {
                    String stringValue = (String) value;
                    String standardizedDateTime = tryParseAndConvert(stringValue);
                    if (standardizedDateTime != null) {
                        jsonArray.put(i, standardizedDateTime);
                    }
                } else if (value instanceof JSONObject) {
                    processJSONObject((JSONObject) value);
                } else if (value instanceof JSONArray) {
                    processJSONArray((JSONArray) value);
                }
            } catch (Exception e) {
                continue;
            }
        }
    }

    private static void processJSONObject(JSONObject jsonObject) {
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                Object value = jsonObject.get(key);

                if (value instanceof String) {
                    String stringValue = (String) value;
                    String standardizedDateTime = tryParseAndConvert(stringValue);
                    if (standardizedDateTime != null) {
                        jsonObject.put(key, standardizedDateTime);
                    }
                } else if (value instanceof JSONObject) {
                    // Đệ quy xử lý JSONObject lồng nhau
                    processJSONObject((JSONObject) value);
                } else if (value instanceof JSONArray) {
                    // Xử lý JSONArray nếu có
                    processJSONArray((JSONArray) value);
                }
            } catch (Exception e) {
                continue;
            }
        }
    }


    private static String tryParseAndConvert(String value) {
        for (DateTimeFormatter formatter : INPUT_FORMATTERS) {
            try {
                OffsetDateTime dateTime;

                if (formatter == DateTimeFormatter.ISO_OFFSET_DATE_TIME) {
                    dateTime = OffsetDateTime.parse(value, formatter);
                } else {
                    // Đối với các định dạng không có timezone
                    LocalDateTime localDateTime = LocalDateTime.parse(value, formatter);
                    dateTime = localDateTime.atOffset(ZoneOffset.UTC);
                }

                return dateTime.format(OUTPUT_FORMATTER.withZone(ZoneOffset.UTC));
            } catch (DateTimeParseException e) {
                continue;
            }
        }
        return null;
    }

    private String getFullUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return "";
        }

        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }

        if (!url.startsWith("/")) {
            url = "/" + url;
        }

        return String.format("%s%s", "http://localhost:9100", url);
    }
}
