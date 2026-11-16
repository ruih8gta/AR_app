# Android Studioから実機で試す

## 初回の準備

1. スマホの「開発者向けオプション」で「USBデバッグ」を有効にする。表示名や有効化方法は機種により異なる。
2. データ通信対応のUSBケーブルでPCにつなぎ、スマホのロックを解除する。
3. スマホに「USBデバッグを許可しますか」が出たら、このPCを確認して許可する。
4. Android StudioのOpenでプロジェクト全体を開き、Gradle同期を待つ。同期は構成と必要部品を読み込む処理。
5. JDKエラー時はSettings > Build, Execution, Deployment > Build Tools > GradleのGradle JDKをJava 25にする。SDKはSDK Managerから既存の場所を指定する。このPCの標準SDKは `%USERPROFILE%\AppData\Local\Android\Sdk`。

## ビルドして起動

1. 上部の実行構成で `app` を選ぶ。
2. その横の端末選択でUSB接続したスマホを選ぶ。
3. 緑色の **▶ Run** を押す。ビルド→インストール→起動が順に実行される。
4. 「てのひらサファリ」と恐竜の3Dプレビューが見えることを確認する。起動だけではカメラ権限を要求しない。
5. キツネへの切り替え、足ぶみ、停止、向き・大きさ変更を試す。
6. 「ARをはじめる」から説明を読み、必要なARサービス導入とカメラ許可を行う。
7. 明るい場所で、模様のある床や机にスマホをゆっくり向ける。リング付近の面をタップして1体を置く。
8. スマホを少し左右に動かし、モデルが同じ現実の場所に見えることを確認する。
9. 「置き直す」で古いモデルが消えて再配置できることを確認する。
10. ホームに戻って復帰し、カメラとモデルが再開することを確認する。画面回転時は配置を破棄してプレビューに戻る仕様。

ARには対応機種が必要。[Google公式対応表](https://developers.google.com/ar/devices)で機種・OS条件を確認する。機種名は現在未確認。非対応機種でも3Dプレビューは利用できる。

同じID `com.ruih8gta.palmsafari` のアプリを再実行すると通常は更新インストールになる。別の新規アプリは必ず別IDで作る。端末内データ削除やアンインストールは通常の確認には不要。

## 自動チェック

Android Studio下部のTerminal（PowerShell）で次を実行する。

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev.ps1 check
```

APK作成・PC単体テスト・Lint（コードの問題を探す検査）をまとめて実行する。単体テストはアニメーション停止・復帰、大きさ制限、脚の高さ、メッシュの正常性を確認する。カメラやAR位置精度を検証するものではない。

StudioのGradleウィンドウから実行する場合はExecute Gradle Taskに `:app:testDebugUnitTest` と `:app:lintDebug` をそれぞれ入力する。普段の▶ Runだけではこの2つをまとめて実行しない。

実機での表示自動テストは `app/src/androidTest` の `StartupTest.kt` を開き、クラス横の緑の▶から実行する。USB端末を選ぶ。テストAPKは別途インストールされるが、アプリデータの削除処理は含まない。

## コマンドでの代替操作

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev.ps1 devices
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev.ps1 install
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev.ps1 device-test
```

- `devices`：ADB（PCとAndroidをつなぐ開発用ツール）で接続確認。
- `install`：ビルドとインストール。起動はスマホのアイコンを押す。
- `device-test`：実機で自動テスト。手動の画面確認は別途行う。
- `unauthorized`：スマホを解除してUSBデバッグ許可を確認。
- `offline` や一覧が空：ケーブル、USB端子、USBデバッグ設定を確認。Windowsの機種別USBドライバーが必要な場合もある。

エミュレーターやシステムイメージの導入は不要。複数端末がつながっている場合、コマンドは複数端末に作用する場合があるので対象1台に絞るかStudioで選択する。

## 記録

実機で追加確認する項目：カメラ権限の拒否と再許可、暗い場所や無地の面での案内、追跡喪失・復帰、ARサービスのインストール拒否、5分程度の連続利用での発熱・カクつき。実機テストAPKの自動テストはプレビューUIのみで、AR実空間の確認は手動で行う。

PCビルド・Lint・実行したテスト件数と、実機の機種・OS・確認日・操作結果は分けて記録する。ひな形のビルド検証は新しいアプリの実機確認を保証しない。
