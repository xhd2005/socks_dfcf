# 股票数据分析与预测系统

本项目是一个基于Java的股票数据分析和预测系统，可以爬取东方财富网的股票数据，进行分析和预测，并提供股票推荐功能。

## 功能特性

1. **数据爬取**：从东方财富网爬取实时股票数据
2. **数据存储**：将爬取的数据存储到MySQL数据库
3. **数据分析**：基于历史数据进行趋势分析
4. **股票预测**：使用机器学习算法预测股票走势
5. **股票推荐**：根据涨幅和趋势推荐股票
6. **AI分析**：集成通义千问大模型，提供专业的AI股票分析和建议

## 技术栈

- 后端：Spring Boot 3.5
- 数据库：MySQL
- 爬虫：Apache HttpClient 5
- 机器学习：Weka
- AI分析：通义千问(DashScope SDK)
- 前端：Vue 3 + Vite

## 环境要求

- JDK 21
- MySQL 8.0+
- Maven 3.6+

## 安装配置

1. 克隆项目到本地
2. 创建MySQL数据库：`stock_db`
3. 在`src/main/resources/application.properties`中配置数据库连接信息
4. 在`src/main/resources/application.properties`中配置DashScope API Key

## 数据库初始化

执行`src/main/resources/schema/stock_data.sql`脚本创建数据表

## 启动项目

```bash
mvn spring-boot:run
```

## API接口

### 数据爬取接口

- `POST /api/stock/crawl` - 爬取第一页数据
- `POST /api/stock/crawl/page/{page}` - 爬取指定页数据
- `POST /api/stock/crawl/all` - 爬取所有数据

### 数据查询接口

- `GET /api/stock/codes` - 获取所有股票代码
- `GET /api/stock/latest` - 分页获取最新股票数据
- `GET /api/stock/{stockCode}/history` - 获取指定股票历史数据

### 分析预测接口

- `GET /api/stock/{stockCode}/predict` - 预测指定股票走势
- `GET /api/stock/recommend` - 获取推荐股票列表

### AI分析接口

- `GET /api/stock/{stockCode}/ai-analyze` - AI分析指定股票
- `GET /api/stock/ai-recommend` - AI推荐股票
- `GET /api/stock/ai-question` - AI回答股票相关问题

## 使用说明

1. 首先通过爬取接口获取数据：
   ```
   curl -X POST http://localhost:8081/api/stock/crawl
   ```

2. 查询股票数据：
   ```
   curl http://localhost:8081/api/stock/latest
   ```

3. 使用AI分析功能：
   ```
   curl http://localhost:8081/api/stock/000001/ai-analyze
   ```

4. 获取AI推荐：
   ```
   curl http://localhost:8081/api/stock/ai-recommend
   ```

5. 向AI提问：
   ```
   curl "http://localhost:8081/api/stock/ai-question?question=应该如何分析股票的基本面?"
   ```

## 获取DashScope API Key

1. 访问[阿里云DashScope官网](https://dashscope.aliyun.com/)
2. 注册并登录账号
3. 在控制台创建API Key
4. 将API Key配置到`application.properties`文件中