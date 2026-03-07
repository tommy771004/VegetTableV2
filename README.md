# 台灣農產品價格追蹤系統 - 原生架構與 API 說明文件

本文件詳細說明本專案從 Web (React) 轉移至 **純原生開發 (Native App)** 與 **C# 後端 (Backend)** 的架構設計、邏輯修正，以及每個檔案的具體職責。

---

## 🏗️ 系統架構總覽 (Architecture Overview)

本系統採用 **BFF (Backend for Frontend)** 與 **Offline-First (離線優先)** 架構：

1. **C# 後端 (BFF)**：作為手機端與政府開放資料 (MOA API) 之間的中繼站。由後端負責每天定時抓取政府資料、清理格式、存入資料庫，並提供分頁與搜尋 API 給手機端。這解決了政府 API 回傳資料過大導致手機 OOM (Out of Memory) 的問題。
2. **手機端 (Android / iOS)**：採用 **Repository Pattern (單一資料來源模式)**。UI 層只與 Repository 溝通，Repository 會自動判斷要從網路 (API) 抓取資料，還是從本地資料庫 (SQLite/CoreData) 讀取離線快取。
3. **背景推播 (Background Jobs)**：雙平台皆實作了背景排程，能在背景比對價格並發送降價推播通知。

---

## 🔐 身分驗證架構 (Authentication Architecture)

本系統採用 **JWT (JSON Web Token)** 進行前後端身份驗證，已從舊版 `X-User-Id` Header 全面升級：

### JWT 認證流程
```
手機端 (冷啟動)
  → POST /auth/token  { "deviceId": "<UUID>" }
  ← { "token": "<JWT Bearer>" }
  → 儲存至 UserDefaults (iOS) / SharedPreferences (Android)
  → 所有後續 API 請求自動附加  Authorization: Bearer <token>
```

### Android JWT 自動注入
*   **`JwtInterceptor.kt`**：OkHttp 攔截器，從 `SharedPreferences` 讀取 JWT token，自動注入所有 API 請求的 `Authorization: Bearer` Header。
*   **`RetrofitClient.kt`**：使用 `BuildConfig.API_BASE_URL` 取得 API 基底 URL（由 `build.gradle` 的 `buildConfigField` 在編譯期注入，而非硬寫在程式碼中）。

### iOS JWT 自動注入
*   **`ProduceService.swift`**：所有 API 請求從 `UserDefaults` 讀取 JWT token，自動注入 `Authorization: Bearer` Header。
*   **`Configuration.swift`**：API 基底 URL 從 `Config.plist` 讀取（Debug 模式若未設定則觸發 `assertionFailure`）。

### 後端 JWT 設定安全性
*   **`appsettings.json`**：`SecretKey` 欄位必須保持空字串，不得包含真實金鑰。
*   **`appsettings.Development.json`**（本地開發專用）：含開發用金鑰，**不得** commit 至正式環境分支。
*   **正式環境**：透過 OS 環境變數 `Jwt__SecretKey` 注入（ASP.NET Core 雙底線 = 巢狀設定）。
*   **`Program.cs`**：啟動時驗證 `SecretKey` 是否已設定，若為空則拋出 `InvalidOperationException` 阻止服務啟動。

---

## 📂 檔案詳細說明 (File Breakdown)

### 🟣 Backend (C# ASP.NET Core 8)
後端負責資料整合、快取與提供 RESTful API。

*   **`Program.cs`**
    *   **職責**：應用程式的進入點 (Entry Point)。負責依賴注入 (DI)，註冊資料庫連線 (Entity Framework)、HttpClient、背景同步服務 (`ProduceSyncWorker`) 以及設定 CORS。
    *   **安全強化**：啟動時驗證 `Jwt__SecretKey` 環境變數，若未設定則明確提示錯誤訊息並阻止服務啟動。
*   **`ProduceController.cs`**
    *   **職責**：RESTful API 控制器。提供手機端呼叫的端點，包含：
        *   `GET /daily-prices`：取得今日價格（支援 `keyword` 搜尋與 `page` 分頁）。
        *   `GET /history/{produceId}`：取得歷史價格趨勢（供手機端畫圖表）。
        *   `GET /top-volume`：今日交易量前 10 名農產品。
        *   `GET /anomalies`：單日漲幅超過 50% 的價格異常警告。
        *   `GET /forecast/{produceId}`：根據 7 日移動平均計算明日價格趨勢。
        *   `GET /seasonal`：當季盛產農作物清單（依月份篩選）。
        *   `GET /weather-alerts`：中央氣象署颱風/豪大雨警報（RSS 解析），無警報時回傳 `alertType: "None"`。
        *   `GET /budget-recipes`：依當季/低價食材推薦的省錢食譜清單。
        *   `GET /favorites`、`DELETE /favorites/{produceId}`：使用者收藏管理。
        *   `POST /community-price`、`GET /community-price/{cropCode}`：社群零售價回報。
        *   `POST /auth/token`：以 `deviceId` 換取 JWT Bearer Token。
    *   **已改善**：注入 `ILogger<ProduceController>`，天氣警報 API 失敗時使用 `LogWarning` 記錄而非 Crash，不影響主畫面資料。
    *   **已移除**：`X-User-Id` Header 識別方式，已全面改為 JWT Bearer 認證。
*   **`ProduceDbContext.cs`**
    *   **職責**：Entity Framework Core 的資料庫上下文。定義了 `UserFavorites`、`PriceHistories`、`CommunityPrices` 資料表結構。
