<script setup>
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <div id="app">
    <el-container>
      <el-header class="header">
        <div class="header-content">
          <h1>港股数据分析与可视化系统</h1>
          <nav class="navigation">
            <el-menu mode="horizontal" router>
              <el-menu-item index="/">首页</el-menu-item>
              <el-menu-item index="/stocks">港股数据</el-menu-item>
              <el-menu-item index="/recommend">港股推荐</el-menu-item>
              <el-menu-item index="/ai-analysis">AI分析</el-menu-item>
            </el-menu>
          </nav>
        </div>
      </el-header>
      
      <el-main class="main-content">
        <RouterView />
      </el-main>
      
      <el-footer class="footer">
        <p>© 2025 港股数据分析与可视化系统 - 基于Spring Boot和Vue 3构建</p>
      </el-footer>
    </el-container>
  </div>
</template>

<style scoped>
#app {
  min-height: 100vh;
}

.header {
  background-color: #409EFF;
  color: white;
  padding: 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.header h1 {
  margin: 0;
  font-size: 1.8rem;
}

.navigation {
  flex-grow: 1;
  margin-left: 30px;
}

.main-content {
  max-width: 1200px;
  margin: 20px auto;
  padding: 0 20px;
  flex-grow: 1;
}

.footer {
  background-color: #f5f5f5;
  text-align: center;
  padding: 20px;
  color: #666;
  margin-top: 40px;
}

.el-menu {
  background-color: transparent !important;
  border: none !important;
}

:deep(.el-menu-item) {
  color: white !important;
  font-weight: 500;
}

:deep(.el-menu-item:hover) {
  background-color: rgba(255, 255, 255, 0.1) !important;
}

:deep(.el-menu-item.is-active) {
  background-color: rgba(255, 255, 255, 0.2) !important;
  color: white !important;
  border-bottom: 2px solid white !important;
}
</style>