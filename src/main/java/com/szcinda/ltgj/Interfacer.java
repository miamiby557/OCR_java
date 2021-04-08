package com.szcinda.ltgj;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class Interfacer {
    // 百度
    private static String AK = "31nciYSFGPlKetnABdsGZnQk";
    private static String SK = "C1IbpC8WRLcqFX3fGz7VWv73AZfRhEAf";

    // 合合
    private static String APPKEY = "ai_demo_text_recog_ch_en_coordinate";
    private static String APPSECRET = "ai_demo_text_recog_ch_en_coordinate";

    private static char[] cnNum = new char[]{'零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖'};
    private static char[] unitArr = new char[]{'厘', '分', '角', '圆', '拾', '佰', '仟', '万', '亿'};


    /**
     * 中文金额转数字
     *
     * @param chineseNumber 中文金额
     * @return
     */
    public static BigDecimal chinese2Number(String chineseNumber) {
        BigDecimal result = new BigDecimal(0);
        int lastUnitIndex = 0, num = 0;
        chineseNumber = chineseNumber.replace("元", "圆")
                .replace("整", "")
                .replace("人民币", "")
                .replaceAll("参", "叁");
        for (int i = 0; i < chineseNumber.length(); i++) {
            boolean isUnit = true;
            char c = chineseNumber.charAt(i);
            for (int j = 0; j < cnNum.length; j++) {
                // 是数字
                if (c == cnNum[j]) {
                    // 数字值 = 索引
                    num = j;
                    isUnit = false;
                    break;
                }
            }
            if (isUnit) {
                // 第一个就是单位，如：拾伍万圆整
                if (i == 0) {
                    num = 1;
                }
                int unitIndex = getUnitIndex(c);
                BigDecimal unit = getUnit(c);
                if (unitIndex > lastUnitIndex) {
                    result = result.add(new BigDecimal(num)).multiply(unit);
                } else {
                    result = result.add(new BigDecimal(num).multiply(unit));
                }
                lastUnitIndex = unitIndex;
                num = 0;
            }
        }
        return result.setScale(2, BigDecimal.ROUND_DOWN);
    }

    private static int getUnitIndex(char c) {
        for (int j = 0; j < unitArr.length; j++) {
            if (c == unitArr[j]) {
                return j;
            }
        }
        return 0;
    }

    private static BigDecimal getUnit(char c) {
        double num = 0;
        int unitIndex = getUnitIndex(c);
        switch (unitIndex) {
            // '厘', '分', '角', '圆', '拾', '佰', '仟', '万', '亿'
            case 4:
                num = 10;
                break;
            case 5:
                num = 100;
                break;
            case 6:
                num = 1000;
                break;
            case 7:
                num = 10000;
                break;
            case 8:
                num = 100000000;
                break;
            case 3:
                num = 1;
                break;
            case 2:
                num = 0.1;
                break;
            case 1:
                num = 0.01;
                break;
            case 0:
                num = 0.001;
                break;
            default:
                break;
        }
        return new BigDecimal(num);
    }

    private static String heheOCR(String filePath)throws Exception {
        String resultText = "0#0#0";
        String url = "https://ocr-api.ccint.com/cci_ai/service/v1/text_recog_ch_en_coordinate";
        BufferedReader in = null;
        DataOutputStream out = null;
        StringBuilder result = new StringBuilder();
        try {
            byte[] imgData = readfile(filePath);
//            String imgStr = Base64Util.encode(imgData);
            URL realUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) realUrl.openConnection();
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestProperty("app-key", APPKEY);
            conn.setRequestProperty("app-secret", APPSECRET);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST"); // 设置请求方式
            out = new DataOutputStream(conn.getOutputStream());
            out.write(imgData);
            out.flush();
            out.close();
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(),"UTF-8"));
            String name = "";
            String money = "1";// 默认
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
            System.out.println(result.toString());
            JSONObject jsonObject = new JSONObject(result.toString());
            JSONObject body = jsonObject.getJSONObject("result");
            String wholeText = body.getString("whole_text");
            System.out.println("合合返回：" + wholeText);
            String[] strings = wholeText.split("\n");
            for (String text : strings) {
//                System.out.println(text);
                if (text.contains("收款人户名") || text.contains("款人户名") || text.contains("收款人户")) {
                    name = text.split(":")[1].trim();
                    break;
                }else if(text.contains("原收款户名:") || text.contains("收款户名:")){
                    String[] array = text.split(":");
                    name = array[array.length-1].trim();
                    break;
                }else if(text.contains("收款人名称:")){
                    String[] array = text.split(":");
                    name = array[array.length-1].trim();
                }
                /*if(text.contains("(小写)")){
                    money = text.split(":")[1].replace("CNY","")
                            .replace(",","").trim();
                }*/
            }
