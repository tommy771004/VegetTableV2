import SwiftUI

struct ElderlyModeView: View {
    @ObservedObject var repository: ProduceRepository
    @StateObject private var ttsHelper = TextToSpeechHelper()
    @State private var searchText = ""

    var body: some View {
        VStack(spacing: 20) {
            Text("大字體模式")
                .font(.system(size: 32, weight: .bold))

            // 語音搜尋按鈕
            Button {
                // 啟動語音辨識（需整合 Speech Framework）
            } label: {
                Label("點擊說話搜尋", systemImage: "mic.fill")
                    .font(.system(size: 24))
                    .padding()
                    .frame(maxWidth: .infinity)
                    .background(Color(red: 0.18, green: 0.49, blue: 0.2))
                    .foregroundColor(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            }

            // 搜尋欄
            TextField("輸入農產品名稱...", text: $searchText)
                .font(.system(size: 22))
                .textFieldStyle(.roundedBorder)
                .onSubmit {
                    Task { await repository.loadDailyPrices(keyword: searchText) }
                }

            // 大字結果列表
            ScrollView {
                VStack(spacing: 12) {
                    ForEach(repository.dailyPrices) { item in
                        HStack {
                            VStack(alignment: .leading) {
                                Text(item.cropName)
                                    .font(.system(size: 26, weight: .bold))
                                Text(item.marketName)
                                    .font(.system(size: 18))
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            Text("$\(String(format: "%.0f", item.avgPrice))")
                                .font(.system(size: 28, weight: .bold))
                                .foregroundColor(Color(red: 0.18, green: 0.49, blue: 0.2))

                            // 語音播報按鈕
                            Button {
                                ttsHelper.speakPrice(
                                    cropName: item.cropName,
                                    avgPrice: item.avgPrice,
                                    unit: "公斤"
                                )
                            } label: {
                                Image(systemName: "speaker.wave.3.fill")
                                    .font(.title2)
                            }
                            .accessibilityLabel("朗讀\(item.cropName)的價格")
                        }
                        .padding()
                        .background(.ultraThinMaterial)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                }
            }
        }
        .padding()
        .navigationTitle("長輩友善模式")
    }
}
