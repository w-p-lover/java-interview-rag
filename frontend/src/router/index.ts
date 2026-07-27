import { createRouter, createWebHistory } from "vue-router";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      name: "knowledge-workbench",
      component: () => import("@/views/KnowledgeWorkbenchView.vue"),
    },
    {
      path: "/interview",
      name: "interview",
      component: () => import("@/views/InterviewView.vue"),
    },
  ],
});

export default router;
