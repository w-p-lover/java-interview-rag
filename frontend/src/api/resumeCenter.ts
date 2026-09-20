import request from "@/api/request";

export interface ResumeVersion {
  readonly id: string;
  readonly resumeId: string;
  readonly versionNumber: number;
  readonly contentHash: string;
  readonly originalFilename: string;
  readonly contentType: string;
  readonly fileSize: number;
  readonly fullTextSaved: boolean;
  readonly createdAt: string;
}

export interface ResumeSummary {
  readonly id: string;
  readonly name: string;
  readonly targetRole: string;
  readonly currentVersionId: string | null;
  readonly version: number;
  readonly versionCount: number;
  readonly currentVersionNumber: number | null;
  readonly currentFullTextSaved: boolean;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface ResumeDetail {
  readonly id: string;
  readonly name: string;
  readonly targetRole: string;
  readonly currentVersionId: string | null;
  readonly version: number;
  readonly createdAt: string;
  readonly updatedAt: string;
  readonly versions: readonly ResumeVersion[];
}

export interface ResumeUploadPayload {
  file: File;
  resumeId?: string;
  name: string;
  targetRole: string;
  saveFullText: boolean;
}

export interface ResumeSaveResult {
  detail: ResumeDetail;
  savedVersionId: string;
  duplicate: boolean;
}

const base = "/resumes";

export async function listResumes(signal?: AbortSignal): Promise<ResumeSummary[]> {
  return (await request.get<ResumeSummary[]>(base, { signal })).data;
}

export async function getResumeDetail(id: string, signal?: AbortSignal): Promise<ResumeDetail> {
  return (await request.get<ResumeDetail>(`${base}/${encodeURIComponent(id)}`, { signal })).data;
}

export async function uploadResumeVersion(payload: ResumeUploadPayload): Promise<ResumeSaveResult> {
  const form = new FormData();
  form.append("file", payload.file);
  if (payload.resumeId) form.append("resumeId", payload.resumeId);
  form.append("name", payload.name);
  form.append("targetRole", payload.targetRole);
  form.append("saveFullText", String(payload.saveFullText));
  return (await request.post<ResumeSaveResult>(`${base}/versions`, form, { timeout: 120_000 })).data;
}

export async function switchResumeCurrentVersion(
  resumeId: string,
  versionId: string,
  expectedVersion: number,
): Promise<ResumeDetail> {
  return (await request.patch<ResumeDetail>(
    `${base}/${encodeURIComponent(resumeId)}/current-version`,
    { versionId, expectedVersion },
  )).data;
}
