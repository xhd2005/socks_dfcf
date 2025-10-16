package com.dong.socks_dfcf.mapper;

import com.dong.socks_dfcf.model.StockData;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface StockDataMapper {
 
    
    @Insert("INSERT INTO stock_data(stock_code, stock_name, current_price, change_amount, change_percent, open_price, high_price, low_price, volume, turnover, market_type, trade_time) " +
            "VALUES(#{stockCode}, #{stockName}, #{currentPrice}, #{changeAmount}, #{changePercent}, #{openPrice}, #{highPrice}, #{lowPrice}, #{volume}, #{turnover}, #{marketType}, #{tradeTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(StockData stockData);
    
    @Select("SELECT * FROM stock_data WHERE stock_code = #{stockCode} ORDER BY trade_time DESC LIMIT 100")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "stockCode", column = "stock_code"),
        @Result(property = "stockName", column = "stock_name"),
        @Result(property = "currentPrice", column = "current_price"),
        @Result(property = "changeAmount", column = "change_amount"),
        @Result(property = "changePercent", column = "change_percent"),
        @Result(property = "openPrice", column = "open_price"),
        @Result(property = "highPrice", column = "high_price"),
        @Result(property = "lowPrice", column = "low_price"),
        @Result(property = "volume", column = "volume"),
        @Result(property = "turnover", column = "turnover"),
        @Result(property = "marketType", column = "market_type"),
        @Result(property = "tradeTime", column = "trade_time")
    })
    List<StockData> findByStockCode(String stockCode);
    
    @Select("SELECT DISTINCT stock_code, stock_name, market_type FROM stock_data ORDER BY stock_code LIMIT 1000")
    @Results({
        @Result(property = "stockCode", column = "stock_code"),
        @Result(property = "stockName", column = "stock_name"),
        @Result(property = "marketType", column = "market_type")
    })
    List<StockData> findAllStockCodes();
    
    @Delete("DELETE FROM stock_data WHERE trade_time < DATE_SUB(NOW(), INTERVAL 30 DAY)")
    int deleteOldData();
    
    // 修改查询最新股票数据的方法，获取每个股票的最新一条记录
    @Select("SELECT s1.* FROM stock_data s1 " +
            "INNER JOIN (SELECT stock_code, MAX(trade_time) AS max_time FROM stock_data GROUP BY stock_code) s2 " +
            "ON s1.stock_code = s2.stock_code AND s1.trade_time = s2.max_time " +
            "ORDER BY s1.change_percent DESC LIMIT 20")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "stockCode", column = "stock_code"),
        @Result(property = "stockName", column = "stock_name"),
        @Result(property = "currentPrice", column = "current_price"),
        @Result(property = "changeAmount", column = "change_amount"),
        @Result(property = "changePercent", column = "change_percent"),
        @Result(property = "openPrice", column = "open_price"),
        @Result(property = "highPrice", column = "high_price"),
        @Result(property = "lowPrice", column = "low_price"),
        @Result(property = "volume", column = "volume"),
        @Result(property = "turnover", column = "turnover"),
        @Result(property = "marketType", column = "market_type"),
        @Result(property = "tradeTime", column = "trade_time")
    })
    List<StockData> findLatestStockData();

    /**
     * 分页查询最新股票数据
     * @param offset 偏移量
     * @param limit 返回数量
     */
    @Select("SELECT s1.* FROM stock_data s1 " +
            "INNER JOIN (SELECT stock_code, MAX(trade_time) AS max_time FROM stock_data GROUP BY stock_code) s2 " +
            "ON s1.stock_code = s2.stock_code AND s1.trade_time = s2.max_time " +
            "ORDER BY s1.change_percent DESC LIMIT #{limit} OFFSET #{offset}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "stockCode", column = "stock_code"),
        @Result(property = "stockName", column = "stock_name"),
        @Result(property = "currentPrice", column = "current_price"),
        @Result(property = "changeAmount", column = "change_amount"),
        @Result(property = "changePercent", column = "change_percent"),
        @Result(property = "openPrice", column = "open_price"),
        @Result(property = "highPrice", column = "high_price"),
        @Result(property = "lowPrice", column = "low_price"),
        @Result(property = "volume", column = "volume"),
        @Result(property = "turnover", column = "turnover"),
        @Result(property = "marketType", column = "market_type"),
        @Result(property = "tradeTime", column = "trade_time")
    })
    List<StockData> findLatestStockDataPaged(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 搜索股票（模糊查询，分页）
     */
    @Select("SELECT s1.* FROM stock_data s1 INNER JOIN (SELECT stock_code, MAX(trade_time) AS max_time FROM stock_data GROUP BY stock_code) s2 ON s1.stock_code = s2.stock_code AND s1.trade_time = s2.max_time WHERE s1.stock_code LIKE CONCAT('%', #{keyword}, '%') OR s1.stock_name LIKE CONCAT('%', #{keyword}, '%') ORDER BY s1.change_percent DESC LIMIT #{limit} OFFSET #{offset}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "stockCode", column = "stock_code"),
        @Result(property = "stockName", column = "stock_name"),
        @Result(property = "currentPrice", column = "current_price"),
        @Result(property = "changeAmount", column = "change_amount"),
        @Result(property = "changePercent", column = "change_percent"),
        @Result(property = "openPrice", column = "open_price"),
        @Result(property = "highPrice", column = "high_price"),
        @Result(property = "lowPrice", column = "low_price"),
        @Result(property = "volume", column = "volume"),
        @Result(property = "turnover", column = "turnover"),
        @Result(property = "marketType", column = "market_type"),
        @Result(property = "tradeTime", column = "trade_time")
    })
    List<StockData> searchStocksPaged(@Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 搜索结果总数
     */
    @Select("SELECT COUNT(DISTINCT stock_code) FROM stock_data WHERE stock_code LIKE CONCAT('%', #{keyword}, '%') OR stock_name LIKE CONCAT('%', #{keyword}, '%')")
    int countSearchStocks(@Param("keyword") String keyword);

    /**
     * 获取所有股票总条数
     */
    @Select("SELECT COUNT(DISTINCT stock_code) FROM stock_data")
    int countAllStockData();
}