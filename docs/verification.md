# 検証記録

確認日：2026-10-30（日本時間）。この記録はPC上の確認と未実施の実機確認を分ける。

## 完了した確認

| 対象 | 結果 | 証拠 |
|---|---|---|
| AndroidデバッグAPK | ビルド成功。11,748,990 bytes（約11.2 MiB） | `app/build/outputs/apk/debug/app-debug.apk` |
| PC単体テスト | 5件成功、失敗0、スキップ0 | `app/build/test-results/testDebugUnitTest/TEST-com.ruih8gta.palmsafari.SceneTest.xml` |
| Lint | エラー0、警告11 | `app/build/reports/lint-results-debug.html` |
| 実機用UIテストAPK | コンパイル成功。テスト実行は未実施 | `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` |
| ブラウザ3Dデモ | 9項目成功 | `demo/verification.json` |
| デモの目視 | 恐竜、キツネ、320px幅、ダーク表示のスクリーンショットを確認 | `demo/*-screenshot.png`（ローカル確認用） |

実行コマンド：

```powershell
.\scripts\dev.ps1 check
.\scripts\dev.ps1 test-build -Offline
node scripts/check-demo.cjs
```

PC単体テストは、不正なサイズ・角度の補正、停止時の時刻・ポーズ保持、バックグラウンド復帰時の時刻、脚が元の高さを下回らないこと、メッシュの有限値・単位法線を確認した。

デモは3D読込、モデル変更、アニメーションによる実際の描画変化、停止中の描画固定、拡縮、回転、320pxでの横はみ出しなし、ダーク表示、ブラウザエラーなしを確認した。Three.jsのCDN読込を含む。検証ブラウザは既存Chromium、WebGLはソフトウェア描画であり、Android端末のGPU性能を測ったものではない。

Lint警告は、ひな形の依存部品の更新案内6件、バックアップ設定の案内1件、未使用のひな形文字列1件、KTX記法2件、バージョンカタログ利用案内1件。ビルドを妨げるエラーではない。指定ひな形と整合するバージョンを維持した。

## 未実施

- 実機の機種名・OS・ARCore対応確認（ユーザーが後で確認）。
- 端末へのインストール・起動、実機UI自動テスト。
- 実際のカメラ映像、平面検出、アンカーの位置安定性、回転・休止復帰、権限拒否、ARサービス導入の実機操作。
- 連続利用時のフレームレート・発熱・電池消費。

APKのビルド成功は実機ARの動作確認ではない。次は `docs/device-testing.md` に沿ってUSB実機で確認する。

## 環境上の補足

通常の制限環境ではGradleキャッシュへの書込みと検証用ブラウザの起動が制限されたため、許可された実行で確認した。デモ確認はローカルファイルで完了。検証用に起動した一時HTTPサーバーは停止済み。GitHubへの送信・公開や端末へのインストールは行っていない。
