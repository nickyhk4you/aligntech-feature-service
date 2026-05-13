// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "AlignTechFeatureSDK",
    platforms: [
        .iOS(.v15),
        .macOS(.v12),
        .tvOS(.v15),
        .watchOS(.v8)
    ],
    products: [
        .library(
            name: "AlignTechFeatureSDK",
            targets: ["AlignTechFeatureSDK"]),
    ],
    targets: [
        .target(
            name: "AlignTechFeatureSDK",
            dependencies: []),
        .testTarget(
            name: "AlignTechFeatureSDKTests",
            dependencies: ["AlignTechFeatureSDK"]),
    ]
)
