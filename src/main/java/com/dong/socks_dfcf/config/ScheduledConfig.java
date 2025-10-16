package com.dong.socks_dfcf.config;

import com.dong.socks_dfcf.service.StockCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledConfig {
    
    private final StockCrawlerService stockCrawlerService;
    
    /**
     * 定时任务：每个工作日的上午9:30-15:00之间每30分钟执行一次数据爬取
     * cron表达式含义：
     * 秒 分 时 日 月 周
     * 0 0/30 9-15 ? * MON-FRI  表示工作日的9点到15点之间每30分钟执行一次
     */
    @Scheduled(cron = "0 0/30 9-15 ? * MON-FRI")
    public void scheduledCrawl() {
        log.info("开始定时爬取股票数据...");
        stockCrawlerService.crawlStockData();
        log.info("定时爬取股票数据完成");
    }
}