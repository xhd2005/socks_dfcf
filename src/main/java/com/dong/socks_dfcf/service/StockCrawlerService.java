package com.dong.socks_dfcf.service;

import com.dong.socks_dfcf.mapper.StockDataMapper;
import com.dong.socks_dfcf.model.StockData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockCrawlerService {
    
    private final StockDataMapper stockDataMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 创建一个HttpClient实例用于所有请求
    private final CloseableHttpClient httpClient = createHttpClient();
    
    // 东方财富网股票数据接口基础URL (根据用户提供的URL修改)
    private static final String STOCK_API_BASE_URL = "https://push2.eastmoney.com/api/qt/clist/get?" +
            "np=1&fltt=1&invt=2&cb=jQuery37105849300946593018_1760442054336&fs=m:128+t:3,m:128+t:4,m:128+t:1,m:128+t:2&" +
            "fields=f12,f13,f14,f19,f1,f2,f4,f3,f152,f17,f18,f15,f16,f5,f6&fid=f3&pn={}&pz=20&po=1&dect=1&" +
            "ut=fa5fd1943c7b386f172d6893dbfba10b&wbp2u=|0|0|0|web&_=1760442054387";
    
    // 线程池配置
    private static final int THREAD_POOL_SIZE = 5;
    private static final int MAX_RETRIES = 3;
    
    /**
     * 创建配置好的HttpClient实例
     * @return 配置好的HttpClient
     */
    private CloseableHttpClient createHttpClient() {
        // 创建连接管理器
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(20);
        connectionManager.setDefaultMaxPerRoute(10);
        
        // 配置请求参数
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(10))
                .setResponseTimeout(Timeout.ofSeconds(15))
                .setConnectionRequestTimeout(Timeout.ofSeconds(5))
                .build();
        
        // 创建并配置HttpClient
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
    
    /**
     * 爬取股票数据（单页）
     */
    public void crawlStockData() {
        crawlStockDataByPage(1);
    }
    
    /**
     * 爬取所有股票数据（多页，多线程）
     */
    public void crawlAllStockData() {
        // 创建线程池
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        // 用于跟踪完成的任务数
        AtomicInteger completedTasks = new AtomicInteger(0);
        // 用于存储所有爬取到的数据
        List<StockData> allStockData = new CopyOnWriteArrayList<>();
        
        try {
            // 首先获取第一页数据以确定总页数
            String firstPageResponse = fetchPageDataWithRetry(1);
            int totalPages = getTotalPages(firstPageResponse);
            
            log.info("Total pages to crawl: {}", totalPages);
            
            // 创建计数信号量，限制并发数
            Semaphore semaphore = new Semaphore(THREAD_POOL_SIZE);
            // 创建CountDownLatch等待所有任务完成
            CountDownLatch latch = new CountDownLatch(totalPages);
            
            // 提交所有爬取任务到线程池
            for (int page = 1; page <= totalPages; page++) {
                final int currentPage = page;
                executorService.submit(() -> {
                    try {
                        semaphore.acquire(); // 获取信号量许可
                        log.info("Crawling page {}/{}", currentPage, totalPages);
                        
                        // 爬取页面数据
                        String response = fetchPageDataWithRetry(currentPage);
                        List<StockData> pageData = parseStockData(response);
                        allStockData.addAll(pageData);
                        
                        log.info("Successfully crawled page {}/{} with {} records", currentPage, totalPages, pageData.size());
                    } catch (Exception e) {
                        log.error("Error crawling page {}: {}", currentPage, e.getMessage(), e);
                    } finally {
                        semaphore.release(); // 释放信号量许可
                        latch.countDown(); // 计数器减1
                        completedTasks.incrementAndGet(); // 增加完成任务数
                    }
                });
            }
            
            // 等待所有任务完成
            try {
                latch.await(30, TimeUnit.MINUTES); // 最多等待30分钟
            } catch (InterruptedException e) {
                log.error("Crawling interrupted", e);
                Thread.currentThread().interrupt();
            }
            
            // 保存所有数据到数据库
            saveStockDataBatch(allStockData);
            
            log.info("Finished crawling all {} pages. Total records: {}", totalPages, allStockData.size());
        } catch (Exception e) {
            log.error("Error crawling all stock data", e);
        } finally {
            // 关闭线程池
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 爬取指定页码的股票数据
     * @param page 页码
     */
    public void crawlStockDataByPage(int page) {
        try {
            String response = fetchPageDataWithRetry(page);
            List<StockData> stockDataList = parseStockData(response);
            saveStockDataBatch(stockDataList);
            log.info("Successfully crawled and saved {} stock records for page {}", stockDataList.size(), page);
        } catch (Exception e) {
            log.error("Error crawling stock data for page {}", page, e);
        }
    }
    
    /**
     * 带重试机制的数据获取方法
     * @param page 页码
     * @return 响应数据
     * @throws IOException IO异常
     */
    private String fetchPageDataWithRetry(int page) throws IOException {
        IOException lastException = null;
        
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                return fetchPageData(page);
            } catch (IOException e) {
                lastException = e;
                log.warn("Failed to fetch page {} on attempt {}/{}. Retrying in {} ms...", 
                        page, i + 1, MAX_RETRIES, (i + 1) * 1000);
                
                try {
                    Thread.sleep((i + 1) * 1000); // 递增延迟重试
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    // 当线程被中断时，直接抛出异常而不是继续重试
                    throw new IOException("Interrupted while retrying", ie);
                }
            }
        }
        
        throw new IOException("Failed to fetch page " + page + " after " + MAX_RETRIES + " attempts", lastException);
    }
    
    /**
     * 获取总页数
     * @param jsonResponse JSON响应
     * @return 总页数
     */
    private int getTotalPages(String jsonResponse) {
        try {
            // 使用正则表达式提取JSON数据 (参考Python代码)
            Pattern pattern = Pattern.compile("\\{\"rc\":.*?\\]\\}\\}");
            Matcher matcher = pattern.matcher(jsonResponse);
            if (matcher.find()) {
                String jsonStr = matcher.group();
                JsonNode rootNode = objectMapper.readTree(jsonStr);
                JsonNode dataNode = rootNode.get("data");
                if (dataNode != null) {
                    JsonNode totalNode = dataNode.get("total");
                    if (totalNode != null) {
                        int totalRecords = totalNode.asInt();
                        // 每页20条记录
                        return (int) Math.ceil((double) totalRecords / 20);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting total pages", e);
        }
        return 1; // 默认返回1页
    }
    
    /**
     * 获取指定页码的数据
     * @param page 页码
     * @return 响应数据
     * @throws IOException IO异常
     */
    private String fetchPageData(int page) throws IOException {
        // 对URL中的特殊字符进行编码处理
        String url = STOCK_API_BASE_URL.replace("{}", String.valueOf(page));
        // 对URL中包含的特殊字符进行编码
        url = encodeUrl(url);
        
        HttpGet request = new HttpGet(url);
        request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        
        return httpClient.execute(request, httpResponse -> {
            return new String(httpResponse.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
        });
    }
    
    /**
     * 对URL进行编码处理，解决特殊字符问题
     * @param url 原始URL
     * @return 编码后的URL
     */
    private String encodeUrl(String url) {
        try {
            // 分离URL的基础部分和查询参数部分
            int queryIndex = url.indexOf('?');
            if (queryIndex == -1) {
                return url; // 没有查询参数
            }
            
            String baseUrl = url.substring(0, queryIndex);
            String query = url.substring(queryIndex + 1);
            
            // 对查询参数中的特殊字符进行编码
            // 注意：我们只需要编码|字符，保留其他参数格式
            query = query.replace("|", URLEncoder.encode("|", StandardCharsets.UTF_8));
            
            return baseUrl + "?" + query;
        } catch (Exception e) {
            log.warn("URL encoding failed, using original URL: {}", e.getMessage());
            return url; // 编码失败时使用原始URL
        }
    }
    
    /**
     * 解析股票数据
     * @param jsonResponse JSON响应
     * @return 股票数据列表
     */
    private List<StockData> parseStockData(String jsonResponse) {
        List<StockData> stockDataList = new ArrayList<>();
        
        try {
            // 使用正则表达式提取JSON数据 (参考Python代码)
            Pattern pattern = Pattern.compile("\\{\"rc\":.*?\\]\\}\\}");
            Matcher matcher = pattern.matcher(jsonResponse);
            if (matcher.find()) {
                String jsonStr = matcher.group();
                JsonNode rootNode = objectMapper.readTree(jsonStr);
                JsonNode dataNode = rootNode.get("data");
                
                if (dataNode != null && dataNode.has("diff")) {
                    JsonNode diffNode = dataNode.get("diff");
                    
                    for (JsonNode node : diffNode) {
                        StockData stockData = new StockData();
                        
                        // 解析字段 (根据用户提供的URL中的字段顺序)
                        stockData.setStockCode(getTextValue(node, "f12"));
                        stockData.setMarketType(getIntValue(node, "f13"));
                        stockData.setStockName(getTextValue(node, "f14"));
                        
                        // 价格相关字段 - 使用安全的解析方法
                        BigDecimal volumeVal = parseBigDecimalWithUnitConversion(node, "f5");
                        BigDecimal turnoverVal = parseBigDecimalWithUnitConversion(node, "f6");
                        stockData.setCurrentPrice(parseBigDecimal(node, "f2"));     // 最新价
                        stockData.setChangeAmount(parseBigDecimal(node, "f4"));     // 涨跌额
                        stockData.setChangePercent(parseChangePercent(node, "f3")); // 涨跌幅 (需要特殊处理)
                        stockData.setHighPrice(parseBigDecimal(node, "f15"));       // 最高
                        stockData.setOpenPrice(parseBigDecimal(node, "f16"));       // 今开
                        stockData.setLowPrice(parseBigDecimal(node, "f17"));        // 最低
                        stockData.setVolume(volumeVal != null ? volumeVal.longValue() : 0L);   // 成交量(单位手)
                        stockData.setTurnover(turnoverVal != null ? turnoverVal : BigDecimal.ZERO); // 成交额(元)
                        stockData.setTradeTime(LocalDateTime.now());
                        // 数据清洗：检查是否所有关键字段都为空
                        if (isValidStockData(stockData)) {
                            stockDataList.add(stockData);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing stock data: ", e);
        }
        
        log.info("Parsed {} stock records from response", stockDataList.size());
        return stockDataList;
    }
    
    /**
     * 批量保存股票数据到数据库
     * @param stockDataList 股票数据列表
     */
    private void saveStockDataBatch(List<StockData> stockDataList) {
        if (stockDataList.isEmpty()) {
            log.info("No stock data to save");
            return;
        }
        
        int successCount = 0;
        int failureCount = 0;
        
        for (StockData stockData : stockDataList) {
            try {
                stockDataMapper.insert(stockData);
                successCount++;
            } catch (Exception e) {
                log.warn("Failed to insert stock data for {}: {}", 
                    stockData.getStockCode(), e.getMessage());
                failureCount++;
            }
        }
        
        log.info("Saved stock data batch: {} success, {} failures", successCount, failureCount);
    }
    
    /**
     * 检查股票数据是否有效
     * @param stockData 股票数据
     * @return 是否有效
     */
    private boolean isValidStockData(StockData stockData) {
        // 检查关键字段是否都为空
        return stockData.getStockCode() != null && !stockData.getStockCode().isEmpty() &&
               stockData.getStockName() != null && !stockData.getStockName().isEmpty();
    }
    
    /**
     * 安全地获取文本值
     * @param node JSON节点
     * @param fieldName 字段名
     * @return 文本值，如果获取失败则返回空字符串
     */
    private String getTextValue(JsonNode node, String fieldName) {
        try {
            JsonNode fieldNode = node.get(fieldName);
            if (fieldNode != null && !fieldNode.isNull()) {
                String text = fieldNode.asText();
                // 处理"-"值，将其转换为空字符串
                if ("-".equals(text)) {
                    return "";
                }
                return text;
            }
        } catch (Exception e) {
            log.warn("Failed to get text value for field {}: {}", fieldName, e.getMessage());
        }
        return "";
    }
    
    /**
     * 安全地获取整数值
     * @param node JSON节点
     * @param fieldName 字段名
     * @return 整数值，如果获取失败则返回0
     */
    private int getIntValue(JsonNode node, String fieldName) {
        try {
            JsonNode fieldNode = node.get(fieldName);
            if (fieldNode != null && !fieldNode.isNull()) {
                // 处理"-"值
                String text = fieldNode.asText();
                if ("-".equals(text)) {
                    return 0;
                }
                return fieldNode.asInt();
            }
        } catch (Exception e) {
            log.warn("Failed to get int value for field {}: {}", fieldName, e.getMessage());
        }
        return 0;
    }
    
    /**
     * 安全地解析BigDecimal值
     * @param node JSON节点
     * @param fieldName 字段名
     * @return BigDecimal值，如果解析失败则返回null
     */
    private BigDecimal parseBigDecimal(JsonNode node, String fieldName) {
        try {
            JsonNode fieldNode = node.get(fieldName);
            if (fieldNode != null && !fieldNode.isNull()) {
                String textValue = fieldNode.asText();
                // 检查是否为有效数字并且不为"-"
                if (textValue != null && !textValue.isEmpty() && !"-".equals(textValue) && isNumeric(textValue)) {
                    return new BigDecimal(textValue);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse field {} as BigDecimal: {}", fieldName, e.getMessage());
        }
        return null;
    }
    
    /**
     * 特殊处理涨跌幅字段
     * 东方财富网返回的涨跌幅是以万分比形式表示的数值(例如7445表示74.45%)，正数为涨，负数为跌
     * @param node JSON节点
     * @param fieldName 字段名
     * @return BigDecimal值，如果解析失败则返回null
     */
    private BigDecimal parseChangePercent(JsonNode node, String fieldName) {
        try {
            JsonNode fieldNode = node.get(fieldName);
            if (fieldNode != null && !fieldNode.isNull()) {
                String textValue = fieldNode.asText();
                // 检查是否为有效数字并且不为"-"
                if (textValue != null && !textValue.isEmpty() && !"-".equals(textValue) && isNumeric(textValue)) {
                    // 东方财富网返回的涨跌幅是以万分比形式表示的(例如7445表示74.45%)
                    // 需要除以10000转换为正常百分比值并限制在合理范围内
                    BigDecimal value = new BigDecimal(textValue);
                    BigDecimal percentValue = value.divide(new BigDecimal("10000"), 6, BigDecimal.ROUND_HALF_UP);
                    
                    // 限制在合理范围内，避免超出数据库字段范围
                    if (percentValue.compareTo(new BigDecimal("999999.999999")) > 0) {
                        return new BigDecimal("999999.999999");
                    }
                    if (percentValue.compareTo(new BigDecimal("-999999.999999")) < 0) {
                        return new BigDecimal("-999999.999999");
                    }
                    
                    return percentValue;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse field {} as ChangePercent: {}", fieldName, e.getMessage());
        }
        return null;
    }
    
    /**
     * 检查字符串是否为有效数字
     * @param str 字符串
     * @return 是否为有效数字
     */
    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            new BigDecimal(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 安全地解析BigDecimal值并处理单位转换
     * 根据用户提供的信息，f5和f6字段的单位是"万"
     * @param node JSON节点
     * @param fieldName 字段名
     * @return BigDecimal值，如果解析失败则返回null
     */
    private BigDecimal parseBigDecimalWithUnitConversion(JsonNode node, String fieldName) {
        try {
            JsonNode fieldNode = node.get(fieldName);
            if (fieldNode != null && !fieldNode.isNull()) {
                String textValue = fieldNode.asText();
                // 检查是否为有效数字并且不为"-"
                if (textValue != null && !textValue.isEmpty() && !"-".equals(textValue) && isNumeric(textValue)) {
                    // 由于f5和f6字段单位是"万"，需要乘以10000转换为标准单位
                    BigDecimal value = new BigDecimal(textValue);
                    return value.multiply(new BigDecimal("10000"));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse field {} as BigDecimal with unit conversion: {}", fieldName, e.getMessage());
        }
        return null;
    }
}