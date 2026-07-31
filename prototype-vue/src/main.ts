import { createApp } from 'vue'
import App from './App.vue'
import { pinia } from './app/pinia'
import { router } from './router'
import './styles/main.css'

createApp(App).use(pinia).use(router).mount('#app')
