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
      path: "/question-bank",
      name: "question-bank",
      component: () => import("@/views/QuestionBankView.vue"),
    },
    {
      path: "/review",
      name: "review-center",
      component: () => import("@/views/ReviewCenterView.vue"),
    },
    {
      path: "/resumes",
      name: "resume-center",
      component: () => import("@/views/ResumeCenterView.vue"),
    },
    {
      path: "/observability",
      name: "observability",
      component: () => import("@/views/ObservabilityView.vue"),
    },
    {
      path: "/model-providers",
      name: "model-providers",
      component: () => import("@/views/ModelProviderView.vue"),
    },
    {
      path: "/evaluations",
      name: "evaluation-experiments",
      component: () => import("@/views/EvaluationExperimentView.vue"),
    },
    {
      path: "/observability/feedback",
      name: "observability-feedback",
      component: () => import("@/views/ObservabilityFeedbackView.vue"),
    },
    {
      path: "/answer",
      name: "knowledge-answer",
      component: () => import("@/views/KnowledgeAnswerView.vue"),
    },
  ],
});

export default router;
