<template>
  <div class="ai-analysis">
    <el-row :gutter="20" class="header-row">
      <el-col :span="12">
        <h2>AI港股分析</h2>
        <p class="subtitle">基于通义千问大模型的智能港股分析</p>
      </el-col>
      <el-col :span="12" class="text-right">
        <el-button type="primary" @click="getAiRecommendations" :loading="loading">获取AI推荐</el-button>
        <el-button type="success" @click="askAi" :disabled="!question.trim()">提问AI</el-button>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="16">
        <el-card class="recommend-card">
          <template #header>
            <div class="card-header">
              <span>AI推荐港股</span>
            </div>
          </template>

          <div v-loading="loading" element-loading-text="AI正在分析中...">
            <div v-if="recommendations.length > 0">
              <el-alert
                title="AI分析结果"
                type="success"
                description="AI基于当前市场情况分析推荐的港股"
                show-icon
                style="margin-bottom: 20px;"
              />

              <div class="recommendation-content">
                <div v-for="(section, index) in formattedRecommendations" :key="index" class="recommendation-section">
                  <h3>{{ section.title }}</h3>
                  <div v-if="section.type === 'list'">
                    <el-card 
                      v-for="(item, itemIndex) in section.content" 
                      :key="itemIndex" 
                      class="recommendation-item"
                      :class="{ 'top-recommendation': itemIndex === 0 }"
                    >
                      <div class="recommendation-item-content">
                        <div class="recommendation-header">
                          <span class="stock-code">{{ item.code }}</span>
                          <span class="stock-name">{{ item.name }}</span>
                        </div>
                        <div class="recommendation-reason">
                          <el-tag type="success" v-if="itemIndex === 0">强烈推荐</el-tag>
                          <p>{{ item.reason }}</p>
                        </div>
                      </div>
                    </el-card>
                  </div>
                  <div v-else-if="section.type === 'text'">
                    <el-card class="strategy-content">
                      <div class="strategy-text" v-html="formatTextContent(section.content)"></div>
                    </el-card>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="no-data">
              <p>暂无AI推荐数据</p>
              <el-button type="primary" @click="getAiRecommendations">点击获取推荐</el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="question-card">
          <template #header>
            <div class="card-header">
              <span>AI问答</span>
            </div>
          </template>

          <el-input
            v-model="question"
            type="textarea"
            placeholder="请输入您想咨询的港股相关问题..."
            :rows="4"
            style="margin-bottom: 15px;"
          ></el-input>

          <el-button type="success" @click="askAi" :disabled="!question.trim()" style="width: 100%;">
            提问AI
          </el-button>

          <div v-if="answer" class="answer-section">
            <h4>AI回答：</h4>
            <el-card class="answer-content">
              <div v-html="formatAnswer(answer)"></div>
            </el-card>
          </div>
        </el-card>

        <el-card class="info-card" style="margin-top: 20px;">
          <template #header>
            <div class="card-header">
              <span>AI分析说明</span>
            </div>
          </template>

          <div class="info-content">
            <h4>功能介绍</h4>
            <ul>
              <li><i class="el-icon-check"></i> AI推荐：基于当前市场数据推荐港股</li>
              <li><i class="el-icon-check"></i> AI问答：咨询港股投资相关问题</li>
              <li><i class="el-icon-check"></i> 智能分析：个股深度分析和趋势预测</li>
            </ul>

            <h4>使用建议</h4>
            <ul>
              <li><i class="el-icon-warning-outline"></i> AI分析结果仅供参考</li>
              <li><i class="el-icon-warning-outline"></i> 投资有风险，决策需谨慎</li>
              <li><i class="el-icon-warning-outline"></i> 建议结合其他分析工具使用</li>
            </ul>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const recommendations = ref([])
const formattedRecommendations = ref([])
const loading = ref(false)
const question = ref('')
const answer = ref('')

// 获取AI推荐
const getAiRecommendations = () => {
  loading.value = true
  recommendations.value = []
  formattedRecommendations.value = []
  
  axios.get('http://localhost:8081/api/stock/ai-recommend')
    .then(response => {
      if (response.data.success) {
        recommendations.value = response.data.data
        formatRecommendations(response.data.data)
      } else {
        ElMessage.error(response.data.message)
      }
    })
    .catch(error => {
      ElMessage.error('获取AI推荐失败: ' + error.message)
    })
    .finally(() => {
      loading.value = false
    })
}

