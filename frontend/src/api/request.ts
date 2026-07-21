import axios, { AxiosError } from "axios";

interface ApiErrorBody {
  message?: string;
}

const request = axios.create({
  baseURL: "/api",
  timeout: 15_000,
});

export function resolveApiError(error: unknown): string {
  if (error instanceof AxiosError) {
    if (!error.response) {
      return "无法连接后端服务，请确认 Spring Boot 已启动。";
    }
    const message = (error.response.data as ApiErrorBody | undefined)?.message;
    return message || `导入失败，后端返回 ${error.response.status}。`;
  }
  return "导入失败，请检查文件后重试。";
}

export default request;
