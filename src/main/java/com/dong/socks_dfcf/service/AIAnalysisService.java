package com.dong.socks_dfcf.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.Constants;
import com.dong.socks_dfcf.mapper.StockDataMapper;
import com.dong.socks_dfcf.model.StockData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIAnalysisService {

    private final StockDataMapper stockDataMapper;

    @Value("${dashscope.api-key:}")
    private String apiKey;

    private Generation generation;

    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.isEmpty()) {
            Constants.apiKey = apiKey;
        }
        generation = new Generation();
    }

    /**
     * 分析股票数据并提供精简投资建议（去除趋势预测等重合内容，仅输出建议和简要理由）
     * @param stockCode 股票代码
     * @return 精简AI分析结果
     */
    public String analyzeStock(String stockCode) {
        try {
            List<StockData> stockHistory = stockDataMapper.findByStockCode(stockCode);
            if (stockHistory == null || stockHistory.isEmpty()) {
                return "未找到股票代码为 " + stockCode + " 的数据";
            }
            StockData latestData = stockHistory.get(0);
            StringBuilder prompt = new StringBuilder();
            prompt.append("请作为专业的股票分析师，仅根据以下数据给出简明投资建议（买入/持有/卖出）及简要理由，不要包含趋势预测、风险提示、目标价位等内容：\n");
            prompt.append("股票代码: ").append(latestData.getStockCode()).append("\n");
            prompt.append("股票名称: ").append(latestData.getStockName()).append("\n");
            prompt.append("最新价格: ").append(latestData.getCurrentPrice()).append("\n");
            prompt.append("涨跌幅: ").append(latestData.getChangePercent()).append("%\n");
            prompt.append("成交量: ").append(formatVolume(latestData.getVolume())).append("\n");
            prompt.append("请直接输出建议及理由，格式如：建议买入，理由：xxx。\n");
            return callQwenAPI(prompt.toString(), 100);
        } catch (Exception e) {
            log.error("AI分析股票时出错", e);
            return "分析失败: " + e.getMessage();
        }
    }

    /**
     * 预测股票趋势
     *
     * @param stockCode 股票代码
     * @return AI预测结果
     */
    public String predictStockTrend(String stockCode) {
        try {
            List<StockData> stockHistory = stockDataMapper.findByStockCode(stockCode);
            if (stockHistory == null || stockHistory.isEmpty()) {
                return "未找到股票代码为 " + stockCode + " 的数据";
            }

            StockData latestData = stockHistory.get(0);
            StringBuilder prompt = new StringBuilder();
            prompt.append("请作为专业的股票分析师，对以下股票进行未来趋势预测:\n");
            prompt.append("股票代码: ").append(latestData.getStockCode()).append("\n");
            prompt.append("股票名称: ").append(latestData.getStockName()).append("\n");
            prompt.append("最新价格: ").append(latestData.getCurrentPrice()).append("\n");
            prompt.append("涨跌额: ").append(latestData.getChangeAmount()).append("\n");
            prompt.append("涨跌幅: ").append(latestData.getChangePercent()).append("%\n");
            prompt.append("开盘价: ").append(latestData.getOpenPrice()).append("\n");
            prompt.append("最高价: ").append(latestData.getHighPrice()).append("\n");
            prompt.append("最低价: ").append(latestData.getLowPrice()).append("\n");

            // 添加历史趋势分析
            if (stockHistory.size() > 7) {
                prompt.append("最近7天价格走势: \n");
                for (int i = 0; i < Math.min(7, stockHistory.size()); i++) {
                    StockData data = stockHistory.get(i);
                    prompt.append("  - 日期:").append(data.getTradeTime().toLocalDate())
                           .append(" 价格:").append(data.getCurrentPrice())
                           .append(" 涨跌幅:").append(data.getChangePercent()).append("%\n");
                }
                prompt.append("\n");
            }

            prompt.append("\n请根据以上数据预测该股票未来短期内（1-2周）的走势，要求包含以下内容:\n");
            prompt.append("1. 趋势预测（上涨/下跌/震荡）\n");
            prompt.append("2. 预测理由\n");
            prompt.append("3. 预计涨跌幅范围\n");
            prompt.append("4. 关键支撑位和压力位\n");
            prompt.append("5. 投资建议\n");
            prompt.append("请以简洁明了的语言输出，便于投资者理解。\n\n");
            prompt.append("重要：请严格按照以下格式输出结果:\n");
            prompt.append("【趋势预测】\n");
            prompt.append("趋势判断：[上涨/下跌/震荡]\n");
            prompt.append("\n【预测理由】\n");
            prompt.append("[详细分析理由]\n");
            prompt.append("\n【涨跌幅预测】\n");
            prompt.append("预计涨跌幅：[具体范围，如5%-10%]\n");
            prompt.append("\n【关键点位】\n");
            prompt.append("支撑位：[价格水平]\n");
            prompt.append("压力位：[价格水平]\n");
            prompt.append("\n【投资建议】\n");
            prompt.append("[具体操作建议，如'建议逢低买入'或'建议暂时观望']\n");

            return callQwenAPI(prompt.toString(), 300);
        } catch (Exception e) {
            log.error("AI预测股票趋势时出错", e);
            return "预测失败: " + e.getMessage();
        }
    }

    /**
     * 生成股票推荐列表
     *
     * @return 推荐分析结果
     */
    public String recommendStocks() {
        try {
            List<StockData> allStocks = stockDataMapper.findAllStockCodes();
            if (allStocks.isEmpty()) {
                return "没有可推荐的股票数据";
            }

            // 获取涨幅前10的股票
            List<StockData> topGainers = getTopGainers(10);

            StringBuilder prompt = new StringBuilder();
            prompt.append("你是一位专业的股票投资顾问，请分析以下股票数据并推荐3-5只最具有投资价值的股票:\n\n");

            for (int i = 0; i < Math.min(10, topGainers.size()); i++) {
                StockData stock = topGainers.get(i);
                prompt.append(i + 1).append(". ")
                        .append(stock.getStockCode()).append(" ")
                        .append(stock.getStockName()).append(" ")
                        .append("最新价: ").append(stock.getCurrentPrice()).append(" ")
                        .append("涨跌幅: ").append(stock.getChangePercent()).append("%\n");
            }

            prompt.append("\n请按照以下格式进行推荐:\n");
            prompt.append("【推荐股票】\n");
            prompt.append("1. [股票代码] [股票名称] - 推荐理由: [具体理由]\n");
            prompt.append("2. [股票代码] [股票名称] - 推荐理由: [具体理由]\n");
            prompt.append("...\n\n");
            prompt.append("【投资策略】\n");
            prompt.append("[提供针对当前市场情况的投资策略建议]\n\n");
            prompt.append("【风险提示】\n");
            prompt.append("[列出投资这些股票可能面临的主要风险]");

            return callQwenAPI(prompt.toString(), 400);
        } catch (Exception e) {
            log.error("AI推荐股票时出错", e);
            return "推荐失败: " + e.getMessage();
        }
    }

    /**
     * 回答用户关于股票的通用问题
     *
     * @param question 用户问题
     * @return AI回答
     */
    public String answerQuestion(String question) {
        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("你是一个专业的股票投资顾问，请回答以下问题:\n");
            prompt.append("问题: ").append(question).append("\n\n");
            prompt.append("请结合当前股市情况和投资常识，提供专业、详细的回答。要求:\n");
            prompt.append("1. 回答要有理有据\n");
            prompt.append("2. 语言简洁明了\n");
            prompt.append("3. 提供实际可操作的建议\n");
            prompt.append("4. 必要时给出风险提示");

            return callQwenAPI(prompt.toString(), 300);
        } catch (Exception e) {
            log.error("AI回答问题时出错", e);
            return "回答失败: " + e.getMessage();
        }
    }

    /**
     * 调用通义千问API
     *
     * @param prompt 提示词
     * @param maxTokens 最大返回token数
     * @return AI回答结果
     * @throws NoApiKeyException
     * @throws ApiException
     * @throws InputRequiredException
     */
    private String callQwenAPI(String prompt, int maxTokens) throws NoApiKeyException, ApiException, InputRequiredException {
        if (apiKey == null || apiKey.isEmpty()) {
            return "API密钥未配置，请在application.properties中设置dashscope.api-key";
        }

        Message systemMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content("你是一位专业的股票投资分析师，擅长分析股票数据并提供投资建议。你的回答应该专业、准确且具有可操作性。")
                .build();

        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(prompt)
                .build();

        GenerationParam param = GenerationParam.builder()
                .model("qwen-plus")
                .messages(List.of(systemMsg, userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .maxTokens(maxTokens)
                .temperature(Float.valueOf("0.7"))
                .build();

        GenerationResult result = generation.call(param);
        return result.getOutput().getChoices().get(0).getMessage().getContent();
    }

    /**
     * 获取涨幅前N的股票
     *
     * @param limit 数量限制
     * @return 涨幅前N的股票列表
     */
    private List<StockData> getTopGainers(int limit) {
        // 获取所有股票的最新数据
        List<StockData> allStocks = stockDataMapper.findAllStockCodes();
        List<StockData> result = new ArrayList<>();

        // 获取每只股票的最新数据
        for (StockData stock : allStocks) {
            if (result.size() >= limit * 2) break; // 多获取一些以确保质量
            List<StockData> history = stockDataMapper.findByStockCode(stock.getStockCode());
            if (!history.isEmpty()) {
                result.add(history.get(0));
            }
        }

        // 按涨幅排序并取前N个
        return result.stream()
                .filter(stock -> stock.getChangePercent() != null)
                .sorted(Comparator.comparing(StockData::getChangePercent).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 格式化成交量
     * @param volume 成交量
     * @return 格式化后的字符串
     */
    private String formatVolume(Long volume) {
        if (volume == null) return "0";
        if (volume >= 100000000) {
            return String.format("%.2f亿", volume / 100000000.0);
        } else if (volume >= 10000) {
            return String.format("%.2f万", volume / 10000.0);
        }
        return String.valueOf(volume);
    }
}

