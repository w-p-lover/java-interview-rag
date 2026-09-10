const RECENT_SESSIONS_STORAGE_KEY = "java-interview-rag.recent-interview-sessions";
const RECENT_SESSION_LIMIT = 8;

export function readRecentInterviewSessionIds(): string[] {
  const value = window.localStorage.getItem(RECENT_SESSIONS_STORAGE_KEY);
  if (!value) {
    return [];
  }
  try {
    const parsed: unknown = JSON.parse(value);
    if (!Array.isArray(parsed)) {
      return [];
    }
    return parsed
      .filter((item): item is string => typeof item === "string")
      .map((item) => item.trim())
      .filter((item, index, values) => item.length > 0
        && item.length <= 100 && values.indexOf(item) === index)
      .slice(0, RECENT_SESSION_LIMIT);
  } catch {
    return [];
  }
}

export function writeRecentInterviewSessionIds(sessionIds: readonly string[]): void {
  window.localStorage.setItem(
    RECENT_SESSIONS_STORAGE_KEY,
    JSON.stringify(sessionIds.slice(0, RECENT_SESSION_LIMIT)),
  );
}
