<template>
  <div class="stock-list">
    <el-row :gutter="20" class="header-row">
      <el-col :span="12">
        <h2>港股数据列表</h2>
      </el-col>
      <el-col :span="12" class="text-right">
        <el-button type="primary" @click="refreshData">刷新数据</el-button>
        <el-button type="warning" @click="crawlAllData" :loading="crawlLoading">全量爬取</el-button>
      </el-col>
    </el-row>
    
    <el-card class="filter-card">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-input 
            v-model="searchKeyword" 
            placeholder="搜索港股代码或名称" 
            clearable
            @clear="searchStocks"
            @keyup.enter="searchStocks">
            <template #append>
              <el-button icon="el-icon-search" @click="searchStocks"></el-button>
            </template>
          </el-input>
        </el-col>
        <el-col :span="4">
          <el-select v-model="marketType" placeholder="市场类型" clearable @change="filterByMarket">
            <el-option label="全部市场" value=""></el-option>
            <el-option label="主板" value="1"></el-option>
            <el-option label="创业板" value="0"></el-option>
            <el-option label="其他" value="2"></el-option>
          </el-select>
        </el-col>
      </el-row>
    </el-card>
    
    <el-table 
      :data="stocks" 
      style="width: 100%" 
      v-loading="loading"
      element-loading-text="数据加载中..."
      element-loading-spinner="el-icon-loading"
      element-loading-background="rgba(0, 0, 0, 0.8)"
      :default-sort="{prop: 'changePercent', order: 'descending'}"
    >
      <el-table-column prop="stockCode" label="港股代码" width="100" sortable>
        <template #default="scope">
          <el-tag>{{ scope.row.stockCode }}</el-tag>
        </template>
      </el-table-column>
      
      <el-table-column prop="stockName" label="港股名称" width="120" sortable></el-table-column>
      
      <el-table-column prop="currentPrice" label="最新价" width="100" sortable></el-table-column>
      
      <el-table-column prop="openPrice" label="今开" width="100" sortable></el-table-column>
      
      <el-table-column prop="highPrice" label="最高" width="100" sortable></el-table-column>
      
      <el-table-column prop="lowPrice" label="最低" width="100" sortable></el-table-column>
      
      <el-table-column prop="volume" label="成交量" width="120" sortable>
        <template #default="scope">
          {{ formatVolume(scope.row.volume) }}
        </template>
      </el-table-column>
      
      <el-table-column prop="turnover" label="成交额" width="120" sortable>
        <template #default="scope">
          {{ formatTurnover(scope.row.turnover) }}
        </template>
      </el-table-column>
      
      <el-table-column prop="changePercent" label="涨跌幅(%)" width="120" sortable>
        <template #default="scope">
          <span :class="scope.
          row.changePercent >= 0 ? 'positive' : 'negative'">
            {{ scope.row.changePercent >= 0 ? '+' : '' }}{{ scope.row.changePercent }}%
          </span>
        </template>
      </el-table-column>
      
      <el-table-column prop="changeAmount" label="涨跌额" width="100" sortable>
        <template #default="scope">
          <span :class="scope.row.changeAmount >= 0 ? 'positive' : 'negative'">
            {{ scope.row.changeAmount >= 0 ? '+' : '' }}{{ scope.row.changeAmount }}
          </span>
        </template>
      </el-table-column>
      
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="viewHistory(scope.row)">历史数据</el-button>
          <el-button size="small" type="primary" @click="predictTrend(scope.row)">趋势预测</el-button>
          <el-button size="small" type="success" @click="aiAnalyze(scope.row)">AI分析</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <el-pagination
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      :current-page="currentPage"
      :page-sizes="[10, 20, 50, 100]"
      :page-size="pageSize"
      layout="total, sizes, prev, pager, next, jumper"
      :total="fetchStocksCount()"
      class="pagination">
    </el-pagination>
    
    <!-- 港股历史数据对话框 -->
    <el-dialog v-model="historyDialogVisible" :title="currentStock ? currentStock.stockName + ' 历史数据' : '港股历史数据'" width="80%">
      <div v-loading="chartLoading">
        <v-chart v-if="chartData.length > 0" :option="chartOption" autoresize style="height: 400px;" />
        <div v-else class="no-data">
          <p>暂无历史数据</p>
        </div>
      </div>
    </el-dialog>
    
    <!-- 预测结果对话框 -->
    <el-dialog v-model="predictDialogVisible" :title="currentStock ? currentStock.stockName + ' 趋势预测' : '港股趋势预测'" width="60%">
      <div class="predict-content">
        <div v-if="predictResult.loading" class="predict-content">
          <el-skeleton :rows="5" animated />
        </div>
        <div v-else-if="predictResult.error" class="predict-content">
          <el-alert :title="predictResult.error" type="error" show-icon />
        </div>
        <div v-else class="predict-content">
          <div v-for="(section, index) in formatPredictionResult(predictResult.data)" :key="index" class="prediction-section">
            <h4 v-if="section.title" class="section-title">{{ section.title }}</h4>
            <div v-if="section.type === 'list'">
              <ul>
                <li v-for="(item, itemIndex) in section.content" :key="itemIndex">{{ item }}</li>
              </ul>
            </div>
            <div v-else-if="section.type === 'keyValue'">
              <p v-for="(item, itemIndex) in section.content" :key="itemIndex">{{ item }}</p>
            </div>
            <div v-else>
              <p v-for="(item, itemIndex) in section.content" :key="itemIndex">{{ item }}</p>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
    
    <!-- AI分析对话框 -->
    <el-dialog v-model="aiDialogVisible" title="AI投资建议" :close-on-click-modal="false" width="500px">
      <div v-if="aiLoading" style="text-align:center;">
        <el-spinner />
        <div>AI分析中，请稍候...</div>
      </div>
      <div v-else>
        <div style="white-space:pre-line;font-size:16px;line-height:1.8;">
          {{ aiAnalysisResult }}
        </div>
      </div>
      <template #footer>
        <el-button @click="aiDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
    
    <!-- 爬取进度对话框 -->
    <el-dialog v-model="crawlDialogVisible" title="数据爬取进度" width="50%" center>
      <div v-if="crawlProgress.show">
        <el-progress 
          :percentage="crawlProgress.percentage" 
          :status="crawlProgress.status"
          :stroke-width="18"
          text-inside>
        </el-progress>
        <p class="progress-message">{{ crawlProgress.message }}</p>
      </div>
      <div v-else>
        <p>准备开始爬取港股数据...</p>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="crawlDialogVisible = false" :disabled="crawlProgress.status === 'success' || crawlProgress.status === 'exception'">取消</el-button>
          <el-button type="primary" @click="crawlDialogVisible = false" :disabled="!(crawlProgress.status === 'success' || crawlProgress.status === 'exception')">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElLoading, ElNotification } from 'element-plus'
