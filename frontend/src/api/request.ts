import axios, { AxiosError } from "axios";

interface ApiErrorBody {
  message?: string;
}

const request = axios.create({
  baseURL: "/api",
  timeout: 15_000,
});

export function resolveApiError(error: unknown, action = "请求处理失败"): string {
  if (error instanceof AxiosError) {
    if (!error.response) {
      return "无法连接后端服务，请确认 Spring Boot 已启动。";
    }
    const message = (error.response.data as ApiErrorBody | undefined)?.message;
    return message || `${action}，后端返回 ${error.response.status}。`;
  }
  return `${action}，请稍后重试。`;
}

export default request;
