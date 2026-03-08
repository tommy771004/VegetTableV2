import SwiftUI

/// 骨架屏 Shimmer Sweep 動畫
struct SkeletonView: View {
    @State private var shimmerPhase: CGFloat = -1.0

    var body: some View {
        RoundedRectangle(cornerRadius: 12)
            .fill(Color.gray.opacity(0.15))
            .frame(height: 56)
            .overlay(
                GeometryReader { geometry in
                    LinearGradient(
                        colors: [
                            .clear,
                            .white.opacity(0.5),
                            .clear
                        ],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                    .frame(width: geometry.size.width * 0.6)
                    .offset(x: geometry.size.width * shimmerPhase)
                }
                .mask(RoundedRectangle(cornerRadius: 12))
            )
            .onAppear {
                withAnimation(
                    .linear(duration: 1.2)
                    .repeatForever(autoreverses: false)
                ) {
                    shimmerPhase = 1.3
                }
            }
    }
}
