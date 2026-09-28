import { onBeforeUnmount, readonly, shallowRef } from "vue";

interface BrowserSpeechRecognitionAlternative {
  transcript: string;
}

interface BrowserSpeechRecognitionResult {
  readonly isFinal: boolean;
  readonly length: number;
  readonly [index: number]: BrowserSpeechRecognitionAlternative;
}

interface BrowserSpeechRecognitionResultList {
  readonly length: number;
  readonly [index: number]: BrowserSpeechRecognitionResult;
}

interface BrowserSpeechRecognitionEvent extends Event {
  readonly resultIndex: number;
  readonly results: BrowserSpeechRecognitionResultList;
}

interface BrowserSpeechRecognitionErrorEvent extends Event {
  readonly error: string;
}

interface BrowserSpeechRecognition {
  continuous: boolean;
  interimResults: boolean;
  lang: string;
  onstart: (() => void) | null;
  onend: (() => void) | null;
  onresult: ((event: BrowserSpeechRecognitionEvent) => void) | null;
  onerror: ((event: BrowserSpeechRecognitionErrorEvent) => void) | null;
  start(): void;
  stop(): void;
  abort(): void;
}

interface BrowserSpeechRecognitionConstructor {
  new (): BrowserSpeechRecognition;
}

type VoiceWindow = Window & {
  SpeechRecognition?: BrowserSpeechRecognitionConstructor;
  webkitSpeechRecognition?: BrowserSpeechRecognitionConstructor;
};

interface UseInterviewVoiceOptions {
  questionText: () => string;
  onFinalTranscript: (text: string) => void;
}

export function useInterviewVoice(options: UseInterviewVoiceOptions) {
  const recognitionSupported = shallowRef(false);
  const synthesisSupported = shallowRef(false);
  const listening = shallowRef(false);
  const speaking = shallowRef(false);
  const interimTranscript = shallowRef("");
  const error = shallowRef("");

  let recognition: BrowserSpeechRecognition | null = null;
  let acceptingRecognitionResults = false;
  let activeUtterance: SpeechSynthesisUtterance | null = null;

  if (typeof window !== "undefined") {
    const voiceWindow = window as VoiceWindow;
    const Recognition = voiceWindow.SpeechRecognition ?? voiceWindow.webkitSpeechRecognition;
    recognitionSupported.value = Boolean(Recognition);
    synthesisSupported.value = "speechSynthesis" in window
      && "SpeechSynthesisUtterance" in window;

    if (Recognition) {
      recognition = new Recognition();
      recognition.lang = "zh-CN";
      recognition.continuous = true;
      recognition.interimResults = true;
      recognition.onstart = () => {
        acceptingRecognitionResults = true;
        listening.value = true;
        error.value = "";
      };
      recognition.onend = () => {
        acceptingRecognitionResults = false;
        listening.value = false;
        interimTranscript.value = "";
      };
      recognition.onerror = (event) => {
        acceptingRecognitionResults = false;
        listening.value = false;
        interimTranscript.value = "";
        if (event.error !== "aborted") {
          error.value = recognitionErrorMessage(event.error);
        }
      };
      recognition.onresult = (event) => {
        if (!acceptingRecognitionResults) {
          return;
        }
        const finalSegments: string[] = [];
        const interimSegments: string[] = [];
        for (let index = event.resultIndex; index < event.results.length; index += 1) {
          const result = event.results[index];
          const transcript = result?.[0]?.transcript?.trim() ?? "";
          if (!transcript) {
            continue;
          }
          if (result.isFinal) {
            finalSegments.push(transcript);
          } else {
            interimSegments.push(transcript);
          }
        }
        interimTranscript.value = interimSegments.join(" ");
        const finalTranscript = finalSegments.join(" ").trim();
        if (finalTranscript) {
          options.onFinalTranscript(finalTranscript);
        }
      };
    }
  }

  function startListening(): void {
    error.value = "";
    stopSpeaking();
    if (!recognition) {
      error.value = "当前浏览器不支持语音听写，请继续使用文字回答。";
      return;
    }
    if (listening.value) {
      return;
    }
    interimTranscript.value = "";
    try {
      recognition.start();
    } catch {
      acceptingRecognitionResults = false;
      error.value = "语音听写启动失败，请稍后重试或继续使用文字回答。";
    }
  }

  function stopListening(): void {
    if (recognition && listening.value) {
      recognition.stop();
    }
  }

  function abortListening(): void {
    acceptingRecognitionResults = false;
    recognition?.abort();
    listening.value = false;
    interimTranscript.value = "";
  }

  function speakQuestion(): void {
    error.value = "";
    abortListening();
    if (!synthesisSupported.value) {
      error.value = "当前浏览器不支持题目播放，请直接阅读题目。";
      return;
    }
    const text = options.questionText().trim();
    if (!text) {
      return;
    }
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    activeUtterance = utterance;
    utterance.lang = "zh-CN";
    utterance.rate = 1;
    utterance.pitch = 1;
    utterance.onstart = () => {
      if (activeUtterance === utterance) {
        speaking.value = true;
      }
    };
    utterance.onend = () => {
      if (activeUtterance === utterance) {
        activeUtterance = null;
        speaking.value = false;
      }
    };
    utterance.onerror = () => {
      if (activeUtterance === utterance) {
        activeUtterance = null;
        speaking.value = false;
        error.value = "题目播放失败，请直接阅读题目。";
      }
    };
    window.speechSynthesis.speak(utterance);
  }

  function stopSpeaking(): void {
    if (typeof window !== "undefined" && "speechSynthesis" in window) {
      if (activeUtterance) {
        activeUtterance.onstart = null;
        activeUtterance.onend = null;
        activeUtterance.onerror = null;
        activeUtterance = null;
      }
      window.speechSynthesis.cancel();
    }
    speaking.value = false;
  }

  function reset(): void {
    abortListening();
    stopSpeaking();
    error.value = "";
  }

  onBeforeUnmount(reset);

  return {
    recognitionSupported: readonly(recognitionSupported),
    synthesisSupported: readonly(synthesisSupported),
    listening: readonly(listening),
    speaking: readonly(speaking),
    interimTranscript: readonly(interimTranscript),
    error: readonly(error),
    startListening,
    stopListening,
    speakQuestion,
    stopSpeaking,
    reset,
  };
}

function recognitionErrorMessage(error: string): string {
  if (error === "not-allowed" || error === "service-not-allowed") {
    return "麦克风权限未开启，请授权后重试或继续使用文字回答。";
  }
  if (error === "no-speech") {
    return "没有识别到语音，请重试或继续使用文字回答。";
  }
  if (error === "audio-capture") {
    return "未检测到可用麦克风，请检查设备或继续使用文字回答。";
  }
  if (error === "network") {
    return "语音识别服务暂时不可用，请继续使用文字回答。";
  }
  return "语音听写失败，请稍后重试或继续使用文字回答。";
}
