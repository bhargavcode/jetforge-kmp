import SwiftUI
import JetForge

/// Drop a published JetForge screen into a SwiftUI hierarchy the same way you would
/// embed a UIKit / Compose view. Pass the published endpoint (id or full URL).
struct JetForgeScreenView: UIViewControllerRepresentable {
    var endpoint: String
    var baseUrl: String = "http://127.0.0.1:43145"

    func makeUIViewController(context: Context) -> UIViewController {
        JetForgeViewControllerKt.JetForgeViewController(endpoint: endpoint, baseUrl: baseUrl)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