*   **`ProduceSyncWorker.cs`**
    *   **職責**：背景託管服務 (Hosted Service)。每天定時在背景執行，向台灣農業部 API 抓取最新資料並寫入 `PriceHistories` 資料庫，確保後端擁有完整的歷史數據。
*   **`ProduceDto.cs`**
    *   **職責**：資料傳輸物件 (Data Transfer Object)。確保前後端傳遞的 JSON 欄位名稱與型別完全一致。包含 `WeatherAlertDto`（天氣警報）與 `BudgetRecipeDto`（省錢食譜）。

---

### 🟢 Android (Kotlin / Jetpack Compose / Hilt)
Android 端採用標準的 **MVVM** 架構，結合 Jetpack Compose UI、Hilt 依賴注入、Retrofit 網路層與 Room 本地快取。

*   **`ProduceRepository.kt`**
    *   **職責**：單一資料來源 (Single Source of Truth)。負責協調 `ProduceService` (網路) 與 `ProduceDao` (Room 本地資料庫)。先嘗試打 API，失敗時自動退回讀取 SQLite 的離線快取。
*   **`ProduceService.kt`** (Retrofit Interface)
    *   **職責**：網路請求層。以 Retrofit 介面定義所有 API 端點，Retrofit 自動序列化/反序列化 JSON。
    *   **新增端點**：`getWeatherAlerts(): WeatherAlertDto`、`getBudgetRecipes(): List<BudgetRecipeDto>`。
*   **`RetrofitClient.kt`**
    *   **職責**：Retrofit 與 OkHttp 初始化。
    *   **API URL**：使用 `BuildConfig.API_BASE_URL`（由 `build.gradle` `buildConfigField` 編譯期注入），避免 URL 硬寫在程式碼中。
    *   **JWT 注入**：透過 `JwtInterceptor` 攔截所有請求，自動附加 `Authorization: Bearer` Header。
*   **`ProduceDto.kt`**
    *   **職責**：對應後端的 Kotlin data class。
    *   **變更**：`HistoricalPriceDto.avgPrice` 與 `PricePredictionDto.predictedPrice` 型別從 `Float` 改為 `Double`（與後端 double 精度對齊）。
    *   **新增**：`WeatherAlertDto`（天氣警報）、`BudgetRecipeDto`（省錢食譜）。
*   **`ProduceViewModel.kt`**
    *   **職責**：MVVM ViewModel 層，使用 `@HiltViewModel` + `StateFlow` 管理 UI 狀態。
    *   **新增 StateFlow**：`weatherAlerts`、`budgetRecipes`（獨立 try-catch，不影響主資料載入）。
*   **`ProduceFirebaseMessagingService.kt`**
    *   **職責**：Firebase Cloud Messaging 推播處理。
    *   **已修復**：移除錯誤的 `com.example.produce.data.ProduceService()` 實例化（ClassNotFoundException），改用 OkHttp 直接發送請求。
    *   **JWT 整合**：FCM Token 向後端註冊時使用 JWT Bearer（從 `SharedPreferences` 讀取）。
    *   **Deep Link**：通知點擊後透過 `PendingIntent` 傳遞 `produceId` Extra，啟動 `MainActivity` 並跳轉到對應農產品詳情頁。
*   **`HomeScreen.kt`**
    *   **職責**：首頁 UI，10 個資訊區塊依緊急程度排序：① 價格異常警報 → ② 今日熱門交易 → ③ 語音搜尋 → ④ 今日菜價 → ⑤ 天氣預警 → ⑥ 省錢食譜 → ⑦ 趨勢圖表 → ⑧ 當季盛產 → ⑨ 最近市場 → ⑩ 購物清單入口。
    *   **毛玻璃效果**：`liquidGlass()` Modifier 以半透明淡綠色 + 白色細邊框實現。
*   **`WeatherAlertCard.kt`**：訂閱 `viewModel.weatherAlerts`，當 `alertType == "None"` 時自動隱藏（不顯示卡片）。
*   **`BudgetRecipeCard.kt`**：訂閱 `viewModel.budgetRecipes`，顯示真實 API 返回的省錢食譜（含 Loading/Error/Empty 三態）。
*   **`PriceTrendChart.kt`**：Canvas 折線圖，加入無障礙語意 (`semantics { contentDescription }`)。
*   **`VoiceSearchButton.kt`**：按鈕文字已修正為「點擊說話」（與實際點擊行為一致）。

---

### 🍎 iOS (SwiftUI / Combine / async-await)
iOS 端採用 SwiftUI + `@MainActor` MVVM，以 URLSession + async/await 處理網路請求。

*   **`ProduceRepository.swift`**
    *   **職責**：單一資料來源。邏輯與 Android 相同，負責協調網路 API 與 SwiftData，實作離線優先 (Offline-First) 邏輯。
*   **`ProduceService.swift`**
    *   **職責**：網路請求層。使用原生 `URLSession` 發送請求，JWT Bearer Token 從 `UserDefaults` 讀取後自動注入每個請求的 `Authorization` Header。
*   **`Configuration.swift`**
    *   **職責**：API 基底 URL 從 `Config.plist` 讀取。
    *   **安全強化**：移除硬寫 fallback URL，Debug 模式若 `Config.plist` 未設定則觸發 `assertionFailure`（開發期間立即發現設定錯誤）。
