import UIKit
import FirebaseCore
import FirebaseMessaging
import UserNotifications

class AppDelegate: NSObject, UIApplicationDelegate, MessagingDelegate, UNUserNotificationCenterDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self

        // 請求推播權限
        UNUserNotificationCenter.current().requestAuthorization(
            options: [.alert, .badge, .sound]
        ) { granted, _ in
            if granted {
                DispatchQueue.main.async {
                    application.registerForRemoteNotifications()
                }
            }
        }

        return true
    }

    // MARK: - FCM Token

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken else { return }
        print("FCM Token: \(token)")

        if let jwt = UserDefaults.standard.string(forKey: "jwt_token") {
            sendFcmTokenToBackend(token, jwt: jwt)
        } else {
            // 冷啟動時 JWT 可能尚未取得，異步等候
            Task {
                await ProduceService.shared.ensureJwtToken()
                if let jwt = UserDefaults.standard.string(forKey: "jwt_token") {
                    sendFcmTokenToBackend(token, jwt: jwt)
                }
            }
        }
    }

    private func sendFcmTokenToBackend(_ fcmToken: String, jwt: String) {
        guard let url = URL(string: "\(Configuration.apiBaseUrl)fcm/token") else { return }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(jwt)", forHTTPHeaderField: "Authorization")
        request.httpBody = try? JSONEncoder().encode(["token": fcmToken])

        URLSession.shared.dataTask(with: request).resume()
    }

    // MARK: - Remote Notifications

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        Messaging.messaging().apnsToken = deviceToken
    }

    // 前景收到通知時顯示
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .badge, .sound])
    }

    // 點擊通知
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        if let produceId = userInfo["produceId"] as? String {
            // Deep Link 到對應農產品頁面
            NotificationCenter.default.post(
                name: .init("DeepLinkToProduce"),
                object: nil,
                userInfo: ["produceId": produceId]
            )
        }
        completionHandler()
    }
}
