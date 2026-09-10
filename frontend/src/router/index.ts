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
    {
      path: "/review",
      name: "review-center",
      component: () => import("@/views/ReviewCenterView.vue"),
    },
    {
      path: "/observability",
      name: "observability",
      component: () => import("@/views/ObservabilityView.vue"),
    },
    {
      path: "/answer",
      name: "knowledge-answer",
      component: () => import("@/views/KnowledgeAnswerView.vue"),
    },
  ],
});

export default router;