*   **`AppDelegate.swift`**
    *   **職責**：Firebase Cloud Messaging 初始化與 FCM Token 管理。
    *   **已修正**：FCM Token 向後端註冊改用 JWT Bearer（移除舊 `X-User-Id` Header）。
    *   **冷啟動處理**：若 JWT 尚未取得，以 `Task { await ProduceService.shared.ensureJwtToken() }` 異步等候後再注冊。
*   **`MainTabView.swift`**
    *   **職責**：底部導覽列，包含四個 Tab：首頁、收藏、探索、設定。
    *   **新增「探索」Tab**：`ExploreMenuView` 以 `NavigationLink` 串聯所有進階功能頁面，解決孤立畫面無導覽入口問題：
        *   長輩友善模式 (`ElderlyModeView`)
        *   當季蔬果日曆 (`SeasonalCalendarView`)
        *   社群物價回報 (`CommunityReportView`)
        *   目標提醒設定 (`PriceAlertSetupView`)
*   **`PriceChartView.swift`**
    *   **職責**：農產品歷史價格趨勢圖（SwiftUI Charts）。
    *   **已重寫**：接受 `produceId` + `produceName` 參數，透過 `ProduceService.shared.fetchPriceHistory(produceId:)` 取得真實 API 資料，支援 Loading/Error/Empty/Chart 四態 UI。
*   **`Components.swift` (SkeletonView)**
    *   **已升級**：從簡單閃爍動畫改為 `LinearGradient` 光澤掃過效果（shimmer sweep），以 `withAnimation(.linear.repeatForever(autoreverses: false))` 驅動。
*   **`FavoritesScreen.swift`**
    *   **已修正**：`item.isReached` → `item.isAlertTriggered`（對應後端 DTO 欄位名稱）。

---

## 🔄 前後端邏輯 Match 重點總結

1. **分頁與搜尋 (Pagination & Search)**：前端不再一次接收 2000 筆資料，而是透過 `page` 與 `keyword` 參數向後端請求，後端透過 LINQ 過濾後只回傳 20 筆，大幅降低手機記憶體消耗。
2. **身分識別 (Authentication)**：已從 `X-User-Id` Header 全面升級為 **JWT Bearer Token**。Android 透過 `JwtInterceptor`，iOS 透過 `ProduceService` 自動注入。後端以 `Jwt__SecretKey` 環境變數管理金鑰，嚴禁硬寫於程式碼中。
3. **離線容錯 (Offline Tolerance)**：透過 Repository Pattern，前端在呼叫後端 API 失敗時，不會再發生 Crash，而是優雅地降級 (Fallback) 讀取本地資料庫。
4. **JSON 解析邏輯修正 (Backend)**：政府 API 回傳的 JSON 欄位是中文 (如 `"作物代號"`)，如果後端直接用 `ProduceDto` 解析，會導致前端收到的也是中文欄位。新增了專門用來解析政府 API 的 `MoaProduceDto` (加上 `[JsonPropertyName]` 標籤)，並在 Controller 中將其映射回標準的 `ProduceDto`，確保前端收到的永遠是標準的英文欄位 (如 `"cropCode"`)。
5. **背景同步服務邏輯修正 (`ProduceSyncWorker.cs`)**：實作了完整的 JSON 解析邏輯 (使用 `MoaProduceDto`)，並將每天抓取到的最新價格寫入 `PriceHistory` 資料表，讓後端真正成為資料的 Source of Truth。
6. **天氣預警與食譜獨立容錯**：`weatherAlerts` 與 `budgetRecipes` API 各自有獨立的 try-catch，失敗時不影響主資料（菜價、異常、熱門交易等）的正常顯示。

---

## ✨ 新增功能 (New Features)

1. **市場比價 (Market Comparison)**：
   - **Backend (`ProduceController.cs`)**：新增 `GET /api/produce/compare/{cropName}`，允許使用者查詢特定農產品在全台各市場的今日價格，並由低到高排序。

2. **價格預測與趨勢分析 (Price Forecasting)**：
   - **Backend (`ProduceController.cs`)**：新增 `GET /api/produce/forecast/{produceId}`，根據過去 14 天的歷史資料，計算 7 日移動平均線，預測未來價格趨勢 (上漲、下跌或持平)。

3. **熱門交易農產品 (Top Volume Crops)**：
   - **Backend (`ProduceController.cs`)**：新增 `GET /api/produce/top-volume`，取得今日交易量最大的前 10 名農產品。

4. **我的收藏與價格提醒 (My Favorites & Price Alerts)**：
   - **Backend (`ProduceController.cs`)**：`GET /api/produce/favorites` 自動比對今日最新價格，判斷是否達到目標價格 (`IsAlertTriggered`)。
   - **Android/iOS**：新增 `FavoriteAlertDto`，確保前後端資料結構完全一致。

5. **天氣警報整合 (Weather Alerts)**：
   - **Backend (`ProduceController.cs`)**：`GET /api/produce/weather-alerts` 解析中央氣象署 RSS 取得颱風/豪大雨警報，無警報時回傳 `alertType: "None"`，失敗時記錄 `LogWarning` 而非 Crash。
   - **Android (`WeatherAlertDto`, `WeatherAlertCard.kt`)**：新增 DTO 與對應的 Compose UI，`alertType == "None"` 時自動隱藏。
   - **iOS**：對應的天氣警報顯示邏輯。

