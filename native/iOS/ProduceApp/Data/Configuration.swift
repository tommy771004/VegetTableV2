import Foundation

enum Configuration {
    /// API 基底 URL，從 Config.plist 讀取
    /// 正式環境透過 CI/CD 產生 Config.plist，不硬寫在程式碼中
    static var apiBaseUrl: String {
        guard let path = Bundle.main.path(forResource: "Config", ofType: "plist"),
              let dict = NSDictionary(contentsOfFile: path),
              let url = dict["API_BASE_URL"] as? String,
              !url.isEmpty else {
            #if DEBUG
            assertionFailure(
                """
                ⚠️ Config.plist 未設定 API_BASE_URL！
                請在專案目錄建立 Config.plist 並加入 API_BASE_URL 欄位。
                詳細說明請參考 README.md「設定 API URL」章節。
                """
            )
            #endif
            return ""
        }
        return url
    }
}
