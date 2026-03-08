import SwiftUI

struct CommunityReportView: View {
    @State private var cropCode = ""
    @State private var retailPrice = ""
    @State private var market = ""
    @State private var reports: [CommunityPriceDto] = []
    @State private var showingAlert = false
    @State private var alertMessage = ""

    var body: some View {
        Form {
            Section("回報零售價") {
                TextField("作物代號", text: $cropCode)
                TextField("零售價格（元/公斤）", text: $retailPrice)
                    .keyboardType(.decimalPad)
                TextField("市場/地點", text: $market)

                Button("提交回報") {
                    submitReport()
                }
                .disabled(cropCode.isEmpty || retailPrice.isEmpty || market.isEmpty)
            }

            Section("近期回報紀錄") {
                if reports.isEmpty {
                    Text("尚無回報紀錄")
                        .foregroundColor(.secondary)
                } else {
                    ForEach(reports) { report in
                        HStack {
                            VStack(alignment: .leading) {
                                Text(report.market)
                                    .fontWeight(.medium)
                                Text(report.reportedAt)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            Text("$\(String(format: "%.1f", report.retailPrice))")
                                .fontWeight(.bold)
                                .foregroundColor(Color(red: 0.18, green: 0.49, blue: 0.2))
                        }
                    }
                }
            }
        }
        .navigationTitle("社群物價回報")
        .alert("回報結果", isPresented: $showingAlert) {
            Button("確定") {}
        } message: {
            Text(alertMessage)
        }
    }

    private func submitReport() {
        guard let price = Double(retailPrice) else {
            alertMessage = "請輸入有效的價格"
            showingAlert = true
            return
        }

        Task {
            do {
                try await ProduceService.shared.submitCommunityPrice(
                    cropCode: cropCode, retailPrice: price, market: market
                )
                alertMessage = "回報成功！感謝您的貢獻"
                showingAlert = true
                // 重新載入該作物的回報
                reports = try await ProduceService.shared.fetchCommunityPrices(cropCode: cropCode)
                retailPrice = ""
                market = ""
            } catch {
                alertMessage = "回報失敗：\(error.localizedDescription)"
                showingAlert = true
            }
        }
    }
}
