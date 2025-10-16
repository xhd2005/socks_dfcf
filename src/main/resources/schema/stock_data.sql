CREATE DATABASE IF NOT EXISTS stock_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE stock_db;

CREATE TABLE IF NOT EXISTS `stock_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stock_code` varchar(20) NOT NULL COMMENT '股票代码',
  `stock_name` varchar(100) NOT NULL COMMENT '股票名称',
  `current_price` decimal(15,4) NOT NULL COMMENT '最新价',
  `change_amount` decimal(15,4) NOT NULL COMMENT '涨跌额',
  `change_percent` decimal(15,6) NOT NULL COMMENT '涨跌幅',  -- 增加整数部分长度以适应极端情况
  `open_price` decimal(15,4) DEFAULT NULL COMMENT '开盘价',
  `high_price` decimal(15,4) DEFAULT NULL COMMENT '最高价',
  `low_price` decimal(15,4) DEFAULT NULL COMMENT '最低价',
  `volume` bigint DEFAULT NULL COMMENT '成交量',
  `turnover` decimal(20,4) DEFAULT NULL COMMENT '成交额',
  `market_type` int DEFAULT NULL COMMENT '市场类型',
  `trade_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '交易时间',
  PRIMARY KEY (`id`),
  KEY `idx_stock_code` (`stock_code`),
  KEY `idx_trade_time` (`trade_time`),
  KEY `idx_change_percent` (`change_percent`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='股票数据表';