# PC-1245解析用の最小入力

ファームウェアを含まない自作の検証データ。
`loop.dmp`は既存の[Debugger fixture](../../assembly/pc1245-debugger-acceptance/src/debugger.asm)の
入口ループとサブルーチンの18 byteだけを抜き出したもの。CASE・データ初期値は含まない。

- `call-external.bas`：A01の静的解析用。CALL先`0xC500`の実装は含めないため、実行検証には使わない。
- `loop.dmp`：A02の最小機械語用。入口`0xC000`、観測先`0xC100`。正常終了しない。
  実行する場合は既存データのない検証用PC-1245 Sessionで、入口Breakpointと停止中のStepを使う。

解析公開はLoadと別操作。DMPを公開してもエミュレーターへLoadされない。
実行手順と観測根拠は[解析ガイド](../../../docs/guides/AI_PROGRAM_ANALYSIS.md)、
[検証記録](../../../docs/development/AI_PROGRAM_ANALYSIS_VERIFICATION.md)を参照。