//            if (name.length() > 0 && money.length() > 0) {
            if (name.length() > 0) {
                resultText = "1#" + name + "#" + money;
            }
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！" + e);
            throw e;
//            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        System.out.println(resultText);
        return resultText;
    }

    public static byte[] readfile(String path) throws Exception {
//        path = URLDecoder.decode(path, "UTF-8");
        byte[] buffer = null;
        File file = new File(path);
        FileInputStream fis;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        fis = new FileInputStream(file);
        byte[] b = new byte[1024];
        int n;
        while ((n = fis.read(b)) != -1) {
            bos.write(b, 0, n);
        }
        fis.close();
        bos.close();
        buffer = bos.toByteArray();
        return buffer;
    }

    private static String baiduOCR(String filePath) {
        String resultText = "0#0#0";
        try {
            String otherHost = "https://aip.baidubce.com/rest/2.0/ocr/v1/general";
            byte[] imgData = FileUtil.readFileByBytes(filePath);
            String imgStr = Base64Util.encode(imgData);
            String params = URLEncoder.encode("image", "UTF-8") + "=" + URLEncoder.encode(imgStr, "UTF-8");
            String accessToken = getAuth(AK, SK);
            String result = HttpUtil.post(otherHost, accessToken, params);
            System.out.println(result);
            JSONObject jsonObject = new JSONObject(result);
            JSONArray jsonArray = jsonObject.getJSONArray("words_result");
            String name = "";
            String money = "";
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jObject = jsonArray.getJSONObject(i);
                String words = jObject.getString("words");
                if (words.contains("收款人户名") || words.contains("款人户名")) {
                    name = words.split(":")[1].trim();
                }else if(words.contains("原收款户名:") || words.contains("收款户名:")){
                    String[] array = words.split(":");
                    name = array[array.length-1].trim();
                }else if(words.contains("收款人名称:")){
                    String[] array = words.split(":");
                    name = array[array.length-1].trim();
                }
                if (words.contains("金额(大写)") || words.contains("额(大写)") || words.contains("(大写)")) {
                    money = chinese2Number(words.split(":")[1]).toString().trim();
                }
            }
            if (name.length() > 0 && money.length() > 0) {
                resultText = "1#" + name + "#" + money;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultText;
    }

    // 注意文件路径不能存在中文
    public static String getNameAndMoney(String filePath) throws Exception {
//       String  resultText = baiduOCR(filePath);
        String resultText = heheOCR(filePath);
        return URLEncoder.encode(resultText, "UTF-8");
    }

    public static String getAuth(String ak, String sk) {
        // 获取token地址
        String authHost = "https://aip.baidubce.com/oauth/2.0/token?";
        String getAccessTokenUrl = authHost
                // 1. grant_type为固定参数
                + "grant_type=client_credentials"
                // 2. 官网获取的 API Key
                + "&client_id=" + ak
                // 3. 官网获取的 Secret Key
                + "&client_secret=" + sk;
        try {
            URL realUrl = new URL(getAccessTokenUrl);
            // 打开和URL之间的连接
            HttpURLConnection connection = (HttpURLConnection) realUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            // 定义 BufferedReader输入流来读取URL的响应
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
            JSONObject jsonObject = new JSONObject(result.toString());
            return jsonObject.getString("access_token");
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        String filePath = "C:\\Users\\cindata-hrs\\Desktop\\orderText.png";
        System.out.println("getNameAndMoney:" + URLDecoder.decode(getNameAndMoney(filePath), "utf-8"));
//        String text = "人民币叁佰玖拾伍元整";
//        System.out.println(chinese2Number(text));
//        heheOCR(filePath);
    }

}
