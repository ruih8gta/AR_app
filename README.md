# てのひらサファリ

恐竜・キツネの3Dプレビューと、カメラ映像の床や机に1体を置くAndroid ARアプリの初版です。名前は仮称。アプリIDは `com.ruih8gta.palmsafari`。隣の `android-compose-starter` を基に構築しています。

## 最初にすること

1. Android Studioでこのフォルダー全体を開く（`app`だけではありません）。
2. Gradle同期を待つ。必要ならGradle JDKをJava 25、SDKの場所を設定する。
3. USB接続したスマホを選び、▶ Runでビルド・インストール・起動する。

詳しい操作は [実機テスト手順](docs/device-testing.md)。最初はカメラを使わない3Dプレビューです。「ARをはじめる」から対応確認・カメラ許可後、床や机に配置できます。AR動作の確認には対応実機が必要です。

## ファイルの場所

| 場所 | 役割 |
|---|---|
| app/src/main/java | Kotlinコード。MainActivity.ktが入口 |
| app/src/main/res | 表示名・文字・アイコン・テーマ |
| app/src/test/java | PC上の単体テスト。業務ロジックを追加するときに作る |
| app/src/androidTest/java | 実機での起動表示テスト |
| docs | 要件・設計・テスト手順 |
| scripts/dev.ps1 | 開発用コマンド |
| gradle | 部品とビルドのバージョン設定 |
| AGENTS.md | 開発ルール |

## PCでの確認

このフォルダーでPowerShellを開いて実行します。

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev.ps1 doctor
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev.ps1 check
```

`doctor` はJava・ADB・Gitと空き容量を確認します。`check` は `assembleDebug testDebugUnitTest lintDebug` を実行します。検証結果と実機で残る確認は [検証記録](docs/verification.md) にまとめます。

必要な部品が取得済みの場合に限り、末尾に `-Offline` を付けられます。不足部品のエラーが出たらオンラインで再実行してください。

APK: `app/build/outputs/apk/debug/app-debug.apk`。デバッグ用であり、ストア公開用の署名設定は含みません。

## 開発の進め方

[要件](docs/requirements.md) → 3Dデモ → [設計](docs/design.md) → 実装 → PC確認 → USB実機確認 → 修正の順で進めます。モデルはオリジナルの簡易立体で、高精細モデル・撮影・自由な散歩は今後の候補です。

## 3Dデモとモデル

`demo/palm-safari.html` はチャットで表示するHTML断片です。`demo/palm-safari.template.html` が画面の正本で、`python scripts/create-models.py` がAndroid用モデルとデモ内のモデルを同時に生成します。表示にはCDN上のThree.jsが必要です。カメラやAR平面認識はデモでは使用しません。

デモの再検証：`scripts/check-demo.cjs`。必要なPlaywrightライブラリは `.tools/` に分離し、既存のChromiumを使います。`CHROMIUM_PATH` で実行ファイルを指定できます。検証用 `demo/preview.html` を作成してから `node scripts/check-demo.cjs` を実行します。ファイルをローカルに読み込み、外部に公開しません。

Android Studio・SDK・Java・ADBはPC内の環境を共用します。Gradleは9.6.0、AGPは9.4.1、Kotlin Compose Pluginは2.2.10、Compose BOMは2026.02.01、SDKは37、最小Androidは7.0です。Java 25でのビルドを前提とします。別PCでは初回セットアップと不足部品の取得が必要です。

## GitHubへの保存と復元

保存先は公開リポジトリ [ruih8gta/AR_app](https://github.com/ruih8gta/AR_app)、ブランチは `main` です。このリポジトリには「てのひらサファリ」のコード・3Dモデル・デモ・ドキュメントを保存します。別フォルダーの家具アプリとパズルアプリは含みません。

復元するときはAndroid Studioの「Get from VCS / Clone Repository」に上記URLを指定するか、保存したい親フォルダーで次を実行します。

```powershell
git clone https://github.com/ruih8gta/AR_app.git
```

復元後は、このREADMEの「最初にすること」に沿ってAndroid Studioで開きます。変更をGitHubへ送信するときは、書込み権限を持つGitHubアカウントでの認証が必要です。

`local.properties`、SDK本体、署名鍵、端末データ、IDE設定、ビルド生成物、`.tools/` の検証用依存ライブラリ、`work/` の一時検証ファイル、デモの検証用ページ・画像・実行結果はGitHubに保存しません。SDKの場所とIDE設定は復元先のPCで設定し、APKや検証結果は必要に応じて作り直します。コードをGitHubに保存してもスマホ内データのバックアップにはなりません。
