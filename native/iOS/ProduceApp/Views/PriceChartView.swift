import SwiftUI
import Charts

struct PriceChartView: View {
    let produceId: String
    let produceName: String

    @State private var history: [HistoricalPriceDto] = []
    @State private var prediction: PricePredictionDto?
    @State private var isLoading = true
    @State private var errorMessage: String?

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                if isLoading {
                    ProgressView("載入中...")
                        .padding(.top, 40)
                } else if let error = errorMessage {
                    ContentUnavailableView(
                        "載入失敗",
                        systemImage: "exclamationmark.triangle",
                        description: Text(error)
                    )
                } else if history.isEmpty {
                    ContentUnavailableView(
                        "無歷史資料",
                        systemImage: "chart.line.downtrend.xyaxis",
                        description: Text("此農產品目前沒有歷史價格資料")
                    )
                } else {
                    chartView
                    predictionView
                }
            }
            .padding()
        }
        .navigationTitle(produceName)
        .task { await loadData() }
    }

    private var chartView: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("價格趨勢")
                .font(.headline)

            Chart(history) { item in
                LineMark(
                    x: .value("日期", item.transDate),
                    y: .value("均價", item.avgPrice)
                )
                .foregroundStyle(Color(red: 0.18, green: 0.49, blue: 0.2))

                AreaMark(
                    x: .value("日期", item.transDate),
                    y: .value("均價", item.avgPrice)
                )
                .foregroundStyle(
                    .linearGradient(
                        colors: [Color(red: 0.18, green: 0.49, blue: 0.2).opacity(0.3), .clear],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
            }
            .frame(height: 250)
            .chartYAxisLabel("元/公斤")
            .accessibilityLabel("農產品價格趨勢折線圖，包含歷史價格")
        }
        .padding()
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    @ViewBuilder
    private var predictionView: some View {
        if let pred = prediction {
            VStack(alignment: .leading, spacing: 8) {
                Label("價格預測", systemImage: "wand.and.stars")
                    .font(.headline)

                HStack {
                    VStack(alignment: .leading) {
                        Text("目前均價")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text("$\(String(format: "%.1f", pred.currentAvgPrice))")
                            .font(.title3)
                            .fontWeight(.bold)
                    }
                    Spacer()
                    VStack(alignment: .trailing) {
                        Text("預測趨勢")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        HStack {
                            Image(systemName: trendIcon(pred.trend))
                            Text(trendText(pred.trend))
                        }
                        .font(.title3)
                        .fontWeight(.bold)
                        .foregroundColor(trendColor(pred.trend))
                    }
                }
            }
            .padding()
            .background(.ultraThinMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
    }

    private func loadData() async {
        isLoading = true
        errorMessage = nil
        do {
            async let historyTask = ProduceService.shared.fetchPriceHistory(produceId: produceId)
            async let predictionTask = ProduceService.shared.fetchForecast(produceId: produceId)
            history = try await historyTask
            prediction = try? await predictionTask
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    private func trendIcon(_ trend: String) -> String {
        switch trend {
        case "上漲": return "arrow.up.right"
        case "下跌": return "arrow.down.right"
        default: return "arrow.right"
        }
    }

    private func trendText(_ trend: String) -> String { trend }

    private func trendColor(_ trend: String) -> Color {
        switch trend {
        case "上漲": return .red
        case "下跌": return .green
        default: return .orange
        }
    }
}
