<template>
  <div class="recommend">
    <el-row :gutter="20" class="header-row">
      <el-col :span="12">
        <h2>推荐港股</h2>
        <p class="subtitle">基于算法分析推荐的潜力港股</p>
      </el-col>
      <el-col :span="12" class="text-right">
        <el-button type="primary" @click="refreshRecommendations" :loading="loading">刷新推荐</el-button>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" class="recommend-stats">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <i class="el-icon-star-off stat-icon"></i>
            <div class="stat-info">
              <div class="stat-number">{{ recommendations.length }}</div>
              <div class="stat-label">推荐港股</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <i class="el-icon-trend stat-icon"></i>
            <div class="stat-info">
              <div class="stat-number">{{ avgIncrease }}%</div>
              <div class="stat-label">平均涨幅</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <i class="el-icon-data-line stat-icon"></i>
            <div class="stat-info">
              <div class="stat-number">{{ highRecommendations }}</div>
              <div class="stat-label">高潜力股</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <i class="el-icon-refresh stat-icon"></i>
            <div class="stat-info">
              <div class="stat-number">{{ updateTime }}</div>
              <div class="stat-label">更新时间</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card class="recommend-card">
          <template #header>
            <div class="card-header">
              <span>推荐列表</span>
            </div>
          </template>
          
          <el-table :data="recommendations" style="width: 100%" v-loading="loading">
            <el-table-column type="index" label="#" width="50"></el-table-column>
            
            <el-table-column prop="code" label="港股代码" width="100">
              <template #default="scope">
                <el-tag>{{ scope.row.code }}</el-tag>
              </template>
            </el-table-column>
            
            <el-table-column prop="name" label="港股名称" width="120"></el-table-column>
            
            <el-table-column prop="price" label="当前价格" width="100" sortable>
              <template #default="scope">
                ¥{{ scope.row.price }}
              </template>
            </el-table-column>
            
            <el-table-column prop="increase" label="涨幅" width="100" sortable>
              <template #default="scope">
                <span :class="scope.row.increase >= 0 ? 'positive' : 'negative'">
                  {{ scope.row.increase >= 0 ? '+' : '' }}{{ scope.row.increase }}%
                </span>
              </template>
            </el-table-column>
            
            <el-table-column prop="volume" label="成交量" width="120" sortable>
              <template #default="scope">
                {{ formatVolume(scope.row.volume) }}
              </template>
            </el-table-column>
            
            <el-table-column prop="recommendReason" label="推荐理由" show-overflow-tooltip></el-table-column>
            
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="scope">
                <el-button size="small" @click="viewDetails(scope.row)">查看详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card class="info-card">
          <template #header>
            <div class="card-header">
              <span>推荐说明</span>
            </div>
          </template>
          
          <div class="info-content">
            <h3>推荐算法</h3>
            <p>本系统采用多种算法综合评估港股投资价值：</p>
            <ul>
              <li>技术分析：价格趋势、成交量变化</li>
              <li>基本面分析：财务数据、行业前景</li>
              <li>市场情绪：新闻舆情、资金流向</li>
              <li>机器学习：历史数据模式识别</li>
            </ul>
            
            <h3>风险提示</h3>
            <el-alert
              title="投资有风险，入市需谨慎"
              type="warning"
              description="推荐结果基于当前市场数据和算法分析，仅供参考，不构成投资建议。"
              show-icon
            />
          </div>
        </el-card>
        
        <el-card class="strategy-card">
          <template #header>
            <div class="card-header">
              <span>投资策略</span>
            </div>
          </template>
          
          <div class="strategy-content">
            <el-tabs type="border-card">
              <el-tab-pane label="短线策略">
                <p>适合1-5个交易日的操作：</p>
                <ul>
                  <li>关注高换手率、高涨幅港股</li>
                  <li>控制仓位，设置止损点</li>
                  <li>关注市场热点板块</li>
                </ul>
              </el-tab-pane>
              <el-tab-pane label="中线策略">
                <p>适合1-3个月的投资：</p>
                <ul>
                  <li>关注基本面良好的成长港股</li>
                  <li>分批建仓，波段操作</li>
                  <li>关注行业发展趋势</li>
                </ul>
              </el-tab-pane>
              <el-tab-pane label="长线策略">
                <p>适合3个月以上的投资：</p>
                <ul>
                  <li>关注行业龙头、优质港股</li>
                  <li>长期持有，忽略短期波动</li>
                  <li>定期评估企业价值变化</li>
                </ul>
              </el-tab-pane>
            </el-tabs>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 港股详情对话框 -->
    <el-dialog v-model="detailDialogVisible" :title="currentStock ? currentStock.name + ' 详情' : '港股详情'" width="60%">
      <div v-loading="detailLoading">
        <el-row :gutter="20">
          <el-col :span="16">
            <v-chart :option="detailChartOption" autoresize style="height: 300px;" />
          </el-col>
          <el-col :span="8">
            <el-descriptions title="基本信息" :column="1" border>
              <el-descriptions-item label="港股代码">{{ currentStock?.code }}</el-descriptions-item>
              <el-descriptions-item label="港股名称">{{ currentStock?.name }}</el-descriptions-item>
              <el-descriptions-item label="当前价格">¥{{ currentStock?.price }}</el-descriptions-item>
              <el-descriptions-item label="涨跌幅">
                <span :class="currentStock?.increase >= 0 ? 'positive' : 'negative'">
                  {{ currentStock?.increase >= 0 ? '+' : '' }}{{ currentStock?.increase }}%
                </span>
              </el-descriptions-item>
              <el-descriptions-item label="成交量">{{ formatVolume(currentStock?.volume) }}</el-descriptions-item>
            </el-descriptions>
          </el-col>
        </el-row>
        
        <el-divider></el-divider>
        
        <h3>推荐理由</h3>
        <el-alert :title="currentStock?.recommendReason" type="success" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const recommendations = ref([])
