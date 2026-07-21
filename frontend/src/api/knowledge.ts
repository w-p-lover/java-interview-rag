import request from "@/api/request";

export interface MarkdownChunk {
  order: number;
  sectionOrder: number;
  headingLevel: number;
  headingPath: string[];
  content: string;
}

export interface MarkdownImportResponse {
  source: string;
  version: string;
  imported: boolean;
  chunkCount: number;
  chunks: MarkdownChunk[];
}

export async function importMarkdown(
  file: File,
  source: string,
): Promise<MarkdownImportResponse> {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("source", source);

  const response = await request.post<MarkdownImportResponse>(
    "/knowledge/documents/import",
    formData,
  );
  return response.data;
}
