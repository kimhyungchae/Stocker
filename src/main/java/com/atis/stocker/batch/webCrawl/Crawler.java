package com.atis.stocker.batch.webCrawl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jsoup.Jsoup.connect;

public class Crawler {
    public static void main(String[] args) throws IOException {
        Logger logger = Logger.getLogger(Crawler2.class.getName());
        final String DOMAIN = "http://finance.naver.com";
        List<String> al = new ArrayList<>();
        List<Map> al2 = new ArrayList<>();
        String targetUrl = DOMAIN+"/sise/sise_market_sum.nhn?sosok=1";
        Document doc = connect(targetUrl).get();
        Elements link = doc.select("td.pgRR a");
        String linkHref = link.attr("href");
        int totalPagCnt = Integer.parseInt(linkHref.substring(linkHref.length()-2,linkHref.length()));
        for (int i = 0; i < totalPagCnt; i++) {
            String targetUrl2 = DOMAIN+"/sise/sise_market_sum.nhn?sosok=1&page="+i;
            Document doc2 = connect(targetUrl2).get();
            Elements links = doc2.select("a.tltle");
            for (Element link2 : links) {
                al.add(link2.attr("href"));
            }
        }
        for (String s : al) {
            Document doc3 = connect(DOMAIN+s).get();

            String HNAME = doc3.select("#middle > dl> dd").eq(1).text();
            HNAME = HNAME.replace(" ", "");
            HNAME = HNAME.replace("종목명", "");

            String SHCODE = doc3.select("#middle > dl> dd").eq(2).text();
            SHCODE = SHCODE.replace(" ", "");
            SHCODE = SHCODE.replace("종목코드", "");
            SHCODE = SHCODE.replace("코스닥", "");
            SHCODE = SHCODE.replace("코스피", "");

            String CPRICE = doc3.select("#middle > dl> dd").eq(3).text();
            CPRICE = CPRICE.replace("현재가", "");
            CPRICE = CPRICE.substring(CPRICE.indexOf(" ")+1,CPRICE.indexOf("전")-1);

            String RO = doc3.select(".tb_type1_ifrs  tr").eq(4).select("td").eq(2).text();
            String ROP = doc3.select(".tb_type1_ifrs  tr").eq(6).select("td").eq(2).text();
            String NPM = doc3.select(".tb_type1_ifrs  tr").eq(7).select("td").eq(2).text();
            String DR = doc3.select(".tb_type1_ifrs  tr").eq(9).select("td").eq(2).text();
            String RR = doc3.select(".tb_type1_ifrs  tr").eq(11).select("td").eq(2).text();

            String ABSCNT = doc3.select("#tab_con1 > div > table > tbody > tr").eq(2).select("td").text();

            String YSALES = doc3.select(".tb_type1_ifrs  tr").eq(3).select("td").eq(2).text();
            String AVLS = doc3.select("#tab_con1 > div > table > tbody > tr").eq(0).select("td").text();
            AVLS = AVLS.replace("억원","");

            Map<String,String> m = new HashMap<>();
            m.put("코드",SHCODE);
            m.put("코드명",HNAME);
            m.put("현재가",CPRICE.replaceAll(",",""));
            m.put("PER",doc3.select("#_per").text().replaceAll(",",""));
            m.put("PBR",doc3.select("#_pbr").text().replaceAll(",",""));
            m.put("영업이익",RO.replaceAll(",","")+"00000000");
            m.put("영업이익율",ROP.replaceAll(",",""));
            m.put("순이익율",NPM.replaceAll(",",""));
            m.put("부채율",DR.replaceAll(",",""));
            m.put("유보율",RR.replaceAll(",",""));
            m.put("상장주식수",ABSCNT.replaceAll(",",""));
            m.put("연매출",YSALES.replaceAll(",","")+"00000000");
            m.put("시가총액",AVLS.replaceAll(",","")+"00000000");

            if(!m.get("코드").equals("")
                && !m.get("코드명").equals("")
                && !m.get("PER") .equals("")
                && !m.get("PBR").equals("")
                && !m.get("영업이익율").equals("")
                && !m.get("순이익율").equals("")
                && !m.get("부채율").equals("")
                && !m.get("유보율").equals("")
                && !m.get("상장주식수") .equals("")
                && !m.get("연매출").equals("")
                && !m.get("시가총액").equals("")) {

                if(Double.parseDouble(m.get("PER")) < 25 && Double.parseDouble(m.get("PER")) > 0 &&
                        Double.parseDouble(m.get("PBR")) < 2 && Double.parseDouble(m.get("PBR")) > 0 &&
                        Double.parseDouble(m.get("영업이익율")) > 1 &&
                        Double.parseDouble(m.get("순이익율")) > 1 &&
                        Double.parseDouble(m.get("부채율")) < 200 &&
                        Double.parseDouble(m.get("유보율")) > 800 &&
                        Double.parseDouble(m.get("상장주식수")) > 5000000 &&
                        (Double.parseDouble(m.get("연매출")) > Double.parseDouble(m.get("시가총액")))
                        ) {
                    RestTemplate restTemplate = new RestTemplate();
                    ResponseEntity<String> response =restTemplate.getForEntity("http://companyinfo.stock.naver.com/v1/company/cF3002.aspx?cmp_cd="+ m.get("코드") +"&frq=0&rpt=1&finGubun=MAIN&frqTyp=0", String.class);

                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(response.getBody());
                    JsonNode data = root.path("DATA");
                    String jsonInString = mapper.writeValueAsString(data);

                    JsonParser parser = new JsonParser();
                    JsonElement element = parser.parse(jsonInString);
                    JsonArray jArray = new JsonArray();
                    if(element.isJsonArray() == true) {
                        jArray = element.getAsJsonArray();
                    } else {
                        continue;
                    }

                    //유동자산
                    String DATA5 = String.valueOf(jArray.get(1).getAsJsonObject().get("DATA5"));

                    BigDecimal decimal3 = new BigDecimal("0");
                    if(!DATA5.equals("null")) {
                        BigDecimal decimal1 = new BigDecimal(String.format("%.2f", Float.parseFloat(DATA5)));
                        BigDecimal decimal2 = new BigDecimal("100000000");
                        decimal3 = decimal1.multiply(decimal2);
                    }

                    m.put("유동자산",String.valueOf(decimal3.toBigInteger()));

                    //투자자산
                    String DATA578 = String.valueOf(jArray.get(78).getAsJsonObject().get("DATA5"));

                    BigDecimal decimal6 = new BigDecimal("0");

                    if(!DATA578.equals("null")){
                        BigDecimal decimal4 = new BigDecimal(String.format("%.2f",Float.parseFloat(DATA578)));
                        BigDecimal decimal5 = new BigDecimal("100000000");
                        decimal6 = decimal4.multiply(decimal5);
                    }

                    m.put("투자자산",String.valueOf(decimal6.toBigInteger()));

                    //유동부채
                    String DATA5151 = String.valueOf(jArray.get(109).getAsJsonObject().get("DATA5"));

                    BigDecimal decimal9 = new BigDecimal("0");
                    if(!DATA5151.equals("null")) {
                        BigDecimal decimal7 = new BigDecimal(String.format("%.2f", Float.parseFloat(DATA5151)));
                        BigDecimal decimal8 = new BigDecimal("100000000");
                        decimal9 = decimal7.multiply(decimal8);
                    }

                    m.put("유동부채",String.valueOf(decimal9.toBigInteger()));

                    //비유동부채
                    String DATA152 = String.valueOf(jArray.get(151).getAsJsonObject().get("DATA5"));

                    BigDecimal decimal12 = new BigDecimal("0");
                    if(!DATA152.equals("null")) {
                        BigDecimal decimal10 = new BigDecimal(String.format("%.2f", Float.parseFloat(DATA152)));
                        BigDecimal decimal11 = new BigDecimal("100000000");
                        decimal12 = decimal10.multiply(decimal11);
                    }

                    m.put("비유동부채",String.valueOf(decimal12.toBigInteger()));

                    BigDecimal bigtotal1;
                    BigDecimal bigtotal2;
                    BigDecimal bigtotal3;

                    BigDecimal big100 = new BigDecimal(m.get("영업이익"));

                    BigDecimal big101 = new BigDecimal(m.get("유동자산"));
                    BigDecimal big102 = new BigDecimal(m.get("투자자산"));
                    BigDecimal big103 = new BigDecimal(m.get("유동부채"));
                    BigDecimal big104 = new BigDecimal(m.get("비유동부채"));
                    BigDecimal big105 = new BigDecimal(m.get("상장주식수"));

                    BigDecimal big106 = new BigDecimal("9");
                    BigDecimal big107 = new BigDecimal("1.2");

                    bigtotal1 = big100.multiply(big106);
                    bigtotal1 = bigtotal1.add(big101);
                    bigtotal1 = bigtotal1.add(big102);

                    bigtotal2 = big103.multiply(big107);
                    bigtotal2 = bigtotal2.add(big104);

                    bigtotal3 = bigtotal1.subtract(bigtotal2);
                    bigtotal3 = bigtotal3.divideToIntegralValue(big105);


                    m.put("적정주가",String.valueOf(bigtotal3.toBigInteger()));
                    al2.add(m);
                }
            }
        }

        for (Map m2: al2) {
            if(Integer.parseInt(String.valueOf(m2.get("현재가"))) < Integer.parseInt(String.valueOf(m2.get("적정주가")))){
                System.out.println(m2.get("코드명") + " | " + m2.get("현재가") + " | " + m2.get("적정주가") );
            }
        }
    }
}