6. **省錢食譜推薦 (Budget Recipe Generator)**：
   - **Backend (`ProduceController.cs`)**：`GET /api/produce/budget-recipes` 依當季/低價食材生成省錢食譜清單。
   - **Android (`BudgetRecipeDto`, `BudgetRecipeCard.kt`)**：新增 DTO 與對應的 Compose UI，支援 Loading/Error/Empty/Success 四態。

7. **主動式推播通知 (Push Notifications)**：
   - **Backend (`PriceAlertWorker.cs`)**：新增 `BackgroundService`，每天清晨自動掃描所有使用者的 `TargetPrice`，透過 Firebase Admin SDK 發送 FCM 推播通知。
   - **Android**：FCM 通知點擊後透過 `PendingIntent` Deep Link 傳遞 `produceId`，開啟農產品詳情頁。
   - **iOS**：FCM Token 以 JWT Bearer 向後端註冊。

8. **離線快取機制 (Offline Caching)**：
   - **Android (`ProduceEntity.kt`, `ProduceDao.kt`, `ProduceDatabase.kt`)**：導入 `Room Database`，在有網路時自動快取「我的收藏」與「今日熱門農產品」的最新價格。
   - **iOS (`ProduceModel.swift`)**：導入 `SwiftData` 進行本地快取。

9. **社群回報機制 (Community Retail Price)**：
   - **Backend (`ProduceController.cs`)**：新增 `POST /api/produce/community-price` 與 `GET /api/produce/community-price/{cropCode}` 讓使用者回報與查詢零售價。

10. **當季盛產日曆 (Seasonal Crop Calendar)**：
    - **Backend (`ProduceController.cs`)**：新增 `GET /api/produce/seasonal`，根據目前的月份，自動篩選出盛產的農產品清單。

11. **農產品價格異常警告 (Price Anomaly Detection)**：
    - **Backend (`ProduceController.cs`)**：新增 `GET /api/produce/anomalies`，自動比對近兩日的歷史價格，若單日漲幅超過 50%，則自動產生異常警報。

12. **多語系與無障礙設計 (i18n & Accessibility)**：
    - **Android/iOS**：針對外籍移工或長輩，加入多國語言支援（如印尼語 `id`、越南語 `vi`）。
    - **Android (`TextToSpeechHelper.kt`)**：導入 `android.speech.tts.TextToSpeech`，提供語音播報功能，並可調慢語速。
    - **iOS (`TextToSpeechHelper.swift`)**：導入 `AVFoundation` 的 `AVSpeechSynthesizer`，實現跨語系的語音播報輔助。
    - **Android (`PriceTrendChart.kt`)**：Canvas 圖表加入 `semantics { contentDescription }` 無障礙語意，方便螢幕閱讀器使用者。

13. **探索 Tab 導覽整合 (iOS)**：
    - **iOS (`MainTabView.swift`)**：新增第四個「探索」Tab，以 `ExploreMenuView` 列出所有進階功能頁面的 `NavigationLink`，解決 `ElderlyModeView`、`SeasonalCalendarView`、`CommunityReportView`、`PriceAlertSetupView` 等畫面無導覽入口的問題。

14. **前端 UI 介面實作 (UI Layer)**：
    - **UI 風格 (iOS 26 Liquid Glass / 毛玻璃)**：全站採用淡綠色漸層底色，搭配半透明、帶有模糊效果與白色細邊框的卡片設計。底部導覽列採用「懸浮式毛玻璃 (Floating Liquid Glass)」設計。
    - **Android (Jetpack Compose)**：
      - `HomeScreen.kt`：10 個資訊區塊依緊急程度排序的首頁 UI。
      - `FavoritesScreen.kt`：我的收藏頁面，根據目標價是否達成顯示不同鈴鐺狀態。
      - `SettingsScreen.kt`：設定頁面，包含推播、語言、離線快取等選項。
      - `PriceTrendChart.kt`：Canvas 折線圖，含無障礙語意標籤。
    - **iOS (SwiftUI)**：
      - `MainTabView.swift`：含「探索」Tab 的四分頁底部導覽列。
      - `HomeScreen.swift`：使用 `.ultraThinMaterial` 的毛玻璃質感首頁。
      - `FavoritesScreen.swift`：收藏頁面，已修正 `isAlertTriggered` 欄位對應。
      - `PriceChartView.swift`：接真實 API 的歷史趨勢圖，含四態 UI。
      - `Components.swift`：`SkeletonView` 升級為 shimmer sweep 光澤動畫。


# 🚀 進階功能整合指南 (Open Data, FCM, Offline-First, Widgets)

我已經為您產生了所有進階功能的**核心程式碼**。由於這些功能涉及深度的原生系統設定（如憑證、套件管理、系統權限），請依照以下指南將這些程式碼整合進您的專案中。

---

## 1. 🇹🇼 串接真實政府開放資料 (Open Data) & 本地端快取 (Offline-First)

我們採用了 **Offline-First (離線優先)** 架構：App 啟動時會先秒速載入本地資料庫的舊資料，同時在背景向政府 API 請求新資料。新資料抓到後會寫入資料庫，並自動觸發 UI 更新。

