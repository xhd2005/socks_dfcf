<template>
  <div class="home">
    <el-row :gutter="20">
      <el-col :span="24">
        <div class="welcome-banner">
          <h2>欢迎使用港股数据分析与可视化系统</h2>
          <p>一站式港股数据获取、分析与预测平台</p>
        </div>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" class="stats-section">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <i class="el-icon-user stat-icon"></i>
            <div class="stat-info">
              <div class="stat-number">{{ stats.stocks }}</div>
              <div class="stat-label">港股数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <i class="el-icon-data-line stat-icon"></i>
            <div class="stat-info">
              <div class="stat-number">{{ stats.predictions }}</div>
              <div class="stat-label">预测模型</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <i class="el-icon-star-off stat-icon"></i>
            <div class="stat-info">
              <div class="stat-number">{{ stats.recommendations }}</div>
              <div class="stat-label">推荐港股</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <i class="el-icon-aim stat-icon"></i>
            <div class="stat-info">
              <div class="stat-number">{{ stats.aiAnalyses }}</div>
              <div class="stat-label">AI分析</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" class="features-section">
      <el-col :span="16">
        <el-card class="feature-card">
          <template #header>
            <div class="card-header">
              <span>核心功能</span>
            </div>
          </template>
          <div class="features-grid">
            <div class="feature-item" @click="viewAllStocks">
              <i class="el-icon-data-analysis"></i>
              <h3>港股数据</h3>
              <p>查看实时港股行情</p>
            </div>
            
            <div class="feature-item" @click="viewRecommendations">
              <i class="el-icon-star-off"></i>
              <h3>智能推荐</h3>
              <p>基于算法推荐潜力港股</p>
            </div>
            
            <div class="feature-item" @click="viewAIAnalysis">
              <i class="el-icon-aim"></i>
              <h3>AI分析</h3>
              <p>通义千问智能分析</p>
            </div>
            
            <div class="feature-item" @click="crawlAllData" :class="{ 'disabled': loading.crawlAll }">
              <i class="el-icon-collection"></i>
              <h3>全量爬取</h3>
              <p>获取所有港股完整数据</p>
              <div v-if="loading.crawlAll" class="loading-overlay">
                <i class="el-icon-loading"></i>
              </div>
            </div>
          </div>
        </el-card>
        
        <el-card class="ai-section">
          <template #header>
            <div class="card-header">
              <span>AI智能分析</span>
            </div>
          </template>
          <div class="ai-content">
            <div class="ai-description">
              <p>基于通义千问大模型的智能港股分析功能，为您提供：</p>
              <ul>
                <li><i class="el-icon-check"></i> 个股深度分析</li>
                <li><i class="el-icon-check"></i> 智能港股推荐</li>
                <li><i class="el-icon-check"></i> 投资策略建议</li>
                <li><i class="el-icon-check"></i> 专业问答咨询</li>
              </ul>
            </div>
            <div class="ai-cta">
              <el-button type="success" @click="viewAIAnalysis">立即体验AI分析</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card class="info-card">
          <template #header>
            <div class="card-header">
              <span>系统介绍</span>
            </div>
          </template>
          <div class="description">
            <p>本系统基于Java技术栈开发，提供全面的港股数据分析服务：</p>
            <ul>
              <li>实时爬取港股市场数据</li>
              <li>港股价格趋势可视化展示</li>
              <li>基于机器学习的价格预测</li>
              <li>智能港股推荐系统</li>
              <li>AI驱动的深度分析</li>
            </ul>
          </div>
        </el-card>
        
        <el-card class="quick-actions">
          <template #header>
            <div class="card-header">
              <span>快捷操作</span>
            </div>
          </template>
          <div class="actions-grid">
            <el-button type="warning" @click="crawlAllData" :loading="loading.crawlAll">全量爬取</el-button>
            <el-button type="primary" @click="viewAllStocks">浏览港股</el-button>
            <el-button type="success" @click="viewRecommendations">查看推荐</el-button>
            <el-button type="info" @click="viewAIAnalysis">AI分析</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
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
        <p>准备开始爬取数据...</p>
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
import { ref, onMounted } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import axios from 'axios'
import { useRouter } from 'vue-router'

const router = useRouter()

const stats = ref({
  stocks: 0,
  predictions: 3,
  recommendations: 0,
  aiAnalyses: 15
})

const loading = ref({
  crawlAll: false
})

const crawlDialogVisible = ref(false)
const crawlProgress = ref({
  show: false,
  percentage: 0,
  status: '',
  message: ''
})

const crawlAllData = () => {
  loading.value.crawlAll = true
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
        updateStats()
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
      loading.value.crawlAll = false
    })
}

const viewRecommendations = () => {
  router.push('/recommend')
}

const viewAllStocks = () => {
  router.push('/stocks')
}

const viewAIAnalysis = () => {
  router.push('/ai-analysis')
}

const updateStats = () => {
  // 模拟更新统计数据
  stats.value.stocks += 20
  stats.value.recommendations += 3
  stats.value.aiAnalyses += 1
}

onMounted(() => {
  // 初始化统计数据
  stats.value.stocks = 128
  stats.value.recommendations = 5
})
</script>

<style scoped>
.home {
  padding: 20px;
}

.welcome-banner {
  background: linear-gradient(135deg, #409EFF, #64b5f6);
  color: white;
  padding: 40px 20px;
  border-radius: 8px;
  text-align: center;
  margin-bottom: 20px;
}

.welcome-banner h2 {
  font-size: 2rem;
  margin-bottom: 10px;
}

.welcome-banner p {
  font-size: 1.1rem;
  opacity: 0.9;
}

.stats-section {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 20px 0 rgba(0, 0, 0, 0.15);
}

.stat-content {
  display: flex;
  align-items: center;
  padding: 20px;
}

.stat-icon {
  font-size: 2.5rem;
  margin-right: 15px;
  color: #409EFF;
}

.stat-info {
  text-align: center;
}

.stat-number {
  font-size: 1.8rem;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 0.9rem;
  color: #666;
  margin-top: 5px;
}

.features-section {
  margin-bottom: 20px;
}

.feature-card,
.info-card,
.quick-actions,
.ai-section {
  margin-bottom: 20px;
}

.card-header {
  font-weight: bold;
  font-size: 1.1rem;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.feature-item {
  text-align: center;
  padding: 20px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #ebeef5;
  position: relative;
}

.feature-item:hover:not(.disabled) {
  background-color: #ecf5ff;
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.feature-item.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.feature-item i {
  font-size: 2rem;
  color: #409EFF;
  margin-bottom: 10px;
}

.feature-item h3 {
  margin: 10px 0;
  color: #333;
}

.feature-item p {
  color: #666;
  font-size: 0.9rem;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
}

.loading-overlay i {
  font-size: 2rem;
  color: #409EFF;
}

.description ul,
.ai-description ul {
  padding-left: 20px;
  margin: 15px 0;
}

.description li,
.ai-description li {
  margin: 8px 0;
  line-height: 1.5;
}

.actions-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.progress-message {
  text-align: center;
  margin-top: 20px;
  font-weight: 500;
}

.dialog-footer {
  text-align: right;
}

.ai-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ai-description {
  flex: 1;
}

.ai-description ul {
  list-style: none;
  padding-left: 0;
}

.ai-description li i {
  color: #67c23a;
  margin-right: 5px;
}

.ai-cta {
  margin-left: 20px;
}
</style>