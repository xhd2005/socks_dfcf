package com.dong.socks_dfcf.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StockData {
    private Long id;
    private String stockCode;        // 股票代码
    private String stockName;        // 股票名称
    private BigDecimal currentPrice; // 最新价
    private BigDecimal changeAmount; // 涨跌额
    private BigDecimal changePercent;// 涨跌幅
    private BigDecimal openPrice;    // 开盘价
    private BigDecimal highPrice;    // 最高价
    private BigDecimal lowPrice;     // 最低价
    private Long volume;             // 成交量
    private BigDecimal turnover;     // 成交额
    private Integer marketType;      // 市场类型
    private LocalDateTime tradeTime; // 交易时间
}