### 🤖 Android (Retrofit + Room)
*   **程式碼位置**：`/native/Android/app/src/main/java/com/example/produceapp/data/ProduceRepository.kt`
*   **您需要做的設定**：
    1. 在 `app/build.gradle` 中加入依賴：
       ```gradle
       implementation "com.squareup.retrofit2:retrofit:2.9.0"
       implementation "com.squareup.retrofit2:converter-gson:2.9.0"
       implementation "androidx.room:room-runtime:2.6.1"
       implementation "androidx.room:room-ktx:2.6.1"
       kapt "androidx.room:room-compiler:2.6.1"
       ```
    2. 在 `AndroidManifest.xml` 加入網路權限：`<uses-permission android:name="android.permission.INTERNET" />`
    3. 在 `app/build.gradle` 的 `android.defaultConfig` 區塊加入 API URL 設定：
       ```gradle
       buildConfigField("String", "API_BASE_URL", "\"https://your-api-server.com/api/produce/\"")
       ```
       正式環境可透過 CI/CD 環境變數注入，避免 URL 硬寫在程式碼中。

### 🍎 iOS (URLSession + SwiftData)
*   **程式碼位置**：`/native/iOS/ProduceApp/Data/ProduceRepository.swift`
*   **您需要做的設定**：
    1. 建立 `Config.plist` 並加入 `API_BASE_URL` 欄位，填入您的後端服務 URL。
    2. 在 `ProduceApp.swift` (主程式入口) 中加入 `.modelContainer(for: ProduceEntity.self)` 來初始化 SwiftData 環境。
    3. 在 UI 視圖中使用 `@Query var produceItems: [ProduceEntity]` 即可自動監聽資料庫變化。
    4. **注意**：`Config.plist` 不應 commit 至版本控制，請加入 `.gitignore`。`Configuration.swift` 在 Debug 模式下若未設定會觸發 `assertionFailure`。

---

## 2. 🔔 推播通知系統 (Firebase Cloud Messaging)

當菜價達標或異常時，即使 App 關閉也能收到通知。

### 🤖 Android
*   **程式碼位置**：`/native/Android/app/src/main/java/com/example/produceapp/fcm/ProduceFirebaseMessagingService.kt`
*   **架構說明**：
    *   FCM Token 取得後以 **JWT Bearer** 向後端 `POST /fcm/token` 註冊，JWT 從 `SharedPreferences("produce_prefs")` 的 key `"jwt_token"` 讀取。
    *   收到推播通知後，若訊息包含 `produceId`，會建立帶有該 Extra 的 `PendingIntent`，點擊通知後開啟 `MainActivity` 並帶入 `produceId` 供 Deep Link 使用。
    *   網路請求直接使用 OkHttp（不依賴 ProduceService 實例化，避免循環依賴）。
    *   API URL 使用 `BuildConfig.API_BASE_URL`（不硬寫）。