import axios from 'axios'
import * as echarts from 'echarts'

const stocks = ref([])
const loading = ref(false)
const crawlLoading = ref(false)
const chartLoading = ref(false)
const historyDialogVisible = ref(false)
const predictDialogVisible = ref(false)
const crawlDialogVisible = ref(false)
const currentStock = ref(null)
const chartData = ref([])
const isSearching = ref(false)
const searchKeyword = ref('')
const marketType = ref('')

// AI分析相关
const aiDialogVisible = ref(false)
const aiLoading = ref(false)
const aiAnalysisResult = ref('')

// 预测结果
const predictResult = ref({
  loading: false,
  error: '',
  data: ''
})

const currentPage = ref(1)
const pageSize = ref(20) // 修改默认页面大小为20，与后端一致
const totalCount = ref(0)

// 爬取进度
const crawlProgress = ref({
  show: false,
  percentage: 0,
  status: '',
  message: ''
})

// 获取股票数据
const fetchStocks = () => {
  loading.value = true
  axios.get('http://localhost:8081/api/stock/latest', {
    params: {
      page: currentPage.value - 1,
      size: pageSize.value
    }
  })
    .then(response => {
      if (response.data.success) {
        stocks.value = response.data.data
        totalCount.value = response.data.total || 0
      } else {
        ElMessage.error(response.data.message)
      }
    })
    .catch(error => {
      ElMessage.error('获取数据失败: ' + error.message)
    })
    .finally(() => {
      loading.value = false
    })
}

// 获取股票总数
const fetchStocksCount = () => {
  return totalCount.value
}

// 刷新数据
const refreshData = () => {
  currentPage.value = 1
  fetchStocks()
}

// 搜索股票
const searchStocks = () => {
  if (!searchKeyword.value) {
    isSearching.value = false
    currentPage.value = 1
    fetchStocks()
    return
  }
  isSearching.value = true
  loading.value = true
  axios.get('http://localhost:8081/api/stock/search', {
    params: {
      keyword: searchKeyword.value,
      page: currentPage.value - 1,
      size: pageSize.value
    }
  })
    .then(response => {
      if (response.data.success) {
        stocks.value = response.data.data
        totalCount.value = response.data.total || 0
      } else {
        ElMessage.error(response.data.message)
      }
    })
    .catch(error => {
      ElMessage.error('搜索失败: ' + error.message)
    })
    .finally(() => {
      loading.value = false
    })
}

