# Porting Notes

この文書は、既存プロジェクトからPGPへ移植・再実装した技術の由来と判断を記録する。

## 参照リポジトリ

| 参照元 | 開発時の位置 | 用途 | 扱い |
|---|---|---|---|
| Pokecom GO | `../pokecom` | CPU、機種定義、LCD、キー、BASIC処理 | 読み取り専用 |
| pcwav | `../pcwav` | WAV Codec、転送プロトコル | 読み取り専用 |

PGPはこれらのリポジトリへビルド時または実行時に依存しない。

## 記録ルール

移植単位ごとに以下を記録する。

- PGP側の機能・クラス名
- 参照元リポジトリとコミットID
- 参照したファイルと範囲
- 流用した仕様またはアルゴリズム
- PGP向けに変更した設計
- 動作一致を確認するテスト
- ライセンス・由来の確認結果

## 記録テンプレート

```markdown
### 機能名

- Date:
- PGP files:
- Reference repository:
- Reference commit:
- Reference files:
- Provenance/license:
- Reused behavior:
- Design changes:
- Verification:
```

## 移植記録

まだ移植は開始していない。
