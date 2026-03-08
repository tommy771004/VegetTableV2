import Foundation

actor ProduceService {
    static let shared = ProduceService()
    private let baseUrl = Configuration.apiBaseUrl

    // MARK: - JWT Management

    func ensureJwtToken() async {
        if UserDefaults.standard.string(forKey: "jwt_token") != nil { return }

        let deviceId = UIDevice.current.identifierForVendor?.uuidString ?? UUID().uuidString
        guard let url = URL(string: "\(baseUrl)auth/token") else { return }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try? JSONEncoder().encode(["deviceId": deviceId])

        do {
            let (data, _) = try await URLSession.shared.data(for: request)
            if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let token = json["token"] as? String {
                UserDefaults.standard.set(token, forKey: "jwt_token")
            }
        } catch {
            print("JWT 取得失敗: \(error)")
        }
    }

    private func authorizedRequest(url: URL) -> URLRequest {
        var request = URLRequest(url: url)
        if let token = UserDefaults.standard.string(forKey: "jwt_token") {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        return request
    }

    // MARK: - API Methods

    func fetchDailyPrices(keyword: String? = nil, page: Int = 1) async throws -> [ProduceDto] {
        var components = URLComponents(string: "\(baseUrl)daily-prices")!
        var queryItems = [URLQueryItem(name: "page", value: "\(page)")]
        if let keyword = keyword, !keyword.isEmpty {
            queryItems.append(URLQueryItem(name: "keyword", value: keyword))
        }
        components.queryItems = queryItems

        let request = authorizedRequest(url: components.url!)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode([ProduceDto].self, from: data)
    }

    func fetchPriceHistory(produceId: String) async throws -> [HistoricalPriceDto] {
        let url = URL(string: "\(baseUrl)history/\(produceId)")!
        let request = authorizedRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode([HistoricalPriceDto].self, from: data)
    }

    func fetchTopVolume() async throws -> [ProduceDto] {
        let url = URL(string: "\(baseUrl)top-volume")!
        let request = authorizedRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode([ProduceDto].self, from: data)
    }

    func fetchAnomalies() async throws -> [PriceAnomalyDto] {
        let url = URL(string: "\(baseUrl)anomalies")!
        let request = authorizedRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode([PriceAnomalyDto].self, from: data)
    }

    func fetchWeatherAlerts() async throws -> WeatherAlertDto {
        let url = URL(string: "\(baseUrl)weather-alerts")!
        let request = authorizedRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(WeatherAlertDto.self, from: data)
    }

    func fetchBudgetRecipes() async throws -> [BudgetRecipeDto] {
        let url = URL(string: "\(baseUrl)budget-recipes")!
        let request = authorizedRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode([BudgetRecipeDto].self, from: data)
    }

    func fetchForecast(produceId: String) async throws -> PricePredictionDto {
        let url = URL(string: "\(baseUrl)forecast/\(produceId)")!
        let request = authorizedRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(PricePredictionDto.self, from: data)
    }

    func fetchSeasonal() async throws -> [ProduceDto] {
        let url = URL(string: "\(baseUrl)seasonal")!
        let request = authorizedRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode([ProduceDto].self, from: data)
    }

    func fetchFavorites() async throws -> [FavoriteAlertDto] {
        let url = URL(string: "\(baseUrl)favorites")!
        let request = authorizedRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode([FavoriteAlertDto].self, from: data)
    }

    func deleteFavorite(produceId: String) async throws {
        let url = URL(string: "\(baseUrl)favorites/\(produceId)")!
        var request = authorizedRequest(url: url)
        request.httpMethod = "DELETE"
        let _ = try await URLSession.shared.data(for: request)
    }

    func submitCommunityPrice(cropCode: String, retailPrice: Double, market: String) async throws {
        let url = URL(string: "\(baseUrl)community-price")!
        var request = authorizedRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        let body: [String: Any] = ["cropCode": cropCode, "retailPrice": retailPrice, "market": market]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        let _ = try await URLSession.shared.data(for: request)
    }

    func fetchCommunityPrices(cropCode: String) async throws -> [CommunityPriceDto] {
        let url = URL(string: "\(baseUrl)community-price/\(cropCode)")!
        let request = authorizedRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode([CommunityPriceDto].self, from: data)
    }
}