// 按市场类型筛选
const filterByMarket = () => {
  // 简单实现，实际应调用后端筛选接口
  ElMessage.info('筛选功能待实现')
}

// 查看历史数据
const viewHistory = (stock) => {
  currentStock.value = stock
  historyDialogVisible.value = true
  chartLoading.value = true
  chartData.value = []
  
  axios.get(`http://localhost:8081/api/stock/${stock.stockCode}/history`)
    .then(response => {
      if (response.data.success) {
        // 处理历史数据用于图表展示
        const data = response.data.data.map(item => ({
          date: new Date(item.tradeTime).toLocaleDateString(),
          price: item.currentPrice,
          volume: item.volume
        }))
        chartData.value = data.reverse() // 反转以按时间顺序显示
      } else {
        ElMessage.error(response.data.message)
      }
    })
    .catch(error => {
      ElMessage.error('获取历史数据失败: ' + error.message)
    })
    .finally(() => {
      chartLoading.value = false
    })
}

// 图表配置
const chartOption = computed(() => {
  return {
    title: {
      text: '股价走势图'
    },
    tooltip: {
      trigger: 'axis'
    },
    xAxis: {
      type: 'category',
      data: chartData.value.map(item => item.date)
    },
    yAxis: {
      type: 'value',
      scale: true,
      axisLabel: {
        formatter: '¥{value}'
      }
    },
    series: [{
      data: chartData.value.map(item => item.price),
      type: 'line',
      smooth: true,
      areaStyle: {}
    }]
  }
})

// 趋势预测
const predictTrend = (stock) => {
  currentStock.value = stock
  predictDialogVisible.value = true
  
  // 初始化预测结果状态
  predictResult.value = {
    loading: true,
    error: '',
    data: ''
  }
  
  axios.get(`http://localhost:8081/api/stock/${stock.stockCode}/predict`)
    .then(response => {
      if (response.data.success) {
        predictResult.value = {
          loading: false,
          error: '',
          data: response.data.data
        }
      } else {
        predictResult.value = {
          loading: false,
          error: response.data.message,
          data: ''
        }
      }
    })
    .catch(error => {
      predictResult.value = {
        loading: false,
        error: '网络错误: ' + error.message,
        data: ''
      }
    })
}

// 格式化预测结果
const formatPredictionResult = (result) => {
  if (!result) return []
  
  const sections = []
  let currentSection = null
  
  const lines = result.split('\n').filter(line => line.trim() !== '')
  
  lines.forEach(line => {
    if (line.startsWith('【') && line.endsWith('】')) {
      // 标题行（新的章节）
      currentSection = {
        title: line,
        type: 'title',
        content: []
      }
      sections.push(currentSection)
    } else if (line.match(/^(\d+\.|\*|\-)/)) {
      // 列表项
      if (!currentSection || (currentSection.type !== 'list' && currentSection.type !== 'title')) {
        currentSection = {
          title: '',
          type: 'list',
          content: []
        }
        sections.push(currentSection)
      }
      currentSection.content.push(line)
    } else if (line.includes('：') || line.includes(':')) {
      // 带冒号的行（键值对）
      if (!currentSection || currentSection.type === 'title') {
        currentSection = {
          title: '',
          type: 'keyValue',
          content: []
        }
        sections.push(currentSection)
      }
      currentSection.content.push(line)
    } else if (currentSection && currentSection.type !== 'title') {
      // 段落内容
      if (currentSection.content.length === 0) {
        currentSection.content.push(line)
      } else {
        // 合并到上一段
        const lastIndex = currentSection.content.length - 1
        currentSection.content[lastIndex] += ' ' + line
      }
    } else if (!currentSection || currentSection.type === 'title') {
      // 默认文本段落
      currentSection = {
        title: '',
        type: 'text',
        content: [line]
      }
      sections.push(currentSection)
    }
  })
  
  return sections
}

// AI分析
const aiAnalyze = (stock) => {
  currentStock.value = stock
  aiDialogVisible.value = true
  aiLoading.value = true
  aiAnalysisResult.value = ''
  
  axios.get(`http://localhost:8081/api/stock/${stock.stockCode}/ai-analyze`)
    .then(response => {
      if (response.data.success) {
        aiAnalysisResult.value = response.data.data
      } else {
        ElMessage.error(response.data.message)
        aiAnalysisResult.value = response.data.message
      }
    })
    .catch(error => {
      ElMessage.error('AI分析失败: ' + error.message)
      aiAnalysisResult.value = 'AI分析失败: ' + error.message
    })
    .finally(() => {
      aiLoading.value = false
    })
}