// 格式化推荐内容
const formatRecommendations = (data) => {
  const sections = []
  let currentSection = null
  
  // 简单分段处理
  const lines = data.split('\n').filter(line => line.trim() !== '')
  
  lines.forEach(line => {
    if (line.includes('【推荐股票】')) {
      currentSection = {
        title: '推荐股票',
        type: 'list',
        content: []
      }
      sections.push(currentSection)
    } else if (line.includes('【投资策略】')) {
      currentSection = {
        title: '投资策略',
        type: 'text',
        content: ''
      }
      sections.push(currentSection)
    } else if (line.includes('【风险提示】')) {
      currentSection = {
        title: '风险提示',
        type: 'text',
        content: ''
      }
      sections.push(currentSection)
    } else if (currentSection && currentSection.type === 'list') {
      // 解析推荐股票
      const match = line.match(/^(\d+)\.\s*\[([^\]]+)\]\s*\[([^\]]+)\]\s*- 推荐理由:\s*(.*)$/)
      if (match) {
        currentSection.content.push({
          index: match[1],
          code: match[2],
          name: match[3],
          reason: match[4]
        })
      }
    } else if (currentSection && currentSection.type === 'text') {
      // 添加文本内容
      if (currentSection.content) {
        currentSection.content += line + '\n'
      } else {
        currentSection.content = line + '\n'
      }
    }
  })
  
  formattedRecommendations.value = sections
}

// 向AI提问
const askAi = () => {
  if (!question.value.trim()) {
    ElMessage.warning('请输入问题')
    return
  }

  answer.value = ''
  const loadingInstance = ElMessage.info('AI正在思考中...')

  axios.get('http://localhost:8081/api/stock/ai-question', {
    params: {
      question: question.value
    }
  })
    .then(response => {
      if (response.data.success) {
        answer.value = response.data.data
      } else {
        ElMessage.error(response.data.message)
        answer.value = response.data.message
      }
    })
    .catch(error => {
      ElMessage.error('AI回答失败: ' + error.message)
      answer.value = 'AI回答失败: ' + error.message
    })
    .finally(() => {
      loadingInstance.close()
    })
}

// 格式化回答内容
const formatAnswer = (content) => {
  if (!content) return ''
  // 将换行符转换为HTML段落
  return content.split('\n').map(p => `<p>${p}</p>`).join('')
}

// 格式化文本内容
const formatTextContent = (content) => {
  if (!content) return ''
  // 将换行符转换为HTML段落
  return content.split('\n').filter(p => p.trim() !== '').map(p => `<p>${p}</p>`).join('')
}

onMounted(() => {
  // 组件挂载时自动获取推荐
  getAiRecommendations()
})
</script>

<style scoped>
.ai-analysis {
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

.card-header {
  font-weight: bold;
  font-size: 1.1rem;
}

.recommend-card {
  margin-bottom: 20px;
}

.recommendation-section {
  margin-bottom: 20px;
}

.recommendation-section h3 {
  margin: 0 0 15px 0;
  color: #333;
  border-left: 4px solid #409EFF;
  padding-left: 10px;
}

.recommendation-item {
  margin-bottom: 15px;
}

.recommendation-item-content {
  padding: 15px;
}

.recommendation-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.stock-code {
  font-weight: bold;
  color: #409EFF;
}

.stock-name {
  font-weight: bold;
}

.recommendation-reason p {
  margin: 10px 0 0 0;
  line-height: 1.5;
}

.top-recommendation {
  border-left: 4px solid #67c23a;
}

.strategy-content {
  margin-bottom: 15px;
}

.strategy-text p {
  margin: 10px 0;
  line-height: 1.6;
}

.question-card {
  margin-bottom: 20px;
}

.answer-section {
  margin-top: 20px;
}

.answer-content {
  max-height: 300px;
  overflow-y: auto;
}

.answer-content p {
  margin: 10px 0;
  line-height: 1.6;
}

.info-content h4 {
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

.no-data {
  text-align: center;
  padding: 40px 0;
  color: #999;
}
</style>