*   **您需要做的設定**：
    1. 前往 [Firebase Console](https://console.firebase.google.com/) 建立專案，下載 `google-services.json` 並放入 `app/` 目錄。
    2. 在 `app/build.gradle` 加入 FCM 依賴：`implementation 'com.google.firebase:firebase-messaging-ktx:23.4.0'`
    3. 在 `AndroidManifest.xml` 註冊 Service：
       ```xml
       <service android:name=".fcm.ProduceFirebaseMessagingService" android:exported="true">
           <intent-filter>
               <action android:name="com.google.firebase.MESSAGING_EVENT" />
           </intent-filter>
       </service>
       ```
    4. 在 `MainActivity.kt` 的 `onCreate` 中讀取 `intent.getStringExtra("produceId")` 以處理 Deep Link 導覽。

### 🍎 iOS
*   **程式碼位置**：`/native/iOS/ProduceApp/FCM/AppDelegate.swift`
*   **架構說明**：
    *   FCM Token 取得後，從 `UserDefaults.standard.string(forKey: "jwt_token")` 讀取 JWT。
    *   若 JWT 已存在，直接呼叫 `sendFcmTokenToBackend(_:jwt:)` 以 Bearer Token 向後端 `POST /fcm/token` 註冊。
    *   若 App 冷啟動時 JWT 尚未取得（首次執行），會先執行 `Task { await ProduceService.shared.ensureJwtToken() }` 異步等候 JWT 後再進行 FCM 注冊，確保不遺漏任何冷啟動情境。
    *   API URL 使用 `Configuration.apiBaseUrl`（從 `Config.plist` 讀取）。
*   **您需要做的設定**：
    1. 前往 Firebase Console 下載 `GoogleService-Info.plist` 並拖入 Xcode 專案。
    2. 使用 Swift Package Manager (SPM) 或 CocoaPods 安裝 `firebase-ios-sdk`。
    3. 在 Apple Developer Portal 建立 **APNs Auth Key**，並上傳至 Firebase Console。
    4. 在 Xcode 的 `Signing & Capabilities` 中新增 **Push Notifications** 與 **Background Modes (Remote notifications)**。

---

## 3. 🌤️ 天氣警報整合 (Weather Alerts)

颱風或豪大雨時自動顯示警報卡片，提醒使用者受影響農作物可能漲價。

### 🤖 Android
*   **程式碼位置**：
    *   `WeatherAlertCard.kt`：訂閱 `viewModel.weatherAlerts`（`StateFlow<Resource<WeatherAlertDto>>`），當 `alertType == "None"` 時自動隱藏整個卡片。
    *   `ProduceDto.kt`：`WeatherAlertDto` 包含 `alertType`, `severity`, `title`, `message`, `affectedCrops`。
    *   `ProduceService.kt`：`@GET("weather-alerts") suspend fun getWeatherAlerts(): WeatherAlertDto`
*   **注意**：天氣警報載入失敗時不影響主資料（使用獨立 try-catch），靜默失敗。

### 🍎 iOS
*   後端 API 相同，前端以 `ProduceService.swift` 呼叫，對應的 SwiftUI 視圖以 Optional binding 判斷是否顯示。

---

## 4. 🍳 省錢食譜推薦 (Budget Recipe Generator)

依當季低價食材推薦省錢食譜，數據來自後端真實 API。

### 🤖 Android
*   **程式碼位置**：
    *   `BudgetRecipeCard.kt`：訂閱 `viewModel.budgetRecipes`，顯示橫向滾動食譜卡片列表，支援 Loading/Error/Empty/Success 四態 UI。
    *   `ProduceDto.kt`：`BudgetRecipeDto` 包含 `recipeName`, `mainIngredients`, `reason`, `imageUrl`（Emoji 字元）, `steps`。
    *   `ProduceService.kt`：`@GET("budget-recipes") suspend fun getBudgetRecipes(): List<BudgetRecipeDto>`
*   **注意**：食譜載入失敗時不影響主資料（使用獨立 try-catch），靜默失敗。

---

## 5. 🔑 JWT 身份驗證整合 (JWT Authentication)

所有 API 請求皆使用 JWT Bearer Token，已從舊版 `X-User-Id` Header 全面遷移。

### 🤖 Android
*   **JWT 取得**：`POST /auth/token` 傳入 `{ "deviceId": "<Android Device ID>" }` 取得 JWT。
*   **JWT 儲存**：存入 `SharedPreferences("produce_prefs")`，key 為 `"jwt_token"`。
*   **JWT 自動注入**：`JwtInterceptor.kt` 攔截所有 OkHttp 請求，自動加上 `Authorization: Bearer <token>`，若收到 401 則重新取得 token 並重試一次。
*   **API URL**：`RetrofitClient.kt` 使用 `BuildConfig.API_BASE_URL`，在 `build.gradle` 設定 `buildConfigField`：
    ```gradle
    buildConfigField("String", "API_BASE_URL", "\"https://your-server.com/api/produce/\"")
    ```

### 🍎 iOS
*   **JWT 取得**：呼叫 `ProduceService.shared.ensureJwtToken()` 以 `deviceId` 換取 JWT。
*   **JWT 儲存**：`UserDefaults.standard.set(token, forKey: "jwt_token")`。
*   **JWT 自動注入**：`ProduceService.swift` 在每個請求 `URLRequest` 中設定 `setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")`。
*   **API URL**：`Configuration.apiBaseUrl` 讀取 `Config.plist` 中的 `API_BASE_URL`，Debug 模式未設定則 `assertionFailure`。

### 🟣 Backend (安全設定)
*   **正式環境**：透過 OS 環境變數 `Jwt__SecretKey` 注入 JWT 簽名金鑰（雙底線 = ASP.NET Core 巢狀設定分隔符）。
*   **本地開發**：在 `appsettings.Development.json` 設定開發專用金鑰（不可 commit 至正式分支）。
*   **`appsettings.json`**：`SecretKey` 必須保持空字串，`Program.cs` 啟動時若發現金鑰為空則拋出 `InvalidOperationException` 阻止服務啟動。

---

## 6. 📱 桌面小工具 (Widgets)

讓使用者不用打開 App，在手機首頁就能看到今天高麗菜的價格。

### 🤖 Android (Glance)
*   **程式碼位置**：`/native/Android/app/src/main/java/com/example/produceapp/widget/ProduceWidget.kt`
*   **您需要做的設定**：
    1. 在 `app/build.gradle` 加入 Glance 依賴：`implementation "androidx.glance:glance-appwidget:1.0.0"`
    2. 建立 `res/xml/produce_widget_info.xml` 定義 Widget 尺寸。
    3. 在 `AndroidManifest.xml` 註冊 Receiver：
       ```xml
       <receiver android:name=".widget.ProduceWidgetReceiver" android:exported="true">
           <intent-filter>
               <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
           </intent-filter>
           <meta-data android:name="android.appwidget.provider" android:resource="@xml/produce_widget_info" />
       </receiver>
       ```

### 🍎 iOS (WidgetKit)
*   **程式碼位置**：`/native/iOS/ProduceWidget/ProduceWidget.swift`
*   **您需要做的設定**：
    1. 在 Xcode 中點擊 `File` -> `New` -> `Target...`，選擇 **Widget Extension**，命名為 `ProduceWidget`。
    2. 將我提供的 `ProduceWidget.swift` 程式碼貼入新產生的檔案中。
    3. **重要**：若要讓 Widget 讀取主 App 的資料 (例如 SwiftData)，您必須在 Xcode 的 `Signing & Capabilities` 中為兩個 Target (主 App 與 Widget) 新增同一個 **App Groups** (例如 `group.com.yourname.produceapp`)。

---

## 7. ♿ 無障礙與長輩友善設計 (Accessibility)

### 🤖 Android
*   **`PriceTrendChart.kt`**：Canvas 折線圖加入 Compose 語意標籤：
    ```kotlin
    Canvas(modifier = Modifier.semantics {
        contentDescription = "農產品價格趨勢折線圖，包含歷史價格（綠色實線）與預測價格（橘色虛線）"
    })
    ```
    使螢幕閱讀器 (TalkBack) 能夠讀出圖表內容。
*   **`VoiceSearchButton.kt`**：按鈕說明文字已修正為「點擊說話」（與實際互動行為一致）。
*   **`TextToSpeechHelper.kt`**：菜價列表每一筆都有語音播報圖示按鈕，點擊後朗讀價格。

### 🍎 iOS
*   **`ElderlyModeView.swift`**：透過探索 Tab 導覽入口可直接進入大字體語音查詢模式。
*   **`TextToSpeechHelper.swift`**：以 `AVSpeechSynthesizer` 朗讀農產品價格。

---

## 8. 🎨 UI 動畫與視覺效果 (UI Animations)

### 🍎 iOS
*   **`Components.swift` (SkeletonView)**：骨架屏升級為 shimmer sweep 光澤掃過動畫：
    *   `LinearGradient` 從 `clear → white.opacity(0.5) → clear` 的漸層覆蓋在灰色底色上
    *   `withAnimation(.linear(duration: 1.2).repeatForever(autoreverses: false))` 驅動 `shimmerPhase` 從 `-1.0` 到 `1.3`，產生由左至右的流動光澤效果。

### 🤖 Android
*   **`HomeScreen.kt`**：`liquidGlass()` Modifier 以 `Color(0x40A5D6A7)` 半透明淡綠底色 + `1.dp` 白色細邊框 (`Color(0x60FFFFFF)`) + `RoundedCornerShape(16.dp)` 實現毛玻璃質感。

# 🍏 macOS 環境建置與雙平台開發部署指南

這份指南將帶領您從零開始，在 macOS 環境上建置 Android 與 iOS 的開發環境，並涵蓋從開發、測試到最終打包上架 (App Store & Google Play) 的完整流程。

---

## 壹、基礎環境準備

在 macOS 上，我們強烈建議使用 **Homebrew** 來管理套件。

1. **安裝 Homebrew** (若尚未安裝)：
   打開終端機 (Terminal) 並輸入：
   ```bash
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   ```

2. **安裝 Git**：
   ```bash
   brew install git
   ```

---

## 貳、Android 開發環境建置 (Jetpack Compose)

### 1. 安裝 Java 開發套件 (JDK)
Android 開發需要 JDK。建議安裝 Zulu JDK (支援 Apple Silicon M1/M2/M3)：
```bash
brew install --cask zulu
```

### 2. 安裝 Android Studio
```bash
brew install --cask android-studio
```
*   開啟 Android Studio，按照安裝精靈 (Setup Wizard) 下載預設的 Android SDK、SDK Platform-Tools 與 Android Emulator。

### 3. 設定環境變數
在您的 `~/.zshrc` (或 `~/.bash_profile`) 中加入：
```bash
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/platform-tools
```
執行 `source ~/.zshrc` 使其生效。

### 4. 設定 API URL (BuildConfig)
本專案的 API 基底 URL 透過 `buildConfigField` 在編譯期注入，不硬寫在程式碼中。
在 `app/build.gradle` 的 `android.defaultConfig` 區塊設定：
```gradle
android {
    defaultConfig {
        buildConfigField("String", "API_BASE_URL", "\"https://your-dev-server.com/api/produce/\"")
    }
    buildTypes {
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://your-prod-server.com/api/produce/\"")
        }
    }
}
```
CI/CD 部署時可透過 Gradle 參數或環境變數覆蓋此值。

### 5. 開發與測試
*   在 Android Studio 中開啟您的專案 (`/native/Android` 目錄)。
*   點擊 **Device Manager** 建立一個虛擬設備 (Virtual Device，建議選擇 Pixel 7)。
*   點擊綠色三角形 **Run (Shift+F10)** 即可在模擬器上預覽 Liquid Glass UI。

### 6. 打包與上架 (Google Play)
1.  **產生簽名金鑰 (Keystore)**：
    在 Android Studio 中，選擇 `Build` -> `Generate Signed Bundle / APK` -> 選擇 `Android App Bundle` -> 點擊 `Create new...` 建立金鑰。
2.  **打包 AAB 檔案**：
    完成設定後，Android Studio 會產出 `.aab` (Android App Bundle) 檔案。
3.  **上架 Google Play Console**：
    登入 [Google Play Console](https://play.google.com/console/)，建立新應用程式，並將產出的 `.aab` 檔案上傳至「正式版 (Production)」或「內部測試 (Internal Testing)」軌道。

---

## 參、iOS 開發環境建置 (SwiftUI)

### 1. 安裝 Xcode
*   請前往 Mac 上的 **App Store** 搜尋並下載 **Xcode** (檔案較大，需耐心等待)。
*   安裝完成後，開啟 Xcode 並同意授權條款。
*   安裝 Command Line Tools：
    ```bash
    xcode-select --install
    ```

### 2. 安裝 CocoaPods (若專案有使用第三方套件)
雖然我們目前主要使用原生 SwiftUI，但若未來需要整合 Firebase 等套件，會需要 CocoaPods：
```bash
brew install cocoapods
```

### 3. 設定 API URL (Config.plist)
本專案的 API 基底 URL 從 `Config.plist` 讀取（透過 `Configuration.swift`），不硬寫在程式碼中。
1. 在 Xcode 專案目錄建立 `Config.plist`（選 Property List 格式）。
2. 加入一個 String 型別的 key：`API_BASE_URL`，填入您的後端服務 URL。
3. **重要**：將 `Config.plist` 加入 `.gitignore`，避免 URL 被 commit 至版本控制。
4. Debug 模式下若 `Config.plist` 未設定，`Configuration.swift` 會觸發 `assertionFailure` 立即提醒開發者。

### 4. 開發與測試
*   使用 Xcode 開啟您的專案 (`/native/iOS/ProduceApp.xcodeproj` 或 `.xcworkspace`)。
*   在左上角選擇目標模擬器 (例如 iPhone 15 Pro)。
*   點擊左上角的 **Play 按鈕 (Cmd + R)** 即可在 iOS 模擬器中編譯並執行 App，檢視 iOS 26 Liquid Glass 效果。

### 5. 打包與上架 (App Store)
1.  **Apple Developer 帳號**：
    您需要付費註冊 [Apple Developer Program](https://developer.apple.com/programs/) (每年 $99 USD)。
2.  **設定 Signing & Capabilities**：
    在 Xcode 專案設定中，選擇您的 Target，進入 `Signing & Capabilities`，登入您的 Apple ID，並設定唯一的 `Bundle Identifier` (例如 `com.yourname.produceapp`)。
3.  **打包 Archive**：
    *   將左上角的目標設備從模擬器改為 **Any iOS Device (arm64)**。
    *   點擊頂部選單 `Product` -> `Archive`。
4.  **上傳至 App Store Connect**：
    *   Archive 完成後會跳出 Organizer 視窗。
    *   點擊 **Distribute App**，選擇 `App Store Connect`，按照步驟上傳。
5.  **TestFlight 與正式上架**：
    上傳成功後，登入 [App Store Connect](https://appstoreconnect.apple.com/)，您可以在 TestFlight 發送測試邀請給內部人員，確認無誤後即可送審 (Submit for Review) 正式上架。

---

## 肆、後端開發環境建置 (ASP.NET Core 8)

### 1. 安裝 .NET 8 SDK
```bash
brew install --cask dotnet-sdk
```
安裝完成後確認版本：
```bash
dotnet --version
```

### 2. 設定 JWT Secret 金鑰（重要安全事項）

本專案採用 JWT Bearer Token 進行身份驗證，金鑰**絕對不可**硬寫在 `appsettings.json` 中。

**本地開發設定**：
1. 在後端目錄建立 `appsettings.Development.json`（已加入 `.gitignore`）。
2. 填入開發專用金鑰（長度至少 32 字元）：
   ```json
   {
     "Jwt": {
       "SecretKey": "YourDevelopment-Only-Key-NotForProduction-Min32Chars!"
     }
   }
   ```
3. 確認 `appsettings.json` 的 `Jwt.SecretKey` 為空字串：
   ```json
   {
     "Jwt": {
       "SecretKey": ""
     }
   }
   ```

**正式環境設定**：
透過 OS 環境變數注入（ASP.NET Core 雙底線 `__` 為巢狀設定分隔符）：
```bash
# Linux / macOS
export Jwt__SecretKey="YourSuperSecretProductionKey_AtLeast32Chars!"

# Windows PowerShell
$env:Jwt__SecretKey = "YourSuperSecretProductionKey_AtLeast32Chars!"

# Docker / Kubernetes
# 在 docker-compose.yml 或 k8s Secret 中設定
```

**`Program.cs` 啟動驗證**：若 `Jwt__SecretKey` 為空，服務啟動時會立即拋出 `InvalidOperationException` 並顯示設定指引，防止以不安全的金鑰運行。

### 3. 本地執行後端
```bash
cd native/Backend
dotnet run
```
後端預設監聽 `http://localhost:5000`，API 文件可在 `http://localhost:5000/swagger` 查看。

### 4. Redis 快取（選用）
天氣警報等 API 支援 Redis 快取。本地開發可使用 Docker：
```bash
docker run -d -p 6379:6379 redis:alpine
```
正式環境請透過環境變數設定 Redis 連線字串，不要硬寫在 `appsettings.json` 中：
```bash
export ConnectionStrings__Redis="your-redis-server:6379,password=yourpassword"
```

---

## 伍、總結與最佳實踐

*   **版本控制**：強烈建議使用 Git 進行版本控制，並將程式碼推播至 GitHub 或 GitLab。
*   **UI 一致性**：目前雙平台皆已實作 Liquid Glass (毛玻璃) 風格。在修改 UI 時，請確保 Android (`liquidGlass` Modifier) 與 iOS (`.ultraThinMaterial`) 的視覺效果保持一致。
*   **機密資訊管理**（API 金鑰、JWT Secret）：
    *   **Android**：使用 `build.gradle` 的 `buildConfigField` 搭配 `local.properties` 或 CI/CD 環境變數。
    *   **iOS**：使用 `Config.plist`（不 commit）或 `.xcconfig` 管理 API URL；敏感金鑰使用 Keychain 儲存。
    *   **Backend**：所有金鑰透過 OS 環境變數或 Kubernetes Secret 注入，**絕對不要**將任何金鑰或密碼 commit 至 `appsettings.json`。
*   **無障礙設計**：Android Canvas 圖表記得加入 `semantics { contentDescription }` 語意標籤；iOS 確保重要元件有適當的 `.accessibilityLabel`。
*   **FCM Deep Link**：Android 收到推播點擊後，在 `MainActivity.kt` 的 `onCreate` 讀取 `intent.getStringExtra("produceId")` 以跳轉至對應農產品詳情頁。