// 格式化AI分析结果
const formatAiAnalysisResult = (result) => {
  if (!result) return []
  
  const sections = []
  let currentSection = null
  
  const lines = result.split('\n').filter(line => line.trim() !== '')
  
  lines.forEach(line => {
    if (line.startsWith('【') && line.endsWith('】')) {
      // 标题行（新的章节）
      currentSection = {
        title: line,
        type: 'title',
        content: []
      }
      sections.push(currentSection)
    } else if (line.match(/^(\d+\.|\*|\-)/)) {
      // 列表项
      if (!currentSection || (currentSection.type !== 'list' && currentSection.type !== 'title')) {
        currentSection = {
          title: '',
          type: 'list',
          content: []
        }
        sections.push(currentSection)
      }
      currentSection.content.push(line)
    } else if (line.includes('：') || line.includes(':')) {
      // 带冒号的行（键值对）
      if (!currentSection || currentSection.type === 'title') {
        currentSection = {
          title: '',
          type: 'keyValue',
          content: []
        }
        sections.push(currentSection)
      }
      currentSection.content.push(line)
    } else if (currentSection && currentSection.type !== 'title') {
      // 段落内容
      if (currentSection.content.length === 0) {
        currentSection.content.push(line)
      } else {
        // 合并到上一段
        const lastIndex = currentSection.content.length - 1
        currentSection.content[lastIndex] += ' ' + line
      }
    } else if (!currentSection || currentSection.type === 'title') {
      // 默认文本段落
      currentSection = {
        title: '',
        type: 'text',
        content: [line]
      }
      sections.push(currentSection)
    }
  })
  
  return sections
}

// 全量爬取
const crawlAllData = () => {
  crawlLoading.value = true
  crawlDialogVisible.value = true
  crawlProgress.value = {
    show: true,
    percentage: 0,
    status: '',
    message: '开始爬取所有港股数据...'
  }
  
  axios.post('http://localhost:8081/api/stock/crawl/all')
    .then(response => {
      if (response.data.success) {
        crawlProgress.value = {
          show: true,
          percentage: 100,
          status: 'success',
          message: response.data.message
        }
        ElNotification({
          title: '成功',
          message: response.data.message,
          type: 'success'
        })
        refreshData()
      } else {
        crawlProgress.value = {
          show: true,
          percentage: 100,
          status: 'exception',
          message: response.data.message
        }
        ElMessage.error(response.data.message)
      }
    })
    .catch(error => {
      crawlProgress.value = {
        show: true,
        percentage: 100,
        status: 'exception',
        message: '数据爬取失败: ' + error.message
      }
      ElMessage.error('数据爬取失败: ' + error.message)
    })
    .finally(() => {
      crawlLoading.value = false
    })
}

// 格式化成交量
const formatVolume = (volume) => {
  if (!volume) return '0'
  const num = parseFloat(volume)
  if (num >= 100000000) {
    return (num / 100000000).toFixed(2) + '亿'
  } else if (num >= 10000) {
    return (num / 10000).toFixed(2) + '万'
  }
  return num.toFixed(0)
}

// 格式化成交额
const formatTurnover = (turnover) => {
  if (!turnover) return '0'
  const num = parseFloat(turnover)
  if (num >= 100000000) {
    return (num / 100000000).toFixed(2) + '亿'
  } else if (num >= 10000) {
    return (num / 10000).toFixed(2) + '万'
  }
  return num.toFixed(0)
}

// 分页相关方法
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  if (isSearching.value) {
    searchStocks()
  } else {
    fetchStocks()
  }
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  if (isSearching.value) {
    searchStocks()
  } else {
    fetchStocks()
  }
}

// 监听分页变化，自动请求新数据
onMounted(() => {
  fetchStocks()
})
</script>

<style scoped>
.stock-list {
  padding: 20px;
}

.header-row {
  margin-bottom: 20px;
  align-items: center;
}

.text-right {
  text-align: right;
}

.filter-card {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}

.positive {
  color: #e74c3c;
  font-weight: bold;
}

.negative {
  color: #27ae60;
  font-weight: bold;
}

.no-data {
  text-align: center;
  padding: 40px;
  color: #999;
}

.predict-content {
  min-height: 200px;
}

.section-title {
  margin: 15px 0 10px 0;
  color: #333;
  font-weight: bold;
}

.prediction-section {
  margin-bottom: 15px;
}

.prediction-section ul {
  padding-left: 20px;
}

.prediction-section li {
  margin: 5px 0;
}

.ai-analysis-content {
  max-height: 500px;
}

.analysis-section {
  margin-bottom: 20px;
}

.analysis-section h3 {
  margin: 0 0 10px 0;
  color: #333;
  border-left: 4px solid #409EFF;
  padding-left: 10px;
}

.analysis-section ul {
  padding-left: 20px;
}

.analysis-section li {
  margin: 5px 0;
}
</style>