const loading = ref(false)
const detailDialogVisible = ref(false)
const detailLoading = ref(false)
const currentStock = ref(null)
const updateTime = ref(new Date().toLocaleTimeString())

// 计算属性
const avgIncrease = computed(() => {
  if (recommendations.value.length === 0) return 0
  const sum = recommendations.value.reduce((acc, item) => acc + item.increase, 0)
  return (sum / recommendations.value.length).toFixed(2)
})

const highRecommendations = computed(() => {
  return recommendations.value.filter(item => item.increase > 5).length
})

// 图表配置
const detailChartOption = computed(() => {
  if (!currentStock.value) return {}
  
  // 模拟股价数据
  const dates = []
  const prices = []
  const basePrice = currentStock.value.price
  for (let i = 30; i >= 0; i--) {
    const date = new Date()
    date.setDate(date.getDate() - i)
    dates.push(date.toLocaleDateString())
    // 模拟价格波动
    const change = (Math.random() - 0.5) * 2
    prices.push((basePrice * (1 + change * 0.01)).toFixed(2))
  }
  
  return {
    title: {
      text: '近30日股价走势'
    },
    tooltip: {
      trigger: 'axis'
    },
    xAxis: {
      type: 'category',
      data: dates
    },
    yAxis: {
      type: 'value',
      scale: true,
      axisLabel: {
        formatter: '¥{value}'
      }
    },
    series: [{
      data: prices,
      type: 'line',
      smooth: true,
      areaStyle: {
        color: '#409EFF'
      },
      lineStyle: {
        color: '#409EFF'
      }
    }]
  }
})

// 获取推荐数据
const getRecommendations = () => {
  loading.value = true
  axios.get('http://localhost:8081/api/stock/recommend')
    .then(response => {
      if (response.data.success) {
        // 模拟推荐数据
        recommendations.value = [
          {
            code: '00700',
            name: '腾讯控股',
            price: 320.50,
            increase: 2.5,
            volume: '12000000',
            recommendReason: '业绩稳步增长，游戏业务持续扩张，具备长期投资价值'
          },
          {
            code: '09988',
            name: '阿里巴巴',
            price: 88.30,
            increase: 1.8,
            volume: '8500000',
            recommendReason: '电商龙头，云计算业务增长迅速，估值合理'
          },
          {
            code: '02331',
            name: '李宁',
            price: 45.60,
            increase: 5.2,
            volume: '5200000',
            recommendReason: '国潮品牌代表，运动服饰市场增长强劲'
          }
        ]
        updateTime.value = new Date().toLocaleTimeString()
      } else {
        ElMessage.error(response.data.message)
      }
    })
    .catch(error => {
      ElMessage.error('获取推荐数据失败: ' + error.message)
    })
    .finally(() => {
      loading.value = false
    })
}

// 刷新推荐
const refreshRecommendations = () => {
  getRecommendations()
}

// 查看详情
const viewDetails = (stock) => {
  currentStock.value = stock
  detailDialogVisible.value = true
  detailLoading.value = true
  
  // 模拟加载过程
  setTimeout(() => {
    detailLoading.value = false
  }, 500)
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

onMounted(() => {
  getRecommendations()
})
</script>

<style scoped>
.recommend {
  padding: 20px;
}

.header-row {
  margin-bottom: 20px;
}

.subtitle {
  color: #666;
  margin-top: 5px;
}

.text-right {
  text-align: right;
}

.recommend-stats {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
  overflow: hidden;
}

.stat-content {
  display: flex;
  align-items: center;
  padding: 20px;
}

.stat-icon {
  font-size: 2rem;
  margin-right: 15px;
  color: #409EFF;
}

.stat-info {
  text-align: center;
}

.stat-number {
  font-size: 1.5rem;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 0.9rem;
  color: #666;
  margin-top: 5px;
}

.recommend-card,
.info-card,
.strategy-card {
  margin-bottom: 20px;
}

.card-header {
  font-weight: bold;
  font-size: 1.1rem;
}

.positive {
  color: #e74c3c;
  font-weight: bold;
}

.negative {
  color: #27ae60;
  font-weight: bold;
}

.info-content h3 {
  margin: 15px 0 10px 0;
  color: #333;
}

.info-content ul {
  padding-left: 20px;
  margin: 10px 0;
}

.info-content li {
  margin: 5px 0;
}

.strategy-content {
  min-height: 200px;
}
</style>