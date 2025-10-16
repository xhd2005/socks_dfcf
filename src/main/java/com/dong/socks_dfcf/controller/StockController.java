package com.dong.socks_dfcf.controller;

import com.dong.socks_dfcf.mapper.StockDataMapper;
import com.dong.socks_dfcf.model.StockData;
import com.dong.socks_dfcf.service.StockCrawlerService;
import com.dong.socks_dfcf.service.StockPredictService;
import com.dong.socks_dfcf.service.AIAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {
    
    private final StockCrawlerService stockCrawlerService;
    private final StockPredictService stockPredictService;
    private final StockDataMapper stockDataMapper;
    private final AIAnalysisService aiAnalysisService;
    
    /**
     * 手动触发爬取数据（单页，默认第1页）
     */
    @PostMapping("/crawl")
    public Map<String, Object> crawlStockData() {
        Map<String, Object> result = new HashMap<>();
        try {
            stockCrawlerService.crawlStockData();
            result.put("success", true);
            result.put("message", "数据爬取完成");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "数据爬取失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 爬取指定页码的数据
     */
    @PostMapping("/crawl/page/{page}")
    public Map<String, Object> crawlStockDataByPage(@PathVariable int page) {
        Map<String, Object> result = new HashMap<>();
        try {
            stockCrawlerService.crawlStockDataByPage(page);
            result.put("success", true);
            result.put("message", "第" + page + "页数据爬取完成");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "第" + page + "页数据爬取失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 爬取所有数据
     */
    @PostMapping("/crawl/all")
    public Map<String, Object> crawlAllStockData() {
        Map<String, Object> result = new HashMap<>();
        try {
            stockCrawlerService.crawlAllStockData();
            result.put("success", true);
            result.put("message", "所有数据爬取完成");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "所有数据爬取失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取所有股票代码
     */
    @GetMapping("/codes")
    public Map<String, Object> getAllStockCodes() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<StockData> stockCodes = stockDataMapper.findAllStockCodes();
            result.put("success", true);
            result.put("data", stockCodes);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取股票代码失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取最新的股票数据
     */
    @GetMapping("/latest")
    public Map<String, Object> getLatestStockDataPaged(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        Map<String, Object> result = new HashMap<>();
        try {
            int offset = page * size;
            List<StockData> latestData = stockDataMapper.findLatestStockDataPaged(offset, size);
            int total = stockDataMapper.countAllStockData();
            result.put("success", true);
            result.put("data", latestData);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "分页获取最新股票数据失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 根据股票代码获取历史数据
     */
    @GetMapping("/{stockCode}/history")
    public Map<String, Object> getStockHistory(@PathVariable String stockCode) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<StockData> historyData = stockDataMapper.findByStockCode(stockCode);
            result.put("success", true);
            result.put("data", historyData);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取历史数据失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 预测股票走势（使用AI实现）
     */
    @GetMapping("/{stockCode}/predict")
    public Map<String, Object> predictStockTrend(@PathVariable String stockCode) {
        Map<String, Object> result = new HashMap<>();
        try {
            String prediction = aiAnalysisService.predictStockTrend(stockCode);
            result.put("success", true);
            result.put("data", prediction);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "预测失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取推荐股票
     */
    @GetMapping("/recommend")
    public Map<String, Object> recommendStocks() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<String> recommendations = stockPredictService.recommendStocks();
            result.put("success", true);
            result.put("data", recommendations);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取推荐失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * AI分析股票数据
     */
    @GetMapping("/{stockCode}/ai-analyze")
    public Map<String, Object> aiAnalyzeStock(@PathVariable String stockCode) {
        Map<String, Object> result = new HashMap<>();
        try {
            String analysis = aiAnalysisService.analyzeStock(stockCode);
            result.put("success", true);
            result.put("data", analysis);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "AI分析失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * AI推荐股票
     */
    @GetMapping("/ai-recommend")
    public Map<String, Object> aiRecommendStocks() {
        Map<String, Object> result = new HashMap<>();
        try {
            String recommendations = aiAnalysisService.recommendStocks();
            result.put("success", true);
            result.put("data", recommendations);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "AI推荐失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * AI回答问题
     */
    @GetMapping("/ai-question")
    public Map<String, Object> aiAnswerQuestion(@RequestParam String question) {
        Map<String, Object> result = new HashMap<>();
        try {
            String answer = aiAnalysisService.answerQuestion(question);
            result.put("success", true);
            result.put("data", answer);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "AI回答失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 搜索股票（模糊查询，分页）
     */
    @GetMapping("/search")
    public Map<String, Object> searchStocks(@RequestParam String keyword,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        Map<String, Object> result = new HashMap<>();
        try {
            int offset = page * size;
            List<StockData> data = stockDataMapper.searchStocksPaged(keyword, offset, size);
            int total = stockDataMapper.countSearchStocks(keyword);
            result.put("success", true);
            result.put("data", data);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "搜索失败: " + e.getMessage());
        }
        return result;
    }
}