import AVFoundation

class TextToSpeechHelper: ObservableObject {
    private let synthesizer = AVSpeechSynthesizer()

    func speak(_ text: String, language: String = "zh-TW") {
        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AVSpeechSynthesisVoice(language: language)
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate * 0.85 // 稍慢語速
        utterance.pitchMultiplier = 1.0

        synthesizer.stopSpeaking(at: .immediate)
        synthesizer.speak(utterance)
    }

    func speakPrice(cropName: String, avgPrice: Double, unit: String) {
        speak("\(cropName)，目前平均價格為每\(unit)\(String(format: "%.0f", avgPrice))元")
    }

    func stop() {
        synthesizer.stopSpeaking(at: .immediate)
    }
}
