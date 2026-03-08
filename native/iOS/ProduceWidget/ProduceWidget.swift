import WidgetKit
import SwiftUI

// MARK: - Timeline Entry

struct ProduceEntry: TimelineEntry {
    let date: Date
    let items: [(name: String, price: String)]
}

// MARK: - Timeline Provider

struct ProduceTimelineProvider: TimelineProvider {
    func placeholder(in context: Context) -> ProduceEntry {
        ProduceEntry(date: Date(), items: [
            ("高麗菜", "$12.5"),
            ("空心菜", "$18.3"),
            ("地瓜葉", "$15.0")
        ])
    }

    func getSnapshot(in context: Context, completion: @escaping (ProduceEntry) -> Void) {
        completion(placeholder(in: context))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<ProduceEntry>) -> Void) {
        Task {
            let items = await fetchTopVolume()
            let entry = ProduceEntry(date: Date(), items: items)
            let nextUpdate = Calendar.current.date(byAdding: .hour, value: 1, to: Date())!
            let timeline = Timeline(entries: [entry], policy: .after(nextUpdate))
            completion(timeline)
        }
    }

    private func fetchTopVolume() async -> [(name: String, price: String)] {
        guard let jwt = UserDefaults(suiteName: "group.com.yourname.produceapp")?
                .string(forKey: "jwt_token"),
              let plistPath = Bundle.main.path(forResource: "Config", ofType: "plist"),
              let dict = NSDictionary(contentsOfFile: plistPath),
              let baseUrl = dict["API_BASE_URL"] as? String,
              let url = URL(string: "\(baseUrl)top-volume") else {
            return []
        }

        var request = URLRequest(url: url)
        request.setValue("Bearer \(jwt)", forHTTPHeaderField: "Authorization")

        do {
            let (data, _) = try await URLSession.shared.data(for: request)
            if let json = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] {
                return json.prefix(5).compactMap { item in
                    guard let name = item["cropName"] as? String,
                          let price = item["avgPrice"] as? Double else { return nil }
                    return (name, String(format: "$%.1f", price))
                }
            }
        } catch {}
        return []
    }
}

// MARK: - Widget View

struct ProduceWidgetView: View {
    let entry: ProduceEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Image(systemName: "leaf.fill")
                    .foregroundColor(.green)
                Text("今日菜價")
                    .font(.headline)
            }

            if entry.items.isEmpty {
                Text("載入中...")
                    .font(.caption)
                    .foregroundColor(.secondary)
            } else {
                ForEach(entry.items, id: \.name) { item in
                    HStack {
                        Text(item.name)
                            .font(.caption)
                        Spacer()
                        Text(item.price)
                            .font(.caption)
                            .fontWeight(.bold)
                            .foregroundColor(.green)
                    }
                }
            }
        }
        .padding()
        .containerBackground(.fill.tertiary, for: .widget)
    }
}

// MARK: - Widget Definition

@main
struct ProduceWidget: Widget {
    let kind: String = "ProduceWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: ProduceTimelineProvider()) { entry in
            ProduceWidgetView(entry: entry)
        }
        .configurationDisplayName("農產品菜價")
        .description("在桌面查看今日熱門農產品價格